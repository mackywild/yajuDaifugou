package com.example.daifugo.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * LoginAttemptServiceの単体テスト。
 */
class LoginAttemptServiceTest {

    @Test
    void 失敗回数が上限に達すると一時ロックされる() {
        LoginAttemptService service = new LoginAttemptService(3, 60_000, 3_600_000);

        service.recordFailure("client");
        service.recordFailure("client");
        assertDoesNotThrow(() -> service.verifyAllowed("client"));

        service.recordFailure("client");
        assertThrows(LoginRateLimitException.class, () -> service.verifyAllowed("client"));
    }

    @Test
    void 認証成功で失敗履歴が解除される() {
        LoginAttemptService service = new LoginAttemptService(2, 60_000, 3_600_000);

        service.recordFailure("client");
        service.recordFailure("client");
        assertThrows(LoginRateLimitException.class, () -> service.verifyAllowed("client"));

        service.recordSuccess("client");
        assertDoesNotThrow(() -> service.verifyAllowed("client"));
    }
}
