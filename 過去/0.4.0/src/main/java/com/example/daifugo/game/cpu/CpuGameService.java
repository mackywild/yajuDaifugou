package com.example.daifugo.game.cpu;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.PendingSevenTransfer;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.mode.GameMode;
import com.example.daifugo.room.GameRoom;
import com.example.daifugo.room.GameRoomService;

/**
 * CPU戦【ひとりでイク】の生成とCPU自動進行を担当する。
 */
@Service
public class CpuGameService {
    private static final int MAX_CPU_COUNT = 3;
    private static final int MAX_AUTO_STEPS = 500;

    private final GameRoomService roomService;
    private final LegalMoveGenerator moveGenerator = new LegalMoveGenerator();
    private final CpuStrategyFactory strategyFactory = new CpuStrategyFactory();

    public CpuGameService(GameRoomService roomService) {
        this.roomService = roomService;
    }

    /** CPU戦作成結果。 */
    public record CpuGameCreation(GameRoom room, Player humanPlayer) {}

    /**
     * 人間1人 + CPU 1～3人でゲームを即時開始する。
     */
    public CpuGameCreation createGame(
            String humanName,
            int cpuCount,
            CpuDifficulty difficulty,
            GameRuleSettings settings
    ) {
        Objects.requireNonNull(difficulty, "difficulty must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        if (cpuCount < 1 || cpuCount > MAX_CPU_COUNT) {
            throw new IllegalArgumentException("CPU人数は1～3人で指定してください");
        }

        GameRoom room = roomService.createRoom(GameMode.CPU, settings);
        Player human = new Player(UUID.randomUUID().toString(), humanName);
        roomService.joinRoom(room.getRoomId(), human);

        for (int i = 1; i <= cpuCount; i++) {
            Player cpu = Player.cpu(
                    UUID.randomUUID().toString(),
                    "CPU-" + i,
                    difficulty
            );
            roomService.joinRoom(room.getRoomId(), cpu);
        }

        roomService.startGame(room.getRoomId());
        processCpuTurns(room.getRoomId());
        return new CpuGameCreation(room, human);
    }

    /**
     * 人間の手番になるかゲーム終了まで、CPUの手を連続実行する。
     */
    public GameRoom processCpuTurns(String roomId) {
        GameRoom room = roomService.getRoom(roomId);
        if (room.getGameMode() != GameMode.CPU || !room.isStarted()) {
            return room;
        }

        synchronized (room) {
            int steps = 0;
            while (!room.isFinished()) {
                if (++steps > MAX_AUTO_STEPS) {
                    throw new IllegalStateException("CPU自動進行が上限回数を超えました");
                }

                GameState state = room.getGameState();
                Player current = state.getCurrentPlayer();
                if (!current.isCpu()) {
                    return room;
                }

                CpuStrategy strategy = strategyFactory.create(current.getCpuDifficulty());

                if (state.hasPendingSevenTransfer()) {
                    PendingSevenTransfer pending = state.getPendingSevenTransfer();
                    if (!pending.sourcePlayerId().equals(current.getId())) {
                        throw new IllegalStateException("7渡し元と現在CPUが一致しません");
                    }
                    List<List<Card>> transfers = moveGenerator.generateSevenTransfers(
                            current,
                            pending.cardCount(),
                            room.getRuleSettings()
                    );
                    List<Card> selected = strategy.chooseSevenTransfer(
                            state,
                            current,
                            transfers,
                            room.getRuleSettings()
                    );
                    room.getGameEngine().transferSeven(state, current.getId(), selected);
                    room.touch();
                    continue;
                }

                List<CpuMove> legalMoves = moveGenerator.generate(
                        state,
                        current,
                        room.getRuleSettings()
                );
                CpuMove move = strategy.chooseMove(
                        state,
                        current,
                        legalMoves,
                        room.getRuleSettings()
                );
                if (move.pass()) {
                    room.getGameEngine().pass(state, current.getId());
                } else {
                    room.getGameEngine().play(state, current.getId(), move.cards());
                }
                room.touch();
            }
            return room;
        }
    }
}
