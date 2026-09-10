package com.example.daifugo.web;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 共通パスワードへの総当たり攻撃を抑止するため、
 * 接続元ごとのログイン失敗回数とロック時間を管理するサービス。
 */
@Service
public class LoginAttemptService {

    /** 接続元ごとのログイン試行状態 */
    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    /** ロックまでに許可する連続失敗回数 */
    private final int maxFailures;

    /** ロック継続時間（ミリ秒） */
    private final long lockDurationMs;

    /** 試行情報を忘れるまでの時間（ミリ秒） */
    private final long retentionMs;

    public LoginAttemptService(
            @Value("${daifugo.login-max-failures:5}") int maxFailures,
            @Value("${daifugo.login-lock-ms:60000}") long lockDurationMs,
            @Value("${daifugo.login-attempt-retention-ms:3600000}") long retentionMs) {

        if (maxFailures < 1 || lockDurationMs < 1 || retentionMs < 1) {
            throw new IllegalArgumentException("ログイン試行制限の設定値が不正です");
        }

        this.maxFailures = maxFailures;
        this.lockDurationMs = lockDurationMs;
        this.retentionMs = retentionMs;
    }

    /**
     * 現在ログイン試行が許可されているか確認する。
     *
     * @param key 接続元識別キー
     */
    public void verifyAllowed(String key) {
        long now = System.currentTimeMillis();
        AttemptState state = attempts.get(normalizeKey(key));

        if (state == null) {
            return;
        }

        if (state.lockedUntil() > now) {
            long retryAfterSeconds = Math.max(1L, (state.lockedUntil() - now + 999L) / 1000L);
            throw new LoginRateLimitException(
                    "ログイン試行回数が上限に達しました。" + retryAfterSeconds + "秒後に再試行してください",
                    retryAfterSeconds);
        }

        if (now - state.lastAttemptAt() > retentionMs) {
            attempts.remove(normalizeKey(key), state);
        }
    }

    /**
     * ログイン失敗を記録する。
     *
     * @param key 接続元識別キー
     */
    public void recordFailure(String key) {
        String normalizedKey = normalizeKey(key);
        long now = System.currentTimeMillis();

        attempts.compute(normalizedKey, (ignored, current) -> {
            int failures = current == null || now - current.lastAttemptAt() > retentionMs
                    ? 1
                    : current.failures() + 1;
            long lockedUntil = failures >= maxFailures ? now + lockDurationMs : 0L;
            return new AttemptState(failures, lockedUntil, now);
        });
    }

    /**
     * 認証成功時に失敗履歴を削除する。
     *
     * @param key 接続元識別キー
     */
    public void recordSuccess(String key) {
        attempts.remove(normalizeKey(key));
    }


    /**
     * 古いログイン試行情報を定期的に削除し、接続元キーが増え続けることを防ぐ。
     */
    @Scheduled(fixedDelayString = "${daifugo.login-attempt-cleanup-ms:600000}")
    public void cleanupExpiredAttempts() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry ->
                now - entry.getValue().lastAttemptAt() > retentionMs
                && entry.getValue().lockedUntil() <= now);
    }

    private String normalizeKey(String key) {
        return key == null || key.isBlank() ? "unknown" : key;
    }

    private record AttemptState(int failures, long lockedUntil, long lastAttemptAt) {
    }
}
