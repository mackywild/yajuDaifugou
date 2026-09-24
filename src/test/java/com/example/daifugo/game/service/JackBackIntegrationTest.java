package com.example.daifugo.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameEventType;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;

/** Jバックと早漏イベントの結合テスト。 */
class JackBackIntegrationTest {

    @Test
    void jackBack_normalState_reversesStrengthUntilFieldClears() {
        Card jack = card(Mark.SPADE, Rank.JACK);
        Card ten = card(Mark.HEART, Rank.TEN);
        Player a = player("A", jack, card(Mark.CLUB, Rank.FIVE));
        Player b = player("B", ten, card(Mark.CLUB, Rank.SIX));
        Player c = player("C", card(Mark.DIAMOND, Rank.NINE));
        GameState state = playingState(a, b, c);
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, "A", List.of(jack));

        assertTrue(state.isJackBack());
        assertTrue(state.isStrengthReversed());
        assertEquals("B", state.getCurrentPlayer().getId());
        assertTrue(state.getEvents().stream().anyMatch(e -> e.type() == GameEventType.JACK_BACK));

        engine.play(state, "B", List.of(ten));
        assertEquals("C", state.getCurrentPlayer().getId());
    }

    @Test
    void jackBack_duringRevolution_cancelsReversedStrength() {
        Card jack = card(Mark.SPADE, Rank.JACK);
        Card queen = card(Mark.HEART, Rank.QUEEN);
        Card ten = card(Mark.CLUB, Rank.TEN);
        Player a = player("A", jack, card(Mark.CLUB, Rank.FIVE));
        Player b = player("B", queen, ten);
        Player c = player("C", card(Mark.DIAMOND, Rank.NINE));
        GameState state = playingState(a, b, c);
        state.toggleRevolution();
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, "A", List.of(jack));

        assertTrue(state.isRevolution());
        assertTrue(state.isJackBack());
        assertFalse(state.isStrengthReversed());
        assertThrows(IllegalArgumentException.class, () -> engine.play(state, "B", List.of(ten)));
        engine.play(state, "B", List.of(queen));
    }

    @Test
    void nextPlayerPlaysThree_emitsEarlyShot() {
        Card jack = card(Mark.SPADE, Rank.JACK);
        Card three = card(Mark.HEART, Rank.THREE);
        Player a = player("A", jack, card(Mark.CLUB, Rank.FIVE));
        Player b = player("B", three, card(Mark.CLUB, Rank.SIX));
        Player c = player("C", card(Mark.DIAMOND, Rank.NINE));
        GameState state = playingState(a, b, c);
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, "A", List.of(jack));
        engine.play(state, "B", List.of(three));

        assertTrue(state.getEvents().stream().anyMatch(e ->
                e.type() == GameEventType.EARLY_SHOT && e.playerId().equals("B")));
    }

    @Test
    void nextPlayerPlaysPairOfThrees_doesNotEmitEarlyShot() {
        Card jack = card(Mark.SPADE, Rank.JACK);
        Card threeHeart = card(Mark.HEART, Rank.THREE);
        Card threeDiamond = card(Mark.DIAMOND, Rank.THREE);
        Player a = player("A", jack, card(Mark.CLUB, Rank.FIVE));
        Player b = player("B", threeHeart, threeDiamond, card(Mark.CLUB, Rank.SIX));
        Player c = player("C", card(Mark.DIAMOND, Rank.NINE));
        GameState state = playingState(a, b, c);
        GameEngine engine = new GameEngineFactory().create();

        /* ペア比較にするため、AもJのペアを場へ出す。 */
        Card jackHeart = card(Mark.HEART, Rank.JACK);
        a.addCard(jackHeart);
        engine.play(state, "A", List.of(jack, jackHeart));
        engine.play(state, "B", List.of(threeHeart, threeDiamond));

        assertFalse(state.getEvents().stream().anyMatch(e -> e.type() == GameEventType.EARLY_SHOT));
    }

    @Test
    void nextPlayerPasses_doesNotEmitEarlyShotForFollowingPlayer() {
        Card jack = card(Mark.SPADE, Rank.JACK);
        Card three = card(Mark.HEART, Rank.THREE);
        Player a = player("A", jack, card(Mark.CLUB, Rank.FIVE));
        Player b = player("B", card(Mark.CLUB, Rank.SIX));
        Player c = player("C", three, card(Mark.DIAMOND, Rank.FOUR));
        GameState state = playingState(a, b, c);
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, "A", List.of(jack));
        engine.pass(state, "B");
        engine.play(state, "C", List.of(three));

        assertFalse(state.getEvents().stream().anyMatch(e -> e.type() == GameEventType.EARLY_SHOT));
    }

    @Test
    void jackBackDuringRevolution_nextPlayerThree_doesNotEmitEarlyShot() {
        Card jack = card(Mark.SPADE, Rank.JACK);
        Card three = card(Mark.HEART, Rank.THREE);
        Player a = player("A", jack, card(Mark.CLUB, Rank.FIVE));
        Player b = player("B", three, card(Mark.CLUB, Rank.SIX));
        Player c = player("C", card(Mark.DIAMOND, Rank.NINE));
        GameState state = playingState(a, b, c);
        state.toggleRevolution();
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, "A", List.of(jack));

        assertFalse(state.isStrengthReversed());
        assertFalse(state.isEarlyShotArmPending());
        assertThrows(IllegalArgumentException.class, () -> engine.play(state, "B", List.of(three)));
        assertFalse(state.getEvents().stream().anyMatch(e -> e.type() == GameEventType.EARLY_SHOT));
    }

    @Test
    void rejectedPlay_doesNotConsumeEarlyShotEligibility() {
        Card jack = card(Mark.SPADE, Rank.JACK);
        Card three = card(Mark.HEART, Rank.THREE);
        Card eight = card(Mark.CLUB, Rank.EIGHT);
        Card ten = card(Mark.DIAMOND, Rank.TEN);
        Player a = player("A", jack, card(Mark.CLUB, Rank.FIVE));
        Player b = player("B", three, eight, ten);
        Player c = player("C", card(Mark.DIAMOND, Rank.NINE));
        GameState state = playingState(a, b, c);
        YajuRuleService yaju = new YajuRuleService();
        yaju.activateIfEligible(state, b);
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, "A", List.of(jack));
        assertTrue(state.isEarlyShotEligible("B"));

        /* 最後の8を消費する不正プレイは野獣ルールで拒否される。 */
        assertThrows(IllegalArgumentException.class, () -> engine.play(state, "B", List.of(eight)));
        assertTrue(state.isEarlyShotEligible("B"));

        engine.play(state, "B", List.of(three));
        assertTrue(state.getEvents().stream().anyMatch(e -> e.type() == GameEventType.EARLY_SHOT));
    }

    @Test
    void jackBackDisabled_jackDoesNotActivate() {
        Card jack = card(Mark.SPADE, Rank.JACK);
        Player a = player("A", jack, card(Mark.CLUB, Rank.FIVE));
        Player b = player("B", card(Mark.HEART, Rank.QUEEN), card(Mark.CLUB, Rank.SIX));
        Player c = player("C", card(Mark.DIAMOND, Rank.NINE));
        GameState state = playingState(a, b, c);
        var settings = new com.example.daifugo.game.config.GameRuleSettings(
                1, true, true, true, true, true, false, true);
        GameEngine engine = new GameEngineFactory().create(settings);

        engine.play(state, "A", List.of(jack));

        assertFalse(state.isJackBack());
        assertFalse(state.getEvents().stream().anyMatch(e -> e.type() == GameEventType.JACK_BACK));
    }

    @Test
    void jackBack_fieldClears_resetsJackBackAndEarlyShotState() {
        Card jack = card(Mark.SPADE, Rank.JACK);
        Player a = player("A", jack, card(Mark.CLUB, Rank.FIVE));
        Player b = player("B", card(Mark.HEART, Rank.TEN));
        Player c = player("C", card(Mark.DIAMOND, Rank.NINE));
        GameState state = playingState(a, b, c);
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, "A", List.of(jack));
        engine.pass(state, "B");
        engine.pass(state, "C");

        assertFalse(state.isJackBack());
        assertFalse(state.isEarlyShotArmPending());
        assertFalse(state.isEarlyShotEligible("B"));
        assertEquals("A", state.getCurrentPlayer().getId());
    }

    private static GameState playingState(Player... players) {
        GameState state = new GameState(List.of(players));
        state.start();
        state.changeCurrentPlayer(0);
        return state;
    }

    private static Player player(String id, Card... cards) {
        Player player = new Player(id, id);
        player.addCards(List.of(cards));
        return player;
    }

    private static Card card(Mark mark, Rank rank) {
        return new Card(mark, rank);
    }

    @Test
    void jackBack_allowsThreePlusJokerPairAsWildcardPair() {
        Card jackSpade = card(Mark.SPADE, Rank.JACK);
        Card jackHeart = card(Mark.HEART, Rank.JACK);
        Player a = player(
                "A",
                jackSpade,
                jackHeart,
                card(Mark.CLUB, Rank.FIVE)
        );

        Card three = card(Mark.SPADE, Rank.THREE);
        Card joker = card(Mark.JOKER, Rank.JOKER);
        Player b = player(
                "B",
                three,
                joker,
                card(Mark.CLUB, Rank.SIX)
        );
        Player c = player("C", card(Mark.DIAMOND, Rank.NINE));

        GameState state = playingState(a, b, c);
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, "A", List.of(jackSpade, jackHeart));

        assertTrue(state.isStrengthReversed());

        engine.play(state, "B", List.of(three, joker));

        assertEquals(
                com.example.daifugo.game.domain.CombinationType.PAIR,
                state.getFieldCombination().getType()
        );
        assertEquals(3, state.getFieldCombination().getBaseStrength());
        assertEquals("C", state.getCurrentPlayer().getId());
    }

}
