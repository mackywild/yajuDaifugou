package com.example.daifugo.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
                "ONLINE CARD GAME · v0.4.1 CPU MOTION",
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
            CasinoPanel(title = "v0.4.1 CPU MOTION") {
                FeatureLine("♣", "2〜4人オンライン対戦")
                FeatureLine("⚡", "WebSocketリアルタイム更新")
                FeatureLine("♛", "革命・8切り・7渡し・野獣ルール")
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
        item { ChoiceRow("CPU人数", listOf(1,2,3), state.cpuCount, viewModel::setCpuCount) { "${it}人" } }
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
                Text("あなたがホストになります。2〜4人集まったらゲーム開始できます。")
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
            CasinoPanel(title = "プレイヤー ${game.players.size}/4") {
                game.players.forEachIndexed { index, player ->
                    PlayerLobbyRow(player, index + 1)
                    if (index != game.players.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                repeat(4 - game.players.size) { index ->
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
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = if (player.self) SoftGreen else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(seat.toString(), fontWeight = FontWeight.Black)
            }
        }
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
                            OpponentCard(player, game.currentPlayerId == player.playerId)
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
                    Text(
                        "あなたの手札  ${self.handCount}枚" +
                            if (self.yajuActive) "  ·  野獣:${self.yajuStatus}" else "",
                        fontWeight = FontWeight.Bold,
                    )
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
            Text("CPU 行動ログ", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
            history.take(4).forEachIndexed { index, line ->
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
    val startX = when (opponentIndex) {
        0 -> (-92).dp
        1 -> 0.dp
        else -> 92.dp
    }

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
        }
    }
}

@Composable
private fun StatusBar(game: GameStateDto) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (game.revolution) item { RulePill("♛ 革命", SoftGold, CasinoGold) }
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
private fun OpponentCard(player: PlayerDto, current: Boolean) {
    Card(
        modifier = Modifier.width(142.dp),
        colors = CardDefaults.cardColors(containerColor = if (current) SoftGold else MaterialTheme.colorScheme.surface),
        border = if (current) BorderStroke(1.dp, CasinoGold) else null,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(player.playerName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("🂠 ${player.handCount}枚", style = MaterialTheme.typography.bodyMedium)
            if (player.yajuActive) {
                Text("野獣 ${player.yajuStatus}", color = CasinoGold, fontWeight = FontWeight.Bold)
            }
            when {
                player.rank != null -> Text("${player.rank}位", color = CasinoGold, fontWeight = FontWeight.Black)
                player.passed -> Text("PASS", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                current -> Text("TURN", color = CasinoGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GameTable(game: GameStateDto) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CasinoGreenDark),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("場のカード", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
            if (game.field.cards.isEmpty()) {
                Text("— EMPTY —", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                    itemsIndexed(game.field.cards) { _, card ->
                        PlayingCard(card = card, selected = false, enabled = false, compact = true, onClick = {})
                    }
                }
                game.field.combinationType?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun PlayingCard(
    card: CardDto,
    selected: Boolean,
    enabled: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val redSuit = card.suit == "HEART" || card.suit == "DIAMOND"
    val width = if (compact) 54.dp else 62.dp
    val height = if (compact) 78.dp else 92.dp

    Card(
        modifier = Modifier
            .width(width)
            .height(height)
            .offset(y = if (selected) (-10).dp else 0.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) CasinoGold else Color(0xFFD7D7D7)),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFFFF9E9) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 6.dp else 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (card.joker) "★" else card.displaySuit,
                color = if (redSuit) Danger else Color(0xFF181818),
                fontSize = if (compact) 17.sp else 20.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                card.displayRank,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = if (card.joker) CasinoGold else if (redSuit) Danger else Color(0xFF181818),
                fontSize = if (card.displayRank == "JOKER") 10.sp else if (compact) 20.sp else 24.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                if (card.joker) "★" else card.displaySuit,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                color = if (redSuit) Danger else Color(0xFF181818),
                fontSize = 13.sp,
            )
        }
    }
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
                    ResultRow(player)
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
private fun ResultRow(player: PlayerDto) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
