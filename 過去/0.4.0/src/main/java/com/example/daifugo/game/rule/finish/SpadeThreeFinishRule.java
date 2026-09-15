package com.example.daifugo.game.rule.finish;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.rule.FinishRule;

/**
 * スペード3での上がりを禁止するルール。
 */
public class SpadeThreeFinishRule implements FinishRule {

    /**
     * 最後に出す組み合わせにスペード3が含まれるか判定する。
     *
     * @param state ゲーム状態
     * @param player プレイヤー
     * @param combination 出そうとしている組み合わせ
     * @return スペード3が含まれる場合true
     */
    @Override
    public boolean isForbidden(GameState state, Player player, CardCombination combination) {
        return combination.getCards().stream()
                .anyMatch(card -> card.getSuit() == Mark.SPADE && card.getRank() == Rank.THREE);
    }

    /**
     * @return エラーメッセージ
     */
    @Override
    public String getMessage() {
        return "スペード3では上がれません";
    }
}
