package com.example.daifugo.game.rule.finish;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.rule.FinishRule;

/**
 * ジョーカー上がり禁止ルール
 */
public class JokerFinishRule implements FinishRule{
    @Override
    public boolean isForbidden(
            GameState state,
            Player player,
            CardCombination combination
    ) {
        return combination.getCards()
            .stream()
            .anyMatch(card -> card.isJoker());
    }

    @Override
    public String getMessage() {
        return "ジョーカーでは上がれません";
    }
}
