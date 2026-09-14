package com.example.daifugo.game.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CardCombination {

    private final List<Card> cards;
    private final CombinationType type;

    private CardCombination(
            List<Card> cards,
            CombinationType type
    ) {
        this.cards = List.copyOf(cards);
        this.type = type;
    }

    public static CardCombination of(List<Card> cards) {
        Objects.requireNonNull(
            cards,
            "cards must not be null"
        );

        if (cards.isEmpty()) {
            return new CardCombination(
                List.of(),
                CombinationType.INVALID
            );
        }

        if (cards.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                "cards must not contain null"
            );
        }

        List<Card> copiedCards =
            new ArrayList<>(cards);

        CombinationType type =
            determineType(copiedCards);

        return new CardCombination(
            copiedCards,
            type
        );
    }

    private static CombinationType determineType(
            List<Card> cards
    ) {
        if (cards.size() == 1) {
            return CombinationType.SINGLE;
        }

        if (sameRank(cards)) {
            return switch (cards.size()) {
                case 2 -> CombinationType.PAIR;
                case 3 -> CombinationType.TRIPLE;
                case 4 -> CombinationType.FOUR;
                default -> CombinationType.INVALID;
            };
        }

        if (isStraight(cards)) {
            return CombinationType.STRAIGHT;
        }

        return CombinationType.INVALID;
    }

    private static boolean sameRank(List<Card> cards) {
        Rank firstRank = cards.get(0).getRank();

        return cards.stream()
            .allMatch(card ->
                card.getRank() == firstRank
            );
    }

    private static boolean isStraight(List<Card> cards) {
        if (cards.size() < 3) {
            return false;
        }

        if (cards.stream().anyMatch(Card::isJoker)) {
            return false;
        }

        Mark firstSuit = cards.get(0).getSuit();

        boolean sameSuit = cards.stream()
            .allMatch(card ->
                card.getSuit() == firstSuit
            );

        if (!sameSuit) {
            return false;
        }

        List<Integer> strengths = cards.stream()
            .map(card -> card.getStrength(false))
            .sorted()
            .toList();

        for (int i = 1; i < strengths.size(); i++) {
            int previous = strengths.get(i - 1);
            int current = strengths.get(i);

            if (current != previous + 1) {
                return false;
            }
        }

        return true;
    }

    public List<Card> getCards() {
        return cards;
    }

    public CombinationType getType() {
        return type;
    }

    public boolean isValid() {
        return type != CombinationType.INVALID;
    }

    public int getCardCount() {
        return cards.size();
    }

    public int getStrength(boolean revolution) {
        if (!isValid()) {
            throw new IllegalStateException(
                "無効な組み合わせの強さは取得できません"
            );
        }

        if (type == CombinationType.STRAIGHT) {
            return cards.stream()
                .mapToInt(card ->
                    card.getStrength(revolution)
                )
                .max()
                .orElseThrow();
        }

        return cards.get(0)
            .getStrength(revolution);
    }
    public boolean isSingle() {
        return type == CombinationType.SINGLE;
    }

    public boolean isSingleJoker() {
        return isSingle()
            && cards.get(0).isJoker();
    }

    public boolean isSingleSpadeThree() {
        if (!isSingle()) {
            return false;
        }

        Card card = cards.get(0);

        return card.getSuit() == Mark.SPADE
            && card.getRank() == Rank.THREE;
    }

    public int getBaseStrength() {
        if (!isValid()) {
            throw new IllegalStateException(
                "無効な組み合わせです"
            );
        }

        if (isSingleJoker()) {
            throw new IllegalStateException(
                "ジョーカー単体は比較対象外"
            );
        }

        return cards.stream()
                .mapToInt(card -> card.getStrength(false))
                .max()
                .orElseThrow();
    }
}