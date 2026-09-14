# Daifugo v0.1.0 Release Notes

## 初のスマホアプリ版
ブラウザMVPで完成させたゲームエンジンをSpring Bootサーバーとして維持し、Androidネイティブクライアントを追加しました。

## Android
- Kotlin / Jetpack Compose
- Login / Lobby / Room / Game / Result
- サーバーURL指定対応
- ルームIDによる参加
- タップによる複数カード選択
- 革命・縛り・手番・場札・相手残枚数を表示
- WebSocket即時更新
- 通知切断時は5秒ポーリングへフォールバック
- GitHub Actions APKビルド

## Server
- WebSocket `/ws/game` を追加
- WebSocketは状態変更通知専用
- WebSocket接続時に認証済みHTTPセッション / roomId / playerId / 部屋所属を検証
- REST更新後に同一ルームへ `STATE_CHANGED` をbroadcast
- 従来のサーバーauthoritativeゲーム判定を維持

## Version
- Server: 0.1.0
- Android: versionName 0.1.0 / versionCode 1
