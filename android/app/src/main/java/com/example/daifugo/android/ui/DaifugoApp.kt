package com.example.daifugo.android.ui

import android.app.Activity
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daifugo.android.CpuAnimationType
import com.example.daifugo.android.CpuTurnAnimation
import com.example.daifugo.android.YajuCutIn
import com.example.daifugo.android.RuleCutIn
import com.example.daifugo.android.RuleCutInType
import com.example.daifugo.android.DaifugoScreen
import com.example.daifugo.android.audio.YajuAudioPlayer
import com.example.daifugo.android.DaifugoUiState
import com.example.daifugo.android.DaifugoViewModel
import com.example.daifugo.android.cosmetics.CosmeticCatalog
import com.example.daifugo.android.cosmetics.CosmeticCategory
import com.example.daifugo.android.cosmetics.CosmeticItemDefinition
import com.example.daifugo.android.data.CardDto
import com.example.daifugo.android.data.GameStateDto
import com.example.daifugo.android.data.PlayerDto
import com.example.daifugo.android.playgames.PlayGamesAccount
import com.example.daifugo.android.playgames.PlayGamesAccountManager
import com.example.daifugo.android.progression.DailyMissionRules
import com.example.daifugo.android.progression.PlayerProgress
import com.example.daifugo.android.ui.theme.CasinoGold
import com.example.daifugo.android.ui.theme.CasinoGreen
import com.example.daifugo.android.ui.theme.CasinoGreenDark
import com.example.daifugo.android.ui.theme.Danger
import com.example.daifugo.android.ui.theme.SoftGold
import com.example.daifugo.android.ui.theme.SoftGreen
import com.example.daifugo.game.config.GameLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun DaifugoApp(viewModel: DaifugoViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val playGamesAccountManager = remember(activity) {
        activity?.let(::PlayGamesAccountManager)
    }

    fun applyPlayGamesAccount(account: PlayGamesAccount) {
        viewModel.updatePlayGamesAccount(
            configured = account.configured,
            authenticated = account.authenticated,
            playerId = account.playerId,
            displayName = account.displayName,
            errorMessage = account.errorMessage,
        )
    }

    LaunchedEffect(playGamesAccountManager) {
        playGamesAccountManager?.refresh(::applyPlayGamesAccount)
            ?: viewModel.updatePlayGamesAccount(
                configured = false,
                authenticated = false,
                playerId = null,
                displayName = null,
            )
    }

    /*
     * 全端末共有の野獣イベントを音声へ変換する。
     * 実音声はres/rawへ後から置くだけでよく、未配置時は無音で動作する。
     */
    LaunchedEffect(viewModel) {
        viewModel.audioCues.collect { cue ->
            YajuAudioPlayer.play(context, cue)
        }
    }

    /*
     * 野獣対象者（ACTIVE / EIGHT_PLAYED）が1人でもいる間は専用BGMをループする。
     * 初期配牌で条件を満たした場合も、7渡しで途中から条件を満たした場合も
     * GameState更新直後に再生状態へ反映される。
     */
    val yajuBgmActive = state.gameState?.let { game ->
        !game.finished && game.players.any { player ->
            player.yajuStatus == "ACTIVE" || player.yajuStatus == "EIGHT_PLAYED"
        }
    } == true

    val menuBgmActive = state.screen != DaifugoScreen.GAME &&
        state.screen != DaifugoScreen.RESULT

    LaunchedEffect(menuBgmActive) {
        if (menuBgmActive) {
            YajuAudioPlayer.startMenuBgm(context)
        } else {
            YajuAudioPlayer.stopMenuBgm()
        }
    }

    LaunchedEffect(yajuBgmActive) {
        if (yajuBgmActive) {
            YajuAudioPlayer.startBgm(context)
        } else {
            YajuAudioPlayer.stopBgm()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            YajuAudioPlayer.stopAllBgm()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (state.screen == DaifugoScreen.TITLE) {
            TitleScreen(onStart = viewModel::enterMainMenu)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                if (
                    state.screen !in setOf(
                        DaifugoScreen.ACCOUNT_SETUP,
                        DaifugoScreen.MAIN_MENU,
                        DaifugoScreen.CPU_SETUP,
                        DaifugoScreen.CHALLENGE_SETUP,
                        DaifugoScreen.MISSION,
                        DaifugoScreen.PROFILE,
                        DaifugoScreen.COLLECTION,
                        DaifugoScreen.PRESENT_BOX,
                        DaifugoScreen.SETTINGS,
                        DaifugoScreen.GAME,
                        DaifugoScreen.RESULT,
                    )
                ) {
                    AppHeader(state)
                }
                MessageStrip(
                    state = state,
                    onDismiss = viewModel::clearMessage,
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    AnimatedContent(
                        targetState = state.screen,
                        transitionSpec = {
                            (fadeIn(tween(260)) + scaleIn(initialScale = 0.965f, animationSpec = tween(320)))
                                .togetherWith(fadeOut(tween(150)))
                        },
                        label = "dvdCasinoScreenTransition",
                    ) { screen ->
                        when (screen) {
                            DaifugoScreen.TITLE -> Unit
                            DaifugoScreen.ACCOUNT_SETUP -> AccountSetupScreen(
                                state = state,
                                viewModel = viewModel,
                                onPlayGamesSignIn = {
                                    playGamesAccountManager?.signIn(::applyPlayGamesAccount)
                                },
                            )
                            DaifugoScreen.LOGIN -> LoginScreen(state, viewModel)
                            DaifugoScreen.MAIN_MENU -> MainMenuScreen(
                                state = state,
                                viewModel = viewModel,
                                onPlayGamesSignIn = {
                                    playGamesAccountManager?.signIn(::applyPlayGamesAccount)
                                },
                            )
                            DaifugoScreen.MULTIPLAYER -> LobbyScreen(state, viewModel)
                            DaifugoScreen.CPU_SETUP -> CpuSetupScreen(state, viewModel)
                            DaifugoScreen.CHALLENGE_SETUP -> ChallengeSetupScreen(state, viewModel)
                            DaifugoScreen.MISSION -> MissionScreen(state, viewModel)
                            DaifugoScreen.PROFILE -> ProfileScreen(
                                state = state,
                                viewModel = viewModel,
                                onPlayGamesSignIn = {
                                    playGamesAccountManager?.signIn(::applyPlayGamesAccount)
                                },
                            )
                            DaifugoScreen.COLLECTION -> CollectionScreen(state, viewModel)
                            DaifugoScreen.PRESENT_BOX -> PresentBoxScreen(state, viewModel)
                            DaifugoScreen.SETTINGS -> MenuPlaceholderScreen(
                                title = "SYSTEM",
                                subtitle = "設定",
                                description = "BGM・SE・演出などを設定する。",
                                onBack = viewModel::backToMenu,
                            )
                            DaifugoScreen.ROOM -> RoomScreen(state, viewModel)
                            DaifugoScreen.GAME -> GameScreen(state, viewModel)
                            DaifugoScreen.RESULT -> ResultScreen(state, viewModel)
                        }
                    }

                    if (state.loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Card(shape = RoundedCornerShape(18.dp)) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(26.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        if (
                                            state.gameState?.isLocalCpuGame == true ||
                                            state.screen == DaifugoScreen.CPU_SETUP ||
                                            state.screen == DaifugoScreen.CHALLENGE_SETUP
                                        ) "処理中…" else "通信中…"
                                    )
                                }
                            }
                        }
                    }

                    state.yajuCutIn?.let { cutIn ->
                        YajuCutInOverlay(
                            cutIn = cutIn,
                            game = state.gameState,
                            onDismiss = viewModel::clearYajuCutIn,
                        )
                    }

                    state.ruleCutIns.firstOrNull()?.let { cutIn ->
                        RuleCutInOverlay(
                            cutIn = cutIn,
                            game = state.gameState,
                            onDismiss = viewModel::clearRuleCutIn,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 起動直後のタイトル画面。
 *
 * android/app/src/main/res/drawable/title_screen.(png|jpg|webp)
 * を配置すると全面背景として使用する。未配置でもビルド可能で、
 * 黒背景のフォールバック表示になる。
 */
@Composable
private fun TitleScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val titleImageId = remember {
        context.resources.getIdentifier(
            "title_screen",
            "drawable",
            context.packageName,
        )
    }
    val tapAlpha = remember { Animatable(0.35f) }

    LaunchedEffect(Unit) {
        while (true) {
            tapAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900),
            )
            tapAlpha.animateTo(
                targetValue = 0.35f,
                animationSpec = tween(durationMillis = 900),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onStart),
    ) {
        if (titleImageId != 0) {
            Image(
                painter = painterResource(titleImageId),
                contentDescription = "野獣大富豪 タイトル",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = "野獣大富豪",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
            )
        }

        // 背景写真の明暗に左右されずTAP TO STARTを読めるよう下部だけ薄く暗転する。
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f),
                        )
                    )
                )
        )

        Text(
            text = "TAP TO START",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 44.dp)
                .graphicsLayer(alpha = tapAlpha.value),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp,
        )
    }
}

@Composable
private fun AppHeader(state: DaifugoUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("♛", color = CasinoGold, fontSize = 28.sp)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Daifugo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = CasinoGreenDark,
            )
            Text(
                "ONLINE CARD GAME · v0.4.4 8-PLAYER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.roomId != null) {
            val local = state.gameState?.isLocalCpuGame == true
            ConnectionPill(connected = state.socketConnected, local = local)
        }
    }
    HorizontalDivider(color = CasinoGold.copy(alpha = 0.35f))
}

