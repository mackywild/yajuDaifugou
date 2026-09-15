package com.example.daifugo.game.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    private final List<Card> cards;

    private Deck(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
    }

    public static Deck createStandardDeck(int jokerCount) {
        if (jokerCount < 0 || jokerCount > 2) {
            throw new IllegalArgumentException(
                "ジョーカー枚数は0〜2枚で指定してください"
            );
        }

        List<Card> cards = new ArrayList<>();

        for (Mark mark : List.of(
        	Mark.SPADE,
        	Mark.HEART,
        	Mark.DIAMOND,
        	Mark.CLUB
        )) {
            for (Rank rank : Rank.values()) {
                if (rank != Rank.JOKER) {
                    cards.add(new Card(mark, rank));
                }
            }
        }

        for (int i = 0; i < jokerCount; i++) {
            cards.add(new Card(Mark.JOKER, Rank.JOKER));
        }

        return new Deck(cards);
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public List<Card> getCards() {
        return List.copyOf(cards);
    }

    public int size() {
        return cards.size();
    }

    public List<List<Card>> deal(int playerCount) {
        if (playerCount < 2) {
            throw new IllegalArgumentException(
                "プレイヤーは2人以上必要です"
            );
        }

        if (playerCount > cards.size()) {
            throw new IllegalArgumentException(
                "プレイヤー数がカード枚数を超えています"
            );
        }

        List<List<Card>> hands = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {
            hands.add(new ArrayList<>());
        }

        int playerIndex = 0;

        while (!cards.isEmpty()) {
            Card card = cards.remove(cards.size() - 1);

            hands.get(playerIndex).add(card);

            playerIndex =
                (playerIndex + 1) % playerCount;
        }

        return hands.stream()
            .map(List::copyOf)
            .toList();
    }
}
