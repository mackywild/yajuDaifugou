package com.example.daifugo.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GameWebSocketConfigTest {

    @Test
    void rejectsWildcardOrigin() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameWebSocketConfig(null, "*"));
        assertThrows(IllegalArgumentException.class,
                () -> new GameWebSocketConfig(null, ""));
    }

    @Test
    void acceptsExplicitProductionAndDevelopmentOrigins() {
        assertDoesNotThrow(() -> new GameWebSocketConfig(null,
                "https://yajudaifugou.com, http://localhost:8080"));
    }
}
