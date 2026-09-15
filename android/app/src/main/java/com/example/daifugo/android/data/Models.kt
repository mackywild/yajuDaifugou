package com.example.daifugo.android.data

import org.json.JSONArray
import org.json.JSONObject

/** サーバーが返すカード情報。 */
data class CardDto(
    val suit: String,
    val rank: String,
    val joker: Boolean = false,
) {
    val displaySuit: String
        get() = when (suit) {
            "SPADE" -> "♠"
            "HEART" -> "♥"
            "DIAMOND" -> "♦"
            "CLUB" -> "♣"
            else -> "★"
        }

    val displayRank: String
        get() = when (rank) {
            "THREE" -> "3"
            "FOUR" -> "4"
            "FIVE" -> "5"
            "SIX" -> "6"
            "SEVEN" -> "7"
            "EIGHT" -> "8"
            "NINE" -> "9"
            "TEN" -> "10"
            "JACK" -> "J"
            "QUEEN" -> "Q"
            "KING" -> "K"
            "ACE" -> "A"
            "TWO" -> "2"
            "JOKER" -> "JOKER"
            else -> rank
        }

    val label: String
        get() = if (joker || suit == "JOKER") "JOKER" else "$displaySuit$displayRank"

    fun toRequestJson(): JSONObject = JSONObject()
        .put("suit", suit)
        .put("rank", rank)
}

/** サーバーが返すプレイヤー情報。 */
data class PlayerDto(
    val playerId: String,
    val playerName: String,
    val handCount: Int,
    val hand: List<CardDto>,
    val passed: Boolean,
    val rank: Int?,
    val self: Boolean,
    val yajuStatus: String = "NONE",
    val cpu: Boolean = false,
    val cpuDifficulty: String? = null,
) {
    val yajuActive: Boolean
        get() = yajuStatus != "NONE"
}

/** 場札情報。 */
data class FieldDto(
    val cards: List<CardDto> = emptyList(),
    val combinationType: String? = null,
)

/** 7渡しの保留状態。 */
data class SevenTransferDto(
    val pending: Boolean = false,
    val sourcePlayerId: String? = null,
    val targetPlayerId: String? = null,
    val cardCount: Int = 0,
)

/** 全端末共有イベント。 */
data class GameEventDto(
    val id: Long,
    val type: String,
    val playerId: String,
    val playerName: String,
)

data class RuleSettingsDto(
    val jokerCount: Int = 1,
    val revolution: Boolean = true,
    val eightCut: Boolean = true,
    val markLock: Boolean = true,
    val sevenTransfer: Boolean = true,
    val yajuRule: Boolean = true,
    val forbiddenFinish: Boolean = true,
)

/** 1プレイヤー視点のゲーム状態。 */
data class GameStateDto(
    val roomId: String,
    val gameMode: String = "MULTIPLAYER",
    val ruleSettings: RuleSettingsDto = RuleSettingsDto(),
    val players: List<PlayerDto>,
    val currentPlayerId: String?,
    val field: FieldDto,
    val revolution: Boolean,
    val lockedMark: String?,
    val host: Boolean,
    val started: Boolean,
    val finished: Boolean,
    val sevenTransfer: SevenTransferDto = SevenTransferDto(),
    val events: List<GameEventDto> = emptyList(),
) {
    val selfPlayer: PlayerDto?
        get() = players.firstOrNull { it.self }

    val currentPlayer: PlayerDto?
        get() = players.firstOrNull { it.playerId == currentPlayerId }

    val isMyTurn: Boolean
        get() = selfPlayer?.playerId == currentPlayerId

    val isMySevenTransfer: Boolean
        get() = sevenTransfer.pending && selfPlayer?.playerId == sevenTransfer.sourcePlayerId

    val sevenTransferTarget: PlayerDto?
        get() = players.firstOrNull { it.playerId == sevenTransfer.targetPlayerId }
}

