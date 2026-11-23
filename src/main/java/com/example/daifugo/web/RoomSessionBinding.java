package com.example.daifugo.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import com.example.daifugo.room.GameRoomService;

/** HTTPセッションに紐づくプレイヤーの本人確認と部屋の参加状態を一元管理する。 */
@Component
public class RoomSessionBinding {
    private final GameRoomService roomService;

    public RoomSessionBinding(GameRoomService roomService) {
        this.roomService = roomService;
    }

    public void ensureNotBoundToRoom(HttpSession session) {
        Object boundPlayerId = session.getAttribute(GameApiController.SESSION_PLAYER_ID);
        Object boundRoomId = session.getAttribute(GameApiController.SESSION_ROOM_ID);

        if (boundPlayerId == null) {
            return;
        }

        if (boundRoomId instanceof String roomId) {
            try {
                roomService.getRoom(roomId);
                throw new IllegalStateException("既に別の部屋へ参加しています。先に退出してください");
            } catch (IllegalArgumentException expiredRoom) {
                // TTLなどで部屋が既に消えている場合は古いセッション紐付けだけ破棄する。
                clearRoomBinding(session);
                return;
            }
        }

        clearRoomBinding(session);
    }

    public void bindSession(HttpSession session, String roomId, String playerId) {
        session.setAttribute(GameApiController.SESSION_ROOM_ID, roomId);
        session.setAttribute(GameApiController.SESSION_PLAYER_ID, playerId);
    }

    public String requireBoundPlayer(HttpSession session, String roomId) {
        Object sessionRoomId = session.getAttribute(GameApiController.SESSION_ROOM_ID);
        Object sessionPlayerId = session.getAttribute(GameApiController.SESSION_PLAYER_ID);

        if (!(sessionRoomId instanceof String boundRoomId)
                || !(sessionPlayerId instanceof String playerId)
                || !boundRoomId.equals(roomId)) {
            throw new IllegalStateException("このブラウザは指定された部屋に参加していません");
        }

        return playerId;
    }

    public void clearRoomBinding(HttpSession session) {
        session.removeAttribute(GameApiController.SESSION_ROOM_ID);
        session.removeAttribute(GameApiController.SESSION_PLAYER_ID);
    }

}
