package com.example.daifugo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.daifugo.room.GameRoomService;

/** Firebase切替時に無条件の公開ログインが残らないことを確認する。 */
class FirebaseWebAuthSessionTest {
    @Test
    void firebase有効時は公開セッションで認証されない() {
        AuthController controller = new AuthController(
                "legacy-secret", true,
                mock(FirebaseWebAuthService.class),
                mock(LoginAttemptService.class),
                mock(GameRoomService.class),
                mock(GameWebSocketHandler.class));
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertFalse((Boolean) controller.publicSession(request).get("authenticated"));
        assertNull(request.getSession(false));
    }

    @Test
    void firebase無効時は既存Webログインを維持() {
        AuthController controller = new AuthController(
                "legacy-secret", false,
                mock(FirebaseWebAuthService.class),
                mock(LoginAttemptService.class),
                mock(GameRoomService.class),
                mock(GameWebSocketHandler.class));
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertEquals(true, controller.publicSession(request).get("authenticated"));
        assertEquals(true, request.getSession(false)
                .getAttribute(AuthController.AUTHENTICATED_SESSION_KEY));
    }
}
