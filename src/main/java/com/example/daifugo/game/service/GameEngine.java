package com.example.daifugo.game.service;

import java.util.List;
import java.util.Objects;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GamePhase;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.PendingSevenTransfer;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.rule.PlayValidator;
import com.example.daifugo.game.rule.RuleEngine;
import com.example.daifugo.game.rule.RuleResult;
import com.example.daifugo.game.rule.finish.FinishValidationResult;
import com.example.daifugo.game.rule.finish.FinishValidator;


public class GameEngine {

    private final PlayValidator playValidator;
    private final TurnManager turnManager;
    private final RuleEngine ruleEngine;
    private final FinishValidator finishValidator;
    private final YajuRuleService yajuRuleService;
    private final GameRuleSettings ruleSettings;

    public GameEngine(
            PlayValidator playValidator,
            TurnManager turnManager,
            RuleEngine ruleEngine,
            FinishValidator finishValidator
    ) {
        this(
            playValidator,
            turnManager,
            ruleEngine,
            finishValidator,
            new YajuRuleService(),
            GameRuleSettings.standard()
        );
    }

    /**
     * ゲームエンジンを生成する。
     *
     * @param playValidator 通常プレイ判定
     * @param turnManager 手番管理
     * @param ruleEngine 特殊ルールエンジン
     * @param finishValidator 禁止上がり判定
     * @param yajuRuleService 野獣ルール判定
     */
    public GameEngine(
            PlayValidator playValidator,
            TurnManager turnManager,
            RuleEngine ruleEngine,
            FinishValidator finishValidator,
            YajuRuleService yajuRuleService
    ) {
        this(
                playValidator,
                turnManager,
                ruleEngine,
                finishValidator,
                yajuRuleService,
                GameRuleSettings.standard()
        );
    }

    /**
     * 対戦ルールを指定してゲームエンジンを生成する。
     */
    public GameEngine(
            PlayValidator playValidator,
            TurnManager turnManager,
            RuleEngine ruleEngine,
            FinishValidator finishValidator,
            YajuRuleService yajuRuleService,
            GameRuleSettings ruleSettings
    ) {
        this.playValidator = Objects.requireNonNull(playValidator);
        this.turnManager = Objects.requireNonNull(turnManager);
        this.ruleEngine = Objects.requireNonNull(ruleEngine);
        this.finishValidator = Objects.requireNonNull(finishValidator);
        this.yajuRuleService = Objects.requireNonNull(yajuRuleService);
        this.ruleSettings = Objects.requireNonNull(ruleSettings);
    }