@Composable
private fun ConnectionPill(connected: Boolean, local: Boolean = false) {
    Surface(
        color = if (connected || local) SoftGreen else SoftGold,
        shape = RoundedCornerShape(100.dp),
    ) {
        Text(
            when {
                local -> "● LOCAL"
                connected -> "● LIVE"
                else -> "○ SYNC"
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (connected || local) CasinoGreen else CasinoGold,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MessageStrip(state: DaifugoUiState, onDismiss: () -> Unit) {
    val text = state.errorMessage ?: state.infoMessage ?: return
    val isError = state.errorMessage != null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else SoftGreen,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else CasinoGreenDark,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AccountSetupScreen(
    state: DaifugoUiState,
    viewModel: DaifugoViewModel,
    onPlayGamesSignIn: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020504),
                        Color(0xFF082319),
                        Color(0xFF030504),
                    )
                )
            ),
    ) {
        PremiumCasinoBackdrop(accent = CasinoGreen)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "FIRST ENTRY",
                    color = CasinoGold,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    fontSize = 11.sp,
                )
                Text(
                    "アカウント設定",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "プレイヤー名を設定すればすぐ遊べます。Google Play Games連携は任意です。",
                    color = Color.White.copy(alpha = 0.60f),
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Black.copy(alpha = 0.36f),
                    border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.42f)),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            "GOOGLE PLAY GAMES",
                            color = CasinoGold,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 10.sp,
                        )
                        Text(
                            when {
                                state.playGamesSignedIn -> "✓ Play Games連携済み（任意）"
                                state.playGamesConfigured -> "未連携でもそのままプレイできます"
                                else -> "Play Games未設定でもプレイできます"
                            },
                            color = if (state.playGamesSignedIn) Color(0xFF78D6A1) else Color.White,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            "Android向けの任意連携です。PC版とのクロスプレイにGoogleアカウントは不要です。",
                            color = Color.White.copy(alpha = 0.48f),
                            fontSize = 11.sp,
                        )
                        if (!state.playGamesSignedIn && state.playGamesConfigured) {
                            PremiumStartButton(
                                label = "PLAY GAMESを連携（任意）",
                                onClick = onPlayGamesSignIn,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "PLAYER PROFILE",
                    color = CasinoGold,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(8.dp))
                PremiumPlayerNameField(
                    value = state.playerName,
                    onValueChange = viewModel::setPlayerName,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "この名前がSOLO・CHALLENGE・マルチ対戦で使用されます。後からプロフィールで変更できます。",
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 11.sp,
                )
            }

            item {
                PremiumStartButton(
                    label = "この名前でゲームを始める  ▶",
                    onClick = viewModel::completeAccountSetup,
                )
            }

            item {
                Text(
                    "Google Play Gamesなしでも全ゲームモードを利用できます。",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.42f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "マルチプレイ接続",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                "オンライン対戦をするときだけSpring Bootサーバーへ接続します。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            TextButton(onClick = viewModel::backToMenu) { Text("← メインメニューへ") }
        }
        item {
            CasinoPanel(title = "接続先") {
                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = viewModel::setServerUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("サーバーURL") },
                    placeholder = { Text("http://192.168.1.10:8080") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText = { Text("実機ではサーバーPCのLAN内IPを指定") },
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::setPassword,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("共通パスワード") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::login,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text("ログイン", fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            CasinoPanel(title = "v0.4.4 8-PLAYER") {
                FeatureLine("♣", "2〜8人オンライン対戦")
                FeatureLine("⚡", "WebSocketリアルタイム更新")
                FeatureLine("♛", "革命・Jバック・8切り・7渡し・野獣ルール")
                FeatureLine("🔒", "手札判定と本人確認はサーバー側")
            }
        }
    }
}

private enum class CasinoMenuItem(
    val symbol: String,
    val label: String,
    val english: String,
    val description: String,
) {
    SOLO("♠", "ひとりでいく", "SOLO PLAY", "CPUと自由なルールで対戦する"),
    MULTI("♦", "みんなでいく", "MULTIPLAYER", "オンラインの同じ卓へ参加する"),
    CHALLENGE("8 + 10", "チャレンジモード", "YAJU CHALLENGE", "野獣状態でゲームを開始する"),
    MISSION("✓", "ミッション", "MISSION", "遊んで条件を達成し、アイテム報酬を獲得する"),
    PROFILE("♛", "プロフィール", "PROFILE", "戦績・レベル・アバターを確認する"),
    COLLECTION("▣", "コレクション", "COLLECTION", "獲得したカスタマイズアイテムを見る"),
    PRESENT("◆", "プレゼントボックス", "PRESENT", "管理者配布・報酬を受け取る"),
    SETTINGS("⚙", "設定", "SYSTEM", "BGM・SE・演出などを調整する"),
}

@Composable
private fun MainMenuScreen(
    state: DaifugoUiState,
    viewModel: DaifugoViewModel,
    onPlayGamesSignIn: () -> Unit,
) {
    var selected by remember { mutableStateOf(CasinoMenuItem.SOLO) }
    val missionBadge = DailyMissionRules.views(state.progress.daily).count { it.claimable }
    val presentBadge = state.progress.presents.size

    fun enterSelected() {
        when (selected) {
            CasinoMenuItem.SOLO -> viewModel.openCpuSetup()
            CasinoMenuItem.MULTI -> viewModel.openMultiplayer()
            CasinoMenuItem.CHALLENGE -> viewModel.openChallengeSetup()
            CasinoMenuItem.MISSION -> viewModel.openMission()
            CasinoMenuItem.PROFILE -> viewModel.openProfile()
            CasinoMenuItem.COLLECTION -> viewModel.openCollection()
            CasinoMenuItem.PRESENT -> viewModel.openPresentBox()
            CasinoMenuItem.SETTINGS -> viewModel.openSettings()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020504),
                        Color(0xFF071B13),
                        Color(0xFF020303),
                    )
                )
            ),
    ) {
        PremiumCasinoBackdrop(
            accent = if (selected == CasinoMenuItem.CHALLENGE) Color(0xFF8D2429) else CasinoGreen,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 30.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "YAJU DAIFUGO",
                            color = CasinoGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                        )
                        Text(
                            "ROYAL CASINO",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                        )
                        Text(
                            "WELCOME TO THE NIGHT TABLE",
                            color = Color.White.copy(alpha = 0.42f),
                            fontSize = 9.sp,
                            letterSpacing = 2.sp,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0B0E0C).copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.72f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("RANK", color = Color.White.copy(alpha = 0.45f), fontSize = 8.sp, letterSpacing = 2.sp)
                            Text(
                                "Lv." + state.progress.level,
                                color = CasinoGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }

            item {
                CasinoSelectionPreview(
                    selected = selected,
                    onEnter = ::enterSelected,
                )
            }

            item { PremiumSectionTitle("MAIN PLAY", "CHOOSE YOUR TABLE") }

            item {
                CasinoMenuTile(
                    item = CasinoMenuItem.SOLO,
                    selected = selected == CasinoMenuItem.SOLO,
                    onSelect = { selected = CasinoMenuItem.SOLO },
                )
            }
            item {
                CasinoMenuTile(
                    item = CasinoMenuItem.MULTI,
                    selected = selected == CasinoMenuItem.MULTI,
                    onSelect = { selected = CasinoMenuItem.MULTI },
                )
            }
            item {
                CasinoMenuTile(
                    item = CasinoMenuItem.CHALLENGE,
                    selected = selected == CasinoMenuItem.CHALLENGE,
                    onSelect = { selected = CasinoMenuItem.CHALLENGE },
                    accent = Color(0xFF9D2C2C),
                )
            }

            item { PremiumSectionTitle("CASINO FLOOR", "REWARDS & COLLECTION") }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CasinoMenuTile(
                        item = CasinoMenuItem.MISSION,
                        selected = selected == CasinoMenuItem.MISSION,
                        onSelect = { selected = CasinoMenuItem.MISSION },
                        badge = missionBadge.takeIf { it > 0 },
                        modifier = Modifier.weight(1f),
                        compact = true,
                    )
                    CasinoMenuTile(
                        item = CasinoMenuItem.PROFILE,
                        selected = selected == CasinoMenuItem.PROFILE,
                        onSelect = { selected = CasinoMenuItem.PROFILE },
                        modifier = Modifier.weight(1f),
                        compact = true,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CasinoMenuTile(
                        item = CasinoMenuItem.COLLECTION,
                        selected = selected == CasinoMenuItem.COLLECTION,
                        onSelect = { selected = CasinoMenuItem.COLLECTION },
                        modifier = Modifier.weight(1f),
                        compact = true,
                    )
                    CasinoMenuTile(
                        item = CasinoMenuItem.PRESENT,
                        selected = selected == CasinoMenuItem.PRESENT,
                        onSelect = { selected = CasinoMenuItem.PRESENT },
                        badge = presentBadge.takeIf { it > 0 },
                        modifier = Modifier.weight(1f),
                        compact = true,
                    )
                }
            }

            item {
                CasinoMenuTile(
                    item = CasinoMenuItem.SETTINGS,
                    selected = selected == CasinoMenuItem.SETTINGS,
                    onSelect = { selected = CasinoMenuItem.SETTINGS },
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun CasinoSelectionPreview(
    selected: CasinoMenuItem,
    onEnter: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "heroGlow")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroGlowAlpha",
    )
    val challenge = selected == CasinoMenuItem.CHALLENGE
    val accent = if (challenge) Color(0xFFD94A4A) else CasinoGold

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (challenge) Color(0xFF3A080B) else Color(0xFF09271B),
                        Color(0xFF090B0A),
                        Color(0xFF020303),
                    )
                ),
                RoundedCornerShape(26.dp),
            )
            .clickable(onClick = onEnter),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = accent.copy(alpha = 0.09f + glow * 0.07f),
                radius = size.minDimension * 0.72f,
                center = Offset(size.width * 0.92f, size.height * 0.20f),
            )
            drawLine(
                color = accent.copy(alpha = 0.28f),
                start = Offset(size.width * 0.05f, size.height * 0.07f),
                end = Offset(size.width * 0.62f, size.height * 0.07f),
                strokeWidth = 2f,
            )
        }

        Text(
            selected.symbol,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
                .graphicsLayer {
                    alpha = 0.13f + glow * 0.09f
                    scaleX = 1f + glow * 0.05f
                    scaleY = 1f + glow * 0.05f
                },
            color = accent,
            fontSize = if (selected == CasinoMenuItem.CHALLENGE) 54.sp else 92.sp,
            fontWeight = FontWeight.Black,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "SELECTED MODE",
                    color = accent,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    selected.label,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    selected.english,
                    color = Color.White.copy(alpha = 0.46f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    selected.description,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                )
            }

            PremiumStartButton(
                label = "ENTER  ▶",
                accent = accent,
                onClick = onEnter,
            )
        }
    }
}

