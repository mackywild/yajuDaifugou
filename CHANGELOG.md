# Changelog

## v0.4.4 - 8-PLAYER UPDATE

- マルチプレイの最大人数を4人から8人へ拡張（2〜8人）
- CPU戦を人間1人 + CPU1〜7人へ拡張
- `GameLimits`を追加し、ゲームコア/部屋/CPU戦の人数制約を共通化
- `GameState`でも9人以上を拒否し、部屋を経由しない実行経路も保護
- 8人・Joker2枚でも配牌差が1枚以内になることを回帰テスト化
- Androidロビーを8席化、CPU人数選択を横スクロール化
- AndroidのCPUプレイモーション開始位置を最大7CPUへ一般化
- iOSのCPU人数を1〜7へ拡張し、相手一覧を横スクロール化
- WebのCPU人数選択を1〜7へ拡張
- 自己対戦データ生成器の人数範囲を2〜8人へ拡張
- 8人ランダムルール500ゲームでdeadlock 0 / exception 0を確認
- Android versionName 0.4.4 / versionCode 8へ更新

## v0.4.3 hotfix — スペード3返し最強化

- JOKERをスペード3で返した場合、そのトリック中のスペード3を絶対最強札として扱うよう修正
- スペード3返し成立後は、場が流れるまで他のカードを一切提出不可（PASSのみ）
- 通常出しのスペード3は従来どおり通常の3として扱う
- CPU合法手生成にも同ルールを反映し、スペード3返し後はPASSだけを生成
- Android workflowがルートのgamecore変更でも起動するようpaths条件を修正
- ルートbuild.gradleのversionを0.4.3へ統一


## v0.4.3 - J-BACK UPDATE

- 野獣上がりを `8×1 + 10×N` に拡張し、8単体→残り10全枚で成立するよう修正
- Jバックを追加（`revolution XOR jackBack` で実効強弱を判定）
- Jバックは場が流れるまで継続
- J提出時の「バック気持ちいい」全体カットインを追加
- 通常状態のJバック直後、次プレイヤーが単体3を出した場合の「早漏」カットインを追加
- 革命中のJバックでは2が最強となるため、早漏判定は発生しない
- CPU合法手生成・ヒューリスティックをJバック対応
- Web / Android / iOSのルール設定・状態表示・カットインを追加
- Android versionNameを0.4.3、versionCodeを7へ更新

## v0.4.2 - PREMIUM TABLE UPDATE

- Android対戦画面を高級感のあるテーブルUIへ刷新
- 市販トランプを意識したオリジナル表面デザインへ変更
- 濃紺×金の裏向きカードデザインを追加
- 相手手札を裏向きカードの実枚数表示へ変更
- 7渡し演出を裏向き表示へ変更し、行動ログからカード内容を秘匿
- 野獣対象確定時の全画面カットインを追加（初期配牌/7渡し途中確定の両方）
- プレイヤー/CPUへ固有アイコンを追加。同一卓で重複なし
- CPU行動ログを直近6件へ拡張
- 野獣カットイン中はCPU手番進行を一時停止
- Android versionNameを0.4.2、versionCodeを6へ更新

## v0.4.1 - CPU MOTION UPDATE

- CPU戦の自動進行を1手ずつUIへ返す方式へ変更
- CPUがカードを出す際、手元側から場へ約3秒かけて移動するモーションを追加
- CPUのPASS表示を追加
- CPUの7渡し演出を追加
- CPUの直近4手を確認できる行動ログを追加
- CPU演出中は人間側のカード操作・提出・パスをロック
- 8切りなどで場が即座に流れても、直前のCPU行動をログから確認可能
- Android versionNameを0.4.1、versionCodeを5へ更新

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
- 8→10を基本とする「野獣上がり」（v0.4.3で8×1 + 10×Nへ拡張）
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
