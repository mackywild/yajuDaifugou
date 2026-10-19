package com.example.daifugo.game.cpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                new GameRuleSettings(2, true, true, false, true, true, true, true)
        ).room();

        assertEquals(2, room.getPlayers().stream().filter(p -> p.isCpu()).count());
        assertTrue(room.getPlayers().stream()
                .filter(p -> p.isCpu())
                .allMatch(p -> p.getCpuDifficulty() == CpuDifficulty.N_GOD));
        assertEquals(2, room.getRuleSettings().jokerCount());
        assertTrue(room.getRuleSettings().yajuRule());
    }

    @Test
    void createsHumanPlusSevenCpusForEightPlayerGame() {
        GameRoomService rooms = new GameRoomService();
        CpuGameService service = new CpuGameService(rooms);

        GameRoom room = service.createGame(
                "human",
                7,
                CpuDifficulty.HARD,
                GameRuleSettings.standard()
        ).room();

        assertEquals(8, room.getPlayerCount());
        assertEquals(7, room.getPlayers().stream().filter(p -> p.isCpu()).count());
        assertTrue(room.isStarted());
    }

    @Test
    void rejectsEightCpuPlayersBecauseTotalWouldExceedEight() {
        GameRoomService rooms = new GameRoomService();
        CpuGameService service = new CpuGameService(rooms);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createGame(
                        "human",
                        8,
                        CpuDifficulty.EASY,
                        GameRuleSettings.standard()
                )
        );
    }
    @Test
    void animatedBrowserCpuGameCanAdvanceCpuTurnsOneByOne() {
        GameRoomService rooms = new GameRoomService();
        CpuGameService service = new CpuGameService(rooms);

        boolean exercisedCpuTurn = false;

        /*
         * 先手はダイヤ3の配牌先で決まるため複数回生成する。
         * ブラウザ向け生成ではCPUを自動消化せず、executeNextCpuTurnで
         * 1手ずつ確実に前進できることを確認する。
         */
        for (int attempt = 0; attempt < 24 && !exercisedCpuTurn; attempt++) {
            GameRoom room = service.createGameForAnimatedClient(
                    "browser-human",
                    3,
                    CpuDifficulty.HARD,
                    GameRuleSettings.standard()
            ).room();

            int steps = 0;
            while (!room.isFinished()
                    && room.getGameState().getCurrentPlayer().isCpu()
                    && steps < 32) {

                String actingPlayerId = room.getGameState().getCurrentPlayer().getId();
                CpuGameService.CpuTurnResult result =
                        service.executeNextCpuTurn(room.getRoomId());

                assertEquals(actingPlayerId, result.playerId());
                assertEquals(room, result.room());
                assertTrue(result.type() != null);
                steps++;
                exercisedCpuTurn = true;
            }

            assertTrue(
                    room.isFinished()
                            || !room.getGameState().getCurrentPlayer().isCpu()
                            || steps < 32,
                    "ブラウザCPU進行がCPU手番で停止してはいけない"
            );
        }

        assertTrue(exercisedCpuTurn, "CPU先手のブラウザゲームを生成できませんでした");
    }

}
