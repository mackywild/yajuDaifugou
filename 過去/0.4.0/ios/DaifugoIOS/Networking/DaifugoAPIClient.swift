import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

struct APIError: LocalizedError {
    let message: String
    let statusCode: Int?

    var errorDescription: String? { message }
}

/// Spring Boot版DaifugoのREST/WebSocketクライアント。
/// RESTをゲーム操作の正本とし、WebSocketは状態更新通知だけに使用する。
final class DaifugoAPIClient {
    private let session: URLSession
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder

    private(set) var baseURL: URL?
    private var csrfToken = ""
    private var socketTask: URLSessionWebSocketTask?
    private var socketReceiveTask: Task<Void, Never>?

    init() {
        let configuration = URLSessionConfiguration.default
        configuration.httpCookieStorage = .shared
        configuration.httpShouldSetCookies = true
        configuration.httpCookieAcceptPolicy = .always
        configuration.timeoutIntervalForRequest = 15
        configuration.timeoutIntervalForResource = 30

        session = URLSession(configuration: configuration)
        encoder = JSONEncoder()
        decoder = JSONDecoder()
    }

    func configure(serverURL: String) throws {
        let normalized = serverURL.trimmingCharacters(in: .whitespacesAndNewlines)
            .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard let url = URL(string: normalized), let scheme = url.scheme,
              scheme == "http" || scheme == "https" else {
            throw APIError(message: "サーバーURLを確認してください", statusCode: nil)
        }

        if baseURL?.absoluteString != url.absoluteString {
            closeRoomSocket()
            csrfToken = ""
            HTTPCookieStorage.shared.removeCookies(since: .distantPast)
        }
        baseURL = url
    }

    func login(password: String) async throws -> LoginResponse {
        let response: LoginResponse = try await perform(
            path: "/api/auth/login",
            method: "POST",
            body: LoginRequest(password: password),
            needsCSRF: false
        )
        csrfToken = response.csrfToken
        return response
    }

    func logout() async throws {
        let _: EmptyResponse = try await perform(
            path: "/api/auth/logout",
            method: "POST",
            body: EmptyBody()
        )
        closeRoomSocket()
        csrfToken = ""
        HTTPCookieStorage.shared.removeCookies(since: .distantPast)
    }

    func createRoom(playerName: String) async throws -> JoinResponse {
        try await perform(
            path: "/api/rooms",
            method: "POST",
            body: PlayerNameRequest(playerName: playerName)
        )
    }

    /// CPU戦【ひとりでイク】を作成し、開始済みのゲーム状態を受け取る。
    func createCpuGame(request: CpuGameRequest) async throws -> GameStateDTO {
        try await perform(
            path: "/api/rooms/cpu",
            method: "POST",
            body: request
        )
    }

    func joinRoom(roomId: String, playerName: String) async throws -> JoinResponse {
        try await perform(
            path: "/api/rooms/\(escapePath(roomId))/join",
            method: "POST",
            body: PlayerNameRequest(playerName: playerName)
        )
    }

    func state(roomId: String) async throws -> GameStateDTO {
        try await perform(path: "/api/rooms/\(escapePath(roomId))/state", method: "GET")
    }

    func start(roomId: String) async throws -> GameStateDTO {
        try await perform(
            path: "/api/rooms/\(escapePath(roomId))/start",
            method: "POST",
            body: EmptyBody()
        )
    }

    func play(roomId: String, cards: [CardDTO]) async throws -> GameStateDTO {
        try await perform(
            path: "/api/rooms/\(escapePath(roomId))/play",
            method: "POST",
            body: PlayRequest(cards: cards.map(CardRequest.init))
        )
    }

    func sevenTransfer(roomId: String, cards: [CardDTO]) async throws -> GameStateDTO {
        try await perform(
            path: "/api/rooms/\(escapePath(roomId))/seven-transfer",
            method: "POST",
            body: PlayRequest(cards: cards.map(CardRequest.init))
        )
    }

