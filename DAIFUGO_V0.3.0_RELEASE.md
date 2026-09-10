# Daifugo v0.3.0 Release — iOS Crossplay Update

## 対応プラットフォーム

- PCブラウザ (Windows / macOS)
- Android
- iPhone (SwiftUI)

全クライアントが同じSpring Bootサーバー、GameRoom、GameEngineを使用するため混成対戦が可能です。

## iOS追加内容

- Login / Lobby / Room / Game / Result
- Cookieセッション / CSRF
- WebSocket状態変更通知 + 5秒フォールバック同期
- カード複数選択、提出、パス
- 7渡し
- 革命 / 縛り / 野獣状態表示
- 野獣共有イベント音声
- LANローカルネットワーク権限 / ATS対応
- GitHub Actions iOS Simulator Build

## 検証

- Swift全ファイル `swiftc -parse`: PASS
- Swift Models + REST/WebSocketクライアント `swiftc -typecheck`: PASS
- サーバーJSON契約Decode smoke: PASS
- Info.plist / project.yml / GitHub Actions YAML: PASS
- Java 21ゲームコア直接コンパイル: PASS
- 野獣ルール専用Smoke: PASS
- 野獣/7渡し込みランダム4人戦300ゲーム: deadlock 0 / exception 0

## 未実施

この実行環境にはXcode/iOS SDKが無いため `xcodebuild` の実ビルドは未実施です。GitHub ActionsまたはMacのXcodeで最終ビルドしてください。iPhone実機へのインストールにはAppleコード署名が必要です。
