package com.example.daifugo.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Webクライアント向け公開Firebase設定（秘密鍵は含めない）。 */
@RestController
public class FirebaseWebConfigController {
    private final boolean enabled;
    private final String apiKey;
    private final String authDomain;
    private final String projectId;
    private final String appId;

    public FirebaseWebConfigController(
            @Value("${daifugo.firebase.web-enabled:false}") boolean enabled,
            @Value("${daifugo.firebase.api-key:}") String apiKey,
            @Value("${daifugo.firebase.auth-domain:}") String authDomain,
            @Value("${daifugo.firebase.project-id:}") String projectId,
            @Value("${daifugo.firebase.app-id:}") String appId) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.authDomain = authDomain;
        this.projectId = projectId;
        this.appId = appId;
    }

    @GetMapping("/api/config/firebase")
    public Map<String, Object> getConfig() {
        if (!enabled) {
            return Map.of("enabled", false);
        }
        return Map.of("enabled", true, "apiKey", apiKey,
                "authDomain", authDomain, "projectId", projectId, "appId", appId);
    }
}
