# Daifugo Android v0.3.0

Spring Boot版Daifugoサーバーへ接続するAndroidクライアントです。

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
2. Lobby - プレイヤー名 / 部屋作成 / ルームID参加
3. Room - 参加者 / ホスト開始 / ルームID共有
4. Game - 相手残枚数 / 場札 / 革命 / 縛り / 野獣状態 / 7渡し / 手札選択 / 出す / パス
5. Result - 最終順位 / ロビーへ戻る

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

v0.2.0ではLAN内開発を優先し、Android Manifestでcleartext HTTPを許可しています。インターネット公開時はHTTPSを使用してください。

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
- 最後は8→10で上がる
- 野獣上がり成功・対象化は全端末音声イベント

音声ファイルの配置方法は [`YAJU_AUDIO.md`](YAJU_AUDIO.md) を参照してください。
