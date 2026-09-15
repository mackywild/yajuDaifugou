package com.example.daifugo.web;

/**
 * ログイン試行回数が上限に達した場合に送出する例外。
 */
public class LoginRateLimitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long retryAfterSeconds;

    public LoginRateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