data class JoinResponse(
    val roomId: String,
    val host: Boolean,
)

data class LoginResponse(
    val authenticated: Boolean,
    val csrfToken: String,
)

object ServerJson {
    fun login(json: String): LoginResponse {
        val root = JSONObject(json)
        return LoginResponse(
            authenticated = root.optBoolean("authenticated", false),
            csrfToken = root.optString("csrfToken", ""),
        )
    }

    fun join(json: String): JoinResponse {
        val root = JSONObject(json)
        return JoinResponse(
            roomId = root.getString("roomId"),
            host = root.optBoolean("host", false),
        )
    }

    fun gameState(json: String): GameStateDto {
        val root = JSONObject(json)
        val players = root.getJSONArray("players").mapObjects(::player)
        val fieldObject = root.optJSONObject("field") ?: JSONObject()
        val fieldCards = fieldObject.optJSONArray("cards")?.mapObjects(::card).orEmpty()
        val sevenTransferObject = root.optJSONObject("sevenTransfer") ?: JSONObject()
        val events = root.optJSONArray("events")?.mapObjects(::event).orEmpty()

        val rules = root.optJSONObject("ruleSettings") ?: JSONObject()
        return GameStateDto(
            roomId = root.getString("roomId"),
            gameMode = root.optString("gameMode", "MULTIPLAYER"),
            ruleSettings = RuleSettingsDto(
                jokerCount = rules.optInt("jokerCount", 1),
                revolution = rules.optBoolean("revolution", true),
                eightCut = rules.optBoolean("eightCut", true),
                markLock = rules.optBoolean("markLock", true),
                sevenTransfer = rules.optBoolean("sevenTransfer", true),
                yajuRule = rules.optBoolean("yajuRule", true),
                forbiddenFinish = rules.optBoolean("forbiddenFinish", true),
            ),
            players = players,
            currentPlayerId = root.nullableString("currentPlayerId"),
            field = FieldDto(
                cards = fieldCards,
                combinationType = fieldObject.nullableString("combinationType"),
            ),
            revolution = root.optBoolean("revolution", false),
            lockedMark = root.nullableString("lockedMark"),
            host = root.optBoolean("host", false),
            started = root.optBoolean("started", false),
            finished = root.optBoolean("finished", false),
            sevenTransfer = SevenTransferDto(
                pending = sevenTransferObject.optBoolean("pending", false),
                sourcePlayerId = sevenTransferObject.nullableString("sourcePlayerId"),
                targetPlayerId = sevenTransferObject.nullableString("targetPlayerId"),
                cardCount = sevenTransferObject.optInt("cardCount", 0),
            ),
            events = events,
        )
    }

    fun message(json: String): String? = runCatching {
        JSONObject(json).optString("message").takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun player(root: JSONObject): PlayerDto = PlayerDto(
        playerId = root.getString("playerId"),
        playerName = root.getString("playerName"),
        handCount = root.optInt("handCount", 0),
        hand = root.optJSONArray("hand")?.mapObjects(::card).orEmpty(),
        passed = root.optBoolean("passed", false),
        rank = if (root.isNull("rank")) null else root.optInt("rank"),
        self = root.optBoolean("self", false),
        yajuStatus = root.optString("yajuStatus", "NONE"),
        cpu = root.optBoolean("cpu", false),
        cpuDifficulty = root.nullableString("cpuDifficulty"),
    )

    private fun card(root: JSONObject): CardDto = CardDto(
        suit = root.getString("suit"),
        rank = root.getString("rank"),
        joker = root.optBoolean("joker", false),
    )

    private fun event(root: JSONObject): GameEventDto = GameEventDto(
        id = root.getLong("id"),
        type = root.getString("type"),
        playerId = root.getString("playerId"),
        playerName = root.getString("playerName"),
    )

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun <T> JSONArray.mapObjects(block: (JSONObject) -> T): List<T> =
        (0 until length()).map { index -> block(getJSONObject(index)) }
}
