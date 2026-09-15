package com.example.daifugo.android.ui

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.daifugo.android.data.CardDto
import com.example.daifugo.android.data.GameStateDto
import com.example.daifugo.android.data.PlayerDto
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

    /*
     * 全端末共有の野獣イベントを音声へ変換する。
     * 実音声はres/rawへ後から置くだけでよく、未配置時は無音で動作する。
     */
    LaunchedEffect(viewModel) {
        viewModel.audioCues.collect { cue ->
            YajuAudioPlayer.play(context, cue)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            AppHeader(state)
            MessageStrip(
                state = state,
                onDismiss = viewModel::clearMessage,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (state.screen) {
                    DaifugoScreen.LOGIN -> LoginScreen(state, viewModel)
                    DaifugoScreen.MAIN_MENU -> MainMenuScreen(state, viewModel)
                    DaifugoScreen.MULTIPLAYER -> LobbyScreen(state, viewModel)
                    DaifugoScreen.CPU_SETUP -> CpuSetupScreen(state, viewModel)
                    DaifugoScreen.ROOM -> RoomScreen(state, viewModel)
                    DaifugoScreen.GAME -> GameScreen(state, viewModel)
                    DaifugoScreen.RESULT -> ResultScreen(state, viewModel)
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
                                Text(if (state.gameState?.gameMode == "CPU_LOCAL" || state.screen == DaifugoScreen.CPU_SETUP) "処理中…" else "通信中…")
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
            val local = state.gameState?.gameMode == "CPU_LOCAL"
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

@Composable
private fun MainMenuScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("メインメニュー", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("遊び方を選択してください", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftGreen), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🌐 マルチプレイ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("サーバーへ接続してPC・Android・iPhoneで同じ卓に参加するオンライン対戦。")
                    Button(onClick = viewModel::openMultiplayer, modifier = Modifier.fillMaxWidth()) { Text("マルチプレイへ") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftGold), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🤖 【ひとりでイク】", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("完全オフライン。サーバー不要でCPU人数・難易度・特殊ルールを設定して即対戦。N-GODは自己対戦学習済み。")
                    Button(onClick = viewModel::openCpuSetup, modifier = Modifier.fillMaxWidth()) { Text("CPU戦へ") }
                }
            }
        }
    }
}

@Composable
private fun CpuSetupScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("【ひとりでイク】", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("CPU戦セットアップ · オフライン", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = viewModel::backToMenu) { Text("← メニュー") }
            }
        }
        item {
            OutlinedTextField(
                value = state.playerName,
                onValueChange = viewModel::setPlayerName,
                label = { Text("プレイヤー名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item { CpuCountChoiceRow(state.cpuCount, viewModel::setCpuCount) }
        item { ChoiceRow("難易度", listOf("EASY","NORMAL","HARD","N_GOD"), state.cpuDifficulty, viewModel::setCpuDifficulty) { difficultyLabel(it) } }
        item { ChoiceRow("ジョーカー", listOf(0,1,2), state.cpuJokerCount, viewModel::setCpuJokerCount) { "${it}枚" } }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("特殊ルール", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    RuleSwitch("革命", state.ruleRevolution, viewModel::setRuleRevolution)
                    RuleSwitch("8切り", state.ruleEightCut, viewModel::setRuleEightCut, enabled = !state.ruleYaju)
                    RuleSwitch("マーク縛り", state.ruleMarkLock, viewModel::setRuleMarkLock)
                    RuleSwitch("7渡し", state.ruleSevenTransfer, viewModel::setRuleSevenTransfer)
                    RuleSwitch("野獣ルール", state.ruleYaju, viewModel::setRuleYaju)
                    RuleSwitch("Jバック", state.ruleJackBack, viewModel::setRuleJackBack)
                    RuleSwitch("禁止上がり", state.ruleForbiddenFinish, viewModel::setRuleForbiddenFinish)
                    if (state.ruleYaju) Text("※野獣ルールON時は8切り必須", style = MaterialTheme.typography.labelSmall, color = CasinoGold)
                }
            }
        }
        item {
            Button(onClick = viewModel::startCpuGame, modifier = Modifier.fillMaxWidth()) {
                Text("オフラインでCPU戦スタート", fontWeight = FontWeight.ExtraBold)
            }
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ロビー", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(state.serverUrl, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = viewModel::backToMenu) { Text("← メニュー") }
            }
        }
        item {
            CasinoPanel(title = "プレイヤー") {
                OutlinedTextField(
                    value = state.playerName,
                    onValueChange = viewModel::setPlayerName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("プレイヤー名") },
                    singleLine = true,
                )
            }
        }
        item {
            CasinoPanel(title = "新しいテーブル") {
                Text("あなたがホストになります。2〜8人でゲーム開始できます。")
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = viewModel::createRoom,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("＋ 部屋を作る", fontWeight = FontWeight.Bold) }
            }
        }
        item {
            CasinoPanel(title = "部屋へ参加") {
                OutlinedTextField(
                    value = state.roomIdInput,
                    onValueChange = viewModel::setRoomIdInput,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ルームID") },
                    singleLine = true,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = viewModel::joinRoom,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("参加する") }
            }
        }
    }
}

