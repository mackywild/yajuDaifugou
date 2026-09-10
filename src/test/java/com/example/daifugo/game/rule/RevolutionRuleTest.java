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

public class RevolutionRuleTest {
    private final RevolutionRule rule =
            new RevolutionRule();

        @Test
        void 四枚組なら革命ルールに一致する() {
            GameState state = createState();
            Player player = state.getPlayers().get(0);

            CardCombination combination =
                fourOfKind(Rank.FIVE);

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
        void 三枚組では革命ルールに一致しない() {
            GameState state = createState();
            Player player = state.getPlayers().get(0);

            CardCombination combination =
                CardCombination.of(List.of(
                    new Card(Mark.SPADE, Rank.FIVE),
                    new Card(Mark.HEART, Rank.FIVE),
                    new Card(Mark.DIAMOND, Rank.FIVE)
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
        void 革命状態を切り替える() {
            GameState state = createState();
            Player player = state.getPlayers().get(0);

            CardCombination combination =
                fourOfKind(Rank.FIVE);

            RuleResult result = new RuleResult();

            rule.apply(
                state,
                player,
                null,
                combination,
                result
            );

            assertTrue(state.isRevolution());
            assertTrue(result.isRevolutionOccurred());
        }

        @Test
        void 革命中に発動すると革命返しになる() {
            GameState state = createState();
            state.toggleRevolution();

            Player player = state.getPlayers().get(0);

            RuleResult result = new RuleResult();

            rule.apply(
                state,
                player,
                null,
                fourOfKind(Rank.FIVE),
                result
            );

            assertFalse(state.isRevolution());
        }

        private CardCombination fourOfKind(Rank rank) {
            return CardCombination.of(List.of(
                new Card(Mark.SPADE, rank),
                new Card(Mark.HEART, rank),
                new Card(Mark.DIAMOND, rank),
                new Card(Mark.CLUB, rank)
            ));
        }

        private GameState createState() {
            return new GameState(List.of(
                new Player("p1", "A"),
                new Player("p2", "B")
            ));
        }
}
