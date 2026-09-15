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

public class EightCutRuleTest {
    private final EightCutRule rule =
            new EightCutRule();

        @Test
        void 八を出した場合はルールに一致する() {
            GameState state = createState();
            Player player = state.getPlayers().get(0);

            CardCombination combination =
                CardCombination.of(List.of(
                    new Card(Mark.SPADE, Rank.EIGHT)
                ));

            assertTrue(
                rule.matches(
                    state,
                    player,
                    null,
                    combination
                )
            );
        }

        @Test
        void 八以外ではルールに一致しない() {
            GameState state = createState();
            Player player = state.getPlayers().get(0);

            CardCombination combination =
                CardCombination.of(List.of(
                    new Card(Mark.SPADE, Rank.NINE)
                ));

            assertFalse(
                rule.matches(
                    state,
                    player,
                    null,
                    combination
                )
            );
        }

        @Test
        void 八切りが発動すると場流しを要求する() {
            GameState state = createState();
            Player player = state.getPlayers().get(0);

            CardCombination combination =
                CardCombination.of(List.of(
                    new Card(Mark.SPADE, Rank.EIGHT)
                ));

            RuleResult result = new RuleResult();

            rule.apply(
                state,
                player,
                null,
                combination,
                result
            );

            assertTrue(result.shouldClearField());
        }

        private GameState createState() {
            return new GameState(List.of(
                new Player("p1", "A"),
                new Player("p2", "B")
            ));
        }
}
