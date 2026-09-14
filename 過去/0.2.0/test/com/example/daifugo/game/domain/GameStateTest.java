package com.example.daifugo.game.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class GameStateTest {

    @Test
    void 初期状態は待機中() {
        GameState state = createState();

        assertEquals(GamePhase.WAITING, state.getPhase());
        assertEquals(0, state.getCurrentPlayerIndex());
        assertEquals("p1", state.getCurrentPlayer().getId());
        assertNull(state.getFieldCombination());
        assertNull(state.getLastPlayedPlayerIndex());
        assertFalse(state.isRevolution());
    }

    @Test
    void ゲームを開始できる() {
        GameState state = createState();

        state.start();

        assertEquals(GamePhase.PLAYING, state.getPhase());
    }

    @Test
    void 待機中以外のゲームは再度開始できない() {
        GameState state = createState();
        state.start();

        assertThrows(
            IllegalStateException.class,
            state::start
        );
    }

    @Test
    void 現在プレイヤーを指定した位置へ変更できる() {
        GameState state = createState();

        state.changeCurrentPlayer(2);

        assertEquals(2, state.getCurrentPlayerIndex());
        assertEquals("p3", state.getCurrentPlayer().getId());
    }

    @Test
    void 不正な位置へ現在プレイヤーを変更できない() {
        GameState state = createState();

        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> state.changeCurrentPlayer(-1)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> state.changeCurrentPlayer(4)
            )
        );
    }

    @Test
    void プレイ中なら場を更新できる() {
        GameState state = createState();
        state.start();

        CardCombination combination = single(Rank.FIVE);

        state.updateField(combination, 1);

        assertSame(combination, state.getFieldCombination());
        assertEquals(1, state.getLastPlayedPlayerIndex());
    }

    @Test
    void ゲーム開始前は場を更新できない() {
        GameState state = createState();

        assertThrows(
            IllegalStateException.class,
            () -> state.updateField(single(Rank.FIVE), 0)
        );
    }

    @Test
    void 不正なプレイヤー位置では場を更新できない() {
        GameState state = createState();
        state.start();

        CardCombination combination = single(Rank.FIVE);

        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> state.updateField(combination, -1)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> state.updateField(combination, 4)
            )
        );
    }

    @Test
    void 場を流すと場札と最終プレイヤーを初期化しパスも解除する() {
        GameState state = createState();
        state.start();

        state.updateField(single(Rank.FIVE), 0);

        state.getPlayers().get(1).pass();
        state.getPlayers().get(2).pass();

        state.clearField();

        assertNull(state.getFieldCombination());
        assertNull(state.getLastPlayedPlayerIndex());

        assertTrue(
            state.getPlayers().stream()
                .noneMatch(Player::isPassed)
        );
    }

    @Test
    void 革命状態を切り替えられる() {
        GameState state = createState();

        state.toggleRevolution();
        assertTrue(state.isRevolution());

        state.toggleRevolution();
        assertFalse(state.isRevolution());
    }

    @Test
    void 順位確定済みプレイヤー数を取得できる() {
        GameState state = createState();

        state.getPlayers().get(0).assignRank(1);
        state.getPlayers().get(1).assignRank(2);

        assertEquals(2, state.getFinishedPlayerCount());
    }

    @Test
    void ゲームを終了状態にできる() {
        GameState state = createState();
        state.start();

        state.finish();

        assertEquals(GamePhase.FINISHED, state.getPhase());
    }

    @Test
    void プレイヤーが2人未満なら生成できない() {
        Player player = new Player("p1", "A");

        assertThrows(
            IllegalArgumentException.class,
            () -> new GameState(List.of(player))
        );
    }

    @Test
    void プレイヤーIDが重複していたら生成できない() {
        Player player1 = new Player("p1", "A");
        Player player2 = new Player("p1", "B");

        assertThrows(
            IllegalArgumentException.class,
            () -> new GameState(List.of(player1, player2))
        );
    }

    @Test
    void 取得したプレイヤー一覧は変更できない() {
        GameState state = createState();

        assertThrows(
            UnsupportedOperationException.class,
            () -> state.getPlayers().clear()
        );
    }

    private GameState createState() {
        return new GameState(List.of(
            new Player("p1", "A"),
            new Player("p2", "B"),
            new Player("p3", "C"),
            new Player("p4", "D")
        ));
    }

    private CardCombination single(Rank rank) {
        return CardCombination.of(List.of(
            new Card(Mark.SPADE, rank)
        ));
    }
}