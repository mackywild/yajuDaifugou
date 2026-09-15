package com.example.daifugo.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GamePhase;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;

/**
 * 禁止上がり時の反則順位処理を確認するテスト。
 */
class ForbiddenFinishPenaltyTest {

    /** テスト対象のゲームエンジン */
    private GameEngine gameEngine;

    /**
     * 各テスト実行前に実運用と同じルール構成でGameEngineを生成する。
     */
    @BeforeEach
    void setUp() {
        gameEngine = new GameEngineFactory().create();
    }

    /**
     * 2で上がった場合、プレイ自体は成立し最下位になることを確認する。
     */
    @Test
    void twoFinish_反則上がりとして最下位になる() {
        assertForbiddenSingleFinish(
            new Card(Mark.SPADE, Rank.TWO)
        );
    }

    /**
     * 8で上がった場合、プレイ自体は成立し最下位になることを確認する。
     */
    @Test
    void eightFinish_反則上がりとして最下位になる() {
        assertForbiddenSingleFinish(
            new Card(Mark.HEART, Rank.EIGHT)
        );
    }

    /**
     * ジョーカーで上がった場合、プレイ自体は成立し最下位になることを確認する。
     */
    @Test
    void jokerFinish_反則上がりとして最下位になる() {
        assertForbiddenSingleFinish(
            new Card(Mark.JOKER, Rank.JOKER)
        );
    }

    /**
     * スペード3で上がった場合、プレイ自体は成立し最下位になることを確認する。
     */
    @Test
    void spadeThreeFinish_反則上がりとして最下位になる() {
        assertForbiddenSingleFinish(
            new Card(Mark.SPADE, Rank.THREE)
        );
    }

    /**
     * 反則上がりで4位が先に確定した後も、通常上がりは1位から採番されることを確認する。
     */
    @Test
    void penaltyFinish後も通常順位は上位から採番される() {
        Player player1 = playerWithCards(
            "p1",
            new Card(Mark.HEART, Rank.EIGHT)
        );
        Player player2 = playerWithCards(
            "p2",
            new Card(Mark.SPADE, Rank.FIVE),
            new Card(Mark.HEART, Rank.SEVEN)
        );
        Player player3 = playerWithCards(
            "p3",
            new Card(Mark.CLUB, Rank.SIX),
            new Card(Mark.CLUB, Rank.NINE)
        );
        Player player4 = playerWithCards(
            "p4",
            new Card(Mark.DIAMOND, Rank.TEN),
            new Card(Mark.DIAMOND, Rank.JACK)
        );

        GameState state = startedState(
            player1,
            player2,
            player3,
            player4
        );

        /* p1は8上がりのため4位確定。8切りで場も流れる。 */
        gameEngine.play(
            state,
            "p1",
            List.of(player1.getHand().get(0))
        );

        assertEquals(4, player1.getRank());
        assertEquals("p2", state.getCurrentPlayer().getId());

        /* p2は通常上がり。2枚を同時には出せないため1枚残してから上がる。 */
        gameEngine.play(
            state,
            "p2",
            List.of(player2.getHand().get(0))
        );

        /* 場があるためp3/p4をパスさせてp2を再度リードにする。 */
        gameEngine.pass(state, "p3");
        gameEngine.pass(state, "p4");

        gameEngine.play(
            state,
            "p2",
            List.of(player2.getHand().get(0))
        );

        assertEquals(1, player2.getRank());
        assertTrue(player2.hasFinished());
    }

    /**
     * 禁止カード1枚で上がった場合の共通確認を行う。
     *
     * @param forbiddenCard 禁止上がり対象カード
     */
    private void assertForbiddenSingleFinish(Card forbiddenCard) {
        Player player1 = playerWithCards("p1", forbiddenCard);
        Player player2 = playerWithCards(
            "p2",
            new Card(Mark.SPADE, Rank.FIVE),
            new Card(Mark.HEART, Rank.SIX)
        );
        Player player3 = playerWithCards(
            "p3",
            new Card(Mark.CLUB, Rank.SEVEN),
            new Card(Mark.DIAMOND, Rank.NINE)
        );
        Player player4 = playerWithCards(
            "p4",
            new Card(Mark.SPADE, Rank.TEN),
            new Card(Mark.HEART, Rank.JACK)
        );

        GameState state = startedState(
            player1,
            player2,
            player3,
            player4
        );

        gameEngine.play(
            state,
            "p1",
            List.of(forbiddenCard)
        );

        assertTrue(player1.hasNoCards());
        assertTrue(player1.hasFinished());
        assertEquals(4, player1.getRank());
        assertEquals(GamePhase.PLAYING, state.getPhase());
    }

    /**
     * プレイ中のGameStateを生成する。
     *
     * @param players 参加プレイヤー
     * @return プレイ中のゲーム状態
     */
    private GameState startedState(Player... players) {
        GameState state = new GameState(List.of(players));
        state.start();
        return state;
    }

    /**
     * 指定カードを持つプレイヤーを生成する。
     *
     * @param id プレイヤーID
     * @param cards 手札
     * @return プレイヤー
     */
    private Player playerWithCards(
            String id,
            Card... cards
    ) {
        Player player = new Player(id, id);
        player.addCards(List.of(cards));
        return player;
    }
}
