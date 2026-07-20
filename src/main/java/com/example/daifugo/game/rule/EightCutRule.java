package com.example.daifugo.game.rule;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;

public class EightCutRule implements Rule {
    @Override
    public boolean matches(
            GameState state,
            Player player,
            CardCombination combination
    ) {
        return combination.getCards()
            .stream()
            .anyMatch(card ->
                card.getRank() == Rank.EIGHT
            );
    }

    @Override
    public void apply(
            GameState state,
            Player player,
            CardCombination combination,
            RuleResult result
    ) {
        result.requestFieldClear();
    }
}
