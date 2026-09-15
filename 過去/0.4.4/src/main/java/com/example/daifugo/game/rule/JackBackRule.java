package com.example.daifugo.game.rule;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameEventType;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;

/**
 * Jバックを適用するルール。
 *
 * Jを含む手が出されたら、場が流れるまで強弱関係を一時的に反転する。
 * 革命中はJバックによって反転が相殺されるため、通常の「高い手が勝つ」状態になる。
 */
public class JackBackRule implements Rule {

    @Override
    public boolean matches(
            GameState state,
            Player player,
            CardCombination previousField,
            CardCombination combination
    ) {
        return combination.getCards().stream()
                .anyMatch(card -> card.getRank() == Rank.JACK);
    }

    @Override
    public void apply(
            GameState state,
            Player player,
            CardCombination previousField,
            CardCombination combination,
            RuleResult result
    ) {
        state.activateJackBack();
        state.emitEvent(GameEventType.JACK_BACK, player);

        /*
         * Jバック適用後に3が最強になる場合だけ、
         * 次プレイヤーの「早漏」判定を予約する。
         * 革命中はJバックで反転が相殺され、2が最強になるため対象外。
         */
        if (state.isStrengthReversed()) {
            state.requestEarlyShotArm();
        }
    }
}
