# 野獣大富豪: HTTPS本番導入・Tomcatセキュリティ対応（2026-10-22）

対象: ConoHa VPS / yajudaifugou.com / Java 21 / Spring Boot / 組み込みTomcat / Caddy。

## 現在の変更内容

- Spring Boot `3.5.0` → `3.5.16`。3.5系は2026年6月でOSSサポート終了のため**暫定対応**。Spring Boot 4.xへの移行を別タスクにする。
- Tomcat 10.1系を `10.1.61` に固定し、2026年10月の修正を取り込む。
- Tomcatの外部公開を避けるため既定バインド先を `127.0.0.1:8080` に変更。
- 信頼済みローカルCaddy（127.0.0.1）からのみ X-Forwarded-For / X-Forwarded-Proto を使用し、クライアントIPとスキームを復元する。
- 本番WebSocket許可Originは `https://yajudaifugou.com`。localhostは開発用のみ。ワイルドカード `*` を禁止。
- Androidのreleaseビルドは平文HTTPを禁止。debugビルドだけLAN用HTTPを許可する（別マニフェスト）。
- Webフロントは既に `https:` → `wss:` の自動切替実装済み。Android/iOSの既定接続先をHTTPSドメインへ更新。
- Secure Cookieは本番では環境変数 `SESSION_COOKIE_SECURE=true` で有効化する。

## 0. 本番反映前の必須条件

- 本番VPSにSSHでログインできること（SSHの22番を閉じないこと）。
- 現行の `yaju-daifugou` systemdサービスや既存Caddyfileを確認し、設定をバックアップすること。
- **既存の他アプリのCaddy設定は上書きしないこと**。以下は単一サイトのサンプル。
- ConoHaのDNSに `A @ 160.251.180.41` を設定。不要/古いAAAAレコードがないか確認する。
- ConoHaセキュリティグループ・ホスト側FWで外部からTCP 80/443を許可。8080は外部公開しない。
- サーバ運用者が実行する。リポジトリのPRをマージしただけではVPSに自動反映されない。

## 1. DNS・本番アプリ確認

```bash
dig +short yajudaifugou.com A
dig +short yajudaifugou.com AAAA
sudo systemctl status yaju-daifugou --no-pager
sudo systemctl cat yaju-daifugou
sudo ss -ltnp | grep -E ':(80|443|8080)\b' || true
```

`A` が本番VPSを指すまで証明書を発行しない。Caddy/Let's Encryptが80番または443番に到達可能である必要がある。

## 2. 更新版のビルドと依存関係の検証

VPSのアプリソース管理ディレクトリで対象コミットをチェックアウトし、Java 21で実施。

```bash
./gradlew --no-daemon clean test bootJar
./gradlew --no-daemon dependencyInsight --dependency tomcat-embed-core --configuration runtimeClasspath
```

結果に `tomcat-embed-core:10.1.61` が表示されること、既存のテストが成功することを確認。**テスト未成功なら本番更新しない**。

運用サービスの `ExecStart` が `bootRun` か `java -jar` か確認する。`java -jar` の場合は、生成した新しいJARへ切り替える必要がある。作業前に既存JARと設定を別ディレクトリにバックアップする。

## 3. Caddyサイト設定（HTTPS・WSS）

`deploy/Caddyfile` の `yajudaifugou.com { ... }` ブロックを `/etc/caddy/Caddyfile` に組み込む。複数のアプリが同居する場合、ファイル全体を置き換えない。

```bash
sudo cp /etc/caddy/Caddyfile "/etc/caddy/Caddyfile.bak.$(date +%Y%m%d-%H%M%S)"
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
sudo systemctl reload caddy
sudo journalctl -u caddy -n 100 --no-pager
```

Caddyはドメイン名を設定するとTLS証明書の自動取得・自動更新とHTTP→HTTPSリダイレクトを行い、`reverse_proxy` はWebSocketも転送する。TLS秘密鍵をGitHubに保存しない。

## 4. Spring Boot本番環境変数

`sudo systemctl edit yaju-daifugou` のサービスoverride例。**既存の `DAIFUGO_PASSWORD` は変更・公開しない**。

```ini
[Service]
Environment="SESSION_COOKIE_SECURE=true"
Environment="DAIFUGO_BIND_ADDRESS=127.0.0.1"
Environment="DAIFUGO_ALLOWED_ORIGINS=https://yajudaifugou.com"
```

反映:

```bash
sudo systemctl daemon-reload
sudo systemctl restart yaju-daifugou
sudo systemctl status yaju-daifugou --no-pager
sudo ss -ltnp | grep ':8080'
```

8080の待受先が `127.0.0.1` のみであることを確認する。必要に応じてConoHaのパケットフィルター／ホストFWでも8080を閉じる。LAN向け開発だけ `DAIFUGO_BIND_ADDRESS=0.0.0.0` とし、開発Originを `DAIFUGO_ALLOWED_ORIGINS` に**明示的に**加える（`*` は禁止）。

## 5. 公開URLで疎通確認

```bash
curl -I http://yajudaifugou.com/
curl -I https://yajudaifugou.com/
curl -si https://yajudaifugou.com/api/auth/session | grep -i -E '^(HTTP/|set-cookie:)'
```

期待結果:

- HTTPで301/308などのHTTPSへのリダイレクトが返る。
- HTTPS証明書が検証可能で、Web画面が開ける（`curl -k` での検証は不可）。
- `Set-Cookie` の `JSESSIONID` に `Secure`, `HttpOnly`, `SameSite=Lax` がある。
- ブラウザで部屋を作り、DevTools Network の `wss://yajudaifugou.com/ws/game` が接続できる。
- Android/iOSがHTTPSの同じ部屋で通信できる（アプリ更新・再ビルドが必要）。
- 外部ネットワークからVPSの8080に直接接続できない。

**注意:** Javaプロセスを再起動するとインメモリの対戦状態・セッションは失われる。対戦者がいない時間に実施する。

## 6. 障害発生時の復旧

1. `journalctl -u yaju-daifugou -n 200 --no-pager` と `journalctl -u caddy -n 200 --no-pager` で切り分ける。
2. DNS/証明書の場合: A/AAAA、80/443、Caddyの設定とログを確認。TLS検証無効化で回避しない。
3. アプリ不具合の場合: 新旧JARとsystemd設定を比較し、バックアップからロールバックする。ただし**脆弱なTomcatをインターネット公開した状態に戻さない**。必要なら一時的にCaddy側でサービス停止する。
4. Caddyの設定を復旧する場合: バックアップした既存Caddyfileを戻し、`caddy validate` 後にreload。SSH/他アプリの経路は維持する。

## 7. 未完了の運用タスク

- 本番ConoHa VPSへの適用と実ドメインの証明書確認はGitHub PRだけでは実行できない。
- Android/iOSのストア・内部テスト用配信、旧HTTP/IPクライアントの切替を実施する。
- Spring Boot 3.5系のOSSサポート終了に伴う4.x移行を計画する。
- 認証設計（`/api/auth/session` がログイン不要で公開セッションを発行する点）を別途レビューする。HTTPS導入自体は認可を強化しない。

公式情報:
- https://tomcat.apache.org/security-10
- https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/
- https://caddyserver.com/docs/automatic-https
- https://caddyserver.com/docs/caddyfile/directives/reverse_proxy
