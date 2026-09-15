import Foundation

/// サーバーが返すカード情報。
struct CardDTO: Codable, Hashable, Identifiable {
    let suit: String
    let rank: String
    let joker: Bool

    var id: String { "\(suit)-\(rank)-\(joker)" }

    var displaySuit: String {
        switch suit {
        case "SPADE": return "♠"
        case "HEART": return "♥"
        case "DIAMOND": return "♦"
        case "CLUB": return "♣"
        default: return "★"
        }
    }

    var displayRank: String {
        switch rank {
        case "THREE": return "3"
        case "FOUR": return "4"
        case "FIVE": return "5"
        case "SIX": return "6"
        case "SEVEN": return "7"
        case "EIGHT": return "8"
        case "NINE": return "9"
        case "TEN": return "10"
        case "JACK": return "J"
        case "QUEEN": return "Q"
        case "KING": return "K"
        case "ACE": return "A"
        case "TWO": return "2"
        case "JOKER": return "JOKER"
        default: return rank
        }
    }

    var label: String {
        joker || suit == "JOKER" ? "JOKER" : "\(displaySuit)\(displayRank)"
    }

    var isRed: Bool { suit == "HEART" || suit == "DIAMOND" }
}

/// カード提出API用の最小DTO。
struct CardRequest: Codable {
    let suit: String
    let rank: String

    init(_ card: CardDTO) {
        suit = card.suit
        rank = card.rank
    }
}

/// プレイヤー表示情報。CPU情報は全端末で共有される。
struct PlayerDTO: Codable, Identifiable {
    let playerId: String
    let playerName: String
    let handCount: Int
    let hand: [CardDTO]
    let passed: Bool
    let rank: Int?
    let isSelf: Bool
    let yajuStatus: String
    let cpu: Bool
    let cpuDifficulty: String?

    var id: String { playerId }
    var yajuActive: Bool { yajuStatus != "NONE" }

    enum CodingKeys: String, CodingKey {
        case playerId, playerName, handCount, hand, passed, rank, yajuStatus, cpu, cpuDifficulty
        case isSelf = "self"
    }
}

struct FieldDTO: Codable {
    let cards: [CardDTO]
    let combinationType: String?
}

struct SevenTransferDTO: Codable {
    let pending: Bool
    let sourcePlayerId: String?
    let targetPlayerId: String?
    let cardCount: Int
}

struct GameEventDTO: Codable, Identifiable {
    let id: Int64
    let type: String
    let playerId: String
    let playerName: String
}

/// CPU戦・マルチプレイ共通の特殊ルール設定。
struct RuleSettingsDTO: Codable {
    let jokerCount: Int
    let revolution: Bool
    let eightCut: Bool
    let markLock: Bool
    let sevenTransfer: Bool
    let yajuRule: Bool
    let forbiddenFinish: Bool
}

struct GameStateDTO: Codable {
    let roomId: String
    let gameMode: String
    let ruleSettings: RuleSettingsDTO
    let players: [PlayerDTO]
    let currentPlayerId: String?
    let field: FieldDTO
    let revolution: Bool
    let lockedMark: String?
    let host: Bool
    let started: Bool
    let finished: Bool
    let sevenTransfer: SevenTransferDTO
    let events: [GameEventDTO]

    var selfPlayer: PlayerDTO? { players.first(where: { $0.isSelf }) }
    var currentPlayer: PlayerDTO? { players.first(where: { $0.playerId == currentPlayerId }) }
    var isMyTurn: Bool {
        guard let myId = players.first(where: { $0.isSelf })?.playerId else { return false }
        return myId == currentPlayerId
    }
    var isMySevenTransfer: Bool {
        guard let myId = players.first(where: { $0.isSelf })?.playerId else { return false }
        return sevenTransfer.pending && myId == sevenTransfer.sourcePlayerId
    }
    var sevenTransferTarget: PlayerDTO? {
        players.first(where: { $0.playerId == sevenTransfer.targetPlayerId })
    }
    var isCpuGame: Bool { gameMode == "CPU" }
}

struct LoginResponse: Codable {
    let authenticated: Bool
    let csrfToken: String
}

struct JoinResponse: Codable {
    let roomId: String
    let host: Bool
}

struct MessageResponse: Codable {
    let message: String?
}

struct PlayerNameRequest: Codable {
    let playerName: String
}

struct LoginRequest: Codable {
    let password: String
}

struct PlayRequest: Codable {
    let cards: [CardRequest]
}

/// CPU戦【ひとりでイク】作成APIのリクエスト。
struct CpuGameRequest: Codable {
    let playerName: String
    let cpuCount: Int
    let difficulty: String
    let jokerCount: Int
    let revolution: Bool
    let eightCut: Bool
    let markLock: Bool
    let sevenTransfer: Bool
    let yajuRule: Bool
    let forbiddenFinish: Bool
}
