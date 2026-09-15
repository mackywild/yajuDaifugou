package com.example.daifugo.game.cpu;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;

/** 普通: 基本的な手札節約・上がり優先を理解するCPU。 */
public class NormalCpuStrategy implements CpuStrategy {
    @Override
    public CpuMove chooseMove(GameState state, Player player, List<CpuMove> legalMoves, GameRuleSettings settings) {
        if (legalMoves.isEmpty()) {
            throw new IllegalStateException("CPUに合法手がありません");
        }
        // 普通難易度は最善候補を基本としつつ、約25%は人間らしいミスをする。
        if (legalMoves.size() > 1 && ThreadLocalRandom.current().nextDouble() < 0.25) {
            return legalMoves.get(ThreadLocalRandom.current().nextInt(legalMoves.size()));
        }
        return legalMoves.stream()
                .max(Comparator.comparingDouble(move -> HeuristicCpuScorer.normal(state, player, move, settings)))
                .orElseThrow();
    }

    @Override
    public List<Card> chooseSevenTransfer(GameState state, Player player, List<List<Card>> legalTransfers, GameRuleSettings settings) {
        return legalTransfers.stream()
                .min(Comparator.comparingDouble(cards -> cards.stream()
                        .mapToInt(card -> card.getRank().getStrength()).sum()))
                .orElseThrow(() -> new IllegalStateException("7渡し可能なカードがありません"));
    }
}
