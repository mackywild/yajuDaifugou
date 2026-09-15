package com.example.daifugo.game.cpu;

import java.util.Comparator;
import java.util.List;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;

/**
 * N-GOD: 自己対戦データで学習したニューラル評価器を使用する最上位CPU。
 *
 * モデル値だけに依存せず、ルールベースの安全項を少量ブレンドすることで
 * 未学習の特殊ルール組み合わせでも破綻しにくくしている。
 */
public class NgodCpuStrategy implements CpuStrategy {
    @Override
    public CpuMove chooseMove(GameState state, Player player, List<CpuMove> legalMoves, GameRuleSettings settings) {
        return legalMoves.stream()
                .max(Comparator.comparingDouble(move -> score(state, player, move, settings)))
                .orElseThrow(() -> new IllegalStateException("CPUに合法手がありません"));
    }

    private double score(GameState state, Player player, CpuMove move, GameRuleSettings settings) {
        double learned = NgodNeuralModel.predict(CpuFeatureExtractor.extract(state, player, move, settings));
        double hard = HeuristicCpuScorer.hard(state, player, move, settings);
        return learned * 100.0 + hard * 0.28;
    }

    @Override
    public List<Card> chooseSevenTransfer(GameState state, Player player, List<List<Card>> legalTransfers, GameRuleSettings settings) {
        // 7渡しは状態分岐が大きいため、v0.4.0では最強ヒューリスティックを採用する。
        return legalTransfers.stream()
                .min(Comparator.comparingDouble(cards -> HeuristicCpuScorer.transferHard(cards, settings)))
                .orElseThrow(() -> new IllegalStateException("7渡し可能なカードがありません"));
    }
}
