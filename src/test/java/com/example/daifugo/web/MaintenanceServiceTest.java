package com.example.daifugo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaintenanceServiceTest {
    @TempDir Path tempDirectory;

    @Test
    void defaultIsNormal() {
        MaintenanceService service = new MaintenanceService("");
        assertEquals(MaintenanceService.Mode.NORMAL, service.mode());
        assertFalse(service.blocks("POST", "/api/rooms"));
    }

    @Test
    void drainingBlocksNewMatchesButAllowsCurrentMatch() throws Exception {
        Path modeFile = tempDirectory.resolve("maintenance.mode");
        Files.writeString(modeFile, "DRAINING\n");
        MaintenanceService service = new MaintenanceService(modeFile.toString());
        assertTrue(service.blocks("POST", "/api/rooms"));
        assertTrue(service.blocks("POST", "/api/rooms/cpu"));
        assertTrue(service.blocks("POST", "/api/rooms/abc123/join"));
        assertTrue(service.blocks("POST", "/api/rooms/abc123/start"));
        assertFalse(service.blocks("POST", "/api/rooms/abc123/play"));
        assertFalse(service.blocks("GET", "/api/rooms/abc123/state"));
        assertFalse(service.blocks("GET", "/api/system/status"));
    }

    @Test
    void maintenanceBlocksGameplayAndProgress() throws Exception {
        Path modeFile = tempDirectory.resolve("maintenance.mode");
        Files.writeString(modeFile, "MAINTENANCE");
        MaintenanceService service = new MaintenanceService(modeFile.toString());
        assertTrue(service.blocks("POST", "/api/rooms/abc123/play"));
        assertTrue(service.blocks("GET", "/api/rooms/abc123/state"));
        assertTrue(service.blocks("POST", "/api/users/me/matches/abc123/record"));
        assertFalse(service.blocks("GET", "/api/system/status"));
        assertFalse(service.blocks("POST", "/api/auth/firebase/session"));
    }

    @Test
    void invalidFileFailsClosed() throws Exception {
        Path modeFile = tempDirectory.resolve("maintenance.mode");
        Files.writeString(modeFile, "invalid");
        assertEquals(MaintenanceService.Mode.MAINTENANCE,
                new MaintenanceService(modeFile.toString()).mode());
    }
}
