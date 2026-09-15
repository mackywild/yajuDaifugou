package com.example.daifugo.web;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.daifugo.room.GameRoom;
import com.example.daifugo.room.GameRoomService;

/**
 * ゲーム状態変更通知専用のWebSocketハンドラー。
 *
 * クライアントからゲーム操作は受け付けず、REST APIで状態が変化したことだけを
 * 同じ部屋の参加者へ通知する。各クライアントは通知を受信後、本人のHTTP
 * セッションを使って /state を再取得するため、他人の手札が混ざることはない。
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    /** 部屋ID -> 接続中WebSocketセッション */
    private final Map<String, Set<WebSocketSession>> sessionsByRoom =
            new ConcurrentHashMap<>();

    /** WebSocketセッションID -> 部屋ID */
    private final Map<String, String> roomBySessionId =
            new ConcurrentHashMap<>();

    private final GameRoomService roomService;

    public GameWebSocketHandler(GameRoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * HTTPセッションからコピーされた部屋・プレイヤー情報を検証して接続を登録する。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = queryParameter(session.getUri(), "roomId");
        Object sessionRoomId = session.getAttributes().get(GameApiController.SESSION_ROOM_ID);
        Object sessionPlayerId = session.getAttributes().get(GameApiController.SESSION_PLAYER_ID);
        Object authenticated = session.getAttributes().get(AuthController.AUTHENTICATED_SESSION_KEY);

        if (!Boolean.TRUE.equals(authenticated)
                || !(sessionRoomId instanceof String boundRoomId)
                || !(sessionPlayerId instanceof String playerId)
                || roomId == null
                || !roomId.equals(boundRoomId)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("認証または部屋情報が不正です"));
            return;
        }

        GameRoom room;
        try {
            room = roomService.getRoom(roomId);
        } catch (IllegalArgumentException e) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("部屋が存在しません"));
            return;
        }

        synchronized (room) {
            if (!room.containsPlayer(playerId)) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("部屋に参加していません"));
                return;
            }
        }

        sessionsByRoom
                .computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
        roomBySessionId.put(session.getId(), roomId);

        session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\"}"));
    }

    /**
     * このWebSocketは通知専用のため、クライアント送信メッセージは処理しない。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // intentionally ignored
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        unregister(session);
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException ignored) {
            // close failure does not affect the game itself
        }
    }

    /**
     * 指定部屋の接続端末へ状態更新通知を送る。
     *
     * @param roomId 更新された部屋ID
     */
    public void broadcastStateChanged(String roomId) {
        Set<WebSocketSession> sessions = sessionsByRoom.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage message = new TextMessage("{\"type\":\"STATE_CHANGED\"}");
        for (WebSocketSession session : Set.copyOf(sessions)) {
            if (!session.isOpen()) {
                unregister(session);
                continue;
            }
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                }
            } catch (IOException e) {
                unregister(session);
            }
        }
    }

    private void unregister(WebSocketSession session) {
        String roomId = roomBySessionId.remove(session.getId());
        if (roomId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByRoom.get(roomId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByRoom.remove(roomId, sessions);
        }
    }

    private String queryParameter(URI uri, String key) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String part : uri.getQuery().split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && key.equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }
}
