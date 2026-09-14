package com.example.daifugo.game.service;

import java.util.List;
import java.util.Objects;

import com.example.daifugo.game.domain.GamePhase;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;

public class TurnManager {
    public void moveToNextPlayer(GameState state) {
        validatePlayingState(state);

        int nextIndex = findNextAvailablePlayerIndex(
            state,
            state.getCurrentPlayerIndex()
        );

        state.changeCurrentPlayer(nextIndex);
    }

    public void startNewTrick(GameState state) {
        validatePlayingState(state);

        Integer lastPlayedPlayerIndex =
            state.getLastPlayedPlayerIndex();

        if (lastPlayedPlayerIndex == null) {
            throw new IllegalStateException(
                "最後にカードを出したプレイヤーが存在しません"
            );
        }

        state.clearField();

        int nextLeaderIndex;

        Player lastPlayedPlayer = state.getPlayers()
            .get(lastPlayedPlayerIndex);

        if (canTakeTurn(lastPlayedPlayer)) {
            nextLeaderIndex = lastPlayedPlayerIndex;
        } else {
            nextLeaderIndex = findNextAvailablePlayerIndex(
                state,
                lastPlayedPlayerIndex
            );
        }

        state.changeCurrentPlayer(nextLeaderIndex);
    }

    public boolean shouldClearField(GameState state) {
        Objects.requireNonNull(state, "state must not be null");

        if (state.getFieldCombination() == null) {
            return false;
        }

        Integer lastPlayedPlayerIndex =
            state.getLastPlayedPlayerIndex();

        if (lastPlayedPlayerIndex == null) {
            return false;
        }

        List<Player> players = state.getPlayers();

        for (int i = 0; i < players.size(); i++) {
            if (i == lastPlayedPlayerIndex) {
                continue;
            }

            Player player = players.get(i);

            if (canTakeTurn(player) && !player.isPassed()) {
                return false;
            }
        }

        return true;
    }

    private int findNextAvailablePlayerIndex(
            GameState state,
            int startIndex
    ) {
        List<Player> players = state.getPlayers();

        for (int offset = 1; offset <= players.size(); offset++) {
            int candidateIndex =
                (startIndex + offset) % players.size();

            Player candidate = players.get(candidateIndex);

            if (canTakeTurn(candidate)) {
                return candidateIndex;
            }
        }

        throw new IllegalStateException(
            "手番を担当できるプレイヤーが存在しません"
        );
    }

    private boolean canTakeTurn(Player player) {
        return !player.isPassed()
            && !player.hasFinished();
    }

    private void validatePlayingState(GameState state) {
        Objects.requireNonNull(state, "state must not be null");

        if (state.getPhase() != GamePhase.PLAYING) {
            throw new IllegalStateException(
                "ゲームがプレイ中ではありません"
            );
        }
    }
}
