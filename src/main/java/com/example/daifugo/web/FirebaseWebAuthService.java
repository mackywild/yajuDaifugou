package com.example.daifugo.web;

import java.io.IOException;
import java.util.Map;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Web版GoogleログインのIDトークンをFirebase Admin SDKで検証する。 */
@Service
public class FirebaseWebAuthService {
    private final boolean enabled;
    private final String projectId;

    public FirebaseWebAuthService(
            @Value("${daifugo.firebase.web-enabled:false}") boolean enabled,
            @Value("${daifugo.firebase.project-id:}") String projectId) {
        this.enabled = enabled;
        this.projectId = projectId;
    }

    /** 管理者認証情報はVPSのADCから読み込み、GitHubには配置しない。 */
    public synchronized String verifyGoogleToken(String idToken) {
        if (!enabled || idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Google認証が必要です");
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                if (projectId.isBlank()) {
                    throw new IllegalStateException("FIREBASE_PROJECT_IDが未設定です");
                }
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(options);
            }
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken, true);
            Object firebaseClaim = token.getClaims().get("firebase");
            if (!(firebaseClaim instanceof Map<?, ?> firebase)
                    || !"google.com".equals(firebase.get("sign_in_provider"))) {
                throw new IllegalArgumentException("Googleアカウントでログインしてください");
            }
            return token.getUid();
        } catch (FirebaseAuthException | IOException exception) {
            throw new IllegalArgumentException("Google認証を確認できませんでした", exception);
        }
    }
}
