package com.example.daifugo.android.local

import com.example.daifugo.android.data.CardDto
import com.example.daifugo.android.data.FieldDto
import com.example.daifugo.android.data.GameEventDto
import com.example.daifugo.android.data.GameStateDto
import com.example.daifugo.android.data.PlayerDto
import com.example.daifugo.android.data.RuleSettingsDto
import com.example.daifugo.android.data.SevenTransferDto
import com.example.daifugo.game.config.GameLimits
import com.example.daifugo.game.config.GameRuleSettings
import com.example.daifugo.game.cpu.CpuDifficulty
import com.example.daifugo.game.cpu.CpuStrategyFactory
import com.example.daifugo.game.cpu.LegalMoveGenerator
import com.example.daifugo.game.domain.Card
import com.example.daifugo.game.domain.GameState
import com.example.daifugo.game.domain.Mark
import com.example.daifugo.game.domain.Player
import com.example.daifugo.game.domain.Rank
import com.example.daifugo.game.service.GameEngine
import com.example.daifugo.game.service.GameEngineFactory
import com.example.daifugo.game.service.GameInitializer
import com.example.daifugo.game.service.YajuRuleService
import java.util.UUID

/** CPUが実行した1アクションの種類。 */
enum class LocalCpuActionType {
    PLAY,
    PASS,
    SEVEN_TRANSFER,
}

/**
 * UI演出用のCPUアクション結果。
 *
 * CPUの手を即座に画面へ反映せず、先にこの内容を使って演出したあと
 * [stateAfter] を表示することで、「何を出したか」を人間が追えるようにする。
 */
data class LocalCpuTurnResult(
    val playerId: String,
    val playerName: String,
    val type: LocalCpuActionType,
    val cards: List<CardDto>,
    val targetPlayerName: String? = null,
    val stateAfter: GameStateDto,
)

/**
 * Android端末だけで完結するCPU戦ランナー。
 *
 * Spring Boot / REST / WebSocketを一切使用せず、既存のゲームコアを
 * アプリ内で直接実行する。v0.4.1からCPUの手番を1手ずつUIへ返し、
 * CPUのプレイ演出を挟めるようにしている。
 */
class LocalCpuGameManager {
    private val moveGenerator = LegalMoveGenerator()
    private val strategyFactory = CpuStrategyFactory()
    private val yajuRuleService = YajuRuleService()

    private var state: GameState? = null
    private var engine: GameEngine? = null
    private var settings: GameRuleSettings? = null
    private var humanPlayerId: String? = null
    private var localGameId: String? = null
    private var localGameMode: String = "CPU_LOCAL"

    /** 人間1人 + CPU 1〜7人でローカルゲームを開始する。 */
    fun start(
        humanName: String,
        cpuCount: Int,
        difficulty: String,
        rules: RuleSettingsDto,
        yajuChallenge: Boolean = false,
    ): GameStateDto {
        require(cpuCount in 1..GameLimits.MAX_CPU_COUNT) { "CPU人数は1〜7人で指定してください" }

        val coreRules = rules.toCore()
        val cpuDifficulty = CpuDifficulty.valueOf(difficulty)
        val humanId = "human-${UUID.randomUUID()}"
        val players = mutableListOf<Player>()
        players += Player(humanId, humanName)
        repeat(cpuCount) { index ->
            players += Player.cpu(
                "cpu-${index + 1}-${UUID.randomUUID()}",
                "CPU-${index + 1}",
                cpuDifficulty,
            )
        }

        val gameState = GameState(players)
        GameInitializer(coreRules.jokerCount()).initialize(gameState)

        if (yajuChallenge) {
            require(coreRules.yajuRule()) { "チャレンジモードでは野獣ルールをONにしてください" }
            guaranteeYajuOpeningHand(gameState, humanId)
        }

        if (coreRules.yajuRule()) {
            yajuRuleService.initializeTargets(gameState)
        }

        state = gameState
        settings = coreRules
        engine = GameEngineFactory().create(coreRules)
        humanPlayerId = humanId
        localGameMode = if (yajuChallenge) "YAJU_CHALLENGE" else "CPU_LOCAL"
        localGameId = if (yajuChallenge) {
            "CHALLENGE-${UUID.randomUUID().toString().take(8).uppercase()}"
        } else {
            "LOCAL-${UUID.randomUUID().toString().take(8).uppercase()}"
        }

        // v0.4.1: CPUをここで一気に処理しない。
        // ViewModelがexecuteNextCpuTurn()を1手ずつ呼び、演出を挟む。
        return snapshot()
    }

