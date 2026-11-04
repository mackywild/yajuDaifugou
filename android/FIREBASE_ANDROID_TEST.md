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

Androidプロジェクトでのローカルビルドは、同名の環境変数または`~/.gradle/gradle.properties` のプロパティで注入できる。ビルドは`cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug`。

**注**：fork由来のPRにはSecretsが渡らない場合がある。その場合はFirebase未設定のAPKとしてビルドされる。Firebase設定の有無はアプリ内の「FIREBASE ACCOUNT」に表示される。

## 3. 検証サーバー側の前提

`deploy/FIREBASE_WEB_MIGRATION.md` に従い、非公開Staging環境でFirestoreとAdmin SDKのADCを準備し、以下を有効にする。

- `FIREBASE_WEB_ENABLED=true`
- `FIREBASE_PROJECT_ID` がAndroid / Webと同一
- `GOOGLE_APPLICATION_CREDENTIALS` などのサーバー専用認証情報
- HTTPS・`POST /api/auth/firebase/session`・`GET /api/users/me/progress` が有効
- Firestore Security Rulesはクライアントからの読み書きを拒否（サーバーAdmin SDK経由のみ）

現状、Androidの公開接続先は`https://yaju-daifugou.com`固定。**Firebase本番環境を使った試験はStaging側の接続先を別途ビルド設定可能にするまで実施しないこと**。PRをマージしただけではVPS側のFirebaseは有効にならない。

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
