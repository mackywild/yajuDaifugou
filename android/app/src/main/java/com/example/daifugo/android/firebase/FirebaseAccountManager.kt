package com.example.daifugo.android.firebase

import android.app.Activity
import android.app.Application
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.daifugo.android.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Play Gamesとは独立した、Web版と同じFirebase Authenticationユーザー。 */
data class FirebaseIdentity(val uid: String, val idToken: String, val email: String?)

/**
 * Androidアプリ登録情報はビルドプロパティから読み込む。
 * 管理者サービスアカウントの秘密鍵はアプリに絶対に含めない。
 */
class FirebaseAccountManager(private val application: Application) {
    val configured: Boolean
        get() = listOf(
            BuildConfig.FIREBASE_API_KEY,
            BuildConfig.FIREBASE_ANDROID_APP_ID,
            BuildConfig.FIREBASE_PROJECT_ID,
            BuildConfig.FIREBASE_WEB_CLIENT_ID,
        ).all(String::isNotBlank)

    val projectId: String get() = BuildConfig.FIREBASE_PROJECT_ID

    private fun auth(): FirebaseAuth {
        check(configured) { "Firebase Android設定がありません。アプリをFirebase設定付きでビルドしてください" }
        val app = FirebaseApp.getApps(application).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(
                application,
                FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_ANDROID_APP_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .build(),
            )
        return FirebaseAuth.getInstance(app)
    }

    /** 保存済みFirebaseログインを復元し、必要に応じて有効なIDトークンへ更新する。 */
    suspend fun restore(): FirebaseIdentity? {
        val user = auth().currentUser ?: return null
        val token = withContext(Dispatchers.IO) { Tasks.await(user.getIdToken(false)).token }
            ?: error("Firebase IDトークンを取得できません")
        return FirebaseIdentity(user.uid, token, user.email)
    }

    /** Credential ManagerでGoogle IDトークンを受け取り、Firebaseへサインインする。 */
    suspend fun signIn(activity: Activity): FirebaseIdentity {
        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.FIREBASE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        require(credential is CustomCredential
            && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google認証情報を取得できませんでした"
        }
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val result = withContext(Dispatchers.IO) {
            Tasks.await(auth().signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)))
        }
        val user = result.user ?: error("Firebaseログインに失敗しました")
        val firebaseToken = withContext(Dispatchers.IO) {
            Tasks.await(user.getIdToken(true)).token
        } ?: error("Firebase IDトークンを取得できません")
        return FirebaseIdentity(user.uid, firebaseToken, user.email)
    }

    /** サーバーログアウト成功後だけ呼び、端末のFirebase認証情報も破棄する。 */
    suspend fun signOut() {
        if (!configured) return
        auth().signOut()
        runCatching {
            CredentialManager.create(application).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
