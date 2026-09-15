package com.example.daifugo.game.rule;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;

public class RuleEngineTest {
    @Test
    void 一致したルールを適用する() {
        RuleEngine engine = new RuleEngine(
            List.of(
                new RevolutionRule(),
                new EightCutRule()
            )
        );

        GameState state = createState();
        Player player = state.getPlayers().get(0);

        CardCombination combination =
            CardCombination.of(List.of(
                new Card(Mark.SPADE, Rank.EIGHT),
                new Card(Mark.HEART, Rank.EIGHT),
                new Card(Mark.DIAMOND, Rank.EIGHT),
                new Card(Mark.CLUB, Rank.EIGHT)
            ));

        RuleResult result = engine.applyRules(
            state,
            player,
            null,
            combination
        );

        assertTrue(state.isRevolution());
        assertTrue(result.isRevolutionOccurred());
        assertTrue(result.shouldClearField());
    }

    @Test
    void 一致しないルールは適用されない() {
        RuleEngine engine = new RuleEngine(
            List.of(
                new RevolutionRule(),
                new EightCutRule()
            )
        );

        GameState state = createState();
        Player player = state.getPlayers().get(0);

        CardCombination combination =
            CardCombination.of(List.of(
                new Card(Mark.SPADE, Rank.NINE)
            ));

        RuleResult result = engine.applyRules(
            state,
            player,
            null,
            combination
        );

        assertFalse(state.isRevolution());
        assertFalse(result.isRevolutionOccurred());
        assertFalse(result.shouldClearField());
    }

    private GameState createState() {
        return new GameState(List.of(
            new Player("p1", "A"),
            new Player("p2", "B")
        ));
    }
}
