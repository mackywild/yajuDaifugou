package com.example.daifugo.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC共通設定。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;
    private final CsrfInterceptor csrfInterceptor;

    public WebMvcConfig(
            AuthenticationInterceptor authenticationInterceptor,
            CsrfInterceptor csrfInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.csrfInterceptor = csrfInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /* ゲームAPIは共通パスワード認証済みセッションのみ許可する。 */
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/api/rooms/**");

        /* ログイン以外の更新APIにはセッション固有CSRFトークンを要求する。 */
        registry.addInterceptor(csrfInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }
}
