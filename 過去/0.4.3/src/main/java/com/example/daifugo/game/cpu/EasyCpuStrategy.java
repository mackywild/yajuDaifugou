package com.example.daifugo.game.cpu;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;

/** 簡単: 合法手からほぼランダムに選択するCPU。 */
public class EasyCpuStrategy implements CpuStrategy {
    @Override
    public CpuMove chooseMove(GameState state, Player player, List<CpuMove> legalMoves, GameRuleSettings settings) {
        if (legalMoves.isEmpty()) {
            throw new IllegalStateException("CPUに合法手がありません");
        }
        return legalMoves.get(ThreadLocalRandom.current().nextInt(legalMoves.size()));
    }

    @Override
    public List<Card> chooseSevenTransfer(GameState state, Player player, List<List<Card>> legalTransfers, GameRuleSettings settings) {
        if (legalTransfers.isEmpty()) throw new IllegalStateException("7渡し可能なカードがありません");
        return legalTransfers.get(ThreadLocalRandom.current().nextInt(legalTransfers.size()));
    }
}
