package com.example.daifugo.room;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.service.GameEngineFactory;

/**
 * GameRoomの退出・終了ライフサイクルを確認するテスト。
 */
class GameRoomLifecycleTest {

    @Test
    void 対戦中は退出できず終了後は退出できる() {
        Player a = new Player("a", "A");
        Player b = new Player("b", "B");
        GameRoom room = new GameRoom("room");
        room.addPlayer(a);
        room.addPlayer(b);

        GameState state = new GameState(List.of(a, b));
        state.start();
        room.start(state, new GameEngineFactory().create());

        assertThrows(IllegalStateException.class, () -> room.removePlayer("a"));

        state.finish();
        assertTrue(room.removePlayer("a"));
        assertFalse(room.containsPlayer("a"));
    }
}
