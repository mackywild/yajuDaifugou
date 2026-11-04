package com.example.daifugo.android

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.daifugo.android.data.ApiException
import com.example.daifugo.android.data.CardDto
import com.example.daifugo.android.data.DaifugoApiClient
import com.example.daifugo.android.data.GameStateDto
import com.example.daifugo.android.data.RuleSettingsDto
import com.example.daifugo.android.firebase.CloudProgressCodec
import com.example.daifugo.android.firebase.FirebaseAccountManager
import com.example.daifugo.android.local.LocalCpuActionType
import com.example.daifugo.android.local.LocalCpuGameManager
import com.example.daifugo.android.progression.DailyMissionRules
import com.example.daifugo.android.progression.PlayerProgress
import com.example.daifugo.android.progression.PlayerProgressRepository
import com.example.daifugo.game.config.GameLimits
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/** アプリ内の主要画面。 */
enum class DaifugoScreen {
    TITLE,
    ACCOUNT_SETUP,
    LOGIN,
    MAIN_MENU,
    MULTIPLAYER,
    CPU_SETUP,
    CHALLENGE_SETUP,
    MISSION,
    PROFILE,
    COLLECTION,
    PRESENT_BOX,
    SETTINGS,
    ROOM,
    GAME,
    RESULT,
}

/** 野獣ルールの音声キュー。実音声はres/rawへ後から配置する。 */
enum class GameAudioCue {
    YAJU_AVAILABLE,
    YAJU_SUCCESS,
}

/** CPU手番演出の種類。 */
enum class CpuAnimationType {
    THINKING,
    PLAY,
    PASS,
    SEVEN_TRANSFER,
}

/** 野獣対象確定時の全画面カットイン情報。 */
data class YajuCutIn(
    val id: Long,
    val playerIds: List<String>,
    val playerNames: List<String>,
)

enum class RuleCutInType {
    EIGHT_CUT,
    JACK_BACK,
    EARLY_SHOT,
}

data class RuleCutIn(
    val id: Long,
    val type: RuleCutInType,
    val playerId: String,
    val playerName: String,
)

/** CPUが現在行っているアクションを画面へ伝える。 */
data class CpuTurnAnimation(
    val id: Long,
    val playerId: String,
    val playerName: String,
    val type: CpuAnimationType,
    val cards: List<CardDto> = emptyList(),
    val cardCount: Int = cards.size,
    val targetPlayerName: String? = null,
)

