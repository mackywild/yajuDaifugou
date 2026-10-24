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

    /**
     * JOKERは同一ランク組み合わせのワイルドカードとして扱う。
     * JOKER以外のカードがすべて同ランクならPAIR/TRIPLE/FOURを構成できる。
     */
    private static boolean sameRank(List<Card> cards) {
        Rank naturalRank = null;

        for (Card card : cards) {
            if (card.isJoker()) {
                continue;
            }

            if (naturalRank == null) {
                naturalRank = card.getRank();
                continue;
            }

            if (card.getRank() != naturalRank) {
                return false;
            }
        }

        return true;
    }

    private static boolean isStraight(List<Card> cards) {
        return !straightStarts(cards).isEmpty();
    }

    /**
     * JOKERは欠けた数字を埋めるワイルドカードとして扱う。
     * 3〜2(数値3〜15)の範囲内で、自然札が同一マークかつ重複しない
     * 連続区間を列挙する。2から3への折り返しは認めない。
     */
    private static List<Integer> straightStarts(List<Card> cards) {
        int length = cards.size();
        if (length < 3 || length > 13) {
            return List.of();
        }

        List<Card> naturalCards = cards.stream()
            .filter(card -> !card.isJoker())
            .toList();

        // 少なくとも2枚の自然札でマークと並びを確定させる。
        if (naturalCards.size() < 2) {
            return List.of();
        }

        Mark suit = naturalCards.get(0).getSuit();
        if (naturalCards.stream().anyMatch(card -> card.getSuit() != suit)) {
            return List.of();
        }

        List<Integer> ranks = naturalCards.stream()
            .map(card -> card.getRank().getStrength())
            .distinct()
            .toList();
        if (ranks.size() != naturalCards.size()) {
            return List.of();
        }

        int lowest = ranks.stream().mapToInt(Integer::intValue).min().orElseThrow();
        int highest = ranks.stream().mapToInt(Integer::intValue).max().orElseThrow();
        List<Integer> starts = new ArrayList<>();

        for (int start = 3; start <= 16 - length; start++) {
            int end = start + length - 1;
            if (lowest >= start && highest <= end) {
                starts.add(start);
            }
        }
        return List.copyOf(starts);
    }

    /**
     * ジョーカーの補完候補が複数ある場合は、実効強度が最大となる区間を採用。
     * 通常時は数字が高い区間、革命/Jバック時は数字が低い区間を選ぶ。
     */
    private int straightStart(boolean strengthReversed) {
        List<Integer> starts = straightStarts(cards);
        if (starts.isEmpty()) {
            throw new IllegalStateException("無効な階段です");
        }
        return strengthReversed ? starts.get(0) : starts.get(starts.size() - 1);
    }

    /** 階段を構成する札のうち、最も弱い札の実効強度。 */
    public int getStraightWeakestStrength(boolean strengthReversed) {
        if (type != CombinationType.STRAIGHT) {
            throw new IllegalStateException("階段ではありません");
        }
        int start = straightStart(strengthReversed);
        return strengthReversed ? 18 - (start + cards.size() - 1) : start;
    }

    /** 階段を構成する札のうち、最も強い札の実効強度。 */
    public int getStraightStrongestStrength(boolean strengthReversed) {
        if (type != CombinationType.STRAIGHT) {
            throw new IllegalStateException("階段ではありません");
        }
        int start = straightStart(strengthReversed);
        return strengthReversed ? 18 - start : start + cards.size() - 1;
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
            return getStraightStrongestStrength(revolution);
        }

        return cards.stream()
            .filter(card -> !card.isJoker())
            .findFirst()
            .orElse(cards.get(0))
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

        if (type == CombinationType.STRAIGHT) {
            return getStraightStrongestStrength(false);
        }

        return cards.stream()
                .filter(card -> !card.isJoker())
                .mapToInt(card -> card.getStrength(false))
                .max()
                .orElse(Rank.JOKER.getStrength(false));
    }
}