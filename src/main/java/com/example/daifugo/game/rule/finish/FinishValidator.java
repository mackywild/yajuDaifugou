package com.example.daifugo.game.rule.finish;

import java.util.List;
import java.util.Objects;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.rule.FinishRule;

public class FinishValidator {
    private final List<FinishRule> rules;

    public FinishValidator(List<FinishRule> rules) {
        Objects.requireNonNull(
            rules,
            "rules must not be null"
        );

        if (rules.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                "rules must not contain null"
            );
        }

        this.rules = List.copyOf(rules);
    }

    public FinishValidationResult validate(
            GameState state,
            Player player,
            CardCombination combination
    ) {
        Objects.requireNonNull(
            state,
            "state must not be null"
        );

        Objects.requireNonNull(
            player,
            "player must not be null"
        );

        Objects.requireNonNull(
            combination,
            "combination must not be null"
        );

        /*
         * 選択カードを出しても手札が残るなら、
         * 上がり禁止判定は不要。
         */
        if (player.getCardCount()
                != combination.getCards().size()) {
            return FinishValidationResult.allowed();
        }

        for (FinishRule rule : rules) {
            if (rule.isForbidden(
                    state,
                    player,
                    combination
            )) {
                return FinishValidationResult.forbidden(
                    rule.getMessage()
                );
            }
        }

        return FinishValidationResult.allowed();
    }
}
