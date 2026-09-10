package com.example.daifugo.game.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;

public class TurnManagerTest {
    private final TurnManager turnManager =
            new TurnManager();

        @Test
        void 次のプレイヤーへ移動できる() {
            GameState state = createStartedState();

            turnManager.moveToNextPlayer(state);

            assertEquals(
                "p2",
                state.getCurrentPlayer().getId()
            );
        }

        @Test
        void パス済みプレイヤーを飛ばす() {
            GameState state = createStartedState();

            state.getPlayers().get(1).pass();

            turnManager.moveToNextPlayer(state);

            assertEquals(
                "p3",
                state.getCurrentPlayer().getId()
            );
        }

        @Test
        void 上がり済みプレイヤーを飛ばす() {
            GameState state = createStartedState();

            state.getPlayers().get(1).assignRank(1);

            turnManager.moveToNextPlayer(state);

            assertEquals(
                "p3",
                state.getCurrentPlayer().getId()
            );
        }

        @Test
        void 末尾から先頭へ手番を移動できる() {
            GameState state = createStartedState();

            state.changeCurrentPlayer(3);

            turnManager.moveToNextPlayer(state);

            assertEquals(
                "p1",
                state.getCurrentPlayer().getId()
            );
        }

        @Test
        void 最後にカードを出した人以外が全員パスなら場を流す() {
            GameState state = createStartedState();

            CardCombination combination = single(Rank.FIVE);

            state.updateField(combination, 0);

            state.getPlayers().get(1).pass();
            state.getPlayers().get(2).pass();
            state.getPlayers().get(3).pass();

            assertTrue(
                turnManager.shouldClearField(state)
            );
        }

        @Test
        void パスしていないプレイヤーが残っていれば場を流さない() {
            GameState state = createStartedState();

            state.updateField(single(Rank.FIVE), 0);

            state.getPlayers().get(1).pass();
            state.getPlayers().get(2).pass();

            assertFalse(
                turnManager.shouldClearField(state)
            );
        }

        @Test
        void 場を流した後は最後に出した人が親になる() {
            GameState state = createStartedState();

            state.updateField(single(Rank.FIVE), 2);

            state.getPlayers().get(0).pass();
            state.getPlayers().get(1).pass();
            state.getPlayers().get(3).pass();

            turnManager.startNewTrick(state);

            assertEquals(
                "p3",
                state.getCurrentPlayer().getId()
            );

            assertNull(state.getFieldCombination());

            assertTrue(
                state.getPlayers().stream()
                    .noneMatch(Player::isPassed)
            );
        }

        @Test
        void 最後に出した人が上がっていたら次の人が親になる() {
            GameState state = createStartedState();

            Player player3 = state.getPlayers().get(2);

            state.updateField(single(Rank.FIVE), 2);
            player3.assignRank(1);

            state.getPlayers().get(0).pass();
            state.getPlayers().get(1).pass();
            state.getPlayers().get(3).pass();

            turnManager.startNewTrick(state);

            assertEquals(
                "p4",
                state.getCurrentPlayer().getId()
            );
        }

        @Test
        void ゲーム開始前は手番を進められない() {
            GameState state = createState();

            assertThrows(
                IllegalStateException.class,
                () -> turnManager.moveToNextPlayer(state)
            );
        }

        private GameState createStartedState() {
            GameState state = createState();
            state.start();
            return state;
        }

        private GameState createState() {
            return new GameState(List.of(
                new Player("p1", "A"),
                new Player("p2", "B"),
                new Player("p3", "C"),
                new Player("p4", "D")
            ));
        }

        private CardCombination single(Rank rank) {
            return CardCombination.of(List.of(
                new Card(Mark.SPADE, rank)
            ));
        }
}
