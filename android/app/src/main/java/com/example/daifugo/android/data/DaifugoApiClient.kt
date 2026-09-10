package com.example.daifugo.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiException(
    override val message: String,
    val statusCode: Int? = null,
) : Exception(message)

/**
 * Spring Boot版DaifugoのREST/WebSocketクライアント。
 * RESTがゲーム操作の正本、WebSocketは状態変更通知専用として使用する。
 */
class DaifugoApiClient {
    private val cookieJar = SessionCookieJar()
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var baseUrl: String = ""
    private var csrfToken: String = ""
    private var webSocket: WebSocket? = null

    fun configure(serverUrl: String) {
        val normalized = serverUrl.trim().trimEnd('/')
        if (normalized != baseUrl) {
            closeRoomSocket()
            cookieJar.clear()
            csrfToken = ""
        }
        baseUrl = normalized
    }

    suspend fun login(password: String): LoginResponse {
        val body = JSONObject().put("password", password).toString()
        val json = execute(
            request("/api/auth/login")
                .post(body.toRequestBody(JSON))
                .build(),
            needsCsrf = false,
        )
        return ServerJson.login(json).also { csrfToken = it.csrfToken }
    }

    suspend fun logout() {
        execute(
            request("/api/auth/logout")
                .post(EMPTY_JSON)
                .build(),
        )
        closeRoomSocket()
        csrfToken = ""
        cookieJar.clear()
    }

    suspend fun createRoom(playerName: String): JoinResponse {
        val json = execute(
            request("/api/rooms")
                .post(playerNameBody(playerName))
                .build(),
        )
        return ServerJson.join(json)
    }

    suspend fun joinRoom(roomId: String, playerName: String): JoinResponse {
        val json = execute(
            request("/api/rooms/${encodePath(roomId)}/join")
                .post(playerNameBody(playerName))
                .build(),
        )
        return ServerJson.join(json)
    }

    suspend fun state(roomId: String): GameStateDto = ServerJson.gameState(
        execute(request("/api/rooms/${encodePath(roomId)}/state").get().build())
    )

    suspend fun start(roomId: String): GameStateDto = ServerJson.gameState(
        execute(
            request("/api/rooms/${encodePath(roomId)}/start")
                .post(EMPTY_JSON)
                .build(),
        )
    )

    suspend fun play(roomId: String, cards: List<CardDto>): GameStateDto {
        val cardArray = JSONArray()
        cards.forEach { cardArray.put(it.toRequestJson()) }
        val body = JSONObject().put("cards", cardArray).toString().toRequestBody(JSON)
        return ServerJson.gameState(
            execute(
                request("/api/rooms/${encodePath(roomId)}/play")
                    .post(body)
                    .build(),
            )
        )
    }

    suspend fun sevenTransfer(roomId: String, cards: List<CardDto>): GameStateDto {
        val cardArray = JSONArray()
        cards.forEach { cardArray.put(it.toRequestJson()) }
        val body = JSONObject().put("cards", cardArray).toString().toRequestBody(JSON)
        return ServerJson.gameState(
            execute(
                request("/api/rooms/${encodePath(roomId)}/seven-transfer")
                    .post(body)
                    .build(),
            )
        )
    }

    suspend fun pass(roomId: String): GameStateDto = ServerJson.gameState(
        execute(
            request("/api/rooms/${encodePath(roomId)}/pass")
                .post(EMPTY_JSON)
                .build(),
        )
    )

    suspend fun leave(roomId: String) {
        execute(
            request("/api/rooms/${encodePath(roomId)}/leave")
                .post(EMPTY_JSON)
                .build(),
        )
        closeRoomSocket()
    }

    /**
     * 部屋の状態変更通知を購読する。
     * 通知本文に手札情報は含まれないため、受信時にREST /stateを再取得する。
     */
    fun connectRoomSocket(
        roomId: String,
        onStateChanged: () -> Unit,
        onConnectionChanged: (Boolean) -> Unit,
    ) {
        closeRoomSocket()
        if (baseUrl.isBlank()) return

        val wsBase = when {
            baseUrl.startsWith("https://") -> "wss://${baseUrl.removePrefix("https://")}" 
            baseUrl.startsWith("http://") -> "ws://${baseUrl.removePrefix("http://")}" 
            else -> "ws://$baseUrl"
        }
        val encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8.toString())
        val request = Request.Builder()
            .url("$wsBase/ws/game?roomId=$encodedRoomId")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onConnectionChanged(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (runCatching { JSONObject(text).optString("type") }.getOrNull() == "STATE_CHANGED") {
                    onStateChanged()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onConnectionChanged(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onConnectionChanged(false)
            }
        })
    }

    fun closeRoomSocket() {
        webSocket?.close(1000, "room closed")
        webSocket = null
    }

    private fun request(path: String): Request.Builder {
        ensureConfigured()
        return Request.Builder()
            .url("$baseUrl$path")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
    }

    private suspend fun execute(request: Request, needsCsrf: Boolean = request.method != "GET"): String =
        withContext(Dispatchers.IO) {
            val finalRequest = if (needsCsrf) {
                if (csrfToken.isBlank()) {
                    throw@withContext ApiException("認証セッションがありません。再ログインしてください")
                }
                request.newBuilder()
                    .header("X-CSRF-Token", csrfToken)
                    .build()
            } else {
                request
            }

            client.newCall(finalRequest).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    throw@withContext ApiException(
                        ServerJson.message(body) ?: "サーバーエラー (${response.code})",
                        response.code,
                    )
                }
                body
            }
        }

    private fun playerNameBody(playerName: String) = JSONObject()
        .put("playerName", playerName.trim())
        .toString()
        .toRequestBody(JSON)

    private fun encodePath(value: String): String = URLEncoder.encode(
        value.trim(),
        StandardCharsets.UTF_8.toString(),
    ).replace("+", "%20")

    private fun ensureConfigured() {
        if (baseUrl.isBlank()) throw ApiException("サーバーURLを設定してください")
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val EMPTY_JSON = "{}".toRequestBody(JSON)
    }
}
