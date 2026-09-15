package com.example.daifugo.game.cpu;

import java.util.Comparator;
import java.util.List;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;

/** 難しい: 相手残枚数・特殊ルール・手札構造まで考慮するCPU。 */
public class HardCpuStrategy implements CpuStrategy {
    @Override
    public CpuMove chooseMove(GameState state, Player player, List<CpuMove> legalMoves, GameRuleSettings settings) {
        return legalMoves.stream()
                .max(Comparator.comparingDouble(move -> HeuristicCpuScorer.hard(state, player, move, settings)))
                .orElseThrow(() -> new IllegalStateException("CPUに合法手がありません"));
    }

    @Override
    public List<Card> chooseSevenTransfer(GameState state, Player player, List<List<Card>> legalTransfers, GameRuleSettings settings) {
        return legalTransfers.stream()
                .min(Comparator.comparingDouble(cards -> HeuristicCpuScorer.transferHard(cards, settings)))
                .orElseThrow(() -> new IllegalStateException("7渡し可能なカードがありません"));
    }
}
