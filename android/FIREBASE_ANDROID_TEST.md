# 野獣大富豪 Android版 Firebase連携・検証手順

## 目的

Android / Web は **同一Firebaseプロジェクト** のGoogleログイン（同一UID）と
Spring Boot `/api/auth/firebase/session` を使用する。
Firestoreの読み書きはAndroidから直接行わず、認証済みサーバーの `/api/users/me/**` 経由で行う。
Google Play GamesのplayerIdとFirebase UIDは**別の認証**であり、同一視しない。

## 現在のスコープ

- Google Credential Manager → Firebase Authentication → サーバーIDトークン検証
- Firebaseログイン復元 / サーバーセッション再確立 / ログアウト
- クラウドプロフィール、戦績、デイリーミッション、プレゼント、装備、BGMの読み書き
- Googleアカウントに既存データがある場合、必ずクラウド正本を優先
- 初回のみ「端末の旧データを移行 / 新規クラウド作成」を明示的に選択
- サーバーで確定したオンライン対戦結果の保存、失敗時リトライ
- サーバー側Firebase無効時は従来の公開セッションへフォールバック
- ローカルCPUゲームは継続してオフライン可能。ただしその戦績は**クラウド報酬に加算しない**（サーバーに結果の正当性を検証する手段がないため）

**注意:** Firebase認証が実際に成功するには、以下のFirebaseプロジェクト設定とサーバーのADC/Firestore設定が必要。
何も設定せずにビルドしたAPKは従来動作するが、Firebaseログインはできない。
現時点ではAndroid実機でのGoogleサインイン・Firestore実接続の合格を宣言しない。

## Firebase Console / Google Cloud 設定

1. Web版で使用中と**同じFirebaseプロジェクト**を使う。
2. Firebase AuthenticationのGoogleプロバイダを有効にする。
3. FirebaseコンソールへAndroidアプリを登録する。
   - Androidパッケージ名: `jp.YajuDaifugou.app`（**大文字小文字を含めて一致**）
   - Debug / Release / Google Play App Signingごとに該当する証明書のSHA-1とSHA-256を登録。
4. Androidアプリの `mobilesdk_app_id`、Web API Key、プロジェクトIDを確認する。
   **Web版のApp ID（`...:web:...`）ではなくAndroid版のApp ID（`...:android:...`）を指定する。**
5. 認証用の**Web OAuthクライアントID**（`*.apps.googleusercontent.com`）を確認する。
   Credential Managerの`setServerClientId`へ設定する値。Android OAuthクライアントIDとは異なる。
6. FirestoreはWeb版と同じものを使用。AndroidアプリへAdmin SDKの秘密鍵を配布しない。
7. VPS上でFirebase Web側がすでに正常に動くことを確認する。
   `GET https://yaju-daifugou.com/api/config/firebase` の `enabled=true` と `projectId` の一致が必須。
   `FIREBASE_WEB_ENABLED=false` のままでは旧セッション方式で動く。

## Androidビルド

設定値は公開Firebaseクライアント設定（管理者秘密鍵ではない）。
ローカルでの例:

```bash
cd android
./gradlew :app:testDebugUnitTest :app:assembleDebug \
  -PYAJU_FIREBASE_API_KEY="your-firebase-web-api-key" \
  -PYAJU_FIREBASE_ANDROID_APP_ID="1:1234567890:android:abcdef" \
  -PYAJU_FIREBASE_PROJECT_ID="your-project-id" \
  -PYAJU_FIREBASE_WEB_CLIENT_ID="your-web-oauth-client-id.apps.googleusercontent.com"
```

生成APK: `android/app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions `Android Build` / `Android Store Test AAB` は、
次のGitHub Actions **Repository Variables** をGradleへ渡す（未設定なら空文字）。

- `YAJU_FIREBASE_API_KEY`
- `YAJU_FIREBASE_ANDROID_APP_ID`
- `YAJU_FIREBASE_PROJECT_ID`
- `YAJU_FIREBASE_WEB_CLIENT_ID`

**署名に注意:** GitHub Actionsの都度生成されるデバッグ署名鍵はSHAが変わることがある。
実機Googleログイン検証はFirebase登録済みSHAの署名でビルドする。
配布/ストア版はPlay App SigningのSHAもFirebaseへ登録する。
`google-services.json` のコミットは不要（FirebaseOptionsをコードで初期化）。
ただしGoogle OAuthのAndroidパッケージ名・SHA登録は省略できない。

## AndroidとWebを使った受入テスト

| No. | 操作 | 期待結果 |
|---|---|---|
| 1 | AndroidでGoogle Aアカウントへログイン | Firebase UIDを使ったサーバーセッション作成に成功 |
| 2 | 初回ログインで「新規作成」 | 新規データがFirestoreに生成。端末ローカル記録は不変 |
| 3 | Androidでプロフィール変更 → Web版でGoogle Aログイン | 名前と戦績を共有できる |
| 4 | Web版で装備・ミッション報酬など操作 → Androidで「同期」 | 所持品・装備・ミッション・プレゼントが更新される |
| 5 | Android AとWeb Aで同じマルチルーム参加・ゲーム終了 | 結果確定後にサーバーから戦績をクラウドへ一度だけ保存 |
| 6 | 同一結果の保存を再試行 | 同一UID + roomIdで経験値が二重付与されない |
| 7 | クラウド通信障害を再現 | 結果画面にエラーと再試行が表示される |
| 8 | アプリ終了→再起動 | Firebaseサインイン状態を復元し、クラウド進行データを読み込む |
| 9 | Google Aログアウト→Google Bログイン | Aのプロフィール/所持品/ルームをBへ引き継がない |
| 10 | Firebase無効サーバーに接続 | 旧公開セッションでマルチプレイできる |
| 11 | ローカルCPU戦をオフラインで遊ぶ | 既存ゲームが続行でき、Firestoreに未検証戦績が加算されない |
| 12 | BGMオフ → Web版で確認 → Android再ログイン | 設定が同期する |
| 13 | Play Games連携とFirebaseログインを別々に操作 | Play GamesのplayerIdをFirebase UIDとして扱わない |
| 14 | 認証なしで `/api/users/me/progress` をGET | 401/403または認証拒否、他人のデータは返さない |

### 既知のリリースブロッカー

- Web版と共通のFirestore「確定戦績の永続アウトボックス」はまだ未実装。
  サーバーのルームTTLが切れた後は再送できない。リリース前に解消すること。
- Firebase Admin SDKを使うVPSのADCとFirestore本番接続は別途検証が必要。
- 正式リリースでは旧`/api/auth/login`互換経路の認可境界を見直すこと（Web移行側の既知課題）。
- 未マージの運用/メンテナンスPRにある新APIはこの変更では未採用。
