package com.example.daifugo.game.cpu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.service.YajuRuleService;

/** CPU合法手列挙の回帰テスト。 */
class LegalMoveGeneratorTest {

    private final LegalMoveGenerator generator = new LegalMoveGenerator();

    @Test
    void emptyFieldDoesNotGeneratePass() {
        Player player = player("p1", Rank.FIVE, Rank.SIX);
        GameState state = playingState(player, player("p2", Rank.SEVEN));

        List<CpuMove> moves = generator.generate(state, player, noYajuSettings());

        assertFalse(moves.stream().anyMatch(CpuMove::pass));
        assertTrue(moves.stream().anyMatch(move -> !move.pass()));
    }

    @Test
    void nonEmptyFieldIncludesPass() {
        Player player = player("p1", Rank.FIVE, Rank.SIX);
        GameState state = playingState(player, player("p2", Rank.SEVEN));
        state.updateField(
                com.example.daifugo.game.domain.CardCombination.of(
                        List.of(new Card(Mark.SPADE, Rank.FOUR))),
                1
        );

        List<CpuMove> moves = generator.generate(state, player, noYajuSettings());

        assertTrue(moves.stream().anyMatch(CpuMove::pass));
    }

    @Test
    void yajuTargetWithOnlyEightAndTenCanOnlyStartWithEight() {
        Player player = new Player("p1", "p1");
        Card eight = new Card(Mark.SPADE, Rank.EIGHT);
        Card ten = new Card(Mark.HEART, Rank.TEN);
        player.addCards(List.of(eight, ten));
        GameState state = playingState(player, player("p2", Rank.THREE));
        new YajuRuleService().initializeTargets(state);

        List<CpuMove> moves = generator.generate(state, player, GameRuleSettings.standard());

        assertTrue(moves.stream().anyMatch(move -> move.cards().equals(List.of(eight))));
        assertFalse(moves.stream().anyMatch(move -> move.cards().equals(List.of(ten))));
    }

    private static GameRuleSettings noYajuSettings() {
        return new GameRuleSettings(1, true, true, true, true, false, true);
    }

    private static GameState playingState(Player... players) {
        GameState state = new GameState(List.of(players));
        state.start();
        state.changeCurrentPlayer(0);
        return state;
    }

    private static Player player(String id, Rank... ranks) {
        Player player = new Player(id, id);
        for (int i = 0; i < ranks.length; i++) {
            player.addCard(new Card(i % 2 == 0 ? Mark.SPADE : Mark.HEART, ranks[i]));
        }
        return player;
    }
}
