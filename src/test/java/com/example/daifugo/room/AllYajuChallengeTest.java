package com.example.daifugo.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.domain.YajuStatus;
import com.example.daifugo.game.mode.GameMode;

class AllYajuChallengeTest {

    @Test
    void 四人戦開始時に全員へ八と十を保証する() {
        GameRoomService service = new GameRoomService();
        GameRoom room = service.createRoom(
                GameMode.MULTI_YAJU_CHALLENGE,
                GameRuleSettings.standard());

        for (int i = 1; i <= 4; i++) {
            service.joinRoom(
                    room.getRoomId(),
                    new Player("p" + i, "P" + i));
        }

        service.startAllYajuChallenge(room.getRoomId());

        assertTrue(room.isStarted());
        assertEquals(GameMode.MULTI_YAJU_CHALLENGE, room.getGameMode());
        assertEquals(4, room.getGameState().getPlayers().size());
        room.getGameState().getPlayers().forEach(player -> {
            assertTrue(player.countRank(Rank.EIGHT) >= 1);
            assertTrue(player.countRank(Rank.TEN) >= 1);
            assertEquals(YajuStatus.ACTIVE, player.getYajuStatus());
        });
    }
}
