# YAJU CONTROL — 戦績を失わない更新・Firebase本番移行の実装計画

## 現時点の確定事項
- Web版の戦績は `FirebaseWebProgressService.recordMatch` によりFirestoreトランザクションで更新され、`users/{uid}/matchResults/{roomId}` による重複防止がある。
- `FirebaseWebProgressController.record` はメモリ上の`GameRoom`から確定順位を読み込んだ後に保存する。このため、保存前にSpring Bootが再起動して部屋を失うと、結果の再送を保証できない。
- ゲーム部屋は`GameRoomService`でメモリ管理され、一定時間で削除される。
- `NORMAL → DRAINING → MAINTENANCE` は実装済み。本番Caddy公開切替・復帰の手動試験は実施済み。

## 本PR（フェーズ0）
- `MaintenanceReadinessService` を追加し、待機部屋・進行中対戦・終了済み部屋をサーバー内で数える。
- **重要**: 保存待ち件数は現時点では判定不能。必ず`pendingPersistenceStatus=UNKNOWN`、`safeToStop=false`。
- セキュリティ未設計の管理APIは公開しない。現時点では読み取り専用の内部サービスと単体テストのみ。
- このPRをマージしてもFirebase本番有効化・ConoHaデプロイを許可するものではない。

## フェーズ1：確定戦績の永続アウトボックス
1. サーバー確定結果を全人間プレイヤー分、UID・roomId・rank・加算値・確定時刻・スキーマバージョンとともに**再起動を越えて残るストレージへ保存**する。
2. 結果確定と永続化の境界を決め、記録がディスク同期される前に対戦を「確定・保存済み」と表示しない。ファイルの原子的置換、ディスク容量、破損時の処理、書込失敗時の拒否を設計する。
3. UIDはGoogle認証済みセッションに基づきサーバーで紐づけ、クライアント申告を信用しない。複数人対戦・切断・CPU戦の各参加者を考慮する。
4. Firestoreへ再送するワーカーを設け、成功後にのみアウトボックスをACKする。途中でクラッシュしても再試行できるよう、`matchResults/{roomId}` の既存の冪等性を維持する。
5. ACK前にプロセス停止・Firestore障害・通信タイムアウト・同じ結果の多重送信・部屋TTL削除が起きても、再送により二重加算なしで最終保存されることをテストする。

## フェーズ2：安全な停止判定
1. 未保存戦績は永続アウトボックス内のACK前件数を正本とする（単なるメモリ内カウンタにしない）。
2. `safeToStop` は`activeMatches == 0`かつ`pendingResults == 0`、書込基盤/保管領域の正常性、保留エラーなし、指定猶予時間経過などを満たしたときのみtrue。
3. 状態照会はSSH経由の運用コマンドまたは厳格認証されたローカル専用経路で提供する。**Caddy配下のSpring Bootはプロキシ越し接続元が127.0.0.1となるので、remoteAddrだけで管理APIを保護しない。**
4. YAJU CONTROLのMAINTENANCE操作は`safeToStop=false`または不明時に必ず拒否する。操作履歴を保存する。

## フェーズ3：Firebase本番導入の受入条件
- `yaju-local` を検証専用に維持し、本番用Firebaseプロジェクト/鍵/ドメインを分離する。
- 旧Webデータの移行、既存Android/iOS互換、Googleログイン前の旧パスワードAPI経由の認可境界を試験する。
- Firestoreの本番バックアップ・復元方法、ロールバック時のクラウドデータ取扱いを策定する。
- 自動テスト、Firestore障害/再起動試験、実機のCaddy切替・復帰がすべてPASSしてから`main`へ反映する。

## フェーズ0の受入観点
- 部屋なし→`waitingRooms=0`、`activeMatches=0`、`safeToStop=false`
- 待機部屋あり→`waitingRooms=1`、`safeToStop=false`
- 追加されたコードは既存のゲーム処理、メンテナンスAPI、Firebase保存処理に介入しない。
- `./gradlew test`をCIと開発PCで実行してPASSを確認する（本PR作成時点の実行結果は別途確認）。