    /** 人間が選択したカードを提出する。7渡し待ちの場合は譲渡として処理する。 */
    fun play(cards: List<CardDto>): GameStateDto {
        val gameState = requireState()
        val gameEngine = requireEngine()
        val humanId = requireHumanId()
        val selected = cards.map { it.toCore() }

        if (gameState.hasPendingSevenTransfer()) {
            gameEngine.transferSeven(gameState, humanId, selected)
        } else {
            gameEngine.play(gameState, humanId, selected)
        }

        return snapshot()
    }

    /** 人間がパスする。 */
    fun pass(): GameStateDto {
        val gameState = requireState()
        require(!gameState.hasPendingSevenTransfer()) { "7渡しするカードを選択してください" }
        requireEngine().pass(gameState, requireHumanId())
        return snapshot()
    }

    /** 現在CPUの手番かを返す。 */
    fun hasCpuTurn(): Boolean {
        val gameState = state ?: return false
        return !gameState.isFinished && gameState.currentPlayer.isCpu
    }

    /** 現在状態をUI向けDTOとして返す。 */
    fun currentSnapshot(): GameStateDto = snapshot()

    /**
     * CPUの手を1手だけ決定・実行する。
     *
     * 返却されたカード情報をUIで約3秒演出したあと、[LocalCpuTurnResult.stateAfter]
     * を表示する想定。CPUが連続する場合でも1手ずつ確認できる。
     */
    fun executeNextCpuTurn(): LocalCpuTurnResult {
        val gameState = requireState()
        val gameEngine = requireEngine()
        val coreRules = requireSettings()

        check(!gameState.isFinished) { "ゲームは終了しています" }
        val current = gameState.currentPlayer
        check(current.isCpu) { "現在の手番はCPUではありません" }
        val strategy = strategyFactory.create(current.cpuDifficulty)

        if (gameState.hasPendingSevenTransfer()) {
            val pending = gameState.pendingSevenTransfer
            check(pending.sourcePlayerId() == current.id) {
                "7渡し元と現在CPUが一致しません"
            }

            val candidates = moveGenerator.generateSevenTransfers(
                current,
                pending.cardCount(),
                coreRules,
            )
            check(candidates.isNotEmpty()) { "CPUが7渡し可能なカードを選べません" }

            val selected = strategy.chooseSevenTransfer(
                gameState,
                current,
                candidates,
                coreRules,
            )
            val targetName = gameState.players
                .firstOrNull { it.id == pending.targetPlayerId() }
                ?.name

            gameEngine.transferSeven(gameState, current.id, selected)

            return LocalCpuTurnResult(
                playerId = current.id,
                playerName = current.name,
                type = LocalCpuActionType.SEVEN_TRANSFER,
                cards = selected.map { it.toDto() },
                targetPlayerName = targetName,
                stateAfter = snapshot(),
            )
        }

        val legalMoves = moveGenerator.generate(gameState, current, coreRules)
        check(legalMoves.isNotEmpty()) { "CPUに合法手がありません" }
        val move = strategy.chooseMove(gameState, current, legalMoves, coreRules)

        return if (move.pass()) {
            gameEngine.pass(gameState, current.id)
            LocalCpuTurnResult(
                playerId = current.id,
                playerName = current.name,
                type = LocalCpuActionType.PASS,
                cards = emptyList(),
                stateAfter = snapshot(),
            )
        } else {
            val selected = move.cards()
            gameEngine.play(gameState, current.id, selected)
            LocalCpuTurnResult(
                playerId = current.id,
                playerName = current.name,
                type = LocalCpuActionType.PLAY,
                cards = selected.map { it.toDto() },
                stateAfter = snapshot(),
            )
        }
    }

    /** ローカル対戦状態を破棄する。 */
    fun close() {
        state = null
        engine = null
        settings = null
        humanPlayerId = null
        localGameId = null
        localGameMode = "CPU_LOCAL"
    }

