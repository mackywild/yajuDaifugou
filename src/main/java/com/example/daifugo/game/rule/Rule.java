package com.example.daifugo.game.rule;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;

public interface Rule {
    boolean matches(
            GameState state,
            Player player,
            CardCombination combination
        );

        void apply(
            GameState state,
            Player player,
            CardCombination combination,
            RuleResult result
        );
}
