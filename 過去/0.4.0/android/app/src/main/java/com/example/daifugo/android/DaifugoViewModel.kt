package com.example.daifugo.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.daifugo.android.data.ApiException
import com.example.daifugo.android.data.DaifugoApiClient
import com.example.daifugo.android.data.GameStateDto
import com.example.daifugo.android.data.RuleSettingsDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** アプリ内の主要画面。 */
enum class DaifugoScreen {
    LOGIN,
    MAIN_MENU,
    MULTIPLAYER,
    CPU_SETUP,
    ROOM,
    GAME,
    RESULT,
}

/** 野獣ルールの音声キュー。実音声はres/rawへ後から配置する。 */
enum class GameAudioCue {
    YAJU_AVAILABLE,
    YAJU_SUCCESS,
}

data class DaifugoUiState(
    val screen: DaifugoScreen = DaifugoScreen.LOGIN,
    val serverUrl: String = "http://10.0.2.2:8080",
    val password: String = "",
    val playerName: String = "",
    val roomIdInput: String = "",
    val cpuCount: Int = 3,
    val cpuDifficulty: String = "HARD",
    val cpuJokerCount: Int = 1,
    val ruleRevolution: Boolean = true,
    val ruleEightCut: Boolean = true,
    val ruleMarkLock: Boolean = true,
    val ruleSevenTransfer: Boolean = true,
    val ruleYaju: Boolean = true,
    val ruleForbiddenFinish: Boolean = true,
    val roomId: String? = null,
    val gameState: GameStateDto? = null,
    val selectedCardIndices: Set<Int> = emptySet(),
    val loading: Boolean = false,
    val socketConnected: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

/**
 * Daifugo v0.4.0 の画面状態と通信を管理するViewModel。
 */
class DaifugoViewModel(application: Application) : AndroidViewModel(application) {
    private val api = DaifugoApiClient()
    private val preferences = application.getSharedPreferences("daifugo", 0)

    private val _uiState = MutableStateFlow(
        DaifugoUiState(
            serverUrl = preferences.getString("serverUrl", "http://10.0.2.2:8080")
                ?: "http://10.0.2.2:8080",
            playerName = preferences.getString("playerName", "") ?: "",
        )
    )
    val uiState: StateFlow<DaifugoUiState> = _uiState.asStateFlow()

    private val _audioCues = MutableSharedFlow<GameAudioCue>(extraBufferCapacity = 16)
    val audioCues: SharedFlow<GameAudioCue> = _audioCues

    /** 最後に音声処理したゲームイベントID。ポーリング再取得時の重複再生を防ぐ。 */
    private var lastHandledEventId: Long = 0L

    private var pollingJob: Job? = null
    private var reconnectJob: Job? = null

    fun setServerUrl(value: String) = update { copy(serverUrl = value) }
    fun setPassword(value: String) = update { copy(password = value) }
    fun setPlayerName(value: String) = update { copy(playerName = value) }
    fun setRoomIdInput(value: String) = update { copy(roomIdInput = value) }
    fun setCpuCount(value: Int) = update { copy(cpuCount = value.coerceIn(1, 3)) }
    fun setCpuDifficulty(value: String) = update { copy(cpuDifficulty = value) }
    fun setCpuJokerCount(value: Int) = update { copy(cpuJokerCount = value.coerceIn(0, 2)) }
    fun setRuleRevolution(value: Boolean) = update { copy(ruleRevolution = value) }
    fun setRuleEightCut(value: Boolean) = update { copy(ruleEightCut = if (ruleYaju) true else value) }
    fun setRuleMarkLock(value: Boolean) = update { copy(ruleMarkLock = value) }
    fun setRuleSevenTransfer(value: Boolean) = update { copy(ruleSevenTransfer = value) }
    fun setRuleYaju(value: Boolean) = update { copy(ruleYaju = value, ruleEightCut = if (value) true else ruleEightCut) }
    fun setRuleForbiddenFinish(value: Boolean) = update { copy(ruleForbiddenFinish = value) }

    fun clearMessage() = update { copy(errorMessage = null, infoMessage = null) }

    fun login() = launchAction {
        val state = _uiState.value
        val serverUrl = normalizeServerUrl(state.serverUrl)
        require(state.password.isNotBlank()) { "パスワードを入力してください" }

        api.configure(serverUrl)
        val response = api.login(state.password)
        if (!response.authenticated) error("ログインに失敗しました")

        preferences.edit().putString("serverUrl", serverUrl).apply()
        update {
            copy(
                screen = DaifugoScreen.MAIN_MENU,
                serverUrl = serverUrl,
                password = "",
                errorMessage = null,
                infoMessage = "サーバーへ接続しました",
            )
        }
    }

    fun openMultiplayer() = update { copy(screen = DaifugoScreen.MULTIPLAYER, errorMessage = null, infoMessage = null) }

    fun openCpuSetup() = update { copy(screen = DaifugoScreen.CPU_SETUP, errorMessage = null, infoMessage = null) }

    fun backToMenu() = update { copy(screen = DaifugoScreen.MAIN_MENU, errorMessage = null, infoMessage = null) }

    fun logout() = launchAction {
        api.logout()
        stopRoomRealtime()
        lastHandledEventId = 0L
        update {
            copy(
                screen = DaifugoScreen.LOGIN,
                roomId = null,
                gameState = null,
                selectedCardIndices = emptySet(),
                socketConnected = false,
                infoMessage = "ログアウトしました",
            )
        }
    }

    fun startCpuGame() = launchAction {
        val state = _uiState.value
        val playerName = validatedPlayerName()
        val rules = RuleSettingsDto(
            jokerCount = state.cpuJokerCount,
            revolution = state.ruleRevolution,
            eightCut = if (state.ruleYaju) true else state.ruleEightCut,
            markLock = state.ruleMarkLock,
            sevenTransfer = state.ruleSevenTransfer,
            yajuRule = state.ruleYaju,
            forbiddenFinish = state.ruleForbiddenFinish,
        )
        val game = api.createCpuGame(
            playerName = playerName,
            cpuCount = state.cpuCount,
            difficulty = state.cpuDifficulty,
            rules = rules,
        )
        savePlayerName(playerName)
        lastHandledEventId = 0L
        update { copy(roomId = game.roomId, roomIdInput = game.roomId) }
        applyGameState(game)
        startRoomRealtime(game.roomId)
    }

    fun createRoom() = launchAction {
        val playerName = validatedPlayerName()
        val response = api.createRoom(playerName)
        savePlayerName(playerName)
        enterRoom(response.roomId)
    }

    fun joinRoom() = launchAction {
        val state = _uiState.value
        val playerName = validatedPlayerName()
        val roomId = state.roomIdInput.trim()
        require(roomId.isNotBlank()) { "ルームIDを入力してください" }

        val response = api.joinRoom(roomId, playerName)
        savePlayerName(playerName)
        enterRoom(response.roomId)
    }

    fun refreshState(silent: Boolean = true) {
        val roomId = _uiState.value.roomId ?: return
        viewModelScope.launch {
            if (!silent) update { copy(loading = true) }
            runCatching { api.state(roomId) }
                .onSuccess(::applyGameState)
                .onFailure(::handleError)
            if (!silent) update { copy(loading = false) }
        }
    }

    fun startGame() = launchAction {
        val roomId = requireRoomId()
        applyGameState(api.start(roomId))
    }

    fun toggleCard(index: Int) {
        val state = _uiState.value
        val game = state.gameState ?: return
        val handSize = game.selfPlayer?.hand?.size ?: return
        if (!game.isMyTurn || index !in 0 until handSize) return

        val selected = state.selectedCardIndices.toMutableSet()
        if (!selected.add(index)) selected.remove(index)
        update { copy(selectedCardIndices = selected) }
    }

    fun playSelected() = launchAction {
        val roomId = requireRoomId()
        val state = _uiState.value
        val game = state.gameState ?: error("ゲーム状態を取得できません")
        val self = game.selfPlayer ?: error("自分の手札を取得できません")
        val selectedCards = state.selectedCardIndices
            .sorted()
            .mapNotNull { self.hand.getOrNull(it) }
        require(selectedCards.isNotEmpty()) { "カードを選択してください" }

        val nextState = if (game.isMySevenTransfer) {
            require(selectedCards.size == game.sevenTransfer.cardCount) {
                "7渡しでは${game.sevenTransfer.cardCount}枚選択してください"
            }
            api.sevenTransfer(roomId, selectedCards)
        } else {
            api.play(roomId, selectedCards)
        }

        applyGameState(nextState)
        update { copy(selectedCardIndices = emptySet()) }
    }

    fun pass() = launchAction {
        val roomId = requireRoomId()
        applyGameState(api.pass(roomId))
        update { copy(selectedCardIndices = emptySet()) }
    }

    fun leaveRoom() = launchAction {
        val roomId = requireRoomId()
        api.leave(roomId)
        stopRoomRealtime()
        lastHandledEventId = 0L
        update {
            copy(
                screen = DaifugoScreen.MAIN_MENU,
                roomId = null,
                roomIdInput = "",
                gameState = null,
                selectedCardIndices = emptySet(),
                socketConnected = false,
                infoMessage = "部屋から退出しました",
            )
        }
    }

    private suspend fun enterRoom(roomId: String) {
        lastHandledEventId = 0L
        update {
            copy(
                roomId = roomId,
                roomIdInput = roomId,
                screen = DaifugoScreen.ROOM,
                selectedCardIndices = emptySet(),
            )
        }
        applyGameState(api.state(roomId))
        startRoomRealtime(roomId)
    }

    private fun startRoomRealtime(roomId: String) {
        stopRoomRealtime()
        connectSocket(roomId)

        // WebSocketが一時的に切れてもゲーム状態が止まって見えないよう、保険として5秒ごとに同期する。
        pollingJob = viewModelScope.launch {
            while (_uiState.value.roomId == roomId) {
                delay(5_000)
                runCatching { api.state(roomId) }
                    .onSuccess(::applyGameState)
            }
        }
    }

    private fun connectSocket(roomId: String) {
        api.connectRoomSocket(
            roomId = roomId,
            onStateChanged = {
                viewModelScope.launch {
                    runCatching { api.state(roomId) }
                        .onSuccess(::applyGameState)
                }
            },
            onConnectionChanged = { connected ->
                viewModelScope.launch {
                    update { copy(socketConnected = connected) }
                    if (!connected && _uiState.value.roomId == roomId) {
                        reconnectJob?.cancel()
                        reconnectJob = launch {
                            delay(3_000)
                            if (_uiState.value.roomId == roomId) connectSocket(roomId)
                        }
                    }
                }
            },
        )
    }

    private fun stopRoomRealtime() {
        pollingJob?.cancel()
        pollingJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        api.closeRoomSocket()
    }

    private fun applyGameState(newState: GameStateDto) {
        processGameEvents(newState)

        val previous = _uiState.value.gameState
        val turnChanged = previous?.currentPlayerId != newState.currentPlayerId
        val handChanged = previous?.selfPlayer?.hand != newState.selfPlayer?.hand

        val screen = when {
            newState.finished -> DaifugoScreen.RESULT
            newState.started -> DaifugoScreen.GAME
            else -> DaifugoScreen.ROOM
        }

        update {
            copy(
                screen = screen,
                roomId = newState.roomId,
                gameState = newState,
                selectedCardIndices = if (turnChanged || handChanged) emptySet() else selectedCardIndices,
                errorMessage = null,
            )
        }
    }

    /**
     * 未処理の全体共有イベントを音声キューへ変換する。
     * 同じGameStateをポーリングで何度取得してもイベントIDで重複再生しない。
     */
    private fun processGameEvents(state: GameStateDto) {
        val unseen = state.events
            .filter { it.id > lastHandledEventId }
            .sortedBy { it.id }

        unseen.forEach { event ->
            when (event.type) {
                "YAJU_AVAILABLE" -> _audioCues.tryEmit(GameAudioCue.YAJU_AVAILABLE)
                "YAJU_SUCCESS" -> _audioCues.tryEmit(GameAudioCue.YAJU_SUCCESS)
            }
            lastHandledEventId = maxOf(lastHandledEventId, event.id)
        }
    }

    private fun launchAction(block: suspend () -> Unit) {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            update { copy(loading = true, errorMessage = null, infoMessage = null) }
            runCatching { block() }
                .onFailure(::handleError)
            update { copy(loading = false) }
        }
    }

    private fun handleError(throwable: Throwable) {
        val message = when (throwable) {
            is ApiException -> throwable.message
            is IllegalArgumentException -> throwable.message ?: "入力内容を確認してください"
            else -> throwable.message ?: "通信エラーが発生しました"
        }
        update { copy(errorMessage = message) }
    }

    private fun validatedPlayerName(): String {
        val playerName = _uiState.value.playerName.trim()
        require(playerName.isNotBlank()) { "プレイヤー名を入力してください" }
        require(playerName.length <= 20) { "プレイヤー名は20文字以内にしてください" }
        return playerName
    }

    private fun savePlayerName(playerName: String) {
        preferences.edit().putString("playerName", playerName).apply()
    }

    private fun requireRoomId(): String =
        _uiState.value.roomId ?: error("部屋に参加していません")

    private fun normalizeServerUrl(raw: String): String {
        var result = raw.trim().trimEnd('/')
        require(result.isNotBlank()) { "サーバーURLを入力してください" }
        if (!result.startsWith("http://") && !result.startsWith("https://")) {
            result = "http://$result"
        }
        return result
    }

    private inline fun update(transform: DaifugoUiState.() -> DaifugoUiState) {
        _uiState.value = _uiState.value.transform()
    }

    override fun onCleared() {
        stopRoomRealtime()
        super.onCleared()
    }
}
