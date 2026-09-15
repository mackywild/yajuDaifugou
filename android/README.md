# Daifugo Android v0.4.3

マルチプレイではSpring Boot版Daifugoサーバーへ接続し、CPU戦【ひとりでイク】では端末内ゲームコアだけで完全オフライン動作するAndroidアプリです。

## 技術構成
- Kotlin 2.4.10
- Jetpack Compose BOM 2026.08.00
- Android Gradle Plugin 9.3.0
- compileSdk / targetSdk 37
- minSdk 26
- OkHttp 5.4.0
- REST + WebSocket

## 通信方針
ゲーム操作はすべてREST API経由でサーバーが判定します。
WebSocket `/ws/game` は `STATE_CHANGED` 通知専用です。通知を受けたAndroid端末は、本人のJSESSIONIDを使って `/state` を再取得します。

この方式により、WebSocketで全員共通のGameStateを配信して他人の手札を漏らすことを防ぎます。

## 画面
1. Login - サーバーURL / 共通パスワード
2. Main Menu - マルチプレイ / CPU戦【ひとりでイク】
3. Multiplayer / CPU Setup - 部屋参加またはCPU人数・難易度・ルール設定
4. Room - 参加者 / ホスト開始 / ルームID共有
5. Game - 相手残枚数 / 場札 / 革命 / 縛り / 野獣状態 / 7渡し / 手札選択 / 出す / パス
6. Result - 最終順位 / ロビーへ戻る

## ローカル実機接続
サーバーPCでDaifugoサーバーを起動します。

```bash
export DAIFUGO_PASSWORD='your-password'
./gradlew bootRun
```

スマホとPCを同じWi-Fiへ接続し、アプリのサーバーURLへPCのLAN内IPを入力します。

例:

```text
http://192.168.1.20:8080
```

Android EmulatorからホストPCへ接続する場合は通常:

```text
http://10.0.2.2:8080
```

v0.4.0のプロトタイプではLAN内開発を優先し、Android Manifestでcleartext HTTPを許可しています。インターネット公開時はHTTPSを使用してください。

## APKビルド

```bash
cd android
./gradlew :app:assembleDebug
```

生成先:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actionsの `Android APK` ワークフローでも同じAPKを生成してArtifactとして保存します。


## v0.2.0 野獣ルール
- 配牌または7渡しで8+10が揃うと野獣対象
- 野獣対象は全プレイヤー共有情報
- 最後の8/10を手放すことはできない
- 余分な8/10は7渡し可能
- 最終局面は8×1 + 10×Nとし、8単体→残り10全枚で上がる
- 野獣上がり成功・対象化は全端末音声イベント

音声ファイルの配置方法は [`YAJU_AUDIO.md`](YAJU_AUDIO.md) を参照してください。

## v0.4.0 CPU戦【ひとりでイク】

ログイン後のメインメニューからCPU戦へ進み、以下を設定できます。

- CPU人数 1〜3人
- 簡単 / 普通 / 難しい / N-GOD
- ジョーカー 0〜2枚
- 革命 / 8切り / マーク縛り / 7渡し / 野獣ルール / 禁止上がり

ゲーム判定・CPU思考ともサーバー側で実行するため、Androidだけ別ルールになることはありません。


## v0.4.1 CPU Motion

CPU戦はサーバー不要のまま、CPUの手番を1手ずつ演出します。
カードを出す場合は約3秒かけてCPU側から場へ移動し、PASS・7渡しも専用表示されます。
v0.4.2では直近6手のCPU行動ログを表示します。


## v0.4.2 Premium Table

- プレイカード表面をクラシックなトランプ調へ変更
- 相手の手札を裏向きカードの重なりで表示
- 7渡し中のカードは裏向き表示。行動ログでも内容を秘匿
- 野獣確定時は初期配牌/途中確定とも全画面カットイン
- プレイヤーアイコンを全員へ付与し、同一卓内で重複なし
- CPU行動ログは直近6件


## v0.4.3 J-BACK

- CPU戦設定にJバックON/OFFを追加
- Jバック中フラグを対戦画面へ表示
- `JACK_BACK` イベントで「バック気持ちいい」カットイン
- 通常状態のJバック直後に次プレイヤーが単体3を出した場合、`EARLY_SHOT` イベントで「早漏」カットイン
- 野獣上がりは `8×1 + 10×N` → 8単体 → 残り10全枚に対応
