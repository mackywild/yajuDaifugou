package com.example.daifugo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.example.daifugo.room.GameRoomService;
import org.junit.jupiter.api.Test;

class MaintenanceReadinessServiceTest {
    @Test
    void emptyRoomsMustNotBeConsideredSafeWithoutPersistenceGuarantees() {
        MaintenanceReadinessService.Snapshot snapshot =
                new MaintenanceReadinessService(new GameRoomService()).snapshot();

        assertEquals(0, snapshot.waitingRooms());
        assertEquals(0, snapshot.activeMatches());
        assertEquals(0, snapshot.finishedRooms());
        assertEquals("UNKNOWN", snapshot.pendingPersistenceStatus());
        assertFalse(snapshot.safeToStop());
    }

    @Test
    void waitingRoomIsCountedWithoutAllowingMaintenance() {
        GameRoomService rooms = new GameRoomService();
        rooms.createRoom();
        MaintenanceReadinessService.Snapshot snapshot =
                new MaintenanceReadinessService(rooms).snapshot();

        assertEquals(1, snapshot.waitingRooms());
        assertEquals(0, snapshot.activeMatches());
        assertFalse(snapshot.safeToStop());
    }
}
