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
}