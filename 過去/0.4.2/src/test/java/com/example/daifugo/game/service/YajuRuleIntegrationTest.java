package com.example.daifugo.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.example.daifugo.game.domain.YajuStatus;

/**
 * 野獣ルールと7渡しの結合テスト。
 */
class YajuRuleIntegrationTest {

    /** 配牌時に8と10を持つプレイヤーが対象化されること。 */
    @Test
    void initialHand_withEightAndTen_activatesYajuForSharedState() {
        Player player = player("A", "A",
                card(Mark.SPADE, Rank.EIGHT),
                card(Mark.HEART, Rank.TEN));
        Player opponent = player("B", "B", card(Mark.CLUB, Rank.THREE));
        GameState state = playingState(player, opponent);

        new YajuRuleService().initializeTargets(state);

        assertEquals(YajuStatus.ACTIVE, player.getYajuStatus());
        assertEquals(GameEventType.YAJU_AVAILABLE, state.getEvents().get(0).type());
    }

    /** 最後の2枚を8→10で出すと野獣上がり成功になること。 */
    @Test
    void yajuFinish_eightThenTen_completesAndEmitsSuccessEvent() {
        Card eight = card(Mark.SPADE, Rank.EIGHT);
        Card ten = card(Mark.HEART, Rank.TEN);
        Player player = player("A", "A", eight, ten);
        Player opponent = player("B", "B", card(Mark.CLUB, Rank.THREE));
        GameState state = playingState(player, opponent);
        new YajuRuleService().initializeTargets(state);
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, player.getId(), List.of(eight));
        assertEquals(YajuStatus.EIGHT_PLAYED, player.getYajuStatus());
        assertEquals(player.getId(), state.getCurrentPlayer().getId());

        engine.play(state, player.getId(), List.of(ten));

        assertEquals(YajuStatus.COMPLETED, player.getYajuStatus());
        assertEquals(1, player.getRank());
        assertTrue(state.getEvents().stream()
                .anyMatch(event -> event.type() == GameEventType.YAJU_SUCCESS));
    }

    /** 8→10以外で上がった場合に反則最下位となること。 */
    @Test
    void yajuFinish_wrongOrderOrCombination_becomesPenaltyFinish() {
        Card eight = card(Mark.SPADE, Rank.EIGHT);
        Card nine = card(Mark.SPADE, Rank.NINE);
        Card ten = card(Mark.SPADE, Rank.TEN);
        Player player = player("A", "A", eight, nine, ten);
        Player opponent = player("B", "B", card(Mark.CLUB, Rank.THREE));
        GameState state = playingState(player, opponent);
        new YajuRuleService().initializeTargets(state);

        new GameEngineFactory().create().play(
                state,
                player.getId(),
                List.of(eight, nine, ten));

        assertEquals(YajuStatus.PENALTY, player.getYajuStatus());
        assertEquals(2, player.getRank());
        assertEquals(1, opponent.getRank());
    }

    /** 余分な8と10を7渡しし、隣のプレイヤーを新規対象化できること。 */
    @Test
    void sevenTransfer_extraEightAndTen_canSpreadYajuToNeighbor() {
        Card seven1 = card(Mark.SPADE, Rank.SEVEN);
        Card seven2 = card(Mark.HEART, Rank.SEVEN);
        Card eight1 = card(Mark.SPADE, Rank.EIGHT);
        Card eight2 = card(Mark.HEART, Rank.EIGHT);
        Card ten1 = card(Mark.SPADE, Rank.TEN);
        Card ten2 = card(Mark.HEART, Rank.TEN);

        Player source = player("A", "A",
                seven1, seven2, eight1, eight2, ten1, ten2);
        Player target = player("B", "B", card(Mark.CLUB, Rank.THREE));
        GameState state = playingState(source, target);
        new YajuRuleService().initializeTargets(state);
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, source.getId(), List.of(seven1, seven2));
        assertEquals(2, state.getPendingSevenTransfer().cardCount());

        engine.transferSeven(state, source.getId(), List.of(eight1, ten1));

        assertEquals(1, source.countRank(Rank.EIGHT));
        assertEquals(1, source.countRank(Rank.TEN));
        assertEquals(YajuStatus.ACTIVE, source.getYajuStatus());
        assertEquals(YajuStatus.ACTIVE, target.getYajuStatus());
        assertEquals(2, state.getEvents().stream()
                .filter(event -> event.type() == GameEventType.YAJU_AVAILABLE)
                .count());
    }

    /** 野獣対象者が最後の8を7渡しして対象解除することはできないこと。 */
    @Test
    void sevenTransfer_cannotGiveAwayLastEightOrTen() {
        Card seven = card(Mark.SPADE, Rank.SEVEN);
        Card eight = card(Mark.HEART, Rank.EIGHT);
        Card ten = card(Mark.CLUB, Rank.TEN);
        Card five = card(Mark.DIAMOND, Rank.FIVE);
        Player source = player("A", "A", seven, eight, ten, five);
        Player target = player("B", "B", card(Mark.CLUB, Rank.THREE));
        GameState state = playingState(source, target);
        new YajuRuleService().initializeTargets(state);
        GameEngine engine = new GameEngineFactory().create();

        engine.play(state, source.getId(), List.of(seven));

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.transferSeven(state, source.getId(), List.of(eight)));
        assertTrue(state.hasPendingSevenTransfer());
        assertEquals(YajuStatus.ACTIVE, source.getYajuStatus());
    }

    private static GameState playingState(Player... players) {
        GameState state = new GameState(List.of(players));
        state.start();
        state.changeCurrentPlayer(0);
        return state;
    }

    private static Player player(String id, String name, Card... cards) {
        Player player = new Player(id, name);
        player.addCards(List.of(cards));
        return player;
    }

    private static Card card(Mark mark, Rank rank) {
        return new Card(mark, rank);
    }
}
