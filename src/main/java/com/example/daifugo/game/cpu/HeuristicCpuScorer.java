package com.example.daifugo.game.cpu;

import java.util.List;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.domain.YajuStatus;

/** 普通/難しいCPUで使用するルールベース評価。 */
final class HeuristicCpuScorer {
    private HeuristicCpuScorer() {}

    static double normal(GameState state, Player player, CpuMove move, GameRuleSettings settings) {
        if (move.pass()) return -3.0;
        CardCombination c = CardCombination.of(move.cards());
        int strength = c.isSingleJoker() ? 16 : c.getBaseStrength();
        double score = move.cards().size() * 7.0;
        score -= strength * 0.30;
        if (state.isStrengthReversed()) score += strength * 0.45;
        if (settings.eightCut() && CpuFeatureExtractor.containsRank(move.cards(), Rank.EIGHT)) score += 2.0;
        if (move.cards().stream().anyMatch(Card::isJoker)) score -= 3.0;
        if (CpuFeatureExtractor.containsRank(move.cards(), Rank.TWO)) score -= 1.5;
        if (player.getCardCount() == move.cards().size()) {
            score += CpuFeatureExtractor.containsForbiddenFinish(move.cards()) && settings.forbiddenFinish()
                    ? -100.0 : 100.0;
        }
        long eightCount = player.getHand().stream().filter(card -> card.getRank() == Rank.EIGHT).count();
        long tenCount = player.getHand().stream().filter(card -> card.getRank() == Rank.TEN).count();
        if (player.getYajuStatus() == YajuStatus.ACTIVE
                && eightCount == 1
                && tenCount >= 1
                && player.getCardCount() == eightCount + tenCount
                && CpuFeatureExtractor.isSingleRank(move.cards(), Rank.EIGHT)) score += 120.0;
        if (player.getYajuStatus() == YajuStatus.EIGHT_PLAYED
                && move.cards().size() == player.getCardCount()
                && !move.cards().isEmpty()
                && move.cards().stream().allMatch(card -> card.getRank() == Rank.TEN)) score += 150.0;
        return score;
    }

    static double hard(GameState state, Player player, CpuMove move, GameRuleSettings settings) {
        double score = normal(state, player, move, settings);
        int minOpp = state.getPlayers().stream()
                .filter(p -> !p.getId().equals(player.getId()) && !p.hasFinished())
                .mapToInt(Player::getCardCount).min().orElse(99);

        if (move.pass()) {
            return score + (minOpp <= 2 ? -12.0 : 0.0);
        }

        List<Card> remaining = new java.util.ArrayList<>(player.getHand());
        move.cards().forEach(remaining::remove);

        // 終盤の上がりやすさをNormalより強く評価する。
        if (remaining.size() <= 3) score += (4 - remaining.size()) * 2.0;

        // 残した手札にペア/トリプルが残る手を少し優先する。
        java.util.Map<Rank, Long> groups = remaining.stream()
                .collect(java.util.stream.Collectors.groupingBy(Card::getRank, java.util.stream.Collectors.counting()));
        score += groups.values().stream().mapToDouble(c -> c >= 2 ? (c - 1) * 0.7 : 0.0).sum();

        // 相手が上がりそうなら8切りと強札を積極投入する。
        if (minOpp <= 2) {
            if (settings.eightCut() && CpuFeatureExtractor.containsRank(move.cards(), Rank.EIGHT)) score += 14.0;
            CardCombination c = CardCombination.of(move.cards());
            int strength = c.isSingleJoker() ? 16 : c.getBaseStrength();
            score += state.isStrengthReversed() ? (16 - strength) * 0.55 : strength * 0.55;
        }

        if (settings.sevenTransfer() && CpuFeatureExtractor.containsRank(move.cards(), Rank.SEVEN)) score += 1.5;
        return score;
    }

    static double transferHard(List<Card> cards, GameRuleSettings settings) {
        double score = 0.0;
        for (Card card : cards) {
            int s = card.getRank().getStrength();
            score += s;
            if (card.isJoker()) score += 10;
            if (card.getRank() == Rank.TWO) score += 6;
            if (card.getRank() == Rank.EIGHT && settings.eightCut()) score -= 5;
            if (card.getRank() == Rank.SEVEN && settings.sevenTransfer()) score -= 1;
        }
        return score;
    }
}
