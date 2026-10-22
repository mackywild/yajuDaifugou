package com.example.daifugo.web;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * スマホ・ブラウザ向けゲーム更新通知WebSocketの設定。
 */
@Configuration
@EnableWebSocket
public class GameWebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final String[] allowedOriginPatterns;

    public GameWebSocketConfig(
            GameWebSocketHandler gameWebSocketHandler,
            @Value("${daifugo.websocket.allowed-origins}") String allowedOrigins) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.allowedOriginPatterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        if (allowedOriginPatterns.length == 0
                || Arrays.stream(allowedOriginPatterns).anyMatch("*"::equals)) {
            throw new IllegalArgumentException(
                    "WebSocketの許可Originは明示的に設定してください（* は禁止）");
        }
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}
