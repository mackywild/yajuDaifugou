# Daifugo v0.2.0

Java 21 + Spring Bootサーバーと、Kotlin + Jetpack Compose Androidアプリで動作するオンライン大富豪です。

## v0.2.0 構成

```text
Android App
   │ REST: login / room / play / pass
   │ WebSocket: STATE_CHANGED notification
   ▼
Spring Boot Server
   ├─ GameRoom / GameState
   ├─ GameEngine
   ├─ PlayValidator
   ├─ RuleEngine
   └─ FinishValidator
```

ゲーム判定と手札の正本はサーバー側だけが保持します。Androidアプリは操作要求と表示を担当します。

## ゲーム機能
- 2〜4人オンライン対戦
- 共通パスワード認証（`DAIFUGO_PASSWORD` 必須）
- HTTPセッションによる本人紐付け
- 部屋作成 / ルームID参加 / 退出
- ホストのみゲーム開始
- シングル / ペア / 3枚 / 4枚 / 階段
- 革命
- 8切り
- 7渡し
- 新ローカルルール「野獣ルール」
  - 8+10所持で対象化
  - 7渡しによる対象伝播
  - 8/10保持義務
  - 最後の2枚を8→10で野獣上がり
  - 違反上がりは反則最下位
  - 全端末共有音声イベント
- マーク縛り
- ジョーカー単体
- スペード3返し
- 2 / 8 / ジョーカー / スペード3の禁止上がり → 反則最下位
- 順位判定
- 他プレイヤーの手札秘匿
- WebSocketリアルタイム更新通知 + 5秒フォールバック同期
- ログイン試行回数制限
- CSRF保護
- 無操作ルームTTL

## サーバー起動
Java 21を使用してください。

```bash
export DAIFUGO_PASSWORD='十分に長いパスワード'
./gradlew bootRun
```

既定ポートは8080です。

## Androidアプリ
詳細は [`android/README.md`](android/README.md)。

実機から同一LAN上のサーバーへ接続する場合、アプリのLogin画面で以下のように入力します。

```text
http://<サーバーPCのLAN内IP>:8080
```

Android Emulatorなら通常 `http://10.0.2.2:8080` を使用できます。

## GitHub Actions
- `Server Test`: Java 21でSpring Boot / JUnitテスト
- `Android APK`: Android debug APKをビルドし、`Daifugo-v0.2.0-debug` Artifactとして保存

## 公開時の注意
LAN内開発ではHTTPを許可しています。本番インターネット公開ではHTTPSを使用し、サーバー側で次を設定してください。

```bash
export SESSION_COOKIE_SECURE=true
```

共通パスワードはGitへコミットしないでください。


## 野獣ルール
詳細仕様は [`YAJU_RULE_SPEC.md`](YAJU_RULE_SPEC.md) を参照してください。

音声はアプリへ同梱していません。友人に録音してもらった音声を
`android/app/src/main/res/raw/` へ配置するだけで有効になります。
詳細は [`android/YAJU_AUDIO.md`](android/YAJU_AUDIO.md) を参照してください。


## iPhone / iOS v0.3.0

`ios/` にSwiftUIクライアントを追加しました。PCブラウザ、Android、iPhoneが同じSpring Bootサーバーでクロスプレイできます。

```bash
brew install xcodegen
cd ios
./generate-project.sh
open DaifugoIOS.xcodeproj
```

実機ではサーバーPCのLAN IP（例 `http://192.168.1.50:8080`）を指定します。
野獣ルール音声は `ios/DaifugoIOS/AudioResources/` に音声を置いてプロジェクトを再生成します。
