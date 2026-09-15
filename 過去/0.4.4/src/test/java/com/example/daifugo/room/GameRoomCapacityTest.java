package com.example.daifugo.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.Player;

/** 8人卓の参加上限を確認する。 */
class GameRoomCapacityTest {

    @Test
    void 八人まで参加でき九人目は拒否される() {
        GameRoom room = new GameRoom("room-8");

        for (int i = 1; i <= 7; i++) {
            room.addPlayer(new Player("p" + i, "P" + i));
        }
        assertFalse(room.isFull());

        room.addPlayer(new Player("p8", "P8"));
        assertTrue(room.isFull());
        assertEquals(8, room.getPlayerCount());

        assertThrows(
            IllegalStateException.class,
            () -> room.addPlayer(new Player("p9", "P9"))
        );
    }
}
