# Javaリファクタリング・拡張ガイド

## 開発ブランチ

`main` は本番基準、`develop` は統合用。変更は `refactor/*` や `feature/*` ブランチを `develop` から作成し、`develop` へPRで取り込む。検証後に `develop` から `main` へPRを作成する。

## 現行の設計で活かすJavaの特長

- `game/domain`: Card / Player / GameState などのドメインモデル。ゲームの状態と不変条件を扱う。
- `game/rule`: Rule インターフェースと具体的なルール。ゲームルールの拡張ポイント。
- `game/cpu`: CpuStrategy インターフェースと各難易度の実装。戦略パターン。
- `game/service`: ゲーム進行とターン操作。
- `room`: マルチプレイ部屋のライフサイクル。
- `web/GameApiController`: HTTPルーティングとAPIレスポンス。
- `web/RoomSessionBinding`: HTTPセッションと部屋/プレイヤーの紐付け。
- `web/PlayerRegistrationFactory`: プレイヤー名の入力検証とID生成。

## 今回の変更範囲

`GameApiController` からセッション紐付けとプレイヤー生成を独立した Spring `@Component` に切り出した。コンストラクタインジェクションで依存関係を明示し、HTTPエンドポイント、入力DTO、出力DTO、セッションキー、既存のバリデーション文言、GameRoomServiceの処理には変更を加えない。

## 守るべき互換性

- Android / Web / iOS のAPI URL、DTOフィールド、HTTPセッションキーは変更禁止。
- CPUの抽選・カードの強さ・8切り・10上がり・Jバック・ジョーカー入り階段は意図せず変更しない。
- Firebase / メンテナンス表示 / WebSocket通知処理とキャッシュポリシーは変更しない。
- シリアライズ仕様やクライアント向けのエラー文言を不用意に変更しない。

## 確認手順

1. JDK 21環境で `./gradlew test`。
2. `./gradlew bootJar` でサーバー成果物を生成。
3. Android側で `cd android && ./gradlew testDebugUnitTest assembleDebug`（SDKおよび署名設定が必要）。
4. APIの create / join / state / leave、CPU戦、セッション不一致の拒否、WebSocket通知を確認。
5. ステージング上でブラウザ・Androidのクロスプレイを確認。実機と本番デプロイは明示的な承認後に実施。

検証結果が確認されるまでは `main` にマージしない。
