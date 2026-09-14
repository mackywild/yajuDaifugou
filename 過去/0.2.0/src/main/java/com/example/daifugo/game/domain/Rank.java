package com.example.daifugo.game.domain;

public enum Rank {

    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(11),
    QUEEN(12),
    KING(13),
    ACE(14),
    TWO(15),
    JOKER(16);

    private final int strength;

    Rank(int strength) {
        this.strength = strength;
    }

    public int getStrength() {
        return strength;
    }

    public int getStrength(boolean revolution) {
        if (this == JOKER) {
            return strength;
        }

        return revolution ? 18 - strength : strength;
    }
}
