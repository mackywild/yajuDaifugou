import Foundation
import Combine

@MainActor
enum DaifugoScreen {
    case login
    case lobby
    case room
    case game
    case result
}

@MainActor
final class DaifugoViewModel: ObservableObject {
    @Published var screen: DaifugoScreen = .login
    @Published var serverURL: String
    @Published var password = ""
    @Published var playerName: String
    @Published var roomIdInput = ""
    @Published var roomId: String?
    @Published var gameState: GameStateDTO?
    @Published var selectedCardIndices: Set<Int> = []
    @Published var loading = false
    @Published var socketConnected = false
    @Published var errorMessage: String?
    @Published var infoMessage: String?

    private let api = DaifugoAPIClient()
    private let audio = YajuAudioPlayer()
    private let defaults = UserDefaults.standard

    private var lastHandledEventId: Int64 = 0
    private var pollingTask: Task<Void, Never>?
    private var reconnectTask: Task<Void, Never>?

    init() {
        serverURL = defaults.string(forKey: "serverURL") ?? "http://192.168.1.10:8080"
        playerName = defaults.string(forKey: "playerName") ?? ""
    }

    func login() { action { try await self.loginImpl() } }
    func logout() { action { try await self.logoutImpl() } }
    func createRoom() { action { try await self.createRoomImpl() } }
    func joinRoom() { action { try await self.joinRoomImpl() } }
    func startGame() { action { try await self.startGameImpl() } }
    func playSelected() { action { try await self.playSelectedImpl() } }
    func pass() { action { try await self.passImpl() } }
    func leaveRoom() { action { try await self.leaveRoomImpl() } }

    func clearMessage() {
        errorMessage = nil
        infoMessage = nil
    }

    func refreshState(silent: Bool = true) {
        guard let roomId else { return }
        Task {
            if !silent { loading = true }
            do {
                applyGameState(try await api.state(roomId: roomId))
            } catch {
                handle(error)
            }
            if !silent { loading = false }
        }
    }

    func toggleCard(index: Int) {
        guard let gameState,
              let selfPlayer = gameState.selfPlayer,
              gameState.isMyTurn,
              selfPlayer.hand.indices.contains(index) else { return }

        if selectedCardIndices.contains(index) {
            selectedCardIndices.remove(index)
        } else {
            selectedCardIndices.insert(index)
        }
    }

    private func loginImpl() async throws {
        let normalized = normalizeServerURL(serverURL)
        guard !password.isEmpty else { throw APIError(message: "パスワードを入力してください", statusCode: nil) }
        try api.configure(serverURL: normalized)
        let result = try await api.login(password: password)
        guard result.authenticated else { throw APIError(message: "ログインに失敗しました", statusCode: nil) }
        defaults.set(normalized, forKey: "serverURL")
        serverURL = normalized
        password = ""
        screen = .lobby
        infoMessage = "サーバーへ接続しました"
    }

    private func logoutImpl() async throws {
        try await api.logout()
        stopRealtime()
        lastHandledEventId = 0
        roomId = nil
        gameState = nil
        selectedCardIndices.removeAll()
        socketConnected = false
        screen = .login
        infoMessage = "ログアウトしました"
    }

    private func createRoomImpl() async throws {
        let name = try validatedPlayerName()
        let response = try await api.createRoom(playerName: name)
        savePlayerName(name)
        try await enterRoom(response.roomId)
    }

    private func joinRoomImpl() async throws {
        let name = try validatedPlayerName()
        let targetRoom = roomIdInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !targetRoom.isEmpty else { throw APIError(message: "ルームIDを入力してください", statusCode: nil) }
        let response = try await api.joinRoom(roomId: targetRoom, playerName: name)
        savePlayerName(name)
        try await enterRoom(response.roomId)
    }

    private func startGameImpl() async throws {
        let roomId = try requireRoomId()
        applyGameState(try await api.start(roomId: roomId))
    }

    private func playSelectedImpl() async throws {
        let roomId = try requireRoomId()
        guard let gameState, let selfPlayer = gameState.selfPlayer else {
            throw APIError(message: "ゲーム状態を取得できません", statusCode: nil)
        }
        let cards = selectedCardIndices.sorted().compactMap { index in
            selfPlayer.hand.indices.contains(index) ? selfPlayer.hand[index] : nil
        }
        guard !cards.isEmpty else { throw APIError(message: "カードを選択してください", statusCode: nil) }

        let next: GameStateDTO
        if gameState.isMySevenTransfer {
            guard cards.count == gameState.sevenTransfer.cardCount else {
                throw APIError(
                    message: "7渡しでは\(gameState.sevenTransfer.cardCount)枚選択してください",
                    statusCode: nil
                )
            }
            next = try await api.sevenTransfer(roomId: roomId, cards: cards)
        } else {
            next = try await api.play(roomId: roomId, cards: cards)
        }
        selectedCardIndices.removeAll()
        applyGameState(next)
    }