@Composable
private fun CasinoMenuTile(
    item: CasinoMenuItem,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int? = null,
    compact: Boolean = false,
    accent: Color = CasinoGreenDark,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.025f else 1f,
        animationSpec = tween(180),
        label = "casinoMenuScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.76f,
        animationSpec = tween(180),
        label = "casinoMenuAlpha",
    )
    val tileAccent = if (item == CasinoMenuItem.CHALLENGE) Color(0xFFE14A4A) else CasinoGold

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(
                Brush.horizontalGradient(
                    if (selected) {
                        listOf(
                            accent.copy(alpha = 0.94f),
                            Color(0xFF101512),
                            Color(0xFF050706),
                        )
                    } else {
                        listOf(
                            Color(0xFF111714).copy(alpha = 0.92f),
                            Color(0xFF070908).copy(alpha = 0.94f),
                        )
                    }
                ),
                RoundedCornerShape(if (compact) 16.dp else 20.dp),
            )
            .clickable(onClick = onSelect),
    ) {
        if (selected) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    color = tileAccent.copy(alpha = 0.75f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.72f, 0f),
                    strokeWidth = 5f,
                )
                drawCircle(
                    color = tileAccent.copy(alpha = 0.10f),
                    radius = size.height * 1.1f,
                    center = Offset(size.width * 0.06f, size.height * 0.5f),
                )
            }
        }

        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 13.dp else 15.dp,
                vertical = if (compact) 12.dp else 15.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(if (compact) 38.dp else 52.dp),
                shape = RoundedCornerShape(if (compact) 12.dp else 16.dp),
                color = if (selected) tileAccent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
                border = BorderStroke(
                    1.dp,
                    if (selected) tileAccent.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.10f),
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        item.symbol,
                        color = if (selected) tileAccent else Color.White.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Black,
                        fontSize = if (compact) 15.sp else 19.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.width(if (compact) 9.dp else 13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.label,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = if (compact) 13.sp else 17.sp,
                )
                if (!compact) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.english,
                        color = tileAccent.copy(alpha = if (selected) 0.82f else 0.48f),
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                    )
                }
            }
            badge?.let {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFD12D39),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                ) {
                    Text(
                        it.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.width(7.dp))
            }
            if (!compact) {
                Text(
                    "›",
                    color = if (selected) tileAccent else Color.White.copy(alpha = 0.28f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                )
            }
        }
    }
}

@Composable
private fun PresentBoxScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    val presents = state.progress.presents

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF020504), Color(0xFF082319), Color(0xFF030504))
                )
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "VAULT DELIVERY",
                        color = CasinoGold,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        "プレゼントボックス",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Text(
                        "ミッション報酬・管理者配布を保管",
                        color = Color.White.copy(alpha = 0.52f),
                    )
                }
                TextButton(onClick = viewModel::backToMenu) { Text("‹ BACK", color = Color.White.copy(alpha = 0.72f)) }
            }
        }

        if (presents.isNotEmpty()) {
            item {
                Button(
                    onClick = viewModel::receiveAllPresents,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("すべて受け取る（${presents.size}件）", fontWeight = FontWeight.Black)
                }
            }
        }

        if (presents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10261C)),
                    border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.35f)),
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("◇", color = CasinoGold, fontSize = 36.sp)
                        Text("EMPTY", color = CasinoGold, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text(
                            "現在受け取れるプレゼントはありません。",
                            color = Color.White.copy(alpha = 0.68f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            presents.forEach { present ->
                item(key = present.id) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0D1712),
                            contentColor = Color.White,
                        ),
                        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.18f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    color = CasinoGold.copy(alpha = 0.14f),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("◆", color = CasinoGold, fontSize = 20.sp)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(present.title, fontWeight = FontWeight.Black)
                                    Text(
                                        present.source,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                contentColor = Color.White,
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        present.itemName,
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "×${present.quantity}",
                                        color = CasinoGold,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.receivePresent(present.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("受け取る")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "管理者配布も同じプレゼント形式で受信できる設計です。",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CollectionScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    var category by remember { mutableStateOf(CosmeticCategory.AVATAR) }
    val items = CosmeticCatalog.items.filter { it.category == category }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF020504), Color(0xFF082319), Color(0xFF030504))
                )
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "SHOW CASE",
                        color = CasinoGold,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        "コレクション",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Text(
                        "獲得したアイテムを装備できます",
                        color = Color.White.copy(alpha = 0.52f),
                    )
                }
                TextButton(onClick = viewModel::backToMenu) { Text("‹ BACK", color = Color.White.copy(alpha = 0.72f)) }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(CosmeticCategory.entries) { _, value ->
                    val selected = value == category
                    if (selected) {
                        Button(onClick = { category = value }) {
                            Text(cosmeticCategoryLabel(value))
                        }
                    } else {
                        OutlinedButton(onClick = { category = value }) {
                            Text(cosmeticCategoryLabel(value))
                        }
                    }
                }
            }
        }

        if (items.isEmpty()) {
            item {
                CasinoPanel(title = "EMPTY") {
                    Text("このカテゴリのアイテムはまだ登録されていません。")
                }
            }
        } else {
            items.forEach { item ->
                item(key = item.id) {
                    CollectionItemCard(
                        item = item,
                        ownedCount = state.progress.inventory[item.id] ?: 0,
                        equipped = state.progress.isEquipped(item),
                        onEquip = { viewModel.equipCosmetic(item.id) },
                    )
                }
            }
        }

        item {
            val totalOwned = CosmeticCatalog.items.count {
                (state.progress.inventory[it.id] ?: 0) > 0
            }
            Text(
                "COLLECTED  $totalOwned / ${CosmeticCatalog.items.size}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = CasinoGold.copy(alpha = 0.75f),
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Composable
private fun CollectionItemCard(
    item: CosmeticItemDefinition,
    ownedCount: Int,
    equipped: Boolean,
    onEquip: () -> Unit,
) {
    val owned = ownedCount > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (owned) Color(0xFF0D1712) else Color(0xFF070A08),
            contentColor = Color.White,
        ),
        border = BorderStroke(
            if (equipped) 2.dp else 1.dp,
            if (equipped) CasinoGold else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (owned) CasinoGold.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (owned) cosmeticCategorySymbol(item.category) else "🔒",
                        fontSize = 22.sp,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.displayName,
                    fontWeight = FontWeight.Black,
                    color = if (owned) Color.White else Color.White.copy(alpha = 0.35f),
                )
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = if (owned) 0.58f else 0.28f),
                )
                if (ownedCount > 1) {
                    Text(
                        "所持 ×$ownedCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = CasinoGold,
                    )
                }
            }
            when {
                equipped -> Text("装備中", color = CasinoGreen, fontWeight = FontWeight.Black)
                owned -> OutlinedButton(onClick = onEquip) { Text("装備") }
                else -> Text("未所持", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun cosmeticCategoryLabel(category: CosmeticCategory): String = when (category) {
    CosmeticCategory.AVATAR -> "アバター"
    CosmeticCategory.FRAME -> "フレーム"
    CosmeticCategory.CARD_SKIN -> "トランプ"
    CosmeticCategory.EFFECT -> "エフェクト"
}

private fun cosmeticCategorySymbol(category: CosmeticCategory): String = when (category) {
    CosmeticCategory.AVATAR -> "♛"
    CosmeticCategory.FRAME -> "◇"
    CosmeticCategory.CARD_SKIN -> "♠"
    CosmeticCategory.EFFECT -> "✦"
}

private fun PlayerProgress.isEquipped(item: CosmeticItemDefinition): Boolean = when (item.category) {
    CosmeticCategory.AVATAR -> equippedAvatarId == item.id
    CosmeticCategory.FRAME -> equippedFrameId == item.id
    CosmeticCategory.CARD_SKIN -> equippedCardSkinId == item.id
    CosmeticCategory.EFFECT -> equippedEffectId == item.id
}

@Composable
private fun MissionScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    val missions = DailyMissionRules.views(state.progress.daily)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF020504), Color(0xFF082319), Color(0xFF030504))
                )
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "MISSION BOARD",
                        color = CasinoGold,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        "ミッション",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Text(
                        "達成報酬はプレゼントボックスへ送られます",
                        color = Color.White.copy(alpha = 0.52f),
                    )
                }
                TextButton(onClick = viewModel::backToMenu) { Text("‹ BACK", color = Color.White.copy(alpha = 0.72f)) }
            }
        }

        item {
            Text(
                "DAILY",
                color = CasinoGold,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
        }

        missions.forEach { mission ->
            item(key = mission.id) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mission.completed) Color(0xFF193322) else Color(0xFF0D1712),
                        contentColor = Color.White,
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (mission.completed) CasinoGold.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                mission.title,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                "${mission.progress.coerceAtMost(mission.target)} / ${mission.target}",
                                color = CasinoGold,
                                fontWeight = FontWeight.Black,
                            )
                        }

                        androidx.compose.material3.LinearProgressIndicator(
                            progress = {
                                (mission.progress.toFloat() / mission.target.toFloat())
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.06f),
                        ) {
                            Text(
                                "報酬  ◆ ${mission.rewardItemName} ×${mission.rewardQuantity}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        when {
                            mission.claimed -> Text(
                                "プレゼントBOXへ送付済み",
                                color = CasinoGreen,
                                fontWeight = FontWeight.Bold,
                            )
                            mission.claimable -> Button(
                                onClick = { viewModel.claimDailyMission(mission.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("報酬を受け取る")
                            }
                            else -> Text(
                                "進行中",
                                color = Color.White.copy(alpha = 0.48f),
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "日付が変わるとデイリー進捗はリセットされます。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileScreen(
    state: DaifugoUiState,
    viewModel: DaifugoViewModel,
    onPlayGamesSignIn: () -> Unit,
) {
    val progress = state.progress
    val avatarName = progress.equippedAvatarId?.let(CosmeticCatalog::find)?.displayName ?: "未設定"
    val frameName = progress.equippedFrameId?.let(CosmeticCatalog::find)?.displayName ?: "未設定"
    val cardName = progress.equippedCardSkinId?.let(CosmeticCatalog::find)?.displayName ?: "未設定"
    val effectName = progress.equippedEffectId?.let(CosmeticCatalog::find)?.displayName ?: "未設定"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF020504), Color(0xFF082319), Color(0xFF030504))
                )
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MEMBERS CARD", color = CasinoGold, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("プロフィール", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                TextButton(onClick = viewModel::backToMenu) {
                    Text("‹ BACK", color = Color.White.copy(alpha = 0.72f))
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF10261C),
                border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.55f)),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = CasinoGold.copy(alpha = 0.14f),
                            border = BorderStroke(2.dp, CasinoGold),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("♛", color = CasinoGold, fontSize = 28.sp)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(progress.displayName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text(
                                if (state.playGamesSignedIn) "Google Play Games 連携済み" else "未連携（任意）",
                                color = if (state.playGamesSignedIn) Color(0xFF78D6A1) else Color.White.copy(alpha = 0.56f),
                            )
                        }
                        Text("Lv." + progress.level, color = CasinoGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }

                    val expRatio = if (progress.expToNextLevel <= 0) 0f
                    else progress.expIntoLevel.toFloat() / progress.expToNextLevel
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { expRatio.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        progress.expIntoLevel.toString() + " / " + progress.expToNextLevel + " EXP",
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        item {
            PremiumCasinoInfoPanel("PLAYER NAME") {
                PremiumPlayerNameField(
                    value = state.playerName,
                    onValueChange = viewModel::setPlayerName,
                )
                Spacer(Modifier.height(10.dp))
                PremiumStartButton(
                    label = "名前を保存",
                    onClick = viewModel::saveProfileName,
                )
            }
        }

        item {
            PremiumCasinoInfoPanel("戦績") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PremiumStatText("対戦", progress.stats.totalMatches.toString())
                    PremiumStatText("勝率", String.format("%.1f%%", progress.winRatePercent))
                    PremiumStatText(
                        "平均順位",
                        if (progress.stats.totalMatches == 0) "—" else String.format("%.2f", progress.averageRank),
                    )
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PremiumStatText("野獣対象", progress.stats.yajuTargets.toString())
                    PremiumStatText("野獣成功", progress.stats.yajuSuccesses.toString())
                    PremiumStatText("成功率", String.format("%.1f%%", progress.yajuSuccessRatePercent))
                }
            }
        }

        item {
            PremiumCasinoInfoPanel("チャレンジ戦績") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PremiumStatText("挑戦", progress.stats.challengeMatches.toString())
                    PremiumStatText("1位", progress.stats.challengeWins.toString())
                    PremiumStatText("勝率", String.format("%.1f%%", progress.challengeWinRatePercent))
                }
            }
        }

        item {
            PremiumCasinoInfoPanel("装備中") {
                PremiumEquipmentRow("アバター", avatarName)
                PremiumEquipmentRow("フレーム", frameName)
                PremiumEquipmentRow("トランプ", cardName)
                PremiumEquipmentRow("エフェクト", effectName)
                Spacer(Modifier.height(6.dp))
                Text("装備変更はコレクションから行います。", color = Color.White.copy(alpha = 0.46f), fontSize = 11.sp)
            }
        }

        if (!state.playGamesSignedIn && state.playGamesConfigured) {
            item {
                PremiumStartButton(
                    label = "PLAY GAMESを連携（任意）",
                    onClick = onPlayGamesSignIn,
                )
            }
        }
    }
}

