package com.example.daifugo.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.rule.EightCutRule;
import com.example.daifugo.game.rule.MarkLockRule;
import com.example.daifugo.game.rule.PlayValidator;
import com.example.daifugo.game.rule.RevolutionRule;
import com.example.daifugo.game.rule.RuleEngine;
import com.example.daifugo.game.rule.finish.EightFinishRule;
import com.example.daifugo.game.rule.finish.FinishValidator;
import com.example.daifugo.game.rule.finish.JokerFinishRule;
import com.example.daifugo.game.rule.finish.SpadeThreeFinishRule;
import com.example.daifugo.game.rule.finish.TwoFinishRule;
import com.example.daifugo.game.service.GameEngine;
import com.example.daifugo.game.service.TurnManager;

/**
 * Web化後もゲームコアの基本進行が壊れていないことを確認するスモークテスト。
 */
class CoreGameSmokeTest {

    @Test
    void カードを出すと次のプレイヤーへ手番が移る() {
        Player a = new Player("a", "A");
        Player b = new Player("b", "B");
        a.addCards(List.of(new Card(Mark.SPADE, Rank.FIVE), new Card(Mark.HEART, Rank.SEVEN)));
        b.addCards(List.of(new Card(Mark.CLUB, Rank.SIX), new Card(Mark.DIAMOND, Rank.NINE)));

        GameState state = new GameState(List.of(a, b));
        state.start();

        GameEngine engine = createEngine();
        engine.play(state, "a", List.of(new Card(Mark.SPADE, Rank.FIVE)));

        assertEquals("b", state.getCurrentPlayer().getId());
        assertEquals(Rank.FIVE, state.getFieldCombination().getCards().get(0).getRank());
    }

    @Test
    void ジョーカーの上にスペード3を出せる() {
        PlayValidator validator = new PlayValidator();
        var joker = com.example.daifugo.game.domain.CardCombination.of(
                List.of(new Card(Mark.JOKER, Rank.JOKER)));
        var spadeThree = com.example.daifugo.game.domain.CardCombination.of(
                List.of(new Card(Mark.SPADE, Rank.THREE)));

        assertTrue(validator.canPlay(spadeThree, joker, false, null));
    }


    @Test
    void 八切り上がりは反則上がりとして最下位になる() {
        Player a = new Player("a", "A");
        Player b = new Player("b", "B");
        Card eight = new Card(Mark.SPADE, Rank.EIGHT);
        a.addCard(eight);
        b.addCards(List.of(new Card(Mark.CLUB, Rank.NINE), new Card(Mark.HEART, Rank.TEN)));

        GameState state = new GameState(List.of(a, b));
        state.start();

        createEngine().play(state, "a", List.of(eight));

        assertEquals(2, a.getRank());
        assertEquals(1, b.getRank());
    }

    @Test
    void スペード3上がりは反則上がりとして最下位になる() {
        Player a = new Player("a", "A");
        Player b = new Player("b", "B");
        Card spadeThree = new Card(Mark.SPADE, Rank.THREE);
        a.addCard(spadeThree);
        b.addCards(List.of(new Card(Mark.CLUB, Rank.FOUR), new Card(Mark.HEART, Rank.FIVE)));

        GameState state = new GameState(List.of(a, b));
        state.start();

        createEngine().play(state, "a", List.of(spadeThree));

        assertEquals(2, a.getRank());
        assertEquals(1, b.getRank());
    }

    private GameEngine createEngine() {
        return new GameEngine(
                new PlayValidator(),
                new TurnManager(),
                new RuleEngine(List.of(
                        new RevolutionRule(),
                        new EightCutRule(),
                        new MarkLockRule())),
                new FinishValidator(List.of(
                        new TwoFinishRule(),
                        new JokerFinishRule(),
                        new EightFinishRule(),
                        new SpadeThreeFinishRule())));
    }
}
