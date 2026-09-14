# Changelog

## v0.4.0 - CPU BATTLE UPDATE

### Game modes
- ログイン後にメインメニューを追加
- 既存オンライン部屋対戦を「マルチプレイ」として整理
- CPU戦【ひとりでイク】を追加
- CPU 1〜3人、2〜4人戦に対応
- CPU戦作成直後にゲーム開始し、人間の手番までCPUを自動進行

### CPU difficulties
- 簡単: ランダム寄り
- 普通: 基本ヒューリスティック + ミス率
- 難しい: 相手残枚数・手札構造・特殊ルールを考慮
- N-GOD: 自己対戦学習済みニューラル評価器 + 安全ヒューリスティック

### Machine learning
- 38次元state-action特徴量
- 5,000ゲーム / 278,188サンプルの自己対戦データ
- 32→16 hidden MLP、最終順位報酬を学習
- 学習済み重みをJavaへ埋め込み、サーバー推論時にPython不要
- 再学習用Generator / Python学習スクリプト / Benchmarkを同梱

### CPU rule settings
- ジョーカー0〜2枚
- 革命 / 8切り / マーク縛り / 7渡し / 野獣ルール / 禁止上がり ON/OFF
- 野獣ルールON時は8切りを必須化

### Clients
- Web / Android / iOSにメインメニューとCPU戦設定画面を追加
- CPUプレイヤー・難易度をゲーム画面に表示

### Verification
- Java 21 CPU/core compile PASS
- CPU feature smoke PASS
- 各難易度4,000ゲーム benchmark deadlock 0
- N-GOD: 1位率31.3%、平均順位2.173（Hard CPU×3対戦）
- ランダム設定自己対戦300ゲーム deadlock 0

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
