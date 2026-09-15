package com.example.daifugo.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * APIで発生したゲームルール・入力エラーをJSONへ変換する。
 */
@RestControllerAdvice
public class ApiExceptionHandler {


    /**
     * ログイン試行回数超過をHTTP 429として返す。
     */
    @ExceptionHandler(LoginRateLimitException.class)
    public ResponseEntity<Map<String, String>> handleLoginRateLimit(LoginRateLimitException exception) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", Long.toString(exception.getRetryAfterSeconds()));

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }
}
