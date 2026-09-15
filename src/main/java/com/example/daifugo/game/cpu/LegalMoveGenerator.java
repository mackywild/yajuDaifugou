package com.example.daifugo.game.cpu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.rule.PlayValidator;
import com.example.daifugo.game.service.YajuRuleService;

/**
 * CPU用の合法手列挙器。
 *
 * 現行CardCombinationで扱える SINGLE / PAIR / TRIPLE / FOUR / STRAIGHTを
 * 手札から生成し、PlayValidatorと野獣ルールで絞り込む。
 */
public class LegalMoveGenerator {
    private final PlayValidator playValidator = new PlayValidator();
    private final YajuRuleService yajuRuleService = new YajuRuleService();

    public List<CpuMove> generate(
            GameState state,
            Player player,
            GameRuleSettings settings
    ) {
        Map<String, List<Card>> candidates = new LinkedHashMap<>();
        List<Card> hand = player.getHand();

        // 単体
        for (Card card : hand) {
            addCandidate(candidates, List.of(card));
        }

        // 同一ランク組み合わせ
        Map<Rank, List<Card>> byRank = new LinkedHashMap<>();
        for (Card card : hand) {
            byRank.computeIfAbsent(card.getRank(), ignored -> new ArrayList<>()).add(card);
        }
        for (List<Card> sameRank : byRank.values()) {
            for (int size = 2; size <= Math.min(4, sameRank.size()); size++) {
                enumerateCombinations(sameRank, size, 0, new ArrayList<>(), combo ->
                        addCandidate(candidates, combo));
            }
        }

        // 階段（同一マーク・3枚以上・連番）
        for (Mark mark : List.of(Mark.SPADE, Mark.HEART, Mark.DIAMOND, Mark.CLUB)) {
            List<Card> suitCards = hand.stream()
                    .filter(card -> card.getSuit() == mark && !card.isJoker())
                    .sorted(Comparator.comparingInt(card -> card.getRank().getStrength()))
                    .toList();

            for (int start = 0; start < suitCards.size(); start++) {
                List<Card> run = new ArrayList<>();
                run.add(suitCards.get(start));
                int previous = suitCards.get(start).getRank().getStrength();
                for (int end = start + 1; end < suitCards.size(); end++) {
                    Card next = suitCards.get(end);
                    int strength = next.getRank().getStrength();
                    if (strength == previous) {
                        continue;
                    }
                    if (strength != previous + 1) {
                        break;
                    }
                    run.add(next);
                    previous = strength;
                    if (run.size() >= 3) {
                        addCandidate(candidates, List.copyOf(run));
                    }
                }
            }
        }

        List<CpuMove> result = new ArrayList<>();
        for (List<Card> cards : candidates.values()) {
            CardCombination combination = CardCombination.of(cards);
            if (!combination.isValid()) {
                continue;
            }
            if (!playValidator.canPlay(
                    combination,
                    state.getFieldCombination(),
                    state.isStrengthReversed(),
                    state.getLockedMark())) {
                continue;
            }
            if (settings.yajuRule()) {
                try {
                    yajuRuleService.validatePlay(player, cards);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
            }
            result.add(CpuMove.playCards(cards));
        }

        if (state.getFieldCombination() != null) {
            result.add(CpuMove.passMove());
        }
        return List.copyOf(result);
    }

    /** 7渡しで選択可能なカード組み合わせを列挙する。 */
    public List<List<Card>> generateSevenTransfers(
            Player player,
            int cardCount,
            GameRuleSettings settings
    ) {
        List<List<Card>> transfers = new ArrayList<>();
        enumerateCombinations(player.getHand(), cardCount, 0, new ArrayList<>(), cards -> {
            if (settings.yajuRule()) {
                try {
                    yajuRuleService.validateSevenTransfer(player, cards);
                } catch (IllegalArgumentException ex) {
                    return;
                }
            }
            transfers.add(List.copyOf(cards));
        });
        return List.copyOf(transfers);
    }

    private void addCandidate(Map<String, List<Card>> target, List<Card> cards) {
        List<Card> sorted = cards.stream()
                .sorted(Comparator
                        .comparing((Card card) -> card.getRank().ordinal())
                        .thenComparing(card -> card.getSuit().ordinal()))
                .toList();
        String key = sorted.stream()
                .map(card -> card.getSuit().name() + ':' + card.getRank().name())
                .reduce((a, b) -> a + '|' + b)
                .orElse("");
        target.putIfAbsent(key, sorted);
    }

    private void enumerateCombinations(
            List<Card> source,
            int choose,
            int index,
            List<Card> current,
            java.util.function.Consumer<List<Card>> consumer
    ) {
        if (current.size() == choose) {
            consumer.accept(List.copyOf(current));
            return;
        }
        int remainingNeeded = choose - current.size();
        for (int i = index; i <= source.size() - remainingNeeded; i++) {
            current.add(source.get(i));
            enumerateCombinations(source, choose, i + 1, current, consumer);
            current.remove(current.size() - 1);
        }
    }
}
