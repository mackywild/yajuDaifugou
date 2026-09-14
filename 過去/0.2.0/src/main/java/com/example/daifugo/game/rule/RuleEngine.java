package com.example.daifugo.game.rule;

import java.util.List;
import java.util.Objects;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;

public class RuleEngine {
    private final List<Rule> rules;
    
    RuleEngine ruleEngine = new RuleEngine(
    	    List.of(
    	        new RevolutionRule(),
    	        new EightCutRule(),
    	        new MarkLockRule()
    	        )
    	    );


    public RuleEngine(List<Rule> rules) {
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

    public RuleResult applyRules(
            GameState state,
            Player player,
            CardCombination previousField,
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

        RuleResult result = new RuleResult();

        for (Rule rule : rules) {
            if (rule.matches(state, player,previousField, combination)) {
                rule.apply(
                    state,
                    player,
                    previousField,
                    combination,
                    result
                );
            }
        }

        return result;
    }
}
