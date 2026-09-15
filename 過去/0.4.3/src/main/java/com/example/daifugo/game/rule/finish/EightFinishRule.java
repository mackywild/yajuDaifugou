package com.example.daifugo.game.rule.finish;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.rule.FinishRule;

/**
 * 8を含む組み合わせでの上がりを禁止するルール。
 */
public class EightFinishRule implements FinishRule {

    /**
     * 最後に出す組み合わせに8が含まれるか判定する。
     *
     * @param state ゲーム状態
     * @param player プレイヤー
     * @param combination 出そうとしている組み合わせ
     * @return 8が含まれる場合true
     */
    @Override
    public boolean isForbidden(GameState state, Player player, CardCombination combination) {
        return combination.getCards().stream()
                .anyMatch(card -> card.getRank() == Rank.EIGHT);
    }

    /**
     * @return エラーメッセージ
     */
    @Override
    public String getMessage() {
        return "8切りでは上がれません";
    }
}
