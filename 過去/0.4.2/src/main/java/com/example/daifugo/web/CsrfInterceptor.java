package com.example.daifugo.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * セッションCookieを利用する更新APIに対するCSRF攻撃を防止するInterceptor。
 */
@Component
public class CsrfInterceptor implements HandlerInterceptor {

    /** クライアントが送信するCSRFヘッダー名 */
    public static final String CSRF_HEADER = "X-CSRF-Token";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        if (isSafeMethod(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Object expectedValue = session == null
                ? null
                : session.getAttribute(AuthController.CSRF_SESSION_KEY);
        String actual = request.getHeader(CSRF_HEADER);

        if (expectedValue instanceof String expected
                && actual != null
                && constantTimeEquals(expected, actual)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"CSRFトークンが不正です\"}");
        return false;
    }

    private boolean isSafeMethod(String method) {
        return "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
