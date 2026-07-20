package com.example.daifugo.game.service;

import java.util.List;
import java.util.Objects;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GamePhase;
import com.example.daifugo.game.domain.GameState;
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

    public GameEngine(
            PlayValidator playValidator,
            TurnManager turnManager,
            RuleEngine ruleEngine,
            FinishValidator finishValidator
    ) {
        this.playValidator =
            Objects.requireNonNull(playValidator);

        this.turnManager =
            Objects.requireNonNull(turnManager);

        this.ruleEngine =
            Objects.requireNonNull(ruleEngine);

        this.finishValidator =
            Objects.requireNonNull(finishValidator);
    }

    public void play(
            GameState state,
            String playerId,
            List<Card> selectedCards
    ) {
        validateGameState(state);

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

        CardCombination selectedCombination =
        	    CardCombination.of(selectedCards);

        	boolean canPlay = playValidator.canPlay(
        	    selectedCombination,
        	    state.getFieldCombination(),
        	    state.isRevolution()
        	);

        	if (!canPlay) {
        	    throw new IllegalArgumentException(
        	        "そのカードは場に出せません"
        	    );
        	}

        	FinishValidationResult finishResult =
        	    finishValidator.validate(
        	        state,
        	        player,
        	        selectedCombination
        	    );

        	if (!finishResult.isAllowed()) {
        	    throw new IllegalArgumentException(
        	        finishResult.getMessage()
        	    );
        	}

        player.removeCards(selectedCards);

        int playerIndex =
            state.getCurrentPlayerIndex();

        state.updateField(
            selectedCombination,
            playerIndex
        );

        player.clearPass();

        RuleResult ruleResult =
            ruleEngine.applyRules(
                state,
                player,
                selectedCombination
            );

        if (player.hasNoCards()) {
            assignRank(state, player);
        }

        if (shouldFinishGame(state)) {
            assignLastRank(state);
            state.finish();
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
    }

    public void pass(
            GameState state,
            String playerId
    ) {
        validateGameState(state);

        Player player = getCurrentPlayer(state, playerId);

        if (state.getFieldCombination() == null) {
            throw new IllegalStateException(
                "場が空のときはパスできません"
            );
        }

        player.pass();

        if (turnManager.shouldClearField(state)) {
            turnManager.startNewTrick(state);
            return;
        }

        turnManager.moveToNextPlayer(state);
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

    private void assignRank(
            GameState state,
            Player player
    ) {
        int nextRank =
            state.getFinishedPlayerCount() + 1;

        player.assignRank(nextRank);
    }

    private boolean shouldFinishGame(GameState state) {
        int activePlayerCount = state.getPlayers().size()
            - state.getFinishedPlayerCount();

        return activePlayerCount <= 1;
    }

    private void assignLastRank(GameState state) {
        int lastRank = state.getPlayers().size();

        state.getPlayers().stream()
            .filter(player -> !player.hasFinished())
            .findFirst()
            .ifPresent(player ->
                player.assignRank(lastRank)
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

	public RuleEngine getRuleEngine() {
		return ruleEngine;
	}
}