@Composable
private fun PremiumCasinoInfoPanel(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0D1712).copy(alpha = 0.96f),
        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.28f)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, color = CasinoGold, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun PremiumStatText(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Black)
        Text(label, color = Color.White.copy(alpha = 0.48f), fontSize = 11.sp)
    }
}

@Composable
private fun PremiumEquipmentRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.width(92.dp), color = Color.White.copy(alpha = 0.50f))
        Text(value, modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatText(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PremiumCasinoBackdrop(
    accent: Color = CasinoGreen,
) {
    val transition = rememberInfiniteTransition(label = "premiumCasinoBackdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "premiumBackdropPhase",
    )

    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            color = CasinoGold.copy(alpha = 0.075f),
            radius = size.minDimension * 0.48f,
            center = Offset(size.width * 0.88f, size.height * 0.12f),
        )
        drawCircle(
            color = accent.copy(alpha = 0.15f),
            radius = size.minDimension * 0.40f,
            center = Offset(size.width * 0.08f, size.height * 0.78f),
        )

        repeat(22) { index ->
            val baseX = ((index * 43) % 101) / 101f
            val baseY = ((index * 67) % 97) / 97f
            val x = ((baseX + phase * (0.035f + (index % 4) * 0.012f)) % 1f) * size.width
            val y = ((baseY + phase * (0.018f + (index % 3) * 0.009f)) % 1f) * size.height
            drawCircle(
                color = if (index % 4 == 0) {
                    CasinoGold.copy(alpha = 0.18f)
                } else {
                    Color.White.copy(alpha = 0.055f)
                },
                radius = 1.4f + (index % 3) * 0.9f,
                center = Offset(x, y),
            )
        }

        val sweepX = size.width * (-0.30f + phase * 1.60f)
        drawLine(
            color = CasinoGold.copy(alpha = 0.055f),
            start = Offset(sweepX, 0f),
            end = Offset(sweepX - size.width * 0.42f, size.height),
            strokeWidth = 34f,
        )
    }
}

@Composable
private fun PremiumSectionTitle(
    title: String,
    subtitle: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .background(CasinoGold, RoundedCornerShape(100.dp)),
        )
        Spacer(Modifier.width(9.dp))
        Column {
            Text(
                title,
                color = CasinoGold,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.5.sp,
                fontSize = 11.sp,
            )
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.36f),
                letterSpacing = 1.4.sp,
                fontSize = 8.sp,
            )
        }
    }
}

@Composable
private fun PremiumStartButton(
    label: String,
    accent: Color = CasinoGold,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "premiumStartButton")
    val pulse by transition.animateFloat(
        initialValue = 0.68f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "premiumStartPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = 0.995f + pulse * 0.005f
                scaleY = 0.995f + pulse * 0.005f
            }
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.78f + pulse * 0.12f),
                        accent,
                        Color(0xFFFFE6A6),
                    )
                ),
                RoundedCornerShape(15.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color(0xFF171006),
            fontWeight = FontWeight.Black,
            letterSpacing = 2.2.sp,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun <T> PremiumChoiceRow(
    title: String,
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    accent: Color = CasinoGold,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            title,
            color = Color.White.copy(alpha = 0.76f),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(values) { _, value ->
                val active = value == selected
                Surface(
                    modifier = Modifier
                        .height(44.dp)
                        .clickable { onSelect(value) },
                    shape = RoundedCornerShape(13.dp),
                    color = if (active) accent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.045f),
                    border = BorderStroke(
                        if (active) 1.5.dp else 1.dp,
                        if (active) accent else Color.White.copy(alpha = 0.10f),
                    ),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label(value),
                            color = if (active) Color.White else Color.White.copy(alpha = 0.56f),
                            fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumRuleToggle(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    enabled: Boolean = true,
    accent: Color = CasinoGold,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (checked) accent.copy(alpha = 0.11f) else Color.White.copy(alpha = 0.035f),
        border = BorderStroke(
            1.dp,
            if (checked) accent.copy(alpha = 0.44f) else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.32f),
                fontWeight = FontWeight.Bold,
            )
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun PremiumPlayerNameField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color = CasinoGold,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("PLAYER NAME") },
        placeholder = { Text("プレイヤー名") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = accent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
            focusedLabelColor = accent,
            unfocusedLabelColor = Color.White.copy(alpha = 0.45f),
            cursorColor = accent,
            focusedContainerColor = Color.Black.copy(alpha = 0.24f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.18f),
            focusedPlaceholderColor = Color.White.copy(alpha = 0.32f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.32f),
        ),
    )
}

@Composable
private fun ChallengeNumberCard(
    value: String,
    subtitle: String,
) {
    Surface(
        modifier = Modifier
            .width(92.dp)
            .height(122.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF120607),
        border = BorderStroke(2.dp, Color(0xFFE34C52)),
    ) {
        Box {
            Text(
                value,
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontSize = 50.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                subtitle,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                color = Color(0xFFE34C52),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

@Composable
private fun ChallengeSetupScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    val accent = Color(0xFFE34C52)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF080203),
                        Color(0xFF26070A),
                        Color(0xFF050202),
                    )
                )
            ),
    ) {
        PremiumCasinoBackdrop(accent = Color(0xFFAA2028))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 17.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = viewModel::backToMenu) {
                    Text("‹ BACK", color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "YAJU CHALLENGE",
                    color = accent,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.5.sp,
                    fontSize = 11.sp,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                item {
                    Column {
                        Text(
                            "TADOKORO BEAST",
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                        )
                        Text(
                            "チャレンジモード",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            "8と10を握って、野獣上がりを決めろ。",
                            color = Color.White.copy(alpha = 0.56f),
                        )
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.38f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.52f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            ChallengeNumberCard("8", "EIGHT")
                            Text(
                                "＋",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = CasinoGold,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                            )
                            ChallengeNumberCard("10", "TEN")
                        }
                    }
                }

                item {
                    PremiumChoiceRow(
                        title = "CPU PLAYERS",
                        values = (1..GameLimits.MAX_CPU_COUNT).toList(),
                        selected = state.cpuCount,
                        onSelect = viewModel::setCpuCount,
                        label = { it.toString() + "人" },
                        accent = accent,
                    )
                }
                item {
                    PremiumChoiceRow(
                        title = "DIFFICULTY",
                        values = listOf("EASY", "NORMAL", "HARD", "N_GOD"),
                        selected = state.cpuDifficulty,
                        onSelect = viewModel::setCpuDifficulty,
                        label = ::difficultyLabel,
                        accent = accent,
                    )
                }
                item {
                    PremiumChoiceRow(
                        title = "JOKER",
                        values = listOf(0, 1, 2),
                        selected = state.cpuJokerCount,
                        onSelect = viewModel::setCpuJokerCount,
                        label = { it.toString() + "枚" },
                        accent = accent,
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = accent.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.48f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(15.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "CHALLENGE RULE",
                                color = accent,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                fontSize = 10.sp,
                            )
                            Text(
                                "野獣状態でゲームを開始します",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }

            PremiumStartButton(
                label = "CHALLENGE START  ▶",
                accent = accent,
                onClick = viewModel::startYajuChallenge,
            )
        }
    }
}

