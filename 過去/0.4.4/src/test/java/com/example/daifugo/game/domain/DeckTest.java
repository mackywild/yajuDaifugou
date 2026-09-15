package com.example.daifugo.game.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

public class DeckTest {
    @Test
    void ジョーカー2枚を含む54枚のデッキを生成できる() {
        Deck deck = Deck.createStandardDeck(2);

        assertEquals(54, deck.size());
    }

    @Test
    void 通常カードに重複がない() {
        Deck deck = Deck.createStandardDeck(0);

        assertEquals(
            deck.size(),
            new HashSet<>(deck.getCards()).size()
        );
    }

    @Test
    void 通常時は2が3より強い() {
        Card three = new Card(Mark.SPADE, Rank.THREE);
        Card two = new Card(Mark.SPADE, Rank.TWO);

        assertTrue(
            two.getStrength(false) > three.getStrength(false)
        );
    }

    @Test
    void 革命時は3が2より強い() {
        Card three = new Card(Mark.SPADE, Rank.THREE);
        Card two = new Card(Mark.SPADE, Rank.TWO);

        assertTrue(
            three.getStrength(true) > two.getStrength(true)
        );
    }

    @Test
    void 不正なジョーカーの組み合わせは生成できない() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Card(Mark.SPADE, Rank.JOKER)
        );
    }
}