    func pass(roomId: String) async throws -> GameStateDTO {
        try await perform(
            path: "/api/rooms/\(escapePath(roomId))/pass",
            method: "POST",
            body: EmptyBody()
        )
    }

    func leave(roomId: String) async throws {
        let _: EmptyResponse = try await perform(
            path: "/api/rooms/\(escapePath(roomId))/leave",
            method: "POST",
            body: EmptyBody()
        )
        closeRoomSocket()
    }

    /// 同じHTTP Cookieセッションを使って通知専用WebSocketへ接続する。
    func connectRoomSocket(
        roomId: String,
        onStateChanged: @escaping @Sendable () -> Void,
        onConnectionChanged: @escaping @Sendable (Bool) -> Void
    ) {
        closeRoomSocket()
        guard let baseURL else { return }

        var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false)
        components?.scheme = baseURL.scheme == "https" ? "wss" : "ws"
        components?.path = "/ws/game"
        components?.queryItems = [URLQueryItem(name: "roomId", value: roomId)]

        guard let url = components?.url else {
            onConnectionChanged(false)
            return
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = 15
        let task = session.webSocketTask(with: request)
        socketTask = task
        task.resume()

        socketReceiveTask = Task { [weak self, weak task] in
            guard let self, let task else { return }
            do {
                while !Task.isCancelled {
                    let message = try await task.receive()
                    let text: String
                    switch message {
                    case .string(let value): text = value
                    case .data(let data): text = String(data: data, encoding: .utf8) ?? ""
                    @unknown default: text = ""
                    }

                    if text.contains("\"type\":\"CONNECTED\"") {
                        onConnectionChanged(true)
                    } else if text.contains("\"type\":\"STATE_CHANGED\"") {
                        onStateChanged()
                    }
                }
            } catch {
                if !Task.isCancelled {
                    onConnectionChanged(false)
                }
                self.socketTask = nil
            }
        }
    }

    func closeRoomSocket() {
        socketReceiveTask?.cancel()
        socketReceiveTask = nil
        socketTask?.cancel(with: .normalClosure, reason: nil)
        socketTask = nil
    }

    private func perform<Response: Decodable>(
        path: String,
        method: String,
        needsCSRF: Bool? = nil
    ) async throws -> Response {
        try await perform(path: path, method: method, bodyData: nil, needsCSRF: needsCSRF)
    }

    private func perform<Body: Encodable, Response: Decodable>(
        path: String,
        method: String,
        body: Body,
        needsCSRF: Bool? = nil
    ) async throws -> Response {
        try await perform(
            path: path,
            method: method,
            bodyData: try encoder.encode(body),
            needsCSRF: needsCSRF
        )
    }

    private func perform<Response: Decodable>(
        path: String,
        method: String,
        bodyData: Data?,
        needsCSRF: Bool?
    ) async throws -> Response {
        guard let baseURL else {
            throw APIError(message: "サーバーURLを設定してください", statusCode: nil)
        }
        guard let url = URL(string: path, relativeTo: baseURL)?.absoluteURL else {
            throw APIError(message: "通信先URLを作成できません", statusCode: nil)
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData

        let shouldSendCSRF = needsCSRF ?? (method != "GET")
        if shouldSendCSRF {
            guard !csrfToken.isEmpty else {
                throw APIError(message: "認証セッションがありません。再ログインしてください", statusCode: nil)
            }
            request.setValue(csrfToken, forHTTPHeaderField: "X-CSRF-Token")
        }

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw APIError(message: "サーバー応答を確認できません", statusCode: nil)
        }
        guard 200..<300 ~= http.statusCode else {
            let message = (try? decoder.decode(MessageResponse.self, from: data).message)
                ?? "サーバーエラー (\(http.statusCode))"
            throw APIError(message: message, statusCode: http.statusCode)
        }

        if Response.self == EmptyResponse.self {
            return EmptyResponse() as! Response
        }
        return try decoder.decode(Response.self, from: data)
    }

    private func escapePath(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines)
            .addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? value
    }
}

private struct EmptyBody: Encodable {}
private struct EmptyResponse: Decodable {}
