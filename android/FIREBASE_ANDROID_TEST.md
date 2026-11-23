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
# 野獣大富豪 Android版 Firebase 検証手順

## 目的と制約

AndroidとWebで同じGoogleアカウント（Firebase UID）を利用し、プロフィール・戦績・コレクション・ミッション・プレゼント・BGMをSpring Boot経由でFirestoreに保存する。**AndroidアプリからFirestoreへ直接書き込まない**。マルチプレイの既存REST/WebSocketとオフラインCPU戦は維持する。

- 本PRは検証用。**Firebase設定がないAPKは引き続き従来のローカルモードで起動できる**。
- CPU戦は端末内で判定されるため、クラウドへの経験値付与は行わない。CPU戦の結果は端末保存（クラウド未同期）。サーバーで確定したオンライン対戦のみFirestoreへ保存する。
- Web側のFirebaseを無効にしている環境では、AndroidのGoogleログインからクラウドへ接続できない。**本番フラグを変更せず、まず非公開Stagingで実施**。
- 旧Play GamesログインはFirebase Authenticationではない。Play Games IDとFirebase UIDを混ぜない。

## 1. Firebase Consoleで同じプロジェクトを指定

1. Web版で使うFirebaseプロジェクトを開く。AuthenticationでGoogleサインインを有効化する。
2. **Androidアプリを追加**。パッケージ名は **`jp.YajuDaifugou.app`**（大文字小文字も一致させる）。
3. Debug署名のSHA-1およびSHA-256をAndroidアプリへ登録する。例：`cd android && ./gradlew signingReport`。Google Playの配布版はPlay App Signingの署名証明書も別途登録する。
4. 対応するOAuth 2.0クライアント設定から **WebクライアントID** を取得する。AndroidクライアントIDとは異なる。`GetGoogleIdOption.setServerClientId()` に指定する。
5. AndroidアプリのFirebase構成からAndroid用 **API key** と **App ID**（例：`1:...:android:...`）、Firebase **Project ID** を取得する。Android App IDとWeb App IDは異なる。
6. **Firebase Admin用のサービスアカウント秘密鍵はAPK・GitHubへ含めない**。サーバー側のADCにのみ配置する。

## 2. GitHub Actions Secrets

GitHub → リポジトリ Settings → Secrets and variables → Actions に以下を登録する。Firebaseクライアント構成値は厳密な秘密鍵ではないが、公開リポジトリに固定しないためActionsの設定から注入する。

| Secret | 値 |
| --- | --- |
| `FIREBASE_ANDROID_API_KEY` | FirebaseプロジェクトのAndroid API key |
| `FIREBASE_ANDROID_APP_ID` | `1:...:android:...` のAndroidアプリID |
| `FIREBASE_PROJECT_ID` | Webと同じFirebaseプロジェクトID |
| `FIREBASE_WEB_CLIENT_ID` | Google OAuthのWebクライアントID（`...apps.googleusercontent.com`） |
| `YAJU_ANDROID_SERVER_URL` | （推奨）デバッグAPKで使うアクセス可能な**HTTPSのStaging URL** |
| `YAJU_ANDROID_TEST_KEYSTORE_B64` | **Firebase用に固定したテスト署名keystore**のbase64。ストア用鍵とは分離 |
| `YAJU_TEST_STORE_PASSWORD` | テストkeystoreパスワード |
| `YAJU_TEST_KEY_ALIAS` | テスト鍵のalias |
| `YAJU_TEST_KEY_PASSWORD` | テスト秘密鍵のパスワード |

Androidプロジェクトでのローカルビルドは、同名の環境変数または`~/.gradle/gradle.properties` のプロパティで注入できる。ビルドは`cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug`。

### GitHub配布APKでGoogleログインを試すための固定署名（重要）

GitHub Actionsの既定のデバッグ署名はランナーごとに変わるため、Firebaseに登録したSHAと一致しない可能性がある。**Firebase構成のあるCIでは固定テスト署名を必須**とする。公開用アップロード鍵ではなく**専用の検証鍵**を作り、リポジトリにはコミットしない。

1. Android StudioのJDK等が使える環境でテスト鍵を1回だけ生成する。例：
   ```bash
   keytool -genkeypair -v -keystore yaju-firebase-test.jks -alias yaju-firebase-test -keyalg RSA -keysize 2048 -validity 3650
   ```
