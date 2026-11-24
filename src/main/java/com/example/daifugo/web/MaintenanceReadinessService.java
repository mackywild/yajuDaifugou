package com.example.daifugo.web;

import com.example.daifugo.room.GameRoom;
import com.example.daifugo.room.GameRoomService;
import org.springframework.stereotype.Service;

/**
 * メンテナンス移行の判定に必要な対戦状況を収集する。
 *
 * 保存待ち件数の永続的な記録機構がない現段階では、対戦数が0でも
 * 安全に停止できるとは判定しない。外部へ公開するAPIではない。
 */
@Service
public class MaintenanceReadinessService {
    private final GameRoomService rooms;

    public MaintenanceReadinessService(GameRoomService rooms) {
        this.rooms = rooms;
    }

    public Snapshot snapshot() {
        int waiting = 0;
        int playing = 0;
        int finished = 0;
        for (GameRoom room : rooms.getRooms()) {
            synchronized (room) {
                if (!room.isStarted()) {
                    waiting++;
                } else if (room.isFinished()) {
                    finished++;
                } else {
                    playing++;
                }
            }
        }
        // Firestoreの未保存結果を全件把握できないため許可しない。
        return new Snapshot(waiting, playing, finished, "UNKNOWN", false);
    }

    public record Snapshot(
            int waitingRooms,
            int activeMatches,
            int finishedRooms,
            String pendingPersistenceStatus,
            boolean safeToStop) {}
}
