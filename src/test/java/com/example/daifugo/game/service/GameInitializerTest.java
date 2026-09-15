package com.example.daifugo.game.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GamePhase;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;

public class GameInitializerTest {
    @Test
    void デッキを配布してゲームを開始できる() {
        GameState state = createState(4);
        GameInitializer initializer =
            new GameInitializer(1);

        initializer.initialize(state);

        assertEquals(
            GamePhase.PLAYING,
            state.getPhase()
        );

        assertEquals(
            53,
            state.getPlayers().stream()
                .mapToInt(Player::getCardCount)
                .sum()
        );
    }

    @Test
    void 配布されたカードに重複がない() {
        GameState state = createState(4);
        GameInitializer initializer =
            new GameInitializer(1);

        initializer.initialize(state);

        List<Card> allCards = state.getPlayers()
            .stream()
            .flatMap(player -> player.getHand().stream())
            .toList();

        Set<Card> distinctCards =
            new HashSet<>(allCards);

        assertEquals(53, allCards.size());
        assertEquals(53, distinctCards.size());
    }

    @Test
    void ダイヤの3を持つプレイヤーが最初の手番になる() {
        GameState state = createState(4);
        GameInitializer initializer =
            new GameInitializer(1);

        initializer.initialize(state);

        Card diamondThree =
            new Card(Mark.DIAMOND, Rank.THREE);

        assertTrue(
            state.getCurrentPlayer()
                .getHand()
                .contains(diamondThree)
        );
    }

    @Test
    void 四人の場合の手札枚数差は一枚以内になる() {
        GameState state = createState(4);
        GameInitializer initializer =
            new GameInitializer(1);

        initializer.initialize(state);

        int minimum = state.getPlayers().stream()
            .mapToInt(Player::getCardCount)
            .min()
            .orElseThrow();

        int maximum = state.getPlayers().stream()
            .mapToInt(Player::getCardCount)
            .max()
            .orElseThrow();

        assertTrue(maximum - minimum <= 1);
    }

    @Test
    void 八人でも全カードを重複なく公平に配布できる() {
        GameState state = createState(8);
        GameInitializer initializer = new GameInitializer(2);

        initializer.initialize(state);

        List<Card> allCards = state.getPlayers().stream()
            .flatMap(player -> player.getHand().stream())
            .toList();

        assertEquals(54, allCards.size());
        assertEquals(2, allCards.stream().filter(Card::isJoker).count());
        assertEquals(52, allCards.stream()
            .filter(card -> !card.isJoker())
            .distinct()
            .count());

        int minimum = state.getPlayers().stream()
            .mapToInt(Player::getCardCount)
            .min()
            .orElseThrow();
        int maximum = state.getPlayers().stream()
            .mapToInt(Player::getCardCount)
            .max()
            .orElseThrow();

        assertTrue(maximum - minimum <= 1);
        assertTrue(state.getCurrentPlayer().getHand().contains(
            new Card(Mark.DIAMOND, Rank.THREE)
        ));
    }

    @Test
    void ジョーカーなしなら五十二枚配布される() {
        GameState state = createState(4);
        GameInitializer initializer =
            new GameInitializer(0);

        initializer.initialize(state);

        int cardCount = state.getPlayers().stream()
            .mapToInt(Player::getCardCount)
            .sum();

        assertEquals(52, cardCount);
    }

    @Test
    void 待機中以外のゲームは初期化できない() {
        GameState state = createState(4);
        state.start();

        GameInitializer initializer =
            new GameInitializer(1);

        assertThrows(
            IllegalStateException.class,
            () -> initializer.initialize(state)
        );
    }

    @Test
    void 手札が既に存在する場合は初期化できない() {
        GameState state = createState(4);

        state.getPlayers().get(0).addCard(
            new Card(Mark.SPADE, Rank.FIVE)
        );

        GameInitializer initializer =
            new GameInitializer(1);

        assertThrows(
            IllegalStateException.class,
            () -> initializer.initialize(state)
        );
    }

    @Test
    void 不正なジョーカー枚数は指定できない() {
        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new GameInitializer(-1)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new GameInitializer(3)
            )
        );
    }

    private GameState createState(int playerCount) {
        List<Player> players =
            java.util.stream.IntStream
                .rangeClosed(1, playerCount)
                .mapToObj(index ->
                    new Player(
                        "p" + index,
                        "Player" + index
                    )
                )
                .toList();

        return new GameState(players);
    }
}
