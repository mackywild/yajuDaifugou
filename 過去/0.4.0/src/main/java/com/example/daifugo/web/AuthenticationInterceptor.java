package com.example.daifugo.web;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * ゲームAPIへの未認証アクセスを拒否するInterceptor。
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        Object authenticated = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(AuthController.AUTHENTICATED_SESSION_KEY);

        if (Boolean.TRUE.equals(authenticated)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"認証が必要です\"}");
        return false;
    }
}
