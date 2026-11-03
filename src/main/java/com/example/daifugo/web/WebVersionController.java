package com.example.daifugo.web;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** デプロイされたWebアプリのビルド識別子を返す。 */
@RestController
public class WebVersionController {
    private final String buildSha;

    public WebVersionController(@Value("${daifugo.build-sha:unknown}") String buildSha) {
        this.buildSha = buildSha;
    }

    @GetMapping("/api/version")
    public Map<String, Object> version() {
        return Map.of("buildSha", buildSha, "schemaVersion", 1);
    }
}
