package com.example.daifugo.game.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CardCombinationTest {
	@Test
	void シングルチェック() {
	
	    CardCombination c =
	            CardCombination.of(List.of(
	                    new Card(Mark.SPADE, Rank.FIVE)));
	
	    assertEquals(
	            CombinationType.SINGLE,
	            c.getType());
	
	}
	
	@Test
	void ペアチェック() {
	
	    CardCombination c =
	            CardCombination.of(List.of(
	                    new Card(Mark.SPADE, Rank.FIVE),
	                    new Card(Mark.HEART, Rank.FIVE)));
	
	    assertEquals(
	            CombinationType.PAIR,
	            c.getType());
	
	}
	
	@Test
	void 数字が違えば無効() {
	
	    CardCombination c =
	            CardCombination.of(List.of(
	                    new Card(Mark.SPADE, Rank.FIVE),
	                    new Card(Mark.HEART, Rank.SIX)));
	
	    assertFalse(c.isValid());
	
	}
	
	@Test
	void 四枚組革命() {
	
	    CardCombination c =
	            CardCombination.of(List.of(
	                    new Card(Mark.SPADE, Rank.ACE),
	                    new Card(Mark.HEART, Rank.ACE),
	                    new Card(Mark.CLUB, Rank.ACE),
	                    new Card(Mark.DIAMOND, Rank.ACE)));
	
	    assertEquals(
	            CombinationType.FOUR,
	            c.getType());
	
	}

    @Test
    void ジョーカーは同ランクカードのワイルドカードとしてペアになる() {
        CardCombination combination = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.THREE),
            new Card(Mark.JOKER, Rank.JOKER)
        ));

        assertEquals(CombinationType.PAIR, combination.getType());
        assertEquals(3, combination.getBaseStrength());
    }

    @Test
    void ジョーカー2枚でも自然札のランクを使ってトリプルになる() {
        CardCombination combination = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.THREE),
            new Card(Mark.JOKER, Rank.JOKER),
            new Card(Mark.JOKER, Rank.JOKER)
        ));

        assertEquals(CombinationType.TRIPLE, combination.getType());
        assertEquals(3, combination.getBaseStrength());
    }

    @Test
    void ジョーカーがいても異なる自然札は同ランク組み合わせにならない() {
        CardCombination combination = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.THREE),
            new Card(Mark.HEART, Rank.FOUR),
            new Card(Mark.JOKER, Rank.JOKER)
        ));

        assertFalse(combination.isValid());
    }

}