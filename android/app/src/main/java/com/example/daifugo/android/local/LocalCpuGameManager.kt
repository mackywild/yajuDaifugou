package com.example.daifugo.android.local

import com.example.daifugo.android.data.CardDto
import com.example.daifugo.android.data.FieldDto
import com.example.daifugo.android.data.GameEventDto
import com.example.daifugo.android.data.GameStateDto
import com.example.daifugo.android.data.PlayerDto
import com.example.daifugo.android.data.RuleSettingsDto
import com.example.daifugo.android.data.SevenTransferDto
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

/**
 * Android端末だけで完結するCPU戦ランナー。
 *
 * Spring Boot / REST / WebSocketを一切使用せず、既存のゲームコアを
 * アプリ内で直接実行する。CPU戦をバグ検出用途としてオフラインで
 * 使えることを最優先にしている。
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

    /** 人間1人 + CPU 1〜3人でローカルゲームを開始する。 */
    fun start(
        humanName: String,
        cpuCount: Int,
        difficulty: String,
        rules: RuleSettingsDto,
    ): GameStateDto {
        require(cpuCount in 1..3) { "CPU人数は1〜3人で指定してください" }

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
        if (coreRules.yajuRule()) {
            yajuRuleService.initializeTargets(gameState)
        }

        state = gameState
        settings = coreRules
        engine = GameEngineFactory().create(coreRules)
        humanPlayerId = humanId
        localGameId = "LOCAL-${UUID.randomUUID().toString().take(8).uppercase()}"

        processCpuTurns()
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

        processCpuTurns()
        return snapshot()
    }

    /** 人間がパスする。 */
    fun pass(): GameStateDto {
        val gameState = requireState()
        require(!gameState.hasPendingSevenTransfer()) { "7渡しするカードを選択してください" }
        requireEngine().pass(gameState, requireHumanId())
        processCpuTurns()
        return snapshot()
    }

    /** ローカル対戦状態を破棄する。 */
    fun close() {
        state = null
        engine = null
        settings = null
        humanPlayerId = null
        localGameId = null
    }

    /** CPUの手番が終わるか、ゲーム終了するまで自動進行する。 */
    private fun processCpuTurns() {
        val gameState = requireState()
        val gameEngine = requireEngine()
        val coreRules = requireSettings()

        var steps = 0
        while (!gameState.isFinished) {
            check(++steps <= 500) { "CPU自動進行が上限回数を超えました" }

            val current = gameState.currentPlayer
            if (!current.isCpu) return
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
                gameEngine.transferSeven(gameState, current.id, selected)
                continue
            }

            val legalMoves = moveGenerator.generate(gameState, current, coreRules)
            check(legalMoves.isNotEmpty()) { "CPUに合法手がありません" }
            val move = strategy.chooseMove(gameState, current, legalMoves, coreRules)
            if (move.pass()) {
                gameEngine.pass(gameState, current.id)
            } else {
                gameEngine.play(gameState, current.id, move.cards())
            }
        }
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
            gameMode = "CPU_LOCAL",
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
        forbiddenFinish,
    )

    private fun GameRuleSettings.toDto(): RuleSettingsDto = RuleSettingsDto(
        jokerCount = jokerCount(),
        revolution = revolution(),
        eightCut = eightCut(),
        markLock = markLock(),
        sevenTransfer = sevenTransfer(),
        yajuRule = yajuRule(),
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
