package com.example.daifugo.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

import com.example.daifugo.room.GameRoom;
import com.example.daifugo.room.GameRoomService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webアプリへの共通パスワード認証を担当するController。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** 認証済みセッションを表す属性名 */
    public static final String AUTHENTICATED_SESSION_KEY = "DAIFUGO_AUTHENTICATED";

    /** CSRFトークンを保存するセッション属性名 */
    public static final String CSRF_SESSION_KEY = "DAIFUGO_CSRF_TOKEN";

    /** 環境変数から設定される共通パスワード */
    private final String accessPassword;

    /** ログイン試行回数制限 */
    private final LoginAttemptService loginAttemptService;

    /** 部屋管理サービス */
    private final GameRoomService roomService;

    /** 部屋状態変更通知 */
    private final GameWebSocketHandler webSocketHandler;

    /**
     * 認証Controllerを生成する。
     * パスワード未設定の状態で公開されることを防ぐため、空文字は許可しない。
     *
     * @param accessPassword 共通パスワード
     */
    public AuthController(
            @Value("${daifugo.access-password}") String accessPassword,
            LoginAttemptService loginAttemptService,
            GameRoomService roomService,
            GameWebSocketHandler webSocketHandler) {
        if (accessPassword == null || accessPassword.isBlank()) {
            throw new IllegalStateException(
                    "DAIFUGO_PASSWORDを設定してください。共通パスワードなしでは起動できません。");
        }
        this.accessPassword = accessPassword;
        this.loginAttemptService = loginAttemptService;
        this.roomService = roomService;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 共通パスワードを検証し、認証済みセッションを作成する。
     * 認証成功時にはセッションIDを変更してSession Fixationを防止する。
     *
     * @param requestBody ログイン要求
     * @param request HTTPリクエスト
     * @return 認証結果
     */
    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody LoginRequest requestBody,
            HttpServletRequest request) {

        String attemptKey = request.getRemoteAddr();
        loginAttemptService.verifyAllowed(attemptKey);

        if (requestBody.password() == null
                || !constantTimeEquals(accessPassword, requestBody.password())) {
            loginAttemptService.recordFailure(attemptKey);
            loginAttemptService.verifyAllowed(attemptKey);
            throw new IllegalArgumentException("パスワードが違います");
        }

        loginAttemptService.recordSuccess(attemptKey);

        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(AUTHENTICATED_SESSION_KEY, Boolean.TRUE);
        String csrfToken = UUID.randomUUID().toString();
        session.setAttribute(CSRF_SESSION_KEY, csrfToken);

        return Map.of(
                "authenticated", true,
                "csrfToken", csrfToken);
    }

    /**
     * 現在のHTTPセッションを破棄する。
     *
     * @param request HTTPリクエスト
     * @return ログアウト結果
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            releaseRoomIfPossible(session);
            session.invalidate();
        }
        return Map.of("authenticated", false);
    }


    /**
     * ログアウト前に部屋との紐付けを安全に解除する。
     * 対戦中は幽霊プレイヤーを残さないためログアウト自体を拒否する。
     */
    private void releaseRoomIfPossible(HttpSession session) {
        Object roomIdValue = session.getAttribute(GameApiController.SESSION_ROOM_ID);
        Object playerIdValue = session.getAttribute(GameApiController.SESSION_PLAYER_ID);

        if (!(roomIdValue instanceof String roomId)
                || !(playerIdValue instanceof String playerId)) {
            return;
        }

        try {
            GameRoom room = roomService.getRoom(roomId);
            synchronized (room) {
                if (room.isStarted() && !room.isFinished()) {
                    throw new IllegalStateException(
                            "対戦中はログアウトできません。ゲーム終了後にログアウトしてください");
                }
                roomService.leaveRoom(roomId, playerId);
            }
            webSocketHandler.broadcastStateChanged(roomId);
        } catch (IllegalArgumentException expiredRoom) {
            // TTLなどですでに部屋が消えている場合はセッションだけ破棄すればよい。
        }

        session.removeAttribute(GameApiController.SESSION_ROOM_ID);
        session.removeAttribute(GameApiController.SESSION_PLAYER_ID);
    }

    /**
     * 文字列を一定時間比較する。
     */
    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    /** ログイン要求DTO */
    public record LoginRequest(String password) {
    }
}
