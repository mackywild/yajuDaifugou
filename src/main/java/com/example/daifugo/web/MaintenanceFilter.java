package com.example.daifugo.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** APIを画面側だけでなくサーバー側でも停止する。 */
@Component
public class MaintenanceFilter extends OncePerRequestFilter {
    private final MaintenanceService maintenanceService;

    public MaintenanceFilter(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!maintenanceService.blocks(request.getMethod(), request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Retry-After", "60");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"現在メンテナンス中です\",\"code\":\"MAINTENANCE\"}");
    }
}
