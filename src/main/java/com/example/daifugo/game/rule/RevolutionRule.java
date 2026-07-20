package com.example.daifugo.game.rule;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.CombinationType;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;

public class RevolutionRule implements Rule{
    @Override
    public boolean matches(
            GameState state,
            Player player,
            CardCombination combination
    ) {
        return combination.getType()
            == CombinationType.FOUR;
    }

    @Override
    public void apply(
            GameState state,
            Player player,
            CardCombination combination,
            RuleResult result
    ) {
        state.toggleRevolution();
        result.markRevolutionOccurred();
    }
}
