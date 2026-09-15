import Foundation
import Combine

@MainActor
enum DaifugoScreen {
    case login
    case mainMenu
    case multiplayer
    case cpuSetup
    case room
    case game
    case result
}

enum RuleCutInKind: Equatable {
    case jackBack
    case earlyShot
}

struct RuleCutInNotice: Identifiable {
    let id: Int64
    let kind: RuleCutInKind
    let playerName: String
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
    @Published var ruleCutIns: [RuleCutInNotice] = []

    // CPU戦【ひとりでイク】設定
    @Published var cpuCount = 3
    @Published var cpuDifficulty = "NORMAL"
    @Published var jokerCount = 1
    @Published var ruleRevolution = true
    @Published var ruleEightCut = true
    @Published var ruleMarkLock = true
    @Published var ruleSevenTransfer = true
    @Published var ruleYaju = true
    @Published var ruleJackBack = true
    @Published var ruleForbiddenFinish = true

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
    func startCpuGame() { action { try await self.startCpuGameImpl() } }
    func startGame() { action { try await self.startGameImpl() } }
    func playSelected() { action { try await self.playSelectedImpl() } }
    func pass() { action { try await self.passImpl() } }
    func leaveRoom() { action { try await self.leaveRoomImpl() } }

    func dismissRuleCutIn(_ id: Int64) {
        ruleCutIns.removeAll { $0.id == id }
    }

    /// メインメニューからマルチプレイへ遷移する。
    func openMultiplayer() {
        clearMessage()
        screen = .multiplayer
    }

    /// メインメニューからCPU戦【ひとりでイク】設定へ遷移する。
    func openCpuSetup() {
        clearMessage()
        screen = .cpuSetup
    }

    /// サブメニューからメインメニューへ戻る。
    func backToMenu() {
        clearMessage()
        screen = .mainMenu
    }

    /// 野獣ルールをONにした場合、成立に必要な8切りも強制ONにする。
    func setYajuRule(_ enabled: Bool) {
        ruleYaju = enabled
        if enabled { ruleEightCut = true }
    }

    /// 野獣ルール中は8切りをOFFにできない。
    func setEightCut(_ enabled: Bool) {
        if ruleYaju && !enabled {
            ruleEightCut = true
            infoMessage = "野獣ルールを使う場合、8切りは必須です"
        } else {
            ruleEightCut = enabled
        }
    }

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
        screen = .mainMenu
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

    private func startCpuGameImpl() async throws {
        let name = try validatedPlayerName()
        guard (1...3).contains(cpuCount) else {
            throw APIError(message: "CPU人数は1〜3人で指定してください", statusCode: nil)
        }
        guard (0...2).contains(jokerCount) else {
            throw APIError(message: "ジョーカー枚数は0〜2枚で指定してください", statusCode: nil)
        }
        if ruleYaju && !ruleEightCut {
            throw APIError(message: "野獣ルールを使う場合は8切りを有効にしてください", statusCode: nil)
        }

        let request = CpuGameRequest(
            playerName: name,
            cpuCount: cpuCount,
            difficulty: cpuDifficulty,
            jokerCount: jokerCount,
            revolution: ruleRevolution,
            eightCut: ruleEightCut,
            markLock: ruleMarkLock,
            sevenTransfer: ruleSevenTransfer,
            yajuRule: ruleYaju,
            jackBack: ruleJackBack,
            forbiddenFinish: ruleForbiddenFinish
        )
        let state = try await api.createCpuGame(request: request)
        savePlayerName(name)
        lastHandledEventId = 0
        roomId = state.roomId
        roomIdInput = state.roomId
        selectedCardIndices.removeAll()
        applyGameState(state)
        startRealtime(roomId: state.roomId)
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
        screen = .mainMenu
        infoMessage = "対戦を終了しました"
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
            case "YAJU_AVAILABLE":
                audio.play(.yajuAvailable)
            case "YAJU_SUCCESS":
                audio.play(.yajuSuccess)
            case "JACK_BACK":
                ruleCutIns.append(RuleCutInNotice(id: event.id, kind: .jackBack, playerName: event.playerName))
            case "EARLY_SHOT":
                ruleCutIns.append(RuleCutInNotice(id: event.id, kind: .earlyShot, playerName: event.playerName))
            default:
                break
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
