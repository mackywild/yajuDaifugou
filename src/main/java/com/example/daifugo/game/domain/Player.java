package com.example.daifugo.game.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * プレイヤー情報
 */
public class Player {

    private final String id;
    private final String name;

    private final List<Card> hand = new ArrayList<>();

    private boolean passed;
    private Integer rank;

    public Player(String id, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return List.copyOf(hand);
    }

    public boolean isPassed() {
        return passed;
    }

    public Integer getRank() {
        return rank;
    }

    public void addCard(Card card) {
        hand.add(Objects.requireNonNull(card, "card must not be null"));
    }

    public void addCards(Collection<Card> cards) {
        Objects.requireNonNull(cards, "cards must not be null");

        if (cards.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                "cards must not contain null"
            );
        }

        hand.addAll(cards);
    }

    public void removeCards(Collection<Card> cards) {
        Objects.requireNonNull(cards, "cards must not be null");

        if (cards.isEmpty()) {
            throw new IllegalArgumentException(
                "削除するカードが指定されていません"
            );
        }

        if (!containsAllCards(cards)) {
            throw new IllegalArgumentException(
                "所持していないカードがあります"
            );
        }

        for (Card card : cards) {
            hand.remove(card);
        }
    }

    public void pass() {
        passed = true;
    }

    public void clearPass() {
        passed = false;
    }

    public boolean hasNoCards() {
        return hand.isEmpty();
    }

    public int getCardCount() {
        return hand.size();
    }

    public void assignRank(int rank) {
        if (rank <= 0) {
            throw new IllegalArgumentException(
                "順位は1以上で指定してください"
            );
        }

        if (this.rank != null) {
            throw new IllegalStateException(
                "順位は既に確定しています"
            );
        }

        this.rank = rank;
    }

    public boolean hasFinished() {
        return rank != null;
    }

    private boolean containsAllCards(Collection<Card> cards) {
        List<Card> copiedHand = new ArrayList<>(hand);

        for (Card card : cards) {
            if (!copiedHand.remove(card)) {
                return false;
            }
        }

        return true;
    }
}