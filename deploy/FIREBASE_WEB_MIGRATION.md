# 野獣大富豪 Web版 Firebase移行 — 導入・受入手順

## 対象範囲
- **Webブラウザ版のみ**。Android/iOSのコードは変更しない。
- Web版はFirebase Authentication（Google）でサインインし、Spring BootがIDトークンを検証する。
- 進行データの正本はCloud Firestore。対戦中のルーム・手札は従来どおりSpring Bootメモリ管理。
- デプロイ時には **FIREBASE_WEB_ENABLED=false（既定）** とし、Firebase設定と受入試験完了後に明示的に有効化する。
- 旧ネイティブクライアント用の共通パスワードAPIは残す。Web版のGoogleログイン必須化と、旧ネイティブ版の認証方式は別々の段階で移行する。

## Firebaseコンソールで準備
1. Firebaseプロジェクトを作成し、Webアプリを登録する。
2. Authentication → Sign-in method で **Google** を有効化する。
3. Authentication → Settings → Authorized domains に `yaju-daifugou.com` を登録する。
4. Firestore（Standard）を作成する。リージョンは近接・コスト・データ管理方針に応じて選ぶ。
5. クライアントからFirestoreを直接操作しないため、セキュリティルールを以下にする。

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} { allow read, write: if false; }
  }
}
```

6. Firebase Admin SDK用サービスアカウントを用意する。キーは **GitHubに置かない**。可能なら短期クレデンシャルを採用し、必要最小限のIAM権限だけを付与する。

## ConoHa VPSの環境設定
`/etc/daifugo/daifugo.env` に以下を追加する（値はFirebaseコンソールから取得）。

```env
FIREBASE_WEB_ENABLED=false
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_WEB_API_KEY=your-web-api-key
FIREBASE_AUTH_DOMAIN=your-project-id.firebaseapp.com
FIREBASE_WEB_APP_ID=your-web-app-id
GOOGLE_APPLICATION_CREDENTIALS=/etc/daifugo/firebase-service-account.json
APP_BUILD_SHA=your-deployed-commit
```

`FIREBASE_WEB_API_KEY` はFirebase Web SDKの公開設定値であり、管理用秘密鍵ではない。Web API Keyに秘密情報を保存しない。
`GOOGLE_APPLICATION_CREDENTIALS` は稼働ユーザーが読める場所に置き、root/運用専用グループ等で権限を最小化する。
現在の `DAIFUGO_PASSWORD` はAndroid/iOS互換のため当面残す。

## 検証・反映
```bash
git pull --ff-only origin main
./gradlew --no-daemon clean test bootJar
sudo systemctl restart daifugo
sudo systemctl status daifugo --no-pager
curl -I https://yaju-daifugou.com/
curl https://yaju-daifugou.com/api/version
```

上記は **対象ブランチがmainへマージされた後** の例。実際のJAR参照先とコピー工程は `systemctl cat daifugo` で確認すること。現行JARをバックアップしてから更新する。

1. テスト環境でFirebaseの全設定を与え、`FIREBASE_WEB_ENABLED=true` にして再起動する。
2. Web版でGoogleログインし、旧データ移行ダイアログを確認する。
3. ブラウザAと別ブラウザBで同じGoogleアカウントにログインし、プロフィール・戦績・装備・報酬の同期を確認する。
4. 別のGoogleアカウントに切り替え、進行データやルームが混ざらないことを確認する。
5. 未ログインで `/api/rooms/**` と `/api/users/**` が使用できないことを確認する。
6. 同じ対戦結果を複数回記録しても経験値が二重に増えないことを確認する。
7. ミッション報酬の連打・同時取得でも二重に入らないことを確認する。
8. PCでCPU戦、マルチプレイ、ジョーカー階段、7渡し、野獣ルール、演出を回帰試験する。
9. 既存Android/iOSは今回変更しないが、同じVPSに接続するため、ログインと対戦のスモーク試験は必須。
10. HTMLのレスポンスに `Cache-Control: no-store` が付き、更新後に古いWebプログラムが残らないことを確認する。

## 運用と既知の制約
- 旧ブラウザデータは `yaju.pc.progress.v1` から移行されるが、元データが改ざんされていないことは証明できない。Firestoreでは `migrationSource=LEGACY_UNVERIFIED` として保存し、ランキングへ自動流用しない。
- 新規アカウントでは旧データを引き継ぐか確認する。移行済みクラウドデータは別のローカルデータで上書きしない。
- Firestoreへの書込みが失敗した場合、Web結果画面から再試行可能。ただし、**完了済みゲームの永続アウトボックスや完全な障害復旧は未実装**。ゲームルームがTTLで削除される前に復旧する必要がある。これが解消するまで本番有効化は不可。
- 本番のGoogleログインを必須にすると、Firebase無効の旧HTMLクライアントは対戦APIを使えなくなる。リリース前に旧ブラウザの更新確認を行う。
- `FIREBASE_WEB_ENABLED=false` でWeb版の旧方式へ戻せるが、**Firestoreに新しく保存した進行データはlocalStorageへ自動逆移行されない**。本番切戻しはデータの読取専用化等の手順を決めてから実施する。
- 新しい保存データには他ユーザーのUIDをクライアントから指定させない。APIは認証済みHTTPセッションからUIDを解決する。
- システムが正常でもFirebaseプロジェクトを未設定・未作成のままではクラウド機能を有効化できない。