@Composable
private fun RoomScreen(state: DaifugoUiState, viewModel: DaifugoViewModel) {
    val game = state.gameState ?: return
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("対戦ルーム", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
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
                        Text("コピー")
                    }
                }
                Text("このIDを友だちに共有してください。", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    StatusBar(game)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                if (game.gameMode == "CPU_LOCAL" && state.cpuActionHistory.isNotEmpty()) {
                    item {
                        CpuActionHistoryCard(state.cpuActionHistory)
                    }
                }
                item {
                    GameTable(game)
                }
                item {
                    val turnLabel = when {
                        state.cpuTurnAnimation?.type == CpuAnimationType.THINKING ->
                            "${state.cpuTurnAnimation.playerName} が考えています…"
                        state.cpuTurnInProgress ->
                            "${state.cpuTurnAnimation?.playerName ?: game.currentPlayer?.playerName ?: "CPU"} のプレイ中"
                        game.isMyTurn -> "あなたの手番です"
                        else -> "${game.currentPlayer?.playerName ?: "-"} の手番"
                    }
                    Text(
                        turnLabel,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (game.isMyTurn && !state.cpuTurnInProgress) {
                            CasinoGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (game.isMySevenTransfer) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SoftGold),
                            border = BorderStroke(1.dp, CasinoGold),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("7渡し", fontWeight = FontWeight.Black, color = CasinoGold)
                                Text(
                                    "${game.sevenTransferTarget?.playerName ?: "隣のプレイヤー"}へ" +
                                        " ${game.sevenTransfer.cardCount}枚渡してください",
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "野獣対象者は最後の8・10を渡すことはできません。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerAvatar(
                            avatarIndex = game.avatarIndex(self.playerId),
                            size = 42.dp,
                            highlighted = game.isMyTurn && !state.cpuTurnInProgress,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(self.playerName, fontWeight = FontWeight.Black)
                            Text(
                                "あなたの手札  ${self.handCount}枚" +
                                    if (self.yajuActive) "  ·  野獣:${self.yajuStatus}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
                    ) {
                        itemsIndexed(self.hand) { index, card ->
                            PlayingCard(
                                card = card,
                                selected = index in state.selectedCardIndices,
                                enabled = game.isMyTurn && !state.cpuTurnInProgress,
                                onClick = { viewModel.toggleCard(index) },
                            )
                        }
                    }
                }
            }

            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::pass,
                        enabled = !state.cpuTurnInProgress &&
                            game.isMyTurn &&
                            !game.isMySevenTransfer &&
                            game.field.cards.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { Text("パス") }
                    Button(
                        onClick = viewModel::playSelected,
                        enabled = !state.cpuTurnInProgress &&
                            game.isMyTurn &&
                            state.selectedCardIndices.isNotEmpty() &&
                            (!game.isMySevenTransfer ||
                                state.selectedCardIndices.size == game.sevenTransfer.cardCount),
                        modifier = Modifier.weight(1.5f),
                    ) {
                        Text(
                            if (game.isMySevenTransfer) {
                                "渡す (${state.selectedCardIndices.size}/${game.sevenTransfer.cardCount})"
                            } else {
                                "出す (${state.selectedCardIndices.size})"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        if (game.gameMode == "CPU_LOCAL") {
            state.cpuTurnAnimation?.let { animation ->
                CpuTurnAnimationOverlay(
                    animation = animation,
                    opponents = opponents,
                )
            }
        }
    }
}

/** 直近のCPU行動を残し、8切りなどで場が流れても何を出したか確認できるようにする。 */
@Composable
private fun CpuActionHistoryCard(history: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("PLAY LOG", color = CasinoGold, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
            history.take(6).forEachIndexed { index, line ->
                Text(
                    text = if (index == 0) "▶ $line" else "  $line",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == 0) CasinoGreenDark else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    modifier = Modifier.offset(y = 104.dp),
                    shape = RoundedCornerShape(100.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    shadowElevation = 8.dp,
                ) {
                    Text(
                        "${animation.playerName}  思考中…",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            CpuAnimationType.PLAY -> {
                val p = progress.value
                Column(
                    modifier = Modifier
                        .offset(
                            x = startX * (1f - p),
                            y = (92 + (240 * p)).dp,
                        )
                        .graphicsLayer {
                            val scale = 0.82f + (0.18f * p)
                            scaleX = scale
                            scaleY = scale
                            alpha = 0.72f + (0.28f * p)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.80f),
                        shape = RoundedCornerShape(100.dp),
                    ) {
                        Text(
                            "${animation.playerName} が出した！",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy((-7).dp)) {
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
                        .offset(y = (190 + (35 * p)).dp)
                        .graphicsLayer {
                            scaleX = 0.88f + (0.18f * p)
                            scaleY = 0.88f + (0.18f * p)
                            alpha = 1f - (0.28f * p)
                        },
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 10.dp,
                ) {
                    Text(
                        "${animation.playerName}  PASS",
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            CpuAnimationType.SEVEN_TRANSFER -> {
                val p = progress.value
                Column(
                    modifier = Modifier
                        .offset(
                            x = ((-80) + (160 * p)).dp,
                            y = 210.dp,
                        )
                        .graphicsLayer { alpha = 0.78f + (0.22f * p) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        color = SoftGold,
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, CasinoGold),
                    ) {
                        Text(
                            "${animation.playerName} → ${animation.targetPlayerName ?: "隣"}  7渡し",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            color = CasinoGold,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy((-9).dp)) {
                        repeat(animation.cardCount) { index ->
                            CardBack(
                                modifier = Modifier
                                    .zIndex(index.toFloat()),
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
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (game.revolution) item { RulePill("♛ 革命", SoftGold, CasinoGold) }
        if (game.jackBack) item { RulePill("J BACK", Color(0xFFE8E1FF), Color(0xFF5B3AA6)) }
        game.lockedMark?.let { mark -> item { RulePill("${markSymbol(mark)} 縛り", SoftGreen, CasinoGreen) } }
        if (game.players.any { it.yajuActive }) {
            item { RulePill("野獣ルール", SoftGold, CasinoGold) }
        }
        item { RulePill("Room ${game.roomId}", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun RulePill(text: String, background: Color, foreground: Color) {
    Surface(color = background, shape = RoundedCornerShape(100.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = foreground,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
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
    Card(
        modifier = Modifier.width(168.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (current) Color(0xFFFFF7E3) else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = if (current) 2.dp else 1.dp,
            color = if (current) CasinoGold else Color(0xFFE2DED2),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (current) 5.dp else 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatar(
                    avatarIndex = avatarIndex,
                    size = 40.dp,
                    highlighted = current,
                    photoAssetName = photoAssetName,
                )
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        player.playerName,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when {
                            player.rank != null -> "${player.rank}位"
                            player.passed -> "PASS"
                            current -> "TURN"
                            else -> if (player.cpu) player.cpuDifficulty ?: "CPU" else "PLAYER"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            player.rank != null -> CasinoGold
                            player.passed -> MaterialTheme.colorScheme.error
                            current -> CasinoGreen
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            OpponentHandBacks(cardCount = player.handCount)

            if (player.yajuActive) {
                Surface(
                    color = Color(0xFF211507),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, CasinoGold),
                ) {
                    Text(
                        "野獣  8 → 10",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color(0xFFFFD369),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                    )
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF073D2B)),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(2.dp, Color(0xFFB78A34)),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A5139), Color(0xFF073D2B)),
                    )
                )
                .padding(vertical = 30.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "TABLE",
                color = Color(0xFFD6BA75),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(12.dp))
            if (game.field.cards.isEmpty()) {
                Surface(
                    color = Color.White.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                ) {
                    Text(
                        "場は空です",
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                        color = Color.White.copy(alpha = 0.72f),
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
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
                game.field.combinationType?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        it,
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
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
    val width = if (compact) 54.dp else 64.dp
    val height = if (compact) 78.dp else 94.dp
    val cornerSize = if (compact) 12.sp else 14.sp

    Card(
        modifier = Modifier
            .width(width)
            .height(height)
            .offset(y = if (selected) (-11).dp else 0.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) CasinoGold else Color(0xFFBDB7A9),
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFFFF7DF) else Color(0xFFFFFEF8),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 5.dp else 6.dp),
        ) {
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
                    Text("♛", color = CasinoGold, fontSize = if (compact) 24.sp else 29.sp)
                    Text(
                        "JOKER",
                        color = CasinoGold,
                        fontSize = if (compact) 8.sp else 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
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
                        fontSize = if (compact) 27.sp else 33.sp,
                        fontWeight = FontWeight.Normal,
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
    if (gameMode != "CPU_LOCAL") return null

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
    val ranking = game.players.sortedWith(compareBy<PlayerDto> { it.rank ?: Int.MAX_VALUE }.thenBy { it.playerName })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("GAME SET", color = CasinoGold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text("リザルト", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        }
        item {
            CasinoPanel(title = "最終順位") {
                ranking.forEachIndexed { index, player ->
                    ResultRow(
                        player = player,
                        avatarIndex = game.avatarIndex(player.playerId),
                        photoAssetName = game.cpuAvatarAssetName(player.playerId),
                    )
                    if (index != ranking.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
        item {
            OutlinedButton(onClick = viewModel::leaveRoom, modifier = Modifier.fillMaxWidth()) {
                Text("ロビーへ戻る")
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        PlayerAvatar(
            avatarIndex = avatarIndex,
            size = 42.dp,
            highlighted = player.rank == 1,
            photoAssetName = photoAssetName,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            player.rank?.let { "${it}位" } ?: "—",
            modifier = Modifier.width(54.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = if (player.rank == 1) CasinoGold else MaterialTheme.colorScheme.onSurface,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(player.playerName, fontWeight = FontWeight.Bold)
            if (player.self) Text("あなた", style = MaterialTheme.typography.labelSmall, color = CasinoGreen)
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
