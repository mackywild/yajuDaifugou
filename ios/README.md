# Daifugo iOS v0.3.0

SwiftUI製のiPhoneクライアントです。PCブラウザ版・Android版と同じSpring Bootサーバーへ接続するため、3プラットフォーム混成で同卓できます。

## 構成

- SwiftUI
- URLSession REST
- URLSessionWebSocketTask（状態変更通知）
- HTTP Cookieセッション
- CSRFトークン
- AVAudioPlayer（野獣ルール音声）
- 外部ライブラリなし

## Xcodeプロジェクト生成

```bash
brew install xcodegen
cd ios
./generate-project.sh
open DaifugoIOS.xcodeproj
```

Xcodeで自分のTeamを Signing & Capabilities に設定し、実機iPhoneを選んでRunします。

## LAN対戦

サーバーPCでSpring Bootを起動し、iPhoneのサーバーURLにPCのLAN IPを入力します。

例:

```text
http://192.168.1.50:8080
```

初回接続時、iOSの「ローカルネットワーク」許可をONにしてください。

## 野獣音声

`DaifugoIOS/AudioResources/` に以下を置き、`./generate-project.sh` を再実行してビルドします。

```text
yaju_available.mp3
yaju_success.mp3
```

wav / m4a でも利用できます。

## クロスプレイ例

- Player 1: Windows Chrome
- Player 2: Android
- Player 3: iPhone
- Player 4: Mac Chrome

ゲームルール・手札・順位はすべて同じサーバーGameEngineが管理します。
