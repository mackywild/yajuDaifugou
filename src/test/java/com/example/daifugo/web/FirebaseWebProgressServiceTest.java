package com.example.daifugo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FirebaseWebProgressServiceTest {
    @Test
    void 旧データは安全な範囲に制限して移行する() {
        Map<String, Object> legacy = Map.of(
                "displayName", "A".repeat(40),
                "totalExp", -10,
                "stats", Map.of("wins", 90000000),
                "inventory", Map.of("avatar_business", 5, "unknown", 100),
                "presents", List.of(Map.of("id", "p1", "itemId", "unknown", "quantity", 100)));

        Map<String, Object> sanitized = FirebaseWebProgressService.sanitizeLegacy(legacy);
        assertEquals(20, ((String) sanitized.get("displayName")).length());
        assertEquals(0, sanitized.get("totalExp"));
        assertEquals(10000000, ((Map<?, ?>) sanitized.get("stats")).get("wins"));
        assertEquals(5, ((Map<?, ?>) sanitized.get("inventory")).get("avatar_business"));
        assertFalse(((Map<?, ?>) sanitized.get("inventory")).containsKey("unknown"));
        assertTrue(((List<?>) sanitized.get("presents")).isEmpty());
    }

    @Test
    void Firebase無効時にはトークンを受け付けない() {
        FirebaseWebAuthService authService = new FirebaseWebAuthService(false, "");
        assertThrows(IllegalArgumentException.class,
                () -> authService.verifyGoogleToken("invalid-token"));
    }
}