data class DaifugoUiState(
    val screen: DaifugoScreen = DaifugoScreen.TITLE,
    val serverUrl: String = DaifugoViewModel.DEFAULT_PUBLIC_SERVER_URL,
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
    val ruleJackBack: Boolean = true,
    val ruleForbiddenFinish: Boolean = true,
    val playGamesConfigured: Boolean = false,
    val playGamesSignedIn: Boolean = false,
    val playGamesDisplayName: String? = null,
    val firebaseConfigured: Boolean = false,
    val firebaseUid: String? = null,
    val firebaseDisplayName: String? = null,
    val cloudReady: Boolean = false,
    val cloudMigrationRequired: Boolean = false,
    val cloudResultStatus: String? = null,
    val bgmEnabled: Boolean = true,
    val progress: PlayerProgress = PlayerProgress(),
    val lastEarnedExp: Int = 0,
    val roomId: String? = null,
    val gameState: GameStateDto? = null,
    val selectedCardIndices: Set<Int> = emptySet(),
    val loading: Boolean = false,
    val socketConnected: Boolean = false,
    val cpuTurnInProgress: Boolean = false,
    val cpuTurnAnimation: CpuTurnAnimation? = null,
    val cpuActionHistory: List<String> = emptyList(),
    val yajuCutIn: YajuCutIn? = null,
    val ruleCutIns: List<RuleCutIn> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

/**
 * Daifugo v0.4.4 の画面状態と通信を管理するViewModel。
 * CPU戦では1手ずつ約3秒の演出を挟み、CPUが何を出したか追えるようにする。
 */
class DaifugoViewModel(application: Application) : AndroidViewModel(application) {
    private val api = DaifugoApiClient()
    private val localCpu = LocalCpuGameManager()
    private val preferences = application.getSharedPreferences("daifugo", 0)
    private val progressRepository = PlayerProgressRepository(application)
    private val firebaseAccountManager = FirebaseAccountManager(application)
    private val cloudResultAttempts = mutableSetOf<String>()
    private val initialProgress = progressRepository.load()

    private val _uiState = MutableStateFlow(
        DaifugoUiState(
            // 製品版MULTIPLAYERは公開サーバーへ固定接続する。
            // 旧10.0.2.2 / LAN内URL入力方式は通常導線から外す。
            serverUrl = DEFAULT_PUBLIC_SERVER_URL,
            playerName = if (initialProgress.profileConfigured) initialProgress.displayName else "",
            progress = initialProgress,
            firebaseConfigured = firebaseAccountManager.isConfigured,
            bgmEnabled = preferences.getBoolean("bgmEnabled", true),
        )
    )
    val uiState: StateFlow<DaifugoUiState> = _uiState.asStateFlow()

    private val _audioCues = MutableSharedFlow<GameAudioCue>(extraBufferCapacity = 16)
    val audioCues: SharedFlow<GameAudioCue> = _audioCues

    /** 最後に音声処理したゲームイベントID。ポーリング再取得時の重複再生を防ぐ。 */
    private var lastHandledEventId: Long = 0L

    private var pollingJob: Job? = null
    private var reconnectJob: Job? = null
    private var cpuTurnJob: Job? = null
    private var cpuAnimationSequence: Long = 0L

    /**
     * WebSocket再接続世代。
     * closeRoomSocket()した古い接続のonClosedが遅れて返ってきても、
     * 新しい接続を上書き・二重再接続しないために使用する。
     */
    private var realtimeGeneration: Long = 0L

    fun setServerUrl(value: String) = update { copy(serverUrl = value) }
    fun setPassword(value: String) = update { copy(password = value) }
    fun setPlayerName(value: String) = update { copy(playerName = value) }
    fun setRoomIdInput(value: String) = update { copy(roomIdInput = value) }
    fun setCpuCount(value: Int) = update { copy(cpuCount = value.coerceIn(1, GameLimits.MAX_CPU_COUNT)) }
    fun setCpuDifficulty(value: String) = update { copy(cpuDifficulty = value) }
    fun setCpuJokerCount(value: Int) = update { copy(cpuJokerCount = value.coerceIn(0, 2)) }
    fun setRuleRevolution(value: Boolean) = update { copy(ruleRevolution = value) }
    fun setRuleEightCut(value: Boolean) = update { copy(ruleEightCut = if (ruleYaju) true else value) }
    fun setRuleMarkLock(value: Boolean) = update { copy(ruleMarkLock = value) }
    fun setRuleSevenTransfer(value: Boolean) = update { copy(ruleSevenTransfer = value) }
    fun setRuleYaju(value: Boolean) = update { copy(ruleYaju = value, ruleEightCut = if (value) true else ruleEightCut) }
    fun setRuleJackBack(value: Boolean) = update { copy(ruleJackBack = value) }
    fun setRuleForbiddenFinish(value: Boolean) = update { copy(ruleForbiddenFinish = value) }

    fun clearMessage() = update { copy(errorMessage = null, infoMessage = null) }

    fun updatePlayGamesAccount(
        configured: Boolean,
        authenticated: Boolean,
        playerId: String?,
        displayName: String?,
        errorMessage: String? = null,
    ) {
        // Firebaseで表示中のユーザーへPlay Gamesの端末データを上書きしない。
        val firebaseActive = firebaseAccountManager.currentUser != null
        val progress = if (!firebaseActive && authenticated && !playerId.isNullOrBlank()) {
            progressRepository.bindPlayGamesAccount(
                playerId = playerId,
                displayName = displayName.orEmpty(),
            )
        } else {
            progressRepository.load()
        }

        update {
            val signedInProgress = if (!firebaseActive && authenticated) progress else this.progress
            copy(
                playGamesConfigured = configured,
                playGamesSignedIn = authenticated,
                playGamesDisplayName = displayName,
                progress = signedInProgress,
                playerName = if (signedInProgress.profileConfigured) signedInProgress.displayName else playerName,
                errorMessage = errorMessage,
            )
        }
    }

    /** 起動時にFirebaseの永続ログインを復元し、UIDの進行データをサーバーから取得する。 */
    fun restoreFirebaseSession() {
        if (firebaseAccountManager.currentUser == null) return
        launchAction { connectFirebaseAccount() }
    }

    /** Googleログイン成功後、必ずSpring Bootで同一UIDを検証してから画面へ反映する。 */
    fun signInFirebase(activity: Activity) = launchAction {
        require(_uiState.value.screen !in setOf(DaifugoScreen.ROOM, DaifugoScreen.GAME, DaifugoScreen.RESULT)) {
            "対戦中はアカウントを切り替えられません"
        }
        firebaseAccountManager.signIn(activity)
        connectFirebaseAccount()
    }

    private suspend fun connectFirebaseAccount() {
        val user = firebaseAccountManager.currentUser ?: error("Googleログインが必要です")
        api.configure(DEFAULT_PUBLIC_SERVER_URL)
        api.clearSession()
        val login = api.openFirebaseSession(firebaseAccountManager.idToken())
        require(login.authenticated) { "Firebaseセッションを開始できませんでした" }
        val result = api.cloudProgress()
        val exists = result.optBoolean("exists")
        val cloud = if (exists) CloudProgressCodec.decode(result.getJSONObject("progress"), user.uid) else null
        val local = progressRepository.load()
        update {
            copy(
                firebaseUid = user.uid,
                firebaseDisplayName = user.displayName ?: user.email,
                cloudReady = exists,
                cloudMigrationRequired = !exists,
                progress = cloud?.progress ?: local,
                bgmEnabled = cloud?.bgmEnabled ?: bgmEnabled,
                playerName = if (exists && cloud != null) {
                    if (cloud.progress.profileConfigured) cloud.progress.displayName else ""
                } else if (local.profileConfigured) local.displayName else "",
                roomId = null,
                gameState = null,
                cloudResultStatus = null,
                infoMessage = if (exists) "Web版と共通のデータを読み込みました" else "クラウド初回登録方法を選んでください",
                errorMessage = null,
            )
        }
    }

    /** 未登録UIDのみに移行を許可する。サーバー側でも既存レコードは上書きしない。 */
    fun initializeCloud(importLocal: Boolean) = launchAction {
        val uid = _uiState.value.firebaseUid ?: error("Googleログインが必要です")
        require(_uiState.value.cloudMigrationRequired) { "既にクラウドデータがあります" }
        val source = if (importLocal) progressRepository.load() else PlayerProgress()
        val json = CloudProgressCodec.encodeMigration(source, preferences.getBoolean("bgmEnabled", true))
        val response = withCloudRetry { api.migrateCloudProgress(json) }
        applyCloudProgress(response.getJSONObject("progress"), uid)
        update {
            copy(
                cloudMigrationRequired = false,
                cloudReady = true,
                screen = if (progress.profileConfigured) DaifugoScreen.MAIN_MENU else DaifugoScreen.ACCOUNT_SETUP,
                infoMessage = if (importLocal) "端末データをクラウドへ引き継ぎました" else "新しいクラウドアカウントを作成しました",
            )
        }
    }

    /** ログアウト時はFirebase UIDの進行データを端末ゲストへ混ぜない。 */
    fun signOutFirebase() = launchAction {
        require(_uiState.value.screen !in setOf(DaifugoScreen.ROOM, DaifugoScreen.GAME, DaifugoScreen.RESULT)) {
            "対戦中はログアウトできません"
        }
        if (_uiState.value.firebaseUid != null) api.logout()
        firebaseAccountManager.signOut(getApplication())
        api.clearSession()
        cloudResultAttempts.clear()
        val local = progressRepository.load()
        update {
            copy(
                firebaseUid = null,
                firebaseDisplayName = null,
                cloudReady = false,
                cloudMigrationRequired = false,
                cloudResultStatus = null,
                progress = local,
                playerName = if (local.profileConfigured) local.displayName else "",
                bgmEnabled = preferences.getBoolean("bgmEnabled", true),
                screen = if (local.profileConfigured) DaifugoScreen.MAIN_MENU else DaifugoScreen.ACCOUNT_SETUP,
                infoMessage = "Googleアカウントからログアウトしました",
            )
        }
    }

    /** HTTPセッション切れ時のみ安全に再認証して1回だけ処理を再実行する。 */
    private suspend fun <T> withCloudRetry(action: suspend () -> T): T {
        return try {
            action()
        } catch (error: ApiException) {
            if (error.statusCode != 401 && error.statusCode != 403) throw error
            val response = api.openFirebaseSession(firebaseAccountManager.idToken())
            require(response.authenticated) { "Google認証を再開できませんでした" }
            action()
        }
    }

    private fun applyCloudProgress(cloud: JSONObject, uid: String) {
        // アカウント切替後に旧UIDの遅延レスポンスを描画しない。
        if (_uiState.value.firebaseUid != uid) return
        val snapshot = CloudProgressCodec.decode(cloud, uid)
        update {
            copy(
                progress = snapshot.progress,
                playerName = snapshot.progress.displayName,
                bgmEnabled = snapshot.bgmEnabled,
                cloudReady = true,
                errorMessage = null,
            )
        }
    }

    /** ローカル操作とFirestore操作を分け、通信失敗時にクラウド状態を楽観更新しない。 */
    private fun progressAction(
        message: String,
        localAction: () -> PlayerProgress,
        remoteAction: suspend () -> JSONObject,
    ) = launchAction {
        val uid = _uiState.value.firebaseUid
        if (uid != null) {
            require(_uiState.value.cloudReady) { "先にクラウドデータの初期登録を完了してください" }
            val response = withCloudRetry(remoteAction)
            applyCloudProgress(response.getJSONObject("progress"), uid)
        } else {
            val progress = localAction()
            update { copy(progress = progress, playerName = progress.displayName) }
        }
        update { copy(infoMessage = message, errorMessage = null) }
    }

    fun setBgmEnabled(enabled: Boolean) = launchAction {
        val uid = _uiState.value.firebaseUid
        if (uid != null) {
            require(_uiState.value.cloudReady) { "先にクラウドデータを初期登録してください" }
            val response = withCloudRetry { api.cloudBgm(enabled) }
            applyCloudProgress(response.getJSONObject("progress"), uid)
        } else {
            preferences.edit().putBoolean("bgmEnabled", enabled).apply()
            update { copy(bgmEnabled = enabled) }
        }
    }

    /**
     * タイトル画面から、端末プロフィール未設定時だけ初回設定へ進む。
     * Google Play Gamesは任意連携で、PC版とのクロスプレイ条件にはしない。
     */
    fun enterMainMenu() = update {
        val ready = progress.profileConfigured
        copy(
            screen = if (ready) DaifugoScreen.MAIN_MENU else DaifugoScreen.ACCOUNT_SETUP,
            playerName = if (progress.profileConfigured) progress.displayName else playerName,
            errorMessage = null,
            infoMessage = null,
        )
    }

    fun completeAccountSetup() = launchAction {
        val name = _uiState.value.playerName.trim()
        val uid = _uiState.value.firebaseUid
        if (uid != null) {
            require(_uiState.value.cloudReady) { "先にクラウド初回登録を完了してください" }
            val result = withCloudRetry { api.cloudProfile(name) }
            applyCloudProgress(result.getJSONObject("progress"), uid)
        } else {
            val progress = progressRepository.updateDisplayName(name)
            update { copy(progress = progress, playerName = progress.displayName) }
        }
        update {
            copy(
                screen = DaifugoScreen.MAIN_MENU,
                infoMessage = "アカウント設定が完了しました",
                errorMessage = null,
            )
        }
    }

    fun saveProfileName() = progressAction(
        message = "プレイヤー名を更新しました",
        localAction = { progressRepository.updateDisplayName(_uiState.value.playerName) },
        remoteAction = { api.cloudProfile(_uiState.value.playerName.trim()) },
    )

    /** 表示中の野獣確定カットインを、同一イベントの場合だけ閉じる。 */
    fun clearYajuCutIn(id: Long) = update {
        if (yajuCutIn?.id == id) copy(yajuCutIn = null) else this
    }

    fun clearRuleCutIn(id: Long) = update {
        copy(ruleCutIns = ruleCutIns.filterNot { it.id == id })
    }

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
                screen = DaifugoScreen.MULTIPLAYER,
                serverUrl = serverUrl,
                password = "",
                errorMessage = null,
                infoMessage = "サーバーへ接続しました",
            )
        }
    }

    /**
     * PCブラウザ版と同じ公開サーバーへ直接接続してMULTIPLAYERを開く。
     * Firebaseが有効ならWebと同じGoogle認証を使い、無効なら従来セッションを維持する。
     */
    fun openMultiplayer() = launchAction {
        stopRoomRealtime()
        val serverUrl = DEFAULT_PUBLIC_SERVER_URL
        api.configure(serverUrl)

        val user = firebaseAccountManager.currentUser
        val response = if (user != null) {
            if (_uiState.value.firebaseUid != user.uid) connectFirebaseAccount()
            require(_uiState.value.cloudReady) {
                "プロフィールでクラウド初回登録を完了してください"
            }
            api.openFirebaseSession(firebaseAccountManager.idToken())
        } else {
            api.openPublicSession()
        }
        if (!response.authenticated) {
            error("Googleログインが必要です。プロフィールから連携してください")
        }

        preferences.edit().putString("serverUrl", serverUrl).apply()
        update {
            copy(
                screen = DaifugoScreen.MULTIPLAYER,
                serverUrl = serverUrl,
                password = "",
                roomId = null,
                roomIdInput = "",
                gameState = null,
                socketConnected = false,
                errorMessage = null,
                infoMessage = if (user != null) "Firebaseアカウントで接続しました" else "ONLINE TABLEへ接続しました",
            )
        }
    }

    fun openCpuSetup() = update { copy(screen = DaifugoScreen.CPU_SETUP, errorMessage = null, infoMessage = null) }

    fun openChallengeSetup() = update { copy(screen = DaifugoScreen.CHALLENGE_SETUP, errorMessage = null, infoMessage = null) }

    fun openMission() = update { copy(screen = DaifugoScreen.MISSION, errorMessage = null, infoMessage = null) }

    fun openProfile() = update {
        copy(
            screen = DaifugoScreen.PROFILE,
            playerName = progress.displayName,
            errorMessage = null,
            infoMessage = null,
        )
    }

    fun openCollection() = update { copy(screen = DaifugoScreen.COLLECTION, errorMessage = null, infoMessage = null) }

    fun openPresentBox() = update { copy(screen = DaifugoScreen.PRESENT_BOX, errorMessage = null, infoMessage = null) }

    fun openSettings() = update { copy(screen = DaifugoScreen.SETTINGS, errorMessage = null, infoMessage = null) }

    fun receivePresent(presentId: String) = progressAction(
        message = "プレゼントを受け取りました",
        localAction = { progressRepository.receivePresent(presentId).progress },
        remoteAction = { api.cloudReceivePresent(presentId) },
    )

    fun receiveAllPresents() = progressAction(
        message = "プレゼントをまとめて受け取りました",
        localAction = { progressRepository.receiveAllPresents().progress },
        remoteAction = { api.cloudReceiveAll() },
    )

    fun equipCosmetic(itemId: String) = progressAction(
        message = "アイテムを装備しました",
        localAction = { progressRepository.equipCosmetic(itemId) },
        remoteAction = { api.cloudEquipment(itemId) },
    )

    fun claimDailyMission(missionId: String) = progressAction(
        message = "デイリーミッション報酬を受け取りました",
        localAction = { progressRepository.claimDailyMission(missionId) },
        remoteAction = { api.cloudClaimMission(missionId) },
    )

    fun backToMenu() = update { copy(screen = DaifugoScreen.MAIN_MENU, errorMessage = null, infoMessage = null) }

    fun logout() = launchAction {
        runCatching { api.logout() }
        localCpu.close()
        stopRoomRealtime()
        lastHandledEventId = 0L
        update {
            copy(
                screen = DaifugoScreen.MAIN_MENU,
                roomId = null,
                gameState = null,
                cloudResultStatus = null,
                selectedCardIndices = emptySet(),
                socketConnected = false,
                yajuCutIn = null,
                ruleCutIns = emptyList(),
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
            jackBack = state.ruleJackBack,
            forbiddenFinish = state.ruleForbiddenFinish,
        )
        /*
         * CPU戦は完全ローカル実行。
         * Spring Bootへのログイン・REST・WebSocket接続は一切不要。
         */
        val game = localCpu.start(
            humanName = playerName,
            cpuCount = state.cpuCount,
            difficulty = state.cpuDifficulty,
            rules = rules,
        )
        lastHandledEventId = 0L
        stopRoomRealtime()
        update { copy(cpuActionHistory = emptyList(), cpuTurnAnimation = null, cpuTurnInProgress = false, yajuCutIn = null, ruleCutIns = emptyList()) }
        applyGameState(game)
        startCpuTurnSequence()
    }

    fun startYajuChallenge() = launchAction {
        val state = _uiState.value
        val playerName = validatedPlayerName()
        val rules = RuleSettingsDto(
            jokerCount = state.cpuJokerCount,
            revolution = state.ruleRevolution,
            eightCut = true,
            markLock = state.ruleMarkLock,
            sevenTransfer = state.ruleSevenTransfer,
            yajuRule = true,
            jackBack = state.ruleJackBack,
            forbiddenFinish = state.ruleForbiddenFinish,
        )
        val game = localCpu.start(
            humanName = playerName,
            cpuCount = state.cpuCount,
            difficulty = state.cpuDifficulty,
            rules = rules,
            yajuChallenge = true,
        )
        lastHandledEventId = 0L
        stopRoomRealtime()
        update {
            copy(
                cpuActionHistory = emptyList(),
                cpuTurnAnimation = null,
                cpuTurnInProgress = false,
                yajuCutIn = null,
                ruleCutIns = emptyList(),
            )
        }
        applyGameState(game)
        startCpuTurnSequence()
    }

    fun createRoom() = launchAction {
        val playerName = validatedPlayerName()
        val response = api.createRoom(playerName)
        enterRoom(response.roomId)
    }

    fun createAllYajuRoom() = launchAction {
        val playerName = validatedPlayerName()
        val response = api.createAllYajuRoom(playerName)
        enterRoom(response.roomId)
    }

    fun joinRoom() = launchAction {
        val state = _uiState.value
        val playerName = validatedPlayerName()
        val roomId = state.roomIdInput.trim().lowercase()
        require(roomId.isNotBlank()) { "ルームIDを入力してください" }
        require(roomId.length == 8) { "ルームIDは8文字です" }

        val response = api.joinRoom(roomId, playerName)
        enterRoom(response.roomId)
    }

    fun refreshState(silent: Boolean = true) {
        if (_uiState.value.gameState?.isLocalCpuGame == true) return
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
        if (state.cpuTurnInProgress || !game.isMyTurn || index !in 0 until handSize) return

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

        val nextState = if (game.isLocalCpuGame) {
            if (game.isMySevenTransfer) {
                require(selectedCards.size == game.sevenTransfer.cardCount) {
                    "7渡しでは${game.sevenTransfer.cardCount}枚選択してください"
                }
            }
            localCpu.play(selectedCards)
        } else if (game.isMySevenTransfer) {
            require(selectedCards.size == game.sevenTransfer.cardCount) {
                "7渡しでは${game.sevenTransfer.cardCount}枚選択してください"
            }
            api.sevenTransfer(roomId, selectedCards)
        } else {
            api.play(roomId, selectedCards)
        }

        applyGameState(nextState)
        update { copy(selectedCardIndices = emptySet()) }
        if (game.isLocalCpuGame) {
            startCpuTurnSequence()
        }
    }

    fun pass() = launchAction {
        val game = _uiState.value.gameState ?: error("ゲーム状態を取得できません")
        val nextState = if (game.isLocalCpuGame) {
            localCpu.pass()
        } else {
            api.pass(requireRoomId())
        }
        applyGameState(nextState)
        update { copy(selectedCardIndices = emptySet()) }
        if (game.isLocalCpuGame) {
            startCpuTurnSequence()
        }
    }

    fun leaveRoom() = launchAction {
        val game = _uiState.value.gameState
        if (game?.isLocalCpuGame == true) {
            cpuTurnJob?.cancel()
            cpuTurnJob = null
            localCpu.close()
        } else {
            api.leave(requireRoomId())
        }
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
                cpuTurnInProgress = false,
                cpuTurnAnimation = null,
                cpuActionHistory = emptyList(),
                yajuCutIn = null,
                ruleCutIns = emptyList(),
                infoMessage = if (game?.isLocalCpuGame == true) "CPU戦を終了しました" else "部屋から退出しました",
            )
        }
    }

    /**
     * CPUの手番を1手ずつ進め、各アクションを人間が確認できる速度で演出する。
     * PLAYは約3秒、PASSと7渡しは少し短く表示する。
     */
    private fun startCpuTurnSequence() {
        cpuTurnJob?.cancel()
        cpuTurnJob = viewModelScope.launch {
            try {
                while (localCpu.hasCpuTurn()) {
                    // 野獣確定カットイン中はCPU演出を進めず、見せ場を潰さない。
                    while (_uiState.value.yajuCutIn != null || _uiState.value.ruleCutIns.isNotEmpty()) {
                        delay(100)
                    }

                    val visibleState = _uiState.value.gameState ?: break
                    val cpu = visibleState.currentPlayer ?: break

                    update {
                        copy(
                            cpuTurnInProgress = true,
                            cpuTurnAnimation = CpuTurnAnimation(
                                id = nextCpuAnimationId(),
                                playerId = cpu.playerId,
                                playerName = cpu.playerName,
                                type = CpuAnimationType.THINKING,
                            ),
                            selectedCardIndices = emptySet(),
                        )
                    }

                    // 一瞬の思考表示を入れて、手番が切り替わったことを認識しやすくする。
                    delay(450)

                    val result = localCpu.executeNextCpuTurn()
                    val animationType = when (result.type) {
                        LocalCpuActionType.PLAY -> CpuAnimationType.PLAY
                        LocalCpuActionType.PASS -> CpuAnimationType.PASS
                        LocalCpuActionType.SEVEN_TRANSFER -> CpuAnimationType.SEVEN_TRANSFER
                    }
                    val history = cpuActionText(
                        playerName = result.playerName,
                        type = animationType,
                        cards = result.cards,
                        targetPlayerName = result.targetPlayerName,
                    )

                    update {
                        copy(
                            cpuTurnAnimation = CpuTurnAnimation(
                                id = nextCpuAnimationId(),
                                playerId = result.playerId,
                                playerName = result.playerName,
                                type = animationType,
                                // 7渡しはUI状態へカード内容を渡さず、枚数だけ保持する。
                                cards = if (animationType == CpuAnimationType.SEVEN_TRANSFER) emptyList() else result.cards,
                                cardCount = result.cards.size,
                                targetPlayerName = result.targetPlayerName,
                            ),
                            cpuActionHistory = (listOf(history) + cpuActionHistory).take(6),
                        )
                    }

                    // カードを手元から場へ運ぶモーションを約3秒見せる。
                    delay(
                        when (animationType) {
                            CpuAnimationType.PLAY -> 3_000L
                            CpuAnimationType.PASS -> 1_500L
                            CpuAnimationType.SEVEN_TRANSFER -> 2_000L
                            CpuAnimationType.THINKING -> 450L
                        }
                    )

                    applyGameState(result.stateAfter)
                    update { copy(cpuTurnAnimation = null) }
                    delay(250)
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // 画面遷移・退出時のキャンセルは正常系。
            } catch (throwable: Throwable) {
                handleError(throwable)
            } finally {
                update { copy(cpuTurnInProgress = false, cpuTurnAnimation = null) }
            }
        }
    }

    private fun nextCpuAnimationId(): Long {
        cpuAnimationSequence += 1
        return cpuAnimationSequence
    }

    private fun cpuActionText(
        playerName: String,
        type: CpuAnimationType,
        cards: List<CardDto>,
        targetPlayerName: String?,
    ): String {
        return when (type) {
            CpuAnimationType.PLAY ->
                "$playerName：${cards.joinToString(" ") { it.label }} を出した"
            CpuAnimationType.PASS -> "$playerName：PASS"
            CpuAnimationType.SEVEN_TRANSFER ->
                "$playerName：${cards.size}枚を ${targetPlayerName ?: "隣"} へ7渡し"
            CpuAnimationType.THINKING -> "$playerName：思考中…"
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
        val generation = realtimeGeneration
        connectSocket(roomId, generation)

        // WebSocketが一時的に切れてもゲーム状態が止まって見えないよう、保険として5秒ごとに同期する。
        pollingJob = viewModelScope.launch {
            while (
                _uiState.value.roomId == roomId &&
                realtimeGeneration == generation
            ) {
                delay(5_000)
                runCatching { api.state(roomId) }
                    .onSuccess { state ->
                        if (
                            _uiState.value.roomId == roomId &&
                            realtimeGeneration == generation
                        ) {
                            applyGameState(state)
                        }
                    }
            }
        }
    }

    private fun connectSocket(roomId: String, generation: Long) {
        if (realtimeGeneration != generation || _uiState.value.roomId != roomId) return

        api.connectRoomSocket(
            roomId = roomId,
            onStateChanged = {
                viewModelScope.launch {
                    if (realtimeGeneration != generation || _uiState.value.roomId != roomId) {
                        return@launch
                    }
                    runCatching { api.state(roomId) }
                        .onSuccess { state ->
                            if (
                                realtimeGeneration == generation &&
                                _uiState.value.roomId == roomId
                            ) {
                                applyGameState(state)
                            }
                        }
                }
            },
            onConnectionChanged = { connected ->
                viewModelScope.launch {
                    if (realtimeGeneration != generation || _uiState.value.roomId != roomId) {
                        return@launch
                    }

                    update { copy(socketConnected = connected) }

                    if (!connected) {
                        reconnectJob?.cancel()
                        reconnectJob = launch {
                            delay(3_000)
                            if (
                                realtimeGeneration == generation &&
                                _uiState.value.roomId == roomId
                            ) {
                                connectSocket(roomId, generation)
                            }
                        }
                    }
                }
            },
        )
    }

    private fun stopRoomRealtime() {
        // 先に世代を進めることで、closeに伴う旧WebSocketのcallbackを無効化する。
        realtimeGeneration += 1
        pollingJob?.cancel()
        pollingJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        api.closeRoomSocket()
        update { copy(socketConnected = false) }
    }

    private fun applyGameState(newState: GameStateDto) {
        processGameEvents(newState)
        val previous = _uiState.value.gameState

        if (newState.finished && previous?.finished != true) {
            val self = newState.selfPlayer
            val rank = self?.rank
            if (self != null && rank != null) {
                val uid = _uiState.value.firebaseUid
                if (uid != null && _uiState.value.cloudReady && !newState.isLocalCpuGame) {
                    // オンライン戦績はサーバーが確定した順位だけをFirestoreに記録する。
                    recordCloudResult(newState.roomId)
                } else {
                    // 完全オフラインCPU戦は署名付き結果がないため端末へ記録し、クラウド報酬には混ぜない。
                    progressRepository.recordMatch(
                        matchId = newState.roomId,
                        rank = rank,
                        playerCount = newState.players.size,
                        yajuStatus = self.yajuStatus,
                        challenge = newState.gameMode == "YAJU_CHALLENGE"
                            || newState.gameMode == "MULTI_YAJU_CHALLENGE",
                    )?.let { result ->
                        update {
                            copy(
                                progress = if (uid == null) result.progress else progress,
                                lastEarnedExp = result.earnedExp,
                                infoMessage = if (uid == null) "+${result.earnedExp} EXP を獲得"
                                    else "オフライン戦績は端末に保存しました（クラウド未同期）",
                            )
                        }
                    }
                }
            }
        }

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

    /** Firestore書込み失敗時に結果画面から再送できる。roomId単位で二重同時送信を防ぐ。 */
    fun retryCloudResult() {
        val game = _uiState.value.gameState ?: return
        if (game.finished && !game.isLocalCpuGame) recordCloudResult(game.roomId)
    }

    private fun recordCloudResult(roomId: String) {
        val uid = _uiState.value.firebaseUid ?: return
        if (!_uiState.value.cloudReady || !cloudResultAttempts.add(roomId)) return
        update { copy(cloudResultStatus = "SAVING", lastEarnedExp = 0) }
        viewModelScope.launch {
            runCatching { withCloudRetry { api.cloudRecordMatch(roomId) } }
                .onSuccess { response ->
                    if (_uiState.value.firebaseUid == uid) {
                        val oldExp = _uiState.value.progress.totalExp
                        applyCloudProgress(response.getJSONObject("progress"), uid)
                        val earned = (_uiState.value.progress.totalExp - oldExp).coerceAtLeast(0)
                        update {
                            copy(
                                cloudResultStatus = "SAVED",
                                lastEarnedExp = earned,
                                infoMessage = if (earned > 0) "+$earned EXP をクラウドに保存しました"
                                    else "クラウドの戦績保存を確認しました",
                            )
                        }
                    }
                }
                .onFailure { throwable ->
                    cloudResultAttempts.remove(roomId)
                    if (_uiState.value.firebaseUid == uid) {
                        update { copy(cloudResultStatus = "FAILED") }
                        handleError(throwable)
                    }
                }
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

        if (unseen.isEmpty()) return

        val yajuAvailable = unseen.filter { it.type == "YAJU_AVAILABLE" }
        if (yajuAvailable.isNotEmpty()) {
            /*
             * 初期配牌で複数人が同時に野獣対象になった場合も、
             * 1回のカットインにまとめて全員を表示する。
             */
            update {
                copy(
                    yajuCutIn = YajuCutIn(
                        id = yajuAvailable.maxOf { it.id },
                        playerIds = yajuAvailable.map { it.playerId },
                        playerNames = yajuAvailable.map { it.playerName },
                    )
                )
            }
            _audioCues.tryEmit(GameAudioCue.YAJU_AVAILABLE)
        }

        if (unseen.any { it.type == "YAJU_SUCCESS" }) {
            _audioCues.tryEmit(GameAudioCue.YAJU_SUCCESS)
        }

        val newRuleCutIns = unseen.mapNotNull { event ->
            val type = when (event.type) {
                "EIGHT_CUT" -> RuleCutInType.EIGHT_CUT
                "JACK_BACK" -> RuleCutInType.JACK_BACK
                "EARLY_SHOT" -> RuleCutInType.EARLY_SHOT
                else -> null
            } ?: return@mapNotNull null
            RuleCutIn(event.id, type, event.playerId, event.playerName)
        }
        if (newRuleCutIns.isNotEmpty()) {
            update { copy(ruleCutIns = ruleCutIns + newRuleCutIns) }
        }

        lastHandledEventId = maxOf(lastHandledEventId, unseen.maxOf { it.id })
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
        val state = _uiState.value
        require(state.progress.profileConfigured) { "プロフィール設定を完了してください" }
        val playerName = state.progress.displayName.trim()
        require(playerName.isNotBlank()) { "プレイヤー名をプロフィールで設定してください" }
        return playerName
    }

    private fun requireRoomId(): String =
        _uiState.value.roomId ?: error("部屋に参加していません")

    private fun normalizeServerUrl(raw: String): String {
        var result = raw.trim().trimEnd('/')
        require(result.isNotBlank()) { "サーバーURLを入力してください" }
        if (!result.startsWith("http://") && !result.startsWith("https://")) {
            result = "https://$result"
        }
        return result
    }

    private inline fun update(transform: DaifugoUiState.() -> DaifugoUiState) {
        _uiState.value = _uiState.value.transform()
    }

    override fun onCleared() {
        cpuTurnJob?.cancel()
        cpuTurnJob = null
        stopRoomRealtime()
        super.onCleared()
    }

    companion object {
        /**
         * 現在の公開PC/Android共通ゲームサーバー。
         * 公開環境は取得済みドメインへのHTTPS接続を標準とする。
         */
        const val DEFAULT_PUBLIC_SERVER_URL = "https://yaju-daifugou.com"
    }
}
