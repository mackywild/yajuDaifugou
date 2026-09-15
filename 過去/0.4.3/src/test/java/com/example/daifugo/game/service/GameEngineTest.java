package com.example.daifugo.game.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GamePhase;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.rule.EightCutRule;
import com.example.daifugo.game.rule.PlayValidator;
import com.example.daifugo.game.rule.RevolutionRule;
import com.example.daifugo.game.rule.RuleEngine;

public class GameEngineTest {
    private GameEngine gameEngine;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngineFactory().create();
    }

    @Test
    void 現在プレイヤーがカードを出せる() {
        Player player1 = playerWithCard(
            "p1",
            Rank.FIVE
        );

        Player player2 = playerWithCard(
            "p2",
            Rank.SIX
        );

        GameState state = startedState(
            player1,
            player2
        );

        Card card = player1.getHand().get(0);

        gameEngine.play(
            state,
            "p1",
            List.of(card)
        );

        assertTrue(player1.hasNoCards());
        assertNotNull(state.getFieldCombination());
    }

    @Test
    void 手番ではないプレイヤーはカードを出せない() {
        Player player1 = playerWithCard(
            "p1",
            Rank.FIVE
        );

        Player player2 = playerWithCard(
            "p2",
            Rank.SIX
        );

        GameState state = startedState(
            player1,
            player2
        );

        assertThrows(
            IllegalStateException.class,
            () -> gameEngine.play(
                state,
                "p2",
                player2.getHand()
            )
        );
    }

    @Test
    void 場より弱いカードは出せない() {
        Player player1 = playerWithCards(
            "p1",
            Rank.TEN,
            Rank.KING
        );

        Player player2 = playerWithCards(
            "p2",
            Rank.FIVE,
            Rank.SIX
        );

        Player player3 = playerWithCards(
            "p3",
            Rank.JACK,
            Rank.QUEEN
        );

        GameState state = startedState(
            player1,
            player2,
            player3
        );

        gameEngine.play(
            state,
            "p1",
            List.of(player1.getHand().get(0))
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> gameEngine.play(
                state,
                "p2",
                List.of(player2.getHand().get(0))
            )
        );
    }

    @Test
    void カードを出すと次のプレイヤーへ移動する() {
        Player player1 = playerWithCards(
            "p1",
            Rank.FIVE,
            Rank.SEVEN
        );

        Player player2 = playerWithCard(
            "p2",
            Rank.SIX
        );

        Player player3 = playerWithCard(
            "p3",
            Rank.EIGHT
        );

        GameState state = startedState(
            player1,
            player2,
            player3
        );

        gameEngine.play(
            state,
            "p1",
            List.of(player1.getHand().get(0))
        );

        assertEquals(
            "p2",
            state.getCurrentPlayer().getId()
        );
    }

    @Test
    void 場が空のときはパスできない() {
        GameState state = startedState(
            playerWithCard("p1", Rank.FIVE),
            playerWithCard("p2", Rank.SIX)
        );

        assertThrows(
            IllegalStateException.class,
            () -> gameEngine.pass(state, "p1")
        );
    }

    @Test
    void パスすると次のプレイヤーへ移動する() {
        Player player1 = playerWithCards(
            "p1",
            Rank.FIVE,
            Rank.SEVEN
        );

        Player player2 = playerWithCard(
            "p2",
            Rank.SIX
        );

        Player player3 = playerWithCard(
            "p3",
            Rank.EIGHT
        );

        GameState state = startedState(
            player1,
            player2,
            player3
        );

        gameEngine.play(
            state,
            "p1",
            List.of(player1.getHand().get(0))
        );

        gameEngine.pass(state, "p2");

        assertTrue(player2.isPassed());

        assertEquals(
            "p3",
            state.getCurrentPlayer().getId()
        );
    }

    @Test
    void 最後の一枚を出すと順位が確定する() {
        Player player1 = playerWithCard(
            "p1",
            Rank.FIVE
        );

        Player player2 = playerWithCards(
            "p2",
            Rank.SIX,
            Rank.SEVEN
        );

        Player player3 = playerWithCards(
            "p3",
            Rank.EIGHT,
            Rank.NINE
        );

        GameState state = startedState(
            player1,
            player2,
            player3
        );

        gameEngine.play(
            state,
            "p1",
            List.of(player1.getHand().get(0))
        );

        assertEquals(1, player1.getRank());
        assertTrue(player1.hasFinished());
    }

    @Test
    void 残り一人になるとゲームが終了する() {
        Player player1 = playerWithCard(
            "p1",
            Rank.FIVE
        );

        Player player2 = playerWithCard(
            "p2",
            Rank.SIX
        );

        GameState state = startedState(
            player1,
            player2
        );

        gameEngine.play(
            state,
            "p1",
            List.of(player1.getHand().get(0))
        );

        assertEquals(
            GamePhase.FINISHED,
            state.getPhase()
        );

        assertEquals(1, player1.getRank());
        assertEquals(2, player2.getRank());
    }

    private GameState startedState(Player... players) {
        GameState state =
            new GameState(List.of(players));

        state.start();

        return state;
    }

    private Player playerWithCard(
            String id,
            Rank rank
    ) {
        return playerWithCards(id, rank);
    }

    private Player playerWithCards(
            String id,
            Rank... ranks
    ) {
        Player player = new Player(id, id);

        for (Rank rank : ranks) {
            player.addCard(
                new Card(Mark.SPADE, rank)
            );
        }

        return player;
    }
}