    private func passImpl() async throws {
        let roomId = try requireRoomId()
        selectedCardIndices.removeAll()
        applyGameState(try await api.pass(roomId: roomId))
    }

    private func leaveRoomImpl() async throws {
        let roomId = try requireRoomId()
        try await api.leave(roomId: roomId)
        stopRealtime()
        lastHandledEventId = 0
        self.roomId = nil
        roomIdInput = ""
        gameState = nil
        selectedCardIndices.removeAll()
        socketConnected = false
        screen = .lobby
        infoMessage = "部屋から退出しました"
    }

    private func enterRoom(_ roomId: String) async throws {
        lastHandledEventId = 0
        self.roomId = roomId
        roomIdInput = roomId
        screen = .room
        selectedCardIndices.removeAll()
        applyGameState(try await api.state(roomId: roomId))
        startRealtime(roomId: roomId)
    }

    private func startRealtime(roomId: String) {
        stopRealtime()
        connectSocket(roomId: roomId)

        pollingTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 5_000_000_000)
                guard let self, self.roomId == roomId else { return }
                if let state = try? await self.api.state(roomId: roomId) {
                    await MainActor.run { self.applyGameState(state) }
                }
            }
        }
    }

    private func connectSocket(roomId: String) {
        api.connectRoomSocket(
            roomId: roomId,
            onStateChanged: { [weak self] in
                Task {
                    guard let self, self.roomId == roomId else { return }
                    if let state = try? await self.api.state(roomId: roomId) {
                        await MainActor.run { self.applyGameState(state) }
                    }
                }
            },
            onConnectionChanged: { [weak self] connected in
                Task { @MainActor in
                    guard let self else { return }
                    self.socketConnected = connected
                    if !connected && self.roomId == roomId {
                        self.reconnectTask?.cancel()
                        self.reconnectTask = Task { [weak self] in
                            try? await Task.sleep(nanoseconds: 3_000_000_000)
                            guard let self, !Task.isCancelled, self.roomId == roomId else { return }
                            await MainActor.run { self.connectSocket(roomId: roomId) }
                        }
                    }
                }
            }
        )
    }

    private func stopRealtime() {
        pollingTask?.cancel()
        pollingTask = nil
        reconnectTask?.cancel()
        reconnectTask = nil
        api.closeRoomSocket()
    }

    private func applyGameState(_ next: GameStateDTO) {
        processGameEvents(next)
        let previous = gameState
        let turnChanged = previous?.currentPlayerId != next.currentPlayerId
        let handChanged = previous?.selfPlayer?.hand != next.selfPlayer?.hand
        if turnChanged || handChanged { selectedCardIndices.removeAll() }

        gameState = next
        roomId = next.roomId
        if next.finished { screen = .result }
        else if next.started { screen = .game }
        else { screen = .room }
        errorMessage = nil
    }

    private func processGameEvents(_ state: GameStateDTO) {
        let unseen = state.events.filter { $0.id > lastHandledEventId }.sorted { $0.id < $1.id }
        for event in unseen {
            switch event.type {
            case "YAJU_AVAILABLE": audio.play(.yajuAvailable)
            case "YAJU_SUCCESS": audio.play(.yajuSuccess)
            default: break
            }
            lastHandledEventId = max(lastHandledEventId, event.id)
        }
    }

    private func validatedPlayerName() throws -> String {
        let value = playerName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else { throw APIError(message: "プレイヤー名を入力してください", statusCode: nil) }
        guard value.count <= 20 else { throw APIError(message: "プレイヤー名は20文字以内にしてください", statusCode: nil) }
        return value
    }

    private func savePlayerName(_ value: String) {
        playerName = value
        defaults.set(value, forKey: "playerName")
    }

    private func requireRoomId() throws -> String {
        guard let roomId else { throw APIError(message: "部屋に参加していません", statusCode: nil) }
        return roomId
    }

    private func normalizeServerURL(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    private func action(_ body: @escaping @MainActor () async throws -> Void) {
        Task { @MainActor in
            loading = true
            defer { loading = false }
            do {
                try await body()
            } catch {
                handle(error)
            }
        }
    }

    private func handle(_ error: Error) {
        errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
    }
}