@Composable
private fun MenuPlaceholderScreen(
    title: String,
    subtitle: String,
    description: String,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080B09), Color(0xFF13251C), Color(0xFF080B09))
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) { Text("← CASINO LOUNGE") }
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                color = CasinoGold,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            )
            Text(
                subtitle,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                description,
                color = Color.White.copy(alpha = 0.72f),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "COMING SOON",
                color = CasinoGold.copy(alpha = 0.45f),
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun CpuSetupScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020504),
                        Color(0xFF082319),
                        Color(0xFF030504),
                    )
                )
            ),
    ) {
        PremiumCasinoBackdrop(accent = CasinoGreen)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 17.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = viewModel::backToMenu) {
                    Text("‹ BACK", color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "PRIVATE TABLE",
                    color = CasinoGold,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.5.sp,
                    fontSize = 11.sp,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.36f),
                        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.42f)),
                    ) {
                        Box(modifier = Modifier.padding(18.dp)) {
                            Column(modifier = Modifier.padding(end = 82.dp)) {
                                Text(
                                    "SOLO PLAY",
                                    color = CasinoGold,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.5.sp,
                                    fontSize = 10.sp,
                                )
                                Text(
                                    "ひとりでイク",
                                    color = Color.White,
                                    fontSize = 29.sp,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    "CPUとバトル！！",
                                    color = Color.White.copy(alpha = 0.56f),
                                )
                            }
                            Text(
                                "♠",
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .graphicsLayer(alpha = 0.18f),
                                color = CasinoGold,
                                fontSize = 76.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }

                item {
                    PremiumChoiceRow(
                        title = "CPU PLAYERS",
                        values = (1..GameLimits.MAX_CPU_COUNT).toList(),
                        selected = state.cpuCount,
                        onSelect = viewModel::setCpuCount,
                        label = { it.toString() + "人" },
                    )
                }
                item {
                    PremiumChoiceRow(
                        title = "DIFFICULTY",
                        values = listOf("EASY", "NORMAL", "HARD", "N_GOD"),
                        selected = state.cpuDifficulty,
                        onSelect = viewModel::setCpuDifficulty,
                        label = ::difficultyLabel,
                    )
                }
                item {
                    PremiumChoiceRow(
                        title = "JOKER",
                        values = listOf(0, 1, 2),
                        selected = state.cpuJokerCount,
                        onSelect = viewModel::setCpuJokerCount,
                        label = { it.toString() + "枚" },
                    )
                }

                item { PremiumSectionTitle("SPECIAL RULES", "BUILD YOUR TABLE") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        PremiumRuleToggle("革命", state.ruleRevolution, viewModel::setRuleRevolution)
                        PremiumRuleToggle("8切り", state.ruleEightCut, viewModel::setRuleEightCut, enabled = !state.ruleYaju)
                        PremiumRuleToggle("マーク縛り", state.ruleMarkLock, viewModel::setRuleMarkLock)
                        PremiumRuleToggle("7渡し", state.ruleSevenTransfer, viewModel::setRuleSevenTransfer)
                        PremiumRuleToggle("野獣ルール", state.ruleYaju, viewModel::setRuleYaju)
                        PremiumRuleToggle("Jバック", state.ruleJackBack, viewModel::setRuleJackBack)
                        PremiumRuleToggle("禁止上がり", state.ruleForbiddenFinish, viewModel::setRuleForbiddenFinish)
                        if (state.ruleYaju) {
                            Text(
                                "※ 野獣ルールON時は8切りが自動で有効になります",
                                color = CasinoGold.copy(alpha = 0.76f),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            PremiumStartButton(
                label = "GAME START  ▶",
                onClick = viewModel::startCpuGame,
            )
        }
    }
}

@Composable
private fun CpuCountChoiceRow(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("CPU人数", fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed((1..GameLimits.MAX_CPU_COUNT).toList()) { _, value ->
                if (value == selected) {
                    Button(
                        onClick = { onSelect(value) },
                        modifier = Modifier.width(64.dp),
                    ) { Text("${value}人", maxLines = 1) }
                } else {
                    OutlinedButton(
                        onClick = { onSelect(value) },
                        modifier = Modifier.width(64.dp),
                    ) { Text("${value}人", maxLines = 1) }
                }
            }
        }
        Text(
            "人間1人 + CPU${selected}人 = ${selected + 1}人戦",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun <T> ChoiceRow(title: String, values: List<T>, selected: T, onSelect: (T)->Unit, label: (T)->String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            values.forEach { value ->
                if (value == selected) Button(onClick = { onSelect(value) }, modifier = Modifier.weight(1f)) { Text(label(value), maxLines = 1) }
                else OutlinedButton(onClick = { onSelect(value) }, modifier = Modifier.weight(1f)) { Text(label(value), maxLines = 1) }
            }
        }
    }
}

@Composable
private fun RuleSwitch(title: String, checked: Boolean, onChecked: (Boolean)->Unit, enabled: Boolean = true) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

private fun difficultyLabel(value: String): String = when(value) {
    "EASY" -> "簡単"
    "NORMAL" -> "普通"
    "HARD" -> "難しい"
    "N_GOD" -> "N-GOD"
    else -> value
}

@Composable
private fun LobbyScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020504),
                        Color(0xFF071B13),
                        Color(0xFF020303),
                    )
                )
            ),
    ) {
        PremiumCasinoBackdrop(accent = CasinoGreen)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MULTIPLAYER",
                            color = CasinoGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                        )
                        Text(
                            "ONLINE TABLE",
                            color = Color.White,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                        )
                        Text(
                            "Android / PC 共通サーバー",
                            color = Color.White.copy(alpha = 0.50f),
                            fontSize = 11.sp,
                            letterSpacing = 1.1.sp,
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFF123323),
                        border = BorderStroke(1.dp, Color(0xFF77D69D).copy(alpha = 0.55f)),
                    ) {
                        Text(
                            "● ONLINE",
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            color = Color(0xFF77D69D),
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }
            }

            item {
                TextButton(onClick = viewModel::backToMenu) {
                    Text("‹ CASINO FLOOR", color = Color.White.copy(alpha = 0.72f))
                }
            }

            item {
                PremiumCasinoInfoPanel("PLAYER") {
                    Text(
                        state.progress.displayName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Google Play Games不要。PCブラウザ版とも同じROOMへ参加できます。",
                        color = Color.White.copy(alpha = 0.52f),
                        fontSize = 11.sp,
                    )
                }
            }

            item {
                PremiumCasinoInfoPanel("CREATE ROOM") {
                    Text(
                        "あなたがホストになります。2〜" + GameLimits.MAX_PLAYER_COUNT + "人でゲーム開始できます。",
                        color = Color.White.copy(alpha = 0.68f),
                    )
                    Spacer(Modifier.height(14.dp))
                    PremiumStartButton(
                        label = "新しい部屋を作る  ▶",
                        onClick = viewModel::createRoom,
                    )
                }
            }

            item {
                PremiumCasinoInfoPanel("JOIN ROOM") {
                    OutlinedTextField(
                        value = state.roomIdInput,
                        onValueChange = viewModel::setRoomIdInput,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("ROOM ID") },
                        placeholder = { Text("8文字のルームID") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = viewModel::joinRoom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text("ROOMへ参加  ▶", fontWeight = FontWeight.Black)
                    }
                }
            }

            item {
                Text(
                    "PUBLIC SERVER  •  " + state.serverUrl,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.30f),
                    fontSize = 9.sp,
                    letterSpacing = 0.7.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RoomScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    val game = state.gameState ?: return
    val clipboard = LocalClipboardManager.current
    val pcInviteUrl = state.serverUrl.trimEnd('/') + "/?room=" + game.roomId

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "ONLINE TABLE",
                        color = CasinoGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp,
                    )
                    Text(
                        "対戦ルーム",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (state.socketConnected) Color(0xFF123323) else Color(0xFF2A210B),
                    border = BorderStroke(
                        1.dp,
                        if (state.socketConnected) Color(0xFF77D69D).copy(alpha = 0.55f)
                        else CasinoGold.copy(alpha = 0.42f),
                    ),
                ) {
                    Text(
                        if (state.socketConnected) "● LIVE" else "○ SYNC",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = if (state.socketConnected) Color(0xFF77D69D) else CasinoGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                    )
                }
            }
        }
        item {
            CasinoPanel(title = if (game.host) "♛ あなたがホスト" else "参加中") {
                Text("ルームID", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        game.roomId,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = CasinoGreenDark,
                    )
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(game.roomId)) }) {
                        Text("IDコピー")
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(pcInviteUrl)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("PC招待URLをコピー")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "PCでは招待URLを開くだけ。Android同士はROOM IDでも参加できます。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        item {
            CasinoPanel(title = "プレイヤー ${game.players.size}/${GameLimits.MAX_PLAYER_COUNT}") {
                game.players.forEachIndexed { index, player ->
                    PlayerLobbyRow(player, index + 1)
                    if (index != game.players.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                repeat((GameLimits.MAX_PLAYER_COUNT - game.players.size).coerceAtLeast(0)) { index ->
                    if (game.players.isNotEmpty() || index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("○ 空席", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            if (game.host) {
                Button(
                    onClick = viewModel::startGame,
                    enabled = game.players.size >= 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text(if (game.players.size >= 2) "ゲームを開始する" else "あと1人必要です", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(color = SoftGold, shape = RoundedCornerShape(14.dp)) {
                    Text("ホストがゲームを開始するまで待機中…", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = viewModel::leaveRoom,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("部屋から退出") }
        }
    }
}

@Composable
private fun PlayerLobbyRow(player: PlayerDto, seat: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PlayerAvatar(
            avatarIndex = (seat - 1).coerceAtLeast(0),
            size = 42.dp,
            highlighted = player.self,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(player.playerName, fontWeight = FontWeight.Bold)
            Text(if (player.self) "あなた" else "準備完了", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GameScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    val game = state.gameState ?: return
    val self = game.selfPlayer ?: return
    val opponents = game.players.filterNot { it.self }
    val actionEnabled = !state.cpuTurnInProgress && game.isMyTurn
    val canPass = actionEnabled && !game.isMySevenTransfer && game.field.cards.isNotEmpty()
    val canPlay = actionEnabled &&
        state.selectedCardIndices.isNotEmpty() &&
        (!game.isMySevenTransfer || state.selectedCardIndices.size == game.sevenTransfer.cardCount)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF010403),
                        Color(0xFF04150F),
                        Color(0xFF020303),
                    )
                )
            ),
    ) {
        PremiumCasinoBackdrop(
            accent = if (game.players.any { it.yajuActive }) Color(0xFF8B221E) else CasinoGreen,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 10.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (game.isLocalCpuGame) "PRIVATE TABLE" else "ONLINE TABLE",
                                color = CasinoGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.5.sp,
                            )
                            Text(
                                "BATTLE TABLE",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.48f),
                            border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.42f)),
                        ) {
                            Text(
                                if (game.isLocalCpuGame) "LOCAL" else if (state.socketConnected) "LIVE" else "SYNC",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                color = if (game.isLocalCpuGame || state.socketConnected) Color(0xFF77D69D) else CasinoGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                            )
                        }
                    }
                }

                item { StatusBar(game) }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 2.dp),
                    ) {
                        itemsIndexed(opponents) { _, player ->
                            OpponentPlayerPanel(
                                player = player,
                                current = game.currentPlayerId == player.playerId,
                                avatarIndex = game.avatarIndex(player.playerId),
                                photoAssetName = game.cpuAvatarAssetName(player.playerId),
                            )
                        }
                    }
                }

                item { GameTable(game) }

                item {
                    BattleTurnBanner(
                        game = game,
                        state = state,
                    )
                }

                if (game.isMySevenTransfer) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(17.dp),
                            color = Color(0xFF2A1A06).copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.72f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    "7 TRANSFER",
                                    color = CasinoGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                )
                                Text(
                                    (game.sevenTransferTarget?.playerName ?: "隣のプレイヤー") +
                                        "へ " + game.sevenTransfer.cardCount + "枚選択",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    "野獣対象者は最後の8・10を渡すことはできません。",
                                    color = Color.White.copy(alpha = 0.52f),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF07100D).copy(alpha = 0.96f),
                        border = BorderStroke(
                            if (game.isMyTurn && !state.cpuTurnInProgress) 1.5.dp else 1.dp,
                            if (game.isMyTurn && !state.cpuTurnInProgress) CasinoGold.copy(alpha = 0.72f)
                            else Color.White.copy(alpha = 0.08f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlayerAvatar(
                                    avatarIndex = game.avatarIndex(self.playerId),
                                    size = 46.dp,
                                    highlighted = game.isMyTurn && !state.cpuTurnInProgress,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        self.playerName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "HAND  " + self.handCount + " CARDS" +
                                            if (self.yajuActive) "   •   YAJU " + self.yajuStatus else "",
                                        color = if (self.yajuActive) CasinoGold else Color.White.copy(alpha = 0.44f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.7.sp,
                                    )
                                }
                                if (state.selectedCardIndices.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = CasinoGold.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.48f)),
                                    ) {
                                        Text(
                                            "SELECT " + state.selectedCardIndices.size,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            color = CasinoGold,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy((-4).dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    start = 12.dp,
                                    end = 18.dp,
                                    top = 13.dp,
                                    bottom = 13.dp,
                                ),
                            ) {
                                itemsIndexed(self.hand) { index, card ->
                                    PlayingCard(
                                        card = card,
                                        selected = index in state.selectedCardIndices,
                                        enabled = actionEnabled,
                                        onClick = { viewModel.toggleCard(index) },
                                    )
                                }
                            }
                        }
                    }
                }

                if (game.isLocalCpuGame && state.cpuActionHistory.isNotEmpty()) {
                    item { CpuActionHistoryCard(state.cpuActionHistory) }
                }
            }

            Surface(
                color = Color(0xFF030605).copy(alpha = 0.98f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                shadowElevation = 12.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::pass,
                        enabled = canPass,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(52.dp),
                        border = BorderStroke(
                            1.dp,
                            if (canPass) Color.White.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.10f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.28f),
                        ),
                    ) {
                        Text("PASS", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    }

                    Button(
                        onClick = viewModel::playSelected,
                        enabled = canPlay,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CasinoGold,
                            contentColor = Color(0xFF171006),
                            disabledContainerColor = Color.White.copy(alpha = 0.08f),
                            disabledContentColor = Color.White.copy(alpha = 0.28f),
                        ),
                    ) {
                        Text(
                            if (game.isMySevenTransfer) {
                                "TRANSFER  " + state.selectedCardIndices.size + "/" + game.sevenTransfer.cardCount + "  ▶"
                            } else {
                                "PLAY  " + state.selectedCardIndices.size + "  ▶"
                            },
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }
            }
        }

        if (game.isLocalCpuGame) {
            state.cpuTurnAnimation?.let { animation ->
                CpuTurnAnimationOverlay(
                    animation = animation,
                    opponents = opponents,
                )
            }
        }
    }
}

