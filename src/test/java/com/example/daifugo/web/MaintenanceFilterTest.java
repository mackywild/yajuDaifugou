package com.example.daifugo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 実際のHTTPフィルターで更新操作が停止することを確認する。 */
class MaintenanceFilterTest {
    @TempDir Path tempDirectory;

    @Test
    void maintenanceRejectsGameRequestWith503() throws Exception {
        Path modeFile = tempDirectory.resolve("mode");
        Files.writeString(modeFile, "MAINTENANCE");
        MaintenanceFilter filter = new MaintenanceFilter(new MaintenanceService(modeFile.toString()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("POST", "/api/rooms/abc/play"),
                response, new MockFilterChain());
        assertEquals(503, response.getStatus());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertTrue(response.getContentAsString().contains("MAINTENANCE"));
    }

    @Test
    void statusRemainsAccessibleDuringMaintenance() throws Exception {
        Path modeFile = tempDirectory.resolve("mode");
        Files.writeString(modeFile, "MAINTENANCE");
        MaintenanceFilter filter = new MaintenanceFilter(new MaintenanceService(modeFile.toString()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/system/status"),
                response, new MockFilterChain());
        assertEquals(200, response.getStatus());
    }
}