    /**
     * チャレンジモードでは人間プレイヤーの初期手札に8と10を最低1枚ずつ保証する。
     * 配牌後に他プレイヤーとカードを1対1交換するため、デッキ総数や各手札枚数は変化しない。
     * ダイヤ3は交換対象から除外し、開始プレイヤー判定も壊さない。
     */
    private fun guaranteeYajuOpeningHand(gameState: GameState, humanId: String) {
        val human = gameState.players.first { it.id == humanId }
        guaranteeRank(gameState, human, Rank.EIGHT)
        guaranteeRank(gameState, human, Rank.TEN)
        gameState.players.forEach { it.sortHand() }
    }

    private fun guaranteeRank(gameState: GameState, human: Player, rank: Rank) {
        if (human.countRank(rank) > 0) return

        val donor = gameState.players.firstOrNull { player ->
            player.id != human.id && player.countRank(rank) > 0
        } ?: error("チャレンジ用の${rank.name}を確保できません")

        val incoming = donor.hand.first { it.rank == rank }
        val outgoing = human.hand.firstOrNull { card ->
            !(card.suit == Mark.DIAMOND && card.rank == Rank.THREE) &&
                card.rank != Rank.EIGHT &&
                card.rank != Rank.TEN
        } ?: error("チャレンジ用の交換カードを確保できません")

        human.removeCards(listOf(outgoing))
        donor.removeCards(listOf(incoming))
        human.addCard(incoming)
        donor.addCard(outgoing)
    }

    /** Android UIが既存のGameStateDtoをそのまま使えるようローカル状態を変換する。 */
    private fun snapshot(): GameStateDto {
        val gameState = requireState()
        val coreRules = requireSettings()
        val humanId = requireHumanId()
        val pending = gameState.pendingSevenTransfer
        val field = gameState.fieldCombination

        return GameStateDto(
            roomId = localGameId ?: "LOCAL",
            gameMode = localGameMode,
            ruleSettings = coreRules.toDto(),
            players = gameState.players.map { player ->
                val self = player.id == humanId
                PlayerDto(
                    playerId = player.id,
                    playerName = player.name,
                    handCount = player.cardCount,
                    hand = if (self) player.hand.map { it.toDto() } else emptyList(),
                    passed = player.isPassed,
                    rank = player.rank,
                    self = self,
                    yajuStatus = player.yajuStatus.name,
                    cpu = player.isCpu,
                    cpuDifficulty = player.cpuDifficulty?.name,
                )
            },
            currentPlayerId = if (gameState.isFinished) null else gameState.currentPlayer.id,
            field = FieldDto(
                cards = field?.cards?.map { it.toDto() }.orEmpty(),
                combinationType = field?.type?.name,
            ),
            revolution = gameState.isRevolution,
            jackBack = gameState.isJackBack,
            lockedMark = gameState.lockedMark?.name,
            host = true,
            started = true,
            finished = gameState.isFinished,
            sevenTransfer = SevenTransferDto(
                pending = pending != null,
                sourcePlayerId = pending?.sourcePlayerId(),
                targetPlayerId = pending?.targetPlayerId(),
                cardCount = pending?.cardCount() ?: 0,
            ),
            events = gameState.events.map {
                GameEventDto(
                    id = it.id(),
                    type = it.type().name,
                    playerId = it.playerId(),
                    playerName = it.playerName(),
                )
            },
        )
    }

    private fun requireState(): GameState = state ?: error("CPU戦が開始されていません")
    private fun requireEngine(): GameEngine = engine ?: error("CPU戦が開始されていません")
    private fun requireSettings(): GameRuleSettings = settings ?: error("CPU戦が開始されていません")
    private fun requireHumanId(): String = humanPlayerId ?: error("CPU戦が開始されていません")

    private fun RuleSettingsDto.toCore(): GameRuleSettings = GameRuleSettings(
        jokerCount,
        revolution,
        if (yajuRule) true else eightCut,
        markLock,
        sevenTransfer,
        yajuRule,
        jackBack,
        forbiddenFinish,
    )

    private fun GameRuleSettings.toDto(): RuleSettingsDto = RuleSettingsDto(
        jokerCount = jokerCount(),
        revolution = revolution(),
        eightCut = eightCut(),
        markLock = markLock(),
        sevenTransfer = sevenTransfer(),
        yajuRule = yajuRule(),
        jackBack = jackBack(),
        forbiddenFinish = forbiddenFinish(),
    )

    private fun CardDto.toCore(): Card = Card(
        Mark.valueOf(suit),
        Rank.valueOf(rank),
    )

    private fun Card.toDto(): CardDto = CardDto(
        suit = suit.name,
        rank = rank.name,
        joker = isJoker,
    )
}