@Composable
private fun BattleTurnBanner(
    game: GameStateDto,
    state: DaifugoUiState,
) {
    val myTurn = game.isMyTurn && !state.cpuTurnInProgress
    val label = when {
        state.cpuTurnAnimation?.type == CpuAnimationType.THINKING ->
            state.cpuTurnAnimation.playerName + "  THINKING..."
        state.cpuTurnInProgress ->
            (state.cpuTurnAnimation?.playerName ?: game.currentPlayer?.playerName ?: "CPU") + "  PLAYING"
        myTurn -> "YOUR TURN"
        else -> (game.currentPlayer?.playerName ?: "-") + "  TURN"
    }

    val glow by animateFloatAsState(
        targetValue = if (myTurn) 1f else 0.35f,
        animationSpec = tween(240),
        label = "battleTurnGlow",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = if (myTurn) CasinoGold.copy(alpha = 0.10f + glow * 0.08f)
        else Color.Black.copy(alpha = 0.34f),
        border = BorderStroke(
            1.dp,
            if (myTurn) CasinoGold.copy(alpha = 0.50f + glow * 0.30f)
            else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (myTurn) "◆" else "◇",
                color = if (myTurn) CasinoGold else Color.White.copy(alpha = 0.28f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                modifier = Modifier.weight(1f),
                color = if (myTurn) Color.White else Color.White.copy(alpha = 0.62f),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                fontSize = 12.sp,
            )
            if (myTurn) {
                Text(
                    "SELECT CARDS",
                    color = CasinoGold,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

/** 直近のCPU行動を残し、8切りなどで場が流れても何を出したか確認できるようにする。 */
@Composable
private fun CpuActionHistoryCard(history: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF070B09).copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                "PLAY LOG",
                color = CasinoGold,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
            )
            history.take(4).forEachIndexed { index, line ->
                Text(
                    text = if (index == 0) "▶  " + line else "    " + line,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == 0) Color.White else Color.White.copy(alpha = 0.40f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * CPUの手元付近からテーブル中央へカードを約3秒かけて移動する演出。
 * UIだけを遅らせ、ゲームロジック自体はLocalCpuGameManagerで確定済みの結果を後から反映する。
 */
@Composable
private fun CpuTurnAnimationOverlay(
    animation: CpuTurnAnimation,
    opponents: List<PlayerDto>,
) {
    val progress = remember(animation.id) { Animatable(0f) }
    val opponentIndex = opponents.indexOfFirst { it.playerId == animation.playerId }.coerceAtLeast(0)
    val opponentCount = opponents.size.coerceAtLeast(1)
    val normalizedPosition = if (opponentCount == 1) {
        0f
    } else {
        (opponentIndex.toFloat() / (opponentCount - 1).toFloat()) * 2f - 1f
    }
    val startX = (normalizedPosition * 110f).dp

    LaunchedEffect(animation.id) {
        progress.snapTo(0f)
        when (animation.type) {
            CpuAnimationType.PLAY -> progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3_000, easing = FastOutSlowInEasing),
            )
            CpuAnimationType.PASS -> progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1_500, easing = FastOutSlowInEasing),
            )
            CpuAnimationType.SEVEN_TRANSFER -> progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2_000, easing = FastOutSlowInEasing),
            )
            CpuAnimationType.THINKING -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (animation.type) {
            CpuAnimationType.THINKING -> {
                Surface(
                    modifier = Modifier.offset(y = 92.dp),
                    shape = RoundedCornerShape(100.dp),
                    color = Color(0xFF030605).copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.42f)),
                    shadowElevation = 12.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("◆", color = CasinoGold, fontSize = 10.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            animation.playerName + "  THINKING...",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }

            CpuAnimationType.PLAY -> {
                val p = progress.value
                Column(
                    modifier = Modifier
                        .offset(
                            x = startX * (1f - p),
                            y = (78 + (245 * p)).dp,
                        )
                        .graphicsLayer {
                            val scale = 0.78f + (0.22f * p)
                            scaleX = scale
                            scaleY = scale
                            alpha = 0.76f + (0.24f * p)
                            rotationZ = (1f - p) * if (opponentIndex % 2 == 0) -6f else 6f
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        color = Color(0xFF030605).copy(alpha = 0.96f),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.55f)),
                        shadowElevation = 12.dp,
                    ) {
                        Text(
                            animation.playerName + "  PLAY!",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            color = CasinoGold,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                        animation.cards.forEach { card ->
                            PlayingCard(
                                card = card,
                                selected = false,
                                enabled = false,
                                compact = true,
                                onClick = {},
                            )
                        }
                    }
                }
            }

            CpuAnimationType.PASS -> {
                val p = progress.value
                Surface(
                    modifier = Modifier
                        .offset(y = (176 + (42 * p)).dp)
                        .graphicsLayer {
                            scaleX = 0.86f + (0.20f * p)
                            scaleY = 0.86f + (0.20f * p)
                            alpha = 1f - (0.30f * p)
                        },
                    color = Color(0xFF2A090C).copy(alpha = 0.94f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFE34C52).copy(alpha = 0.72f)),
                    shadowElevation = 12.dp,
                ) {
                    Text(
                        animation.playerName + "   PASS",
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                        color = Color(0xFFFFB9BE),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                    )
                }
            }

            CpuAnimationType.SEVEN_TRANSFER -> {
                val p = progress.value
                Column(
                    modifier = Modifier
                        .offset(
                            x = ((-84) + (168 * p)).dp,
                            y = 194.dp,
                        )
                        .graphicsLayer {
                            alpha = 0.78f + (0.22f * p)
                            rotationZ = -4f + (8f * p)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        color = Color(0xFF2A1A06).copy(alpha = 0.96f),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.72f)),
                    ) {
                        Text(
                            animation.playerName + "  →  " + (animation.targetPlayerName ?: "隣") + "   7 TRANSFER",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            color = CasinoGold,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.7.sp,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                        repeat(animation.cardCount) { index ->
                            CardBack(
                                modifier = Modifier.zIndex(index.toFloat()),
                                compact = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBar(game: GameStateDto) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 1.dp),
    ) {
        if (game.revolution) item { RulePill("♛ REVOLUTION", Color(0xFF2B1D08), CasinoGold) }
        if (game.jackBack) item { RulePill("J BACK", Color(0xFF171228), Color(0xFFB7A1FF)) }
        game.lockedMark?.let { mark ->
            item { RulePill(markSymbol(mark) + " LOCK", Color(0xFF092219), Color(0xFF78D6A1)) }
        }
        if (game.players.any { it.yajuActive }) {
            item { RulePill("YAJU 8→10", Color(0xFF2C120B), Color(0xFFFFC46B)) }
        }
        item { RulePill("ROOM " + game.roomId, Color(0xFF0B0E0C), Color.White.copy(alpha = 0.56f)) }
    }
}

@Composable
private fun RulePill(text: String, background: Color, foreground: Color) {
    Surface(
        color = background.copy(alpha = 0.94f),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, foreground.copy(alpha = 0.30f)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            color = foreground,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
    }
}

@Composable
private fun OpponentPlayerPanel(
    player: PlayerDto,
    current: Boolean,
    avatarIndex: Int,
    photoAssetName: String?,
) {
    val borderColor = when {
        current -> CasinoGold
        player.rank != null -> CasinoGold.copy(alpha = 0.35f)
        player.passed -> Color(0xFFE34C52).copy(alpha = 0.35f)
        else -> Color.White.copy(alpha = 0.08f)
    }

    Surface(
        modifier = Modifier.width(158.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (current) Color(0xFF142219).copy(alpha = 0.98f) else Color(0xFF070B09).copy(alpha = 0.96f),
        border = BorderStroke(if (current) 1.5.dp else 1.dp, borderColor),
        shadowElevation = if (current) 8.dp else 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatar(
                    avatarIndex = avatarIndex,
                    size = 38.dp,
                    highlighted = current,
                    photoAssetName = photoAssetName,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        player.playerName,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when {
                            player.rank != null -> "RANK " + player.rank
                            player.passed -> "PASS"
                            current -> "● ACTIVE TURN"
                            else -> if (player.cpu) player.cpuDifficulty ?: "CPU" else "PLAYER"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            player.rank != null -> CasinoGold
                            player.passed -> Color(0xFFFF8B93)
                            current -> Color(0xFF78D6A1)
                            else -> Color.White.copy(alpha = 0.42f)
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                    )
                }
            }

            OpponentHandBacks(cardCount = player.handCount)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "HAND " + player.handCount,
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                if (player.yajuActive) {
                    Surface(
                        color = Color(0xFF2A1607),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.55f)),
                    ) {
                        Text(
                            "8→10",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Color(0xFFFFD369),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

/** 相手の残り手札を、枚数テキストではなく裏向きカードの重なりで表現する。 */
@Composable
private fun OpponentHandBacks(cardCount: Int) {
    val safeCount = cardCount.coerceAtLeast(0)
    val totalWidth = 132.dp
    val cardWidth = 28.dp
    val step = if (safeCount <= 1) {
        0f
    } else {
        min(8f, (totalWidth.value - cardWidth.value) / (safeCount - 1))
    }

    Box(
        modifier = Modifier
            .width(totalWidth)
            .height(44.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        repeat(safeCount) { index ->
            CardBack(
                modifier = Modifier
                    .offset(x = (step * index).dp)
                    .zIndex(index.toFloat()),
                compact = true,
            )
        }
    }
}

@Composable
private fun GameTable(game: GameStateDto) {
    val activeAccent = if (game.players.any { it.yajuActive }) Color(0xFFD27835) else CasinoGold

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFF062D20),
        border = BorderStroke(2.dp, activeAccent.copy(alpha = 0.72f)),
        shadowElevation = 10.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0C6244),
                            Color(0xFF073C2B),
                            Color(0xFF04281D),
                        )
                    )
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.055f),
                    topLeft = Offset(size.width * 0.055f, size.height * 0.09f),
                    size = Size(size.width * 0.89f, size.height * 0.82f),
                    cornerRadius = CornerRadius(size.height * 0.18f),
                    style = Stroke(width = 2f),
                )
                drawCircle(
                    color = activeAccent.copy(alpha = 0.035f),
                    radius = size.minDimension * 0.72f,
                    center = Offset(size.width * 0.5f, size.height * 0.52f),
                )
            }

            Text(
                "ROYAL TABLE",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp),
                color = activeAccent.copy(alpha = 0.76f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.5.sp,
            )

            if (game.field.cards.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "♠  ♥  ♦  ♣",
                        color = Color.White.copy(alpha = 0.12f),
                        fontSize = 26.sp,
                        letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "WAITING FOR PLAY",
                        color = Color.White.copy(alpha = 0.40f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy((-13).dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                    ) {
                        itemsIndexed(game.field.cards) { _, card ->
                            PlayingCard(
                                card = card,
                                selected = false,
                                enabled = false,
                                compact = true,
                                onClick = {},
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    game.field.combinationType?.let {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color.Black.copy(alpha = 0.34f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        ) {
                            Text(
                                it.uppercase(),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                color = Color.White.copy(alpha = 0.62f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                            )
                        }
                    }
                }
            }

            Text(
                if (game.revolution) "REVOLUTION ACTIVE" else "YAJU DAIFUGO",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                color = if (game.revolution) CasinoGold else Color.White.copy(alpha = 0.13f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
        }
    }
}

/** 市販トランプを意識した、四隅インデックス付きのカード表示。 */
@Composable
private fun PlayingCard(
    card: CardDto,
    selected: Boolean,
    enabled: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val redSuit = card.suit == "HEART" || card.suit == "DIAMOND"
    val foreground = when {
        card.joker -> CasinoGold
        redSuit -> Color(0xFFB51F2E)
        else -> Color(0xFF151515)
    }
    val width = if (compact) 54.dp else 66.dp
    val height = if (compact) 78.dp else 98.dp
    val cornerSize = if (compact) 12.sp else 14.sp
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.055f else 1f,
        animationSpec = tween(150),
        label = "playingCardScale",
    )

    Card(
        modifier = Modifier
            .width(width)
            .height(height)
            .offset(y = if (selected) (-14).dp else 0.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) CasinoGold else Color(0xFFD1CBC0),
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFFFF1C9) else Color(0xFFFFFEF9),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 12.dp else 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 5.dp else 6.dp),
        ) {
            if (selected) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRoundRect(
                        color = CasinoGold.copy(alpha = 0.09f),
                        cornerRadius = CornerRadius(10f),
                    )
                }
            }

            CardCornerIndex(
                card = card,
                color = foreground,
                fontSize = cornerSize,
                modifier = Modifier.align(Alignment.TopStart),
            )

            if (card.joker) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("♛", color = CasinoGold, fontSize = if (compact) 24.sp else 30.sp)
                    Text(
                        "JOKER",
                        color = CasinoGold,
                        fontSize = if (compact) 8.sp else 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.7.sp,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        card.displaySuit,
                        color = foreground,
                        fontSize = if (compact) 27.sp else 34.sp,
                    )
                    if (card.displayRank in setOf("J", "Q", "K")) {
                        Text(
                            card.displayRank,
                            color = CasinoGold,
                            fontSize = if (compact) 12.sp else 14.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            CardCornerIndex(
                card = card,
                color = foreground,
                fontSize = cornerSize,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .graphicsLayer { rotationZ = 180f },
            )

            if (selected && !compact) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 1.dp),
                    shape = RoundedCornerShape(100.dp),
                    color = CasinoGold,
                ) {
                    Text(
                        "SELECT",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        color = Color(0xFF171006),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.7.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardCornerIndex(
    card: CardDto,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (card.joker) "J" else card.displayRank,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            lineHeight = fontSize,
        )
        Text(
            if (card.joker) "★" else card.displaySuit,
            color = color,
            fontSize = fontSize,
            lineHeight = fontSize,
        )
    }
}

/** 濃紺×金のオリジナル裏面。特定製品の意匠を模倣せず、卓上で見分けやすい柄にする。 */
@Composable
private fun CardBack(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val width = if (compact) 28.dp else 54.dp
    val height = if (compact) 40.dp else 78.dp

    Card(
        modifier = modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(if (compact) 4.dp else 7.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF102747)),
        border = BorderStroke(1.dp, Color(0xFFB79245)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color(0xFF102747))
            val lineColor = Color(0xFFDBC27B).copy(alpha = 0.30f)
            val gap = size.minDimension / 4.5f
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x + size.height, size.height),
                    strokeWidth = 1f,
                )
                drawLine(
                    color = lineColor,
                    start = Offset(x + size.height, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                )
                x += gap
            }
            drawRoundRect(
                color = Color(0xFFE3C878).copy(alpha = 0.75f),
                topLeft = Offset(size.width * 0.11f, size.height * 0.08f),
                size = Size(size.width * 0.78f, size.height * 0.84f),
                cornerRadius = CornerRadius(size.width * 0.08f),
                style = Stroke(width = 1.2f),
            )
            drawCircle(
                color = Color(0xFFE3C878).copy(alpha = 0.82f),
                radius = size.minDimension * 0.11f,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = 1.4f),
            )
        }
    }
}

