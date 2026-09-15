package com.example.daifugo.game.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

public class PlayerTest {
    @Test
    void カードを1枚追加できる() {
        Player player = new Player("p1", "まっきー");
        Card card = new Card(Mark.SPADE, Rank.THREE);

        player.addCard(card);

        assertEquals(1, player.getCardCount());
        assertTrue(player.getHand().contains(card));
    }

    @Test
    void 複数カードを追加できる() {
        Player player = new Player("p1", "まっきー");

        player.addCards(List.of(
            new Card(Mark.SPADE, Rank.THREE),
            new Card(Mark.HEART, Rank.FOUR)
        ));

        assertEquals(2, player.getCardCount());
    }

    @Test
    void 所持カードを削除できる() {
        Player player = new Player("p1", "まっきー");
        Card card = new Card(Mark.SPADE, Rank.THREE);

        player.addCard(card);
        player.removeCards(List.of(card));

        assertTrue(player.hasNoCards());
    }

    @Test
    void 所持していないカードは削除できない() {
        Player player = new Player("p1", "まっきー");

        assertThrows(
            IllegalArgumentException.class,
            () -> player.removeCards(List.of(
                new Card(Mark.SPADE, Rank.THREE)
            ))
        );
    }

    @Test
    void パス状態を変更できる() {
        Player player = new Player("p1", "まっきー");

        player.pass();
        assertTrue(player.isPassed());

        player.clearPass();
        assertFalse(player.isPassed());
    }

    @Test
    void 順位を確定できる() {
        Player player = new Player("p1", "まっきー");

        player.assignRank(1);

        assertEquals(1, player.getRank());
        assertTrue(player.hasFinished());
    }

    @Test
    void 順位を再設定できない() {
        Player player = new Player("p1", "まっきー");
        player.assignRank(1);

        assertThrows(
            IllegalStateException.class,
            () -> player.assignRank(2)
        );
    }
}
