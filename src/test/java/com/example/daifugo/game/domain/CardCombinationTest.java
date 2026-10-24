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


    @Test
    void ジョーカーで欠けた数字を補完して階段になる() {
        CardCombination combination = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.FIVE),
            new Card(Mark.JOKER, Rank.JOKER),
            new Card(Mark.SPADE, Rank.SEVEN)
        ));

        assertEquals(CombinationType.STRAIGHT, combination.getType());
        assertEquals(5, combination.getStraightWeakestStrength(false));
        assertEquals(7, combination.getBaseStrength());
    }

    @Test
    void ジョーカーを階段の先頭または末尾にも使える() {
        CardCombination combination = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.FIVE),
            new Card(Mark.SPADE, Rank.SIX),
            new Card(Mark.JOKER, Rank.JOKER)
        ));

        assertEquals(CombinationType.STRAIGHT, combination.getType());
        // 通常時は5-6-7、強弱反転時は4-5-6として扱う。
        assertEquals(7, combination.getBaseStrength());
        assertEquals(14, combination.getStraightStrongestStrength(true));
        assertEquals(12, combination.getStraightWeakestStrength(true));
    }

    @Test
    void ジョーカー二枚で階段の穴を二つ埋められる() {
        CardCombination combination = CardCombination.of(List.of(
            new Card(Mark.HEART, Rank.FIVE),
            new Card(Mark.JOKER, Rank.JOKER),
            new Card(Mark.HEART, Rank.EIGHT),
            new Card(Mark.JOKER, Rank.JOKER)
        ));

        assertEquals(CombinationType.STRAIGHT, combination.getType());
        assertEquals(8, combination.getBaseStrength());
    }

    @Test
    void ジョーカーだけでは届かない数字や混合マークは無効() {
        assertFalse(CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.FIVE),
            new Card(Mark.SPADE, Rank.NINE),
            new Card(Mark.JOKER, Rank.JOKER)
        )).isValid());

        assertFalse(CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.FIVE),
            new Card(Mark.HEART, Rank.SEVEN),
            new Card(Mark.JOKER, Rank.JOKER)
        )).isValid());

        assertFalse(CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.FIVE),
            new Card(Mark.SPADE, Rank.FIVE),
            new Card(Mark.SPADE, Rank.SEVEN),
            new Card(Mark.JOKER, Rank.JOKER)
        )).isValid());
    }

    @Test
    void 二から三への折り返し階段は認めない() {
        assertFalse(CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.ACE),
            new Card(Mark.SPADE, Rank.TWO),
            new Card(Mark.JOKER, Rank.JOKER),
            new Card(Mark.SPADE, Rank.THREE)
        )).isValid());
    }

    @Test
    void 自然札一枚とジョーカー二枚は従来通りトリプルになる() {
        CardCombination combination = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.FIVE),
            new Card(Mark.JOKER, Rank.JOKER),
            new Card(Mark.JOKER, Rank.JOKER)
        ));
        assertEquals(CombinationType.TRIPLE, combination.getType());
    }

}