private const val CPU_AVATAR_DECODE_EDGE_PX = 512

/** JPEGに保存されたEXIFの向き情報を取得する。 */
private fun readCpuAvatarOrientation(assets: AssetManager, assetName: String): Int =
    runCatching {
        assets.open(assetName).use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

/**
 * EXIF OrientationをBitmapへ実際に反映する。
 * スマホ写真はピクセル自体を回転せず、EXIFだけに90°回転などを記録する場合があるため、
 * BitmapFactoryで読み込んだ後に補正してからComposeへ渡す。
 */
private fun applyCpuAvatarOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()

    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setRotate(180f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return bitmap
    }

    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true,
    ).also { corrected ->
        if (corrected !== bitmap) {
            bitmap.recycle()
        }
    }
}

/** 大きな写真でもメモリを使いすぎないよう、アバター用途のサイズへ縮小し、EXIFの向きも補正して読み込む。 */
private fun decodeCpuAvatar(assets: AssetManager, assetName: String): ImageBitmap? =
    runCatching {
        val orientation = readCpuAvatarOrientation(assets, assetName)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        assets.open(assetName).use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        var sampleSize = 1
        val longestEdge = max(bounds.outWidth, bounds.outHeight)
        while (longestEdge / (sampleSize * 2) >= CPU_AVATAR_DECODE_EDGE_PX) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = assets.open(assetName).use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return@runCatching null

        applyCpuAvatarOrientation(decoded, orientation).asImageBitmap()
    }.getOrNull()

