package com.example.daifugo.game.rule;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;

public interface FinishRule {
    boolean isForbidden(
            GameState state,
            Player player,
            CardCombination combination
        );

        String getMessage();
}
