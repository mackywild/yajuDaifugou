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

struct PlayerDTO: Codable, Identifiable {
    let playerId: String
    let playerName: String
    let handCount: Int
    let hand: [CardDTO]
    let passed: Bool
    let rank: Int?
    let isSelf: Bool
    let yajuStatus: String

    var id: String { playerId }
    var yajuActive: Bool { yajuStatus != "NONE" }

    enum CodingKeys: String, CodingKey {
        case playerId, playerName, handCount, hand, passed, rank, yajuStatus
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

struct GameStateDTO: Codable {
    let roomId: String
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
    var isMyTurn: Bool { selfPlayer?.playerId != nil && selfPlayer?.playerId == currentPlayerId }
    var isMySevenTransfer: Bool {
        sevenTransfer.pending && selfPlayer?.playerId == sevenTransfer.sourcePlayerId
    }
    var sevenTransferTarget: PlayerDTO? {
        players.first(where: { $0.playerId == sevenTransfer.targetPlayerId })
    }
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