2. `keytool -list -v -keystore yaju-firebase-test.jks -alias yaju-firebase-test` でSHA-1/SHA-256を表示し、Firebase Androidアプリに登録する。
3. Windows PowerShellなら `[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\\path\\yaju-firebase-test.jks"))` をSecretsに登録する。
4. `YAJU_ANDROID_TEST_KEYSTORE_B64` と3つのパスワード／alias Secretを登録して再ビルドする。**Firebase公開設定だけが入力され、固定署名がないCIは明示的に失敗する**。
5. Playストア経由の場合はGoogle Play App Signingの別証明書もFirebaseに登録する。異なる署名のAPKは既存インストールにそのまま上書きできないため、アンインストール前に端末データのバックアップを検討する。

ローカルビルドは通常の`~/.android/debug.keystore`が利用されるため、そのSHAをFirebaseに登録すれば固定テスト鍵なしでも検証できる。

**注**：fork由来のPRにはSecretsが渡らない場合がある。その場合はFirebase未設定のAPKとしてビルドされる。Firebase設定の有無はアプリ内の「FIREBASE ACCOUNT」に表示される。

## 3. 検証サーバー側の前提

`deploy/FIREBASE_WEB_MIGRATION.md` に従い、非公開Staging環境でFirestoreとAdmin SDKのADCを準備し、以下を有効にする。

- `FIREBASE_WEB_ENABLED=true`
- `FIREBASE_PROJECT_ID` がAndroid / Webと同一
- `GOOGLE_APPLICATION_CREDENTIALS` などのサーバー専用認証情報
- HTTPS・`POST /api/auth/firebase/session`・`GET /api/users/me/progress` が有効
- Firestore Security Rulesはクライアントからの読み書きを拒否（サーバーAdmin SDK経由のみ）

**debug APK**のみ`YAJU_ANDROID_SERVER_URL`でHTTPS接続先を上書きできる。未指定なら`https://yaju-daifugou.com`に接続するため、**本番側のFirebaseを有効にせずに試すにはStagingのURLを必ず設定すること**。公開サイトのlocalhost:8081は端末から直接アクセスできないため、VPN等で端末が到達できるHTTPSゲートウェイを用意する。**release APKは公開URL固定**。PRをマージしただけではVPS側のFirebaseは有効にならない。

## 4. Android 実機の受入試験

| No. | テスト | 合格条件 |
| --- | --- | --- |
| 1 | Googleログイン → アプリ終了 → 再起動 | Firebase UIDを復元し、同じクラウド進行データを読み込む |
| 2 | 新規UIDで「端末データを引き継ぐ」 | 初回のみFirestoreへ移行、ローカル元データも保持 |
| 3 | 新規UIDで「新規アカウントとして開始」 | 端末の古い戦績をクラウドへ混入しない |
| 4 | Web版でプロフィール名変更後、Androidでクラウド再読込 | WebとAndroidで名前・経験値・装備・戦績が一致 |
| 5 | Androidのミッション受取／プレゼント受取／装備／BGM切替 | Webに反映され、同じ報酬の二重受取がない |
| 6 | Android↔Webでマルチプレイを完走 | サーバー確定順位に対応する戦績・EXPが1回だけ増える |
| 7 | 結果画面でクラウド保存が失敗した後「戦績を再送」 | 復旧後に保存され、再送しても二重加算なし |
| 8 | GoogleアカウントAからログアウト → Bでログイン | Aのクラウドプロフィール・戦績がBへ表示されない |
| 9 | ログイン状態で端末のPlay Games認証が完了 | Firebaseアカウントのデータが上書きされない |
| 10 | ネットワーク遮断中のプロフ更新・報酬操作 | クラウド状態を成功表示せず、復旧後に再操作できる |
| 11 | Firebase未設定のAPK | 既存のCPU戦・マルチプレイが従来どおり機能する |
| 12 | Firebaseログイン状態でオフラインCPU戦を完走 | CPU戦は端末に記録、クラウドEXP・戦績は不正加算されない |
| 13 | Web↔Androidの既存ルール（Joker階段・7渡し・野獣チャレンジ） | ゲーム進行・手札の秘匿・演出のデグレなし |

**この一覧は受入条件であり、実機試験済みの報告ではない**。必ず実機・Staging・Firebase Consoleで確認すること。

## 5. 既知のリリース停止条件

- Firebase本番移行資料記載の**Firestore障害時の確定戦績永続アウトボックスが未実装**。ルームTTL後は再送できないため、本番Firebase有効化は不可。
- Firebaseの**実機Google認証/Firestore接続**は本PRコードだけでは検証できず、Google OAuth設定と有効なサーバー環境が必要。
- オフラインCPU結果をサーバー側で検証できる仕組みは未実装。クラウドへ無条件にEXPを付与しない。
- Android版の本番公開切替・WebのFirebase必須化には、旧クライアントを含む回帰試験と段階配信が必要。
