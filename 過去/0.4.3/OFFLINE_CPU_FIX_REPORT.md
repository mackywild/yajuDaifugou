# Daifugo v0.4.0 Offline CPU Fix

## 修正理由

旧v0.4.0 Android版はログイン画面が初期表示で、CPU戦開始にもSpring Boot APIを使用していた。
これは「CPU戦をサーバー接続なしで遊び、ゲーム基盤のバグを早期検出する」という実装目的に反していた。

## 修正内容

- 初期画面を `MAIN_MENU` へ変更。
- 「マルチプレイ」を選択した場合のみサーバー接続画面へ遷移。
- 【ひとりでイク】はログイン不要でCPU設定画面へ遷移。
- Androidに `:gamecore` Javaモジュールを追加。
- 既存 `com.example.daifugo.game` の純粋ゲームコアをAndroid APKへ直接組み込み。
- Spring依存の `CpuGameService` はAndroid gamecoreから除外。
- `LocalCpuGameManager` を追加し、GameState/GameEngine/CPU Strategy/N-GODを端末内で直接実行。
- CPU戦ではREST/WebSocketを一切使用しない。
- 7渡し・野獣ルール・禁止上がり・N-GODもローカルGameEngineを使用。
- CPU戦中の表示を `LOCAL` に変更。

## 期待される画面遷移

```
アプリ起動
  ↓
メインメニュー
  ├─ マルチプレイ
  │    ↓
  │  サーバーURL + パスワード
  │    ↓
  │  ロビー / 部屋
  │
  └─ 【ひとりでイク】
       ↓
     CPU人数 / 難易度 / 特殊ルール
       ↓
     即ゲーム開始（ネットワーク不要）
```

## 検証

- Javaゲームコア（Spring依存クラス除外）をJava 17で直接コンパイル: PASS
- `LocalCpuGameManager.kt` をゲームコアjar + DTOスタブでKotlin/JVM型チェック: PASS
- CPU戦コードから `DaifugoApiClient` 呼び出しを除去済み