    public void play(
            GameState state,
            String playerId,
            List<Card> selectedCards
    ) {
        validateGameState(state);
        ensureNoPendingSevenTransfer(state);

        Player player = getCurrentPlayer(state, playerId);

        Objects.requireNonNull(
            selectedCards,
            "selectedCards must not be null"
        );

        if (selectedCards.isEmpty()) {
            throw new IllegalArgumentException(
                "カードが選択されていません"
            );
        }

        boolean earlyShotCandidate = state.isEarlyShotEligible(playerId);

        CardCombination selectedCombination =
            CardCombination.of(selectedCards);

        boolean canPlay = playValidator.canPlay(
            selectedCombination,
            state.getFieldCombination(),
            state.isStrengthReversed(),
            state.getLockedMark(),
            state.isSpadeThreeJokerReturnActive()
        );

        if (!canPlay) {
            throw new IllegalArgumentException(
                "そのカードは場に出せません"
            );
        }

        YajuPlayDecision yajuDecision = ruleSettings.yajuRule()
            ? yajuRuleService.validatePlay(player, selectedCards)
            : YajuPlayDecision.none();

        FinishValidationResult finishResult =
            finishValidator.validate(
                state,
                player,
                selectedCombination
            );

        /*
         * 禁止上がりの場合でもカードを出すこと自体は許可する。
         *
         * ここでプレイを拒否すると、残り1枚が禁止カードで
         * 場が空の場合に「カードも出せない・パスもできない」
         * 状態となり、ゲームが進行不能になるため。
         *
         * 禁止上がりはプレイ後に反則上がりとして扱い、
         * 空いている最下位順位を確定する。
         */
        boolean forbiddenFinish =
            !finishResult.isAllowed() || yajuDecision.penaltyFinish();

        CardCombination previousField = state.getFieldCombination();

        player.removeCards(selectedCards);

        int playerIndex =
            state.getCurrentPlayerIndex();
        
        state.updateField(
            selectedCombination,
            playerIndex
        );

        /*
         * JOKERをスペード3で返した場合、そのスペード3をこのトリックの最強札とする。
         * 場が流れるまでは他のカードを一切提出できず、残りプレイヤーはPASSのみ。
         */
        if (previousField != null
                && previousField.isSingleJoker()
                && selectedCombination.isSingleSpadeThree()) {
            state.activateSpadeThreeJokerReturn();
        }

        player.clearPass();

        /*
         * 早漏判定はカード除去と場更新が成功した後に確定する。
         * これにより、野獣ルール等の事前検証でプレイが拒否された場合に
         * 判定権だけが消費されることを防ぐ。
         */
        if (earlyShotCandidate) {
            state.clearEarlyShotEligibility();
            if (state.isStrengthReversed() && isSingleRank(selectedCards, Rank.THREE)) {
                state.emitEvent(com.example.daifugo.game.domain.GameEventType.EARLY_SHOT, player);
            }
        }

        RuleResult ruleResult =
            ruleEngine.applyRules(
                state,
                player,
                previousField,
                selectedCombination
            );
        
        if (ruleResult.shouldLockMark()) {
            state.lockMark(ruleResult.getMarkToLock());
        }


        if (ruleSettings.yajuRule() && yajuDecision.startsEightStep()) {
            yajuRuleService.applyEightStep(player);
        }

        if (player.hasNoCards()) {
            if (ruleSettings.yajuRule() && yajuDecision.successfulFinish()) {
                yajuRuleService.completeYajuFinish(state, player);
            } else if (ruleSettings.yajuRule() && yajuDecision.penaltyFinish()) {
                yajuRuleService.applyPenalty(player);
            }

            if (forbiddenFinish) {
                assignPenaltyRank(state, player);
            } else {
                assignNormalRank(state, player);
            }
        }

        if (shouldFinishGame(state)) {
            assignLastRank(state);
            state.finish();
            return;
        }

        int sevenCount = ruleSettings.sevenTransfer()
                ? countRank(selectedCards, Rank.SEVEN)
                : 0;
        int transferableCardCount = ruleSettings.yajuRule()
                ? yajuRuleService.getTransferableCardCount(player)
                : player.getCardCount();
        int transferCount = Math.min(sevenCount, transferableCardCount);

        if (!player.hasFinished() && transferCount > 0) {
            Player transferTarget = findNextUnfinishedPlayer(state, playerIndex);
            boolean clearFieldAfterTransfer =
                ruleResult.shouldClearField() || turnManager.shouldClearField(state);

            state.beginSevenTransfer(
                new PendingSevenTransfer(
                    player.getId(),
                    transferTarget.getId(),
                    transferCount,
                    clearFieldAfterTransfer
                )
            );
            return;
        }

        if (ruleResult.shouldClearField()) {
            turnManager.startNewTrick(state);
            return;
        }

        if (turnManager.shouldClearField(state)) {
            turnManager.startNewTrick(state);
            return;
        }

        turnManager.moveToNextPlayer(state);
        armEarlyShotForCurrentPlayerIfPending(state);
    }

    public void pass(
            GameState state,
            String playerId
    ) {
        validateGameState(state);
        ensureNoPendingSevenTransfer(state);

        Player player = getCurrentPlayer(state, playerId);

        if (state.getFieldCombination() == null) {
            throw new IllegalStateException(
                "場が空のときはパスできません"
            );
        }

        player.pass();

        if (state.isEarlyShotEligible(playerId)) {
            state.clearEarlyShotEligibility();
        }

        if (turnManager.shouldClearField(state)) {
            turnManager.startNewTrick(state);
            return;
        }

        turnManager.moveToNextPlayer(state);
        armEarlyShotForCurrentPlayerIfPending(state);
    }

