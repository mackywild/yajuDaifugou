# 野獣大富豪 — ConoHa実機向けメンテナンス・更新手順

## 実機で確認した構成（2026-11）
- systemd: `yaju-daifugou.service`
- WorkingDirectory: `/opt/yajuDaifugou`
- 稼働JAR: `/opt/yajuDaifugou/app.jar`
- 環境変数: `/etc/yaju-daifugou.env`
- Caddy: `/etc/caddy/Caddyfile`、公開ドメイン `yaju-daifugou.com`
- リバースプロキシ先: `127.0.0.1:8080`
- VPSのメモリ: 約960MiB。Javaヒープの上限: 384MiB。2つ目のJavaサービス常駐は当面採用しない。
- **注意:** 現状 `*:8080` にbindされている。Caddyのメンテナンス画面だけでは8080への直接アクセスを遮断できない。ファイアウォールとループバックbindの確認が先決。
- ユーザーがJARバックアップ `/opt/yajuDaifugou/backups/app-before-maintenance.jar` と現行JARのSHA-256一致を確認済み。

## 方式
開発時は自分のPCでLocal起動。本番デプロイの直前にCaddyで公開経路だけをメンテナンス画面へ切替え、本番Javaサーバーを更新して、SSHポートフォワードで非公開スモーク試験する。

**同じVPS上で8080/8081の2プロセスは実行しない。** 将来VPSを増強する場合のみ `deploy/daifugo-staging.service` を参考に常設Stagingを設置する。

更新状態は `NORMAL`（通常） → `DRAINING`（新規対戦禁止、現在の対戦は継続） → `MAINTENANCE`（ゲームAPI停止）。Caddyは公開画面の制御、Spring BootはAPIの安全な停止を担当する。

## 準備: 最初に確認・保存
```bash
cd /opt/yajuDaifugou
sudo systemctl cat yaju-daifugou
sudo ss -lntp | grep ':8080'
sha256sum app.jar backups/app-before-maintenance.jar
sudo cp -p /etc/yaju-daifugou.env /etc/yaju-daifugou.env.before-maintenance
sudo cp -p /etc/caddy/Caddyfile /etc/caddy/Caddyfile.before-maintenance
```

`/etc/yaju-daifugou.env` には秘密情報があるため、バックアップもGitHubへアップロードしない。

Spring Bootの新バージョンでメンテナンス状態ファイルを有効にする場合は、`/etc/yaju-daifugou.env` に次を加える。**既存の環境変数は削除しない。**

```dotenv
DAIFUGO_MAINTENANCE_FILE=/opt/yajuDaifugou/ops/maintenance.mode
SERVER_ADDRESS=127.0.0.1
```

サーバー起動時に `ops/` は存在しなければ作成。初期モードは `NORMAL`。
外部から8080へ到達できる場合は、bind変更とConoHaのポートフィルタリングを行い、外部アクセスが不可能であることを確認する。Androidアプリ等が直8080ではなく公開ドメイン経由で通信していることも確認する。

## 通常更新の手順
### A. 停止予告・DRAINING
新JARがCIでPASSし、旧データ復旧手順が用意されていることが前提。新バージョンのサーバーがメンテナンス状態ファイルを認識することを確認した後で実行する。

```bash
sudo install -d -m 755 /opt/yajuDaifugou/ops
printf 'DRAINING\n' | sudo tee /opt/yajuDaifugou/ops/maintenance.mode
curl --fail http://127.0.0.1:8080/api/system/status
```

現行対戦が終了し、戦績が永続保存されたことを確認する。現在は進行中対戦の一覧・保存待ち件数の運営APIがないため、検証環境での改善が必要。

### B. 公開URLをメンテナンス画面に切替える
`deploy/maintenance.html` を `/var/www/yaju-maintenance/maintenance.html` に配置する。Caddy設定は**実機の現状と差分比較したうえで** `deploy/Caddyfile.maintenance` を適用する。

```bash
cd /opt/yajuDaifugou
sudo install -d -m 755 /var/www/yaju-maintenance
sudo install -m 644 deploy/maintenance.html /var/www/yaju-maintenance/maintenance.html
sudo cp -p /etc/caddy/Caddyfile /etc/caddy/Caddyfile.before-maintenance
sudo install -m 644 deploy/Caddyfile.maintenance /etc/caddy/Caddyfile
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

`/api/system/status` のみバックエンドへ接続し、それ以外のゲームAPI・WebSocketをCaddyが503で遮断する。
**設定検証失敗時にはreloadせず元のCaddyfileに戻す。** 一般公開URLがメンテナンス画面であることを確認し、以下を実行する。

```bash
printf 'MAINTENANCE\n' | sudo tee /opt/yajuDaifugou/ops/maintenance.mode
```

### C. JAR更新
JARのコピー元はビルド結果を確認して指定する。誤って `*-plain.jar` を置かない。
**JARを更新するだけではGitのワークツリーと一致しない場合がある**ため、稼働バージョンを `/api/version` で確認する。

```bash
cd /opt/yajuDaifugou
git pull --ff-only origin main
./gradlew --no-daemon clean test bootJar
ls -lh build/libs/
sudo systemctl stop yaju-daifugou
# ここで確認済みのSpring Boot実行JARをapp.jarへコピーする
sudo systemctl start yaju-daifugou
sudo systemctl status yaju-daifugou --no-pager
```

### D. 非公開スモーク試験
公開URLはメンテナンス表示のまま。サーバーの `MAINTENANCE` を一時的に `NORMAL` に変えて操作試験する。**8080は外部に公開しない。**

PC側:
```bash
ssh -N -L 18080:127.0.0.1:8080 <VPSログインユーザー>@<VPSホスト>
```

PCブラウザで `http://localhost:18080` を開く。VPS内で同じJavaプロセスを使うため本番Firebaseや保存先への影響がある。破壊的なテストは行わず、別のテストアカウントを使用すること。Googleログインを使う場合はOAuth承認済みドメインに `localhost` が必要。

検証が終わったら再び `MAINTENANCE` に戻し、状態を確認する。

### E. 一般公開へ復帰
```bash
sudo cp -p /etc/caddy/Caddyfile.before-maintenance /etc/caddy/Caddyfile
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy
printf 'NORMAL\n' | sudo tee /opt/yajuDaifugou/ops/maintenance.mode
```

※通常公開の再開は、**先にSpring Boot側をNORMALへ戻してヘルスチェックを通してから、Caddyを公開設定へ復元**する順序が望ましい。上の一括コマンドはその順序を必ず守って分割実行すること。

## トラブル時の切戻し
現行バージョンのJARが正常に保存されていることは確認済み。停止状態で元のJARを戻し、`/etc/yaju-daifugou.env.before-maintenance` の設定も必要に応じて復元する。システムサービス名とパスは実機で確認済み。

```bash
sudo systemctl stop yaju-daifugou
sudo cp -p /opt/yajuDaifugou/backups/app-before-maintenance.jar /opt/yajuDaifugou/app.jar
sudo systemctl start yaju-daifugou
sudo systemctl status yaju-daifugou --no-pager
```

Firestoreの保存データはJAR復元では戻らない。Firebase本番有効化は戦績永続化・後方互換・バックアップ設計が整うまで行わない。

## リリース停止条件
1. 本番JARとFirebaseの互換性を検証済みであること。
2. 8080番へのインターネットからの直接到達ができないこと。
3. 現行対戦終了と報酬書込みを確認する方法があること。
4. Caddy構文チェック・サーバーCI・実機スモークテストがPASSすること。
