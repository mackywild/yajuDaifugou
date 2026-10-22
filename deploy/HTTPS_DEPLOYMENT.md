# 野獣大富豪 — HTTPS / Tomcatセキュリティ対応（ConoHa VPS / Ubuntu系OSを想定）

## 適用範囲と前提
- Java 21 + Spring Boot 3.5.16、組み込みTomcat 10.1.61へのパッチ適用。
- CaddyでTLS終端、HTTPからHTTPSへ自動リダイレクト。WebSocketも `wss://` に切り替え。
- サーバー管理者権限、購入済みドメイン `yajudaifugou.com`、ConoHa VPS（公開IP `160.251.180.41`）を利用する想定。実際の稼働IPは管理画面でも確認する。
- **このリポジトリへの変更だけでは本番HTTPS化は完了しない。** DNS・FW・サービスへの反映はサーバで実施する。
- デプロイ前にサーバのOS、稼働中サービス、既存ドメイン、既存リバースプロキシの有無を調べる。同一サーバで複数アプリを使う場合は既存サイト設定にマージする。

## 1. 公開前チェック（サーバ）
```bash
java -version
sudo ss -lntp | grep -E ':(80|443|8080)\b' || true
systemctl list-units --type=service --state=running | grep -E 'caddy|nginx|apache|daifugo' || true
```
- 既存の80/443ポート利用サービスがあれば **Caddyを上書き起動しない**。
- ConoHaのDNSで `yajudaifugou.com` のAレコードを `160.251.180.41` へ向ける（現在のVPS IPを確認したうえで反映）。AAAAレコードを使うならIPv6も到達可能にする。
- TCP 80/443をインバウンド許可（VPS事業者側FW・OS側FW）。8080はインターネットから遮断する。
- 以前のHTTP接続元アプリ（Android/iOS）も本番ドメインのHTTPSへ変更する必要がある。

## 2. アプリ更新・検証
```bash
./gradlew --no-daemon clean test bootJar
./gradlew dependencyInsight --dependency tomcat-embed-core --configuration runtimeClasspath
```
- 解決された `tomcat-embed-core` が **10.1.61** であること、テストがPASSすることを確認。
- `build/libs/` の `*-plain.jar` ではない実行可能Jarをデプロイする。
- いったん既存稼働Jarと設定をバックアップする。既存のログインセッションや進行中のゲームは再起動で切れる。

## 3. サーバ側設定例
Ubuntu系なら、Caddy公式パッケージの手順に従いインストールする。
- `deploy/Caddyfile` の **yajudaifugou.com** が設定されていることを確認して `/etc/caddy/Caddyfile` に配置する。
- 既存のCaddyfileを使っている場合はサイトブロックだけ追加し、既存設定を残す。
- `deploy/daifugo.service` を `/etc/systemd/system/daifugo.service` に配置。実際のユーザー・パスは環境に合わせる。
- `deploy/daifugo.env.example` を `/etc/daifugo/daifugo.env` にコピーし、**強いパスワード**を設定。環境ファイルは秘密情報を含むので Git 管理しない。
- `SERVER_ADDRESS=127.0.0.1` は重要（8080を外部に公開しない）。
- `SESSION_COOKIE_SECURE=true` は **公開HTTPS切替のタイミング** で有効化する。HTTPでのログインCookieは送信されなくなる。
- `server.forward-headers-strategy=framework` を利用するため、Tomcatへの接続を127.0.0.1のCaddyに限定し、外部からの偽装Forwardedヘッダを直接受け付けない。

コマンド例（初回セットアップを済ませた後）:
```bash
sudo chmod 600 /etc/daifugo/daifugo.env
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl daemon-reload
sudo systemctl enable --now daifugo
sudo systemctl reload caddy
sudo systemctl status daifugo --no-pager
sudo journalctl -u daifugo -n 100 --no-pager
```

## 4. 本番切替の受入確認
```bash
# サーバ内部からSpring Bootへ疎通確認
curl -I http://127.0.0.1:8080/
# 外部から証明書・HTTPSリダイレクトを確認
curl -I http://yajudaifugou.com/
curl -I https://yajudaifugou.com/
```
- 実ドメインに置き換えて実行。HTTPはHTTPSへリダイレクト、HTTPSは有効な証明書。
- ブラウザでログイン、部屋作成、他端末で参加、カードを出す、野獣ルールまで確認。
- ブラウザのNetworkタブでWebSocketが `wss://<domain>/ws/game` に接続し、ゲーム状態が更新されることを確認。
- ログインレスで `/api/rooms/**` を操作できないこと、別部屋・別ユーザーの情報が漏れないことを確認。
- HTTPS応答のセッションCookieがSecure・HttpOnly・SameSite=Laxであることを確認。
- Androidのリリースビルドはcleartext通信を禁止。ローカル開発用debugビルドはLAN上のHTTPに対応。
- クロスオリジンからのブラウザWebSocket接続は拒否する設計。別ドメインにWeb UIを分離する場合は独立して認証・Originポリシーを見直す。
- Android/iOSは既定の公開API接続先が更新済み。既存ユーザーが手動設定した旧HTTP URLは必要に応じて変更する。運用環境のAPI基点URLを `https://yajudaifugou.com` に設定し、端末間クロスプレイを確認。
- OSや公開ポート以外も定期更新し、GitHub Dependabot等の依存更新を継続する。

## 5. 切戻し
- パッチ適用前のJarを安全な場所に残す（秘密情報は除外）。起動失敗なら旧Jarで `daifugo` を再起動しログを確認。
- HTTPSの障害時もまずCaddyとDNSを調べる。原則HTTPの外部公開へ安易に戻さない。
- **旧Jarへ戻すことは脆弱性も復活させる。** 必要最小限の緊急措置にとどめ、公開アクセスの制限を優先する。

## 今後の別PR
Spring Boot 3.5.16は3.5系の最終OSS版。長期保守のためSpring Boot 4.1系への移行を計画する。
