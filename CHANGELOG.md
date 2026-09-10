# Changelog

## v0.3.0 - iOS Crossplay Update

- iPhone SwiftUIクライアント追加
- PCブラウザ / Android / iPhoneの3-wayクロスプレイ
- URLSession Cookie/CSRF認証
- URLSessionWebSocketTaskによるリアルタイム状態更新
- 7渡し・野獣ルール・音声イベント対応
- iOS GitHub Actions Simulatorビルド追加

## v0.2.0 - 野獣ルールアップデート

### Game Core
- 新ローカルルール「野獣ルール」を実装
- 配牌時の8+10所持で野獣対象化
- 7渡しで8+10が揃った隣プレイヤーへ野獣ルールを伝播
- 一度対象になった野獣ルールは解除不可
- 8/10を最低1枚ずつ保持する制約
- 余剰8/10の通常利用・7渡しに対応
- 最後の2枚を8→10で出す「野獣上がり」
- 野獣上がり違反を反則最下位として処理
- 7渡しを新規実装

### Android
- 野獣対象者と進行状態を表示
- 7渡しカード選択UIを追加
- `YAJU_AVAILABLE` / `YAJU_SUCCESS` 全体共有イベントを音声キュー化
- `yaju_available` / `yaju_success` の差し替え式音声に対応
- 音声未配置時は無音フォールバック

### Verification
- Java 21コアコンパイル成功
- 野獣ルール専用スモーク成功
- ランダム4人戦300ゲーム: deadlock 0 / exception 0

## v0.1.0 - Android First Release

### Android
- Kotlin + Jetpack Composeのネイティブアプリを追加
- Login / Lobby / Room / Game / Result の5画面を実装
- サーバーURL入力に対応（LAN内IP / Android Emulator 10.0.2.2）
- ルーム作成・ルームID参加
- タップによる複数カード選択
- 場札、相手残枚数、手番、革命、マーク縛り、順位を表示
- REST APIによるサーバーauthoritative操作
- WebSocketによるリアルタイム更新通知
- WebSocket切断時は5秒ごとのREST同期へフォールバック
- v0.1.0アプリアイコンを追加
- GitHub Actionsでdebug APK自動ビルド

### Server
- `spring-boot-starter-websocket` を追加
- `/ws/game?roomId=...` を追加
- WebSocketは `CONNECTED` / `STATE_CHANGED` 通知専用
- WebSocket接続時に認証済みHTTPセッション、ルーム紐付け、プレイヤー所属を検証
- create / join / start / play / pass / leave / logout後に同一ルームへ状態変更通知

### Game Core
- 2〜4人対戦
- シングル / ペア / 3枚 / 4枚 / 階段
- 革命 / 8切り / マーク縛り
- ジョーカー単体 / スペード3返し
- 2 / 8 / ジョーカー / スペード3の禁止上がりを反則最下位として処理
- 禁止上がりによるデッドロック修正済み
- 2〜4人ランダム1000ゲーム検証: deadlock 0 / exception 0 / 順位重複 0（Web版完成時検証）