    /**
     * 保留中の7渡しを実行する。
     *
     * 7を出した枚数（ただし実際に渡せる枚数まで）と同じ枚数を、
     * 時計回りで次の未上がりプレイヤーへ渡す。
     * 野獣対象者は最後の8/10を渡して野獣ルールを手放すことはできない。
     * 受け手に8と10が揃った場合は、その場で新たな野獣対象者となる。
     *
     * @param state ゲーム状態
     * @param playerId 渡す側プレイヤーID
     * @param transferCards 渡すカード
     */
    public void transferSeven(
            GameState state,
            String playerId,
            List<Card> transferCards
    ) {
        validateGameState(state);
        if (!ruleSettings.sevenTransfer()) {
            throw new IllegalStateException("7渡しは無効です");
        }
        Objects.requireNonNull(transferCards, "transferCards must not be null");

        PendingSevenTransfer pending = state.getPendingSevenTransfer();
        if (pending == null) {
            throw new IllegalStateException("7渡し待ちではありません");
        }

        if (!pending.sourcePlayerId().equals(playerId)) {
            throw new IllegalStateException("7渡しを実行できるプレイヤーではありません");
        }

        if (transferCards.size() != pending.cardCount()) {
            throw new IllegalArgumentException(
                "7渡しでは" + pending.cardCount() + "枚選択してください"
            );
        }

        Player source = findPlayerById(state, pending.sourcePlayerId());
        Player target = findPlayerById(state, pending.targetPlayerId());

        if (ruleSettings.yajuRule()) {
            yajuRuleService.validateSevenTransfer(source, transferCards);
        }

        source.removeCards(transferCards);
        target.addCards(transferCards);
        source.sortHand();
        target.sortHand();

        /* 7渡しで8と10が揃った受け手にも野獣ルールを適用する。 */
        if (ruleSettings.yajuRule()) {
            yajuRuleService.activateIfEligible(state, target);
        }

        boolean clearFieldAfterTransfer = pending.clearFieldAfterTransfer();
        state.completeSevenTransfer();

        /*
         * 7渡しで手札を全て渡し切った場合も通常上がりとして扱う。
         * 野獣対象者は8/10保持義務があるため、この経路では上がれない。
         */
        if (source.hasNoCards() && !source.hasFinished()) {
            assignNormalRank(state, source);
        }

        if (shouldFinishGame(state)) {
            assignLastRank(state);
            state.finish();
            return;
        }

        if (clearFieldAfterTransfer) {
            turnManager.startNewTrick(state);
            return;
        }

        turnManager.moveToNextPlayer(state);
        armEarlyShotForCurrentPlayerIfPending(state);
    }

    /** 7渡し保留中は通常のplay/passを禁止する。 */
    private void ensureNoPendingSevenTransfer(GameState state) {
        if (state.hasPendingSevenTransfer()) {
            throw new IllegalStateException(
                "7渡しするカードを選択してください"
            );
        }
    }

    private void armEarlyShotForCurrentPlayerIfPending(GameState state) {
        if (state.isEarlyShotArmPending() && state.isJackBack() && !state.isFinished()) {
            state.armEarlyShotFor(state.getCurrentPlayer().getId());
        }
    }


    private boolean isSingleRank(List<Card> cards, Rank rank) {
        return cards.size() == 1 && cards.get(0).getRank() == rank;
    }

    /** 指定ランクの枚数を数える。 */
    private int countRank(List<Card> cards, Rank rank) {
        return (int) cards.stream()
            .filter(card -> card.getRank() == rank)
            .count();
    }

