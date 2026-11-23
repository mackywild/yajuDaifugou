package com.example.daifugo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import com.example.daifugo.room.GameRoomService;

/** APIセッションを使った本人認証と部屋退出時の紐付け解除を検証する。 */
class RoomSessionBindingTest {
    private final GameRoomService roomService = mock(GameRoomService.class);
    private final RoomSessionBinding binding = new RoomSessionBinding(roomService);
    private final HttpSession session = mock(HttpSession.class);

    @Test
    void bindsAndClearsBothSessionValues() {
        binding.bindSession(session, "room-1", "player-1");
        verify(session).setAttribute(GameApiController.SESSION_ROOM_ID, "room-1");
        verify(session).setAttribute(GameApiController.SESSION_PLAYER_ID, "player-1");
        binding.clearRoomBinding(session);
        verify(session).removeAttribute(GameApiController.SESSION_ROOM_ID);
        verify(session).removeAttribute(GameApiController.SESSION_PLAYER_ID);
    }

    @Test
    void refusesRoomMismatchInsteadOfTrustingClient() {
        when(session.getAttribute(GameApiController.SESSION_ROOM_ID)).thenReturn("room-A");
        when(session.getAttribute(GameApiController.SESSION_PLAYER_ID)).thenReturn("player-1");
        var error = assertThrows(IllegalStateException.class,
                () -> binding.requireBoundPlayer(session, "room-B"));
        assertEquals("このブラウザは指定された部屋に参加していません", error.getMessage());
    }

    @Test
    void removesOrphanedBindingWhenRoomExpired() {
        when(session.getAttribute(GameApiController.SESSION_ROOM_ID)).thenReturn("expired");
        when(session.getAttribute(GameApiController.SESSION_PLAYER_ID)).thenReturn("player-1");
        when(roomService.getRoom("expired")).thenThrow(new IllegalArgumentException("部屋が存在しません"));
        assertDoesNotThrow(() -> binding.ensureNotBoundToRoom(session));
        verify(session).removeAttribute(GameApiController.SESSION_ROOM_ID);
        verify(session).removeAttribute(GameApiController.SESSION_PLAYER_ID);
    }
}