/** CPU戦では写真を優先し、未配置時は端末内描画へフォールバックするプレイヤーアイコン。 */
@Composable
private fun PlayerAvatar(
    avatarIndex: Int,
    size: androidx.compose.ui.unit.Dp,
    highlighted: Boolean = false,
    photoAssetName: String? = null,
) {
    val index = ((avatarIndex % 8) + 8) % 8
    val context = LocalContext.current
    val photoBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = photoAssetName,
    ) {
        value = photoAssetName?.let { assetName ->
            withContext(Dispatchers.IO) {
                decodeCpuAvatar(context.assets, assetName)
            }
        }
    }
    val backgrounds = listOf(
        Color(0xFF315D7A), Color(0xFF7A3F4E), Color(0xFF486A46), Color(0xFF6A4B7A),
        Color(0xFF8A5A2E), Color(0xFF2E6E69), Color(0xFF5A5D87), Color(0xFF7B513B),
    )
    val hairColors = listOf(
        Color(0xFF201B19), Color(0xFF4A2D1D), Color(0xFF1D1D23), Color(0xFF70482E),
        Color(0xFF2F2A28), Color(0xFF5B3726), Color(0xFF191919), Color(0xFF805E3D),
    )
    val skinColors = listOf(
        Color(0xFFF2C6A2), Color(0xFFE6B58F), Color(0xFFDFA57C), Color(0xFFF0C3A1),
        Color(0xFFD39B73), Color(0xFFE9B991), Color(0xFFC98F68), Color(0xFFF3C9AA),
    )

    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = backgrounds[index],
        border = BorderStroke(
            if (highlighted) 2.dp else 1.dp,
            if (highlighted) CasinoGold else Color.White.copy(alpha = 0.65f),
        ),
        shadowElevation = if (highlighted) 5.dp else 2.dp,
    ) {
        photoBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "CPU avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } ?: run {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = this.size.width / 2f
                val faceCenter = Offset(cx, this.size.height * 0.55f)
                val faceRadius = this.size.minDimension * 0.29f
            drawCircle(skinColors[index], faceRadius, faceCenter)

            // 髪型をインデックスごとに変え、同一卓で見分けられるようにする。
            when (index % 4) {
                0 -> drawArc(
                    hairColors[index], 180f, 180f, true,
                    topLeft = Offset(cx - faceRadius, faceCenter.y - faceRadius * 1.25f),
                    size = Size(faceRadius * 2f, faceRadius * 1.45f),
                )
                1 -> {
                    drawCircle(hairColors[index], faceRadius * 0.96f, Offset(cx, faceCenter.y - faceRadius * 0.48f))
                    drawCircle(skinColors[index], faceRadius * 0.90f, faceCenter)
                }
                2 -> {
                    drawArc(
                        hairColors[index], 190f, 160f, true,
                        topLeft = Offset(cx - faceRadius * 1.05f, faceCenter.y - faceRadius * 1.35f),
                        size = Size(faceRadius * 2.1f, faceRadius * 1.55f),
                    )
                    drawLine(
                        hairColors[index],
                        Offset(cx - faceRadius * 0.65f, faceCenter.y - faceRadius * 0.70f),
                        Offset(cx + faceRadius * 0.25f, faceCenter.y - faceRadius * 1.10f),
                        strokeWidth = 3f,
                    )
                }
                else -> {
                    drawArc(
                        hairColors[index], 180f, 180f, true,
                        topLeft = Offset(cx - faceRadius, faceCenter.y - faceRadius * 1.30f),
                        size = Size(faceRadius * 2f, faceRadius * 1.50f),
                    )
                    drawCircle(hairColors[index], faceRadius * 0.18f, Offset(cx - faceRadius * 0.75f, faceCenter.y - faceRadius * 0.72f))
                    drawCircle(hairColors[index], faceRadius * 0.18f, Offset(cx + faceRadius * 0.75f, faceCenter.y - faceRadius * 0.72f))
                }
            }

            val eyeY = faceCenter.y - faceRadius * 0.05f
            val eyeDx = faceRadius * 0.38f
            if (index % 3 == 2) {
                drawLine(Color(0xFF2A211D), Offset(cx - eyeDx - 3f, eyeY), Offset(cx - eyeDx + 3f, eyeY), 2f)
                drawLine(Color(0xFF2A211D), Offset(cx + eyeDx - 3f, eyeY), Offset(cx + eyeDx + 3f, eyeY), 2f)
            } else {
                drawCircle(Color(0xFF2A211D), faceRadius * 0.065f, Offset(cx - eyeDx, eyeY))
                drawCircle(Color(0xFF2A211D), faceRadius * 0.065f, Offset(cx + eyeDx, eyeY))
            }

            val mouthY = faceCenter.y + faceRadius * 0.42f
            when (index % 3) {
                0 -> drawArc(
                    Color(0xFF8C3F3F), 10f, 160f, false,
                    topLeft = Offset(cx - faceRadius * 0.28f, mouthY - faceRadius * 0.12f),
                    size = Size(faceRadius * 0.56f, faceRadius * 0.28f),
                    style = Stroke(width = 2f),
                )
                1 -> drawLine(
                    Color(0xFF8C3F3F),
                    Offset(cx - faceRadius * 0.20f, mouthY),
                    Offset(cx + faceRadius * 0.20f, mouthY),
                    strokeWidth = 2f,
                )
                else -> drawArc(
                    Color(0xFF8C3F3F), 190f, 160f, false,
                    topLeft = Offset(cx - faceRadius * 0.27f, mouthY),
                    size = Size(faceRadius * 0.54f, faceRadius * 0.26f),
                    style = Stroke(width = 2f),
                )
            }
        }
        }
    }
}

/** Jバック・早漏を全画面で知らせるカットイン。 */
@Composable
private fun RuleCutInOverlay(
    cutIn: RuleCutIn,
    game: GameStateDto?,
    onDismiss: (Long) -> Unit,
) {
    val scale = remember(cutIn.id) { Animatable(0.82f) }
    val alpha = remember(cutIn.id) { Animatable(0f) }
    val isEarlyShot = cutIn.type == RuleCutInType.EARLY_SHOT

    LaunchedEffect(cutIn.id) {
        alpha.animateTo(1f, tween(120))
        scale.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        delay(if (isEarlyShot) 1_650 else 1_900)
        alpha.animateTo(0f, tween(180))
        onDismiss(cutIn.id)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(110f)
            .background(Color.Black.copy(alpha = 0.80f * alpha.value)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val accent = if (isEarlyShot) Color(0xFFFF5B5B) else Color(0xFF8A6DFF)
            repeat(10) { index ->
                val y = size.height * (0.10f + index * 0.09f)
                drawLine(
                    color = accent.copy(alpha = 0.14f * alpha.value),
                    start = Offset(-size.width * 0.15f, y),
                    end = Offset(size.width * 1.15f, y - size.height * 0.20f),
                    strokeWidth = 6f,
                )
            }
        }

        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerAvatar(
                avatarIndex = game?.avatarIndex(cutIn.playerId) ?: 0,
                size = 72.dp,
                highlighted = true,
                photoAssetName = game?.cpuAvatarAssetName(cutIn.playerId),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (isEarlyShot) "早漏" else "J BACK",
                color = if (isEarlyShot) Color(0xFFFF6B6B) else Color(0xFFB7A8FF),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(
                if (isEarlyShot) "早すぎるッ！" else "バック気持ちいい",
                color = Color.White,
                fontSize = if (isEarlyShot) 38.sp else 34.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                cutIn.playerName,
                color = Color.White.copy(alpha = 0.78f),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** 野獣対象が確定した瞬間に全画面で表示する共通カットイン。 */
@Composable
private fun YajuCutInOverlay(
    cutIn: YajuCutIn,
    game: GameStateDto?,
    onDismiss: (Long) -> Unit,
) {
    val scale = remember(cutIn.id) { Animatable(0.78f) }
    val alpha = remember(cutIn.id) { Animatable(0f) }

    LaunchedEffect(cutIn.id) {
        alpha.animateTo(1f, tween(180))
        scale.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        delay(1_950)
        alpha.animateTo(0f, tween(220))
        onDismiss(cutIn.id)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.78f * alpha.value)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bandColor = Color(0xFFD5A43A).copy(alpha = 0.28f * alpha.value)
            val stripeHeight = size.height * 0.17f
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, bandColor, Color.Transparent),
                ),
                topLeft = Offset(0f, size.height * 0.36f),
                size = Size(size.width, stripeHeight),
            )
            repeat(8) { index ->
                val y = size.height * (0.18f + index * 0.09f)
                drawLine(
                    color = Color(0xFFFFD46A).copy(alpha = 0.09f * alpha.value),
                    start = Offset(-size.width * 0.1f, y),
                    end = Offset(size.width * 1.1f, y - size.height * 0.16f),
                    strokeWidth = 3f,
                )
            }
        }

        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "YAJU CHANCE",
                color = Color(0xFFFFD56A),
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            )
            Text(
                "野獣上がり確定",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                cutIn.playerIds.forEachIndexed { index, playerId ->
                    val avatarIndex = game?.avatarIndex(playerId) ?: index
                    Column(
                        modifier = Modifier.width(64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlayerAvatar(
                            avatarIndex = avatarIndex,
                            size = 52.dp,
                            highlighted = true,
                            photoAssetName = game?.cpuAvatarAssetName(playerId),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            cutIn.playerNames.getOrNull(index) ?: "PLAYER",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                color = Color(0xFFFFD56A),
                shape = RoundedCornerShape(100.dp),
            ) {
                Text(
                    "8  →  10",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 9.dp),
                    color = Color(0xFF241707),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

private fun GameStateDto.avatarIndex(playerId: String): Int =
    players.indexOfFirst { it.playerId == playerId }
        .takeIf { it >= 0 }
        ?: 0

/** CPUローカル戦で使う写真アセット名。CPU以外・通信対戦ではnull。 */
private fun GameStateDto.cpuAvatarAssetName(playerId: String): String? {
    if (!isLocalCpuGame) return null

    val cpuIndex = players
        .filter { it.cpu }
        .indexOfFirst { it.playerId == playerId }

    if (cpuIndex !in 0..6) return null
    val number = (cpuIndex + 1).toString().padStart(2, '0')
    return "cpu_avatars/cpu_${number}.jpg"
}

@Composable
private fun ResultScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    val game = state.gameState ?: return
    val ranking = game.players.sortedWith(
        compareBy<PlayerDto> { it.rank ?: Int.MAX_VALUE }.thenBy { it.playerName }
    )
    val selfRank = game.selfPlayer?.rank

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020403),
                        Color(0xFF0A1C14),
                        Color(0xFF020303),
                    )
                )
            ),
    ) {
        PremiumCasinoBackdrop(accent = CasinoGold)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "GAME SET",
                        color = CasinoGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                    )
                    Text(
                        if (selfRank == 1) "VICTORY" else "RESULT",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        selfRank?.let { "YOUR RANK  #" + it } ?: "MATCH COMPLETE",
                        color = if (selfRank == 1) CasinoGold else Color.White.copy(alpha = 0.52f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                    )
                }
            }

            itemsIndexed(ranking) { index, player ->
                ResultRow(
                    player = player,
                    avatarIndex = game.avatarIndex(player.playerId),
                    photoAssetName = game.cpuAvatarAssetName(player.playerId),
                )
            }

            item {
                Button(
                    onClick = viewModel::leaveRoom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CasinoGold,
                        contentColor = Color(0xFF171006),
                    ),
                ) {
                    Text(
                        "RETURN TO LOUNGE  ▶",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.3.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    player: PlayerDto,
    avatarIndex: Int,
    photoAssetName: String?,
) {
    val first = player.rank == 1
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (first) Color(0xFF1C1A0E).copy(alpha = 0.96f) else Color(0xFF080C0A).copy(alpha = 0.94f),
        border = BorderStroke(
            if (first) 1.5.dp else 1.dp,
            if (first) CasinoGold.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                player.rank?.let { "#" + it } ?: "—",
                modifier = Modifier.width(48.dp),
                fontSize = if (first) 24.sp else 20.sp,
                fontWeight = FontWeight.Black,
                color = if (first) CasinoGold else Color.White.copy(alpha = 0.62f),
            )
            PlayerAvatar(
                avatarIndex = avatarIndex,
                size = 44.dp,
                highlighted = first,
                photoAssetName = photoAssetName,
            )
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.playerName,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    if (player.self) "YOU" else if (player.cpu) "CPU" else "PLAYER",
                    color = if (player.self) CasinoGold else Color.White.copy(alpha = 0.38f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
            if (first) {
                Text("♛", color = CasinoGold, fontSize = 26.sp)
            }
        }
    }
}

@Composable
private fun CasinoPanel(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun FeatureLine(symbol: String, title: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, modifier = Modifier.width(34.dp), fontSize = 20.sp, textAlign = TextAlign.Center)
        Text(title, fontWeight = FontWeight.SemiBold)
    }
}

private fun markSymbol(mark: String): String = when (mark) {
    "SPADE" -> "♠"
    "HEART" -> "♥"
    "DIAMOND" -> "♦"
    "CLUB" -> "♣"
    else -> mark
}