    /** 時計回りで次の未上がりプレイヤーを取得する。 */
    private Player findNextUnfinishedPlayer(GameState state, int sourceIndex) {
        List<Player> players = state.getPlayers();
        for (int offset = 1; offset < players.size(); offset++) {
            Player candidate = players.get((sourceIndex + offset) % players.size());
            if (!candidate.hasFinished()) {
                return candidate;
            }
        }
        throw new IllegalStateException("7渡しの受け手が存在しません");
    }

    /** プレイヤーIDからプレイヤーを取得する。 */
    private Player findPlayerById(GameState state, String playerId) {
        return state.getPlayers().stream()
            .filter(player -> player.getId().equals(playerId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "プレイヤーが存在しません。playerId=" + playerId
            ));
    }

    private Player getCurrentPlayer(
            GameState state,
            String playerId
    ) {
        Objects.requireNonNull(
            playerId,
            "playerId must not be null"
        );

        Player currentPlayer = state.getCurrentPlayer();

        if (!currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException(
                "現在の手番ではありません"
            );
        }

        return currentPlayer;
    }

    /**
     * 通常上がりの順位を確定する。
     *
     * 反則上がりによって下位順位が先に使用されている可能性があるため、
     * 単純な「上がった人数 + 1」ではなく、空いている最上位順位を採用する。
     *
     * @param state ゲーム状態
     * @param player 上がったプレイヤー
     */
    private void assignNormalRank(
            GameState state,
            Player player
    ) {
        player.assignRank(findBestAvailableRank(state));
    }

    /**
     * 禁止上がりをしたプレイヤーの順位を確定する。
     *
     * 2・8・ジョーカー・スペード3などの禁止上がりは
     * 反則上がりとして扱い、空いている最下位順位を割り当てる。
     *
     * @param state ゲーム状態
     * @param player 反則上がりしたプレイヤー
     */
    private void assignPenaltyRank(
            GameState state,
            Player player
    ) {
        player.assignRank(findWorstAvailableRank(state));
    }

    /**
     * 未使用順位のうち最も上位の順位を取得する。
     *
     * @param state ゲーム状態
     * @return 未使用の最上位順位
     */
    private int findBestAvailableRank(GameState state) {
        int playerCount = state.getPlayers().size();

        for (int rank = 1; rank <= playerCount; rank++) {
            if (!isRankUsed(state, rank)) {
                return rank;
            }
        }

        throw new IllegalStateException(
            "割り当て可能な順位が存在しません"
        );
    }

    /**
     * 未使用順位のうち最も下位の順位を取得する。
     *
     * @param state ゲーム状態
     * @return 未使用の最下位順位
     */
    private int findWorstAvailableRank(GameState state) {
        int playerCount = state.getPlayers().size();

        for (int rank = playerCount; rank >= 1; rank--) {
            if (!isRankUsed(state, rank)) {
                return rank;
            }
        }

        throw new IllegalStateException(
            "割り当て可能な順位が存在しません"
        );
    }

    /**
     * 指定順位が既に使用済みか判定する。
     *
     * @param state ゲーム状態
     * @param rank 確認する順位
     * @return 使用済みの場合true
     */
    private boolean isRankUsed(
            GameState state,
            int rank
    ) {
        return state.getPlayers().stream()
            .map(Player::getRank)
            .filter(Objects::nonNull)
            .anyMatch(assignedRank -> assignedRank == rank);
    }

    private boolean shouldFinishGame(GameState state) {
        int activePlayerCount = state.getPlayers().size()
            - state.getFinishedPlayerCount();

        return activePlayerCount <= 1;
    }

    private void assignLastRank(GameState state) {
        state.getPlayers().stream()
            .filter(player -> !player.hasFinished())
            .findFirst()
            .ifPresent(player ->
                player.assignRank(findBestAvailableRank(state))
            );
    }

    private void validateGameState(GameState state) {
        Objects.requireNonNull(
            state,
            "state must not be null"
        );

        if (state.getPhase() != GamePhase.PLAYING) {
            throw new IllegalStateException(
                "ゲームがプレイ中ではありません"
            );
        }
    }
}
