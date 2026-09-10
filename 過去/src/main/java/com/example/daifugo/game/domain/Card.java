package com.example.daifugo.game.domain;

import java.util.Objects;

/**
 * カードクラス
 */
public final class Card {
    private final Mark suit;
    private final Rank rank;

    public Card(Mark suit, Rank rank) {
        validate(suit, rank);
        this.suit = suit;
        this.rank = rank;
    }

    private void validate(Mark suit, Rank rank) {
        Objects.requireNonNull(suit, "suit must not be null");
        Objects.requireNonNull(rank, "rank must not be null");

        boolean jokerSuit = suit == Mark.JOKER;
        boolean jokerRank = rank == Rank.JOKER;

        if (jokerSuit != jokerRank) {
            throw new IllegalArgumentException(
                "JOKERのMarkとRank不一致エラー"
            );
        }
    }

    public Mark getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    public boolean isJoker() {
        return rank == Rank.JOKER;
    }

    public int getStrength(boolean revolution) {
        return rank.getStrength(revolution);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Card card)) {
            return false;
        }

        return suit == card.suit && rank == card.rank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit, rank);
    }

    @Override
    public String toString() {
        return isJoker()
            ? "JOKER"
            : suit + "-" + rank;
    }
}
