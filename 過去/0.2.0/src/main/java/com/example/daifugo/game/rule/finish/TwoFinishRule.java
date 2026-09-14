package com.example.daifugo.game.rule.finish;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.rule.FinishRule;

/**
 * 2上がりを禁止にする
 */
public class TwoFinishRule implements FinishRule{
    @Override
    public boolean isForbidden(
            GameState state,
            Player player,
            CardCombination combination
    ) {
        return combination.getCards()
            .stream()
            .anyMatch(card ->
                card.getRank() == Rank.TWO
            );
    }

    @Override
    public String getMessage() {
        return "2を含む組み合わせでは上がれません";
    }
}
