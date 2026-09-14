package com.example.daifugo.game.cpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.mode.GameMode;
import com.example.daifugo.room.GameRoom;
import com.example.daifugo.room.GameRoomService;

/** CPU戦【ひとりでイク】の生成と自動進行を確認する。 */
class CpuGameServiceTest {

    @Test
    void createsHumanPlusThreeCpusAndStartsImmediately() {
        GameRoomService rooms = new GameRoomService();
        CpuGameService service = new CpuGameService(rooms);

        CpuGameService.CpuGameCreation creation = service.createGame(
                "human",
                3,
                CpuDifficulty.HARD,
                GameRuleSettings.standard()
        );

        GameRoom room = creation.room();
        assertEquals(GameMode.CPU, room.getGameMode());
        assertTrue(room.isStarted());
        assertEquals(4, room.getPlayerCount());
        assertEquals(3, room.getPlayers().stream().filter(p -> p.isCpu()).count());
        assertTrue(room.isFinished()
                || !room.getGameState().getCurrentPlayer().isCpu(),
                "createGame後は人間の手番かゲーム終了状態までCPUが進行する");
    }

    @Test
    void configuredDifficultyIsAppliedToAllCpuPlayers() {
        GameRoomService rooms = new GameRoomService();
        CpuGameService service = new CpuGameService(rooms);

        GameRoom room = service.createGame(
                "human",
                2,
                CpuDifficulty.N_GOD,
                new GameRuleSettings(2, true, true, false, true, true, true)
        ).room();

        assertEquals(2, room.getPlayers().stream().filter(p -> p.isCpu()).count());
        assertTrue(room.getPlayers().stream()
                .filter(p -> p.isCpu())
                .allMatch(p -> p.getCpuDifficulty() == CpuDifficulty.N_GOD));
        assertEquals(2, room.getRuleSettings().jokerCount());
        assertTrue(room.getRuleSettings().yajuRule());
    }
}
