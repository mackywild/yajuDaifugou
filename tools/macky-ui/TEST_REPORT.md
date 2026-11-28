# Macky UI 1.0.0 試験結果

実施日: 2026-11-28  
対象実装: 307430a393a77fe720c0be43f337e853ccc68ad7

## 実行済み

|試験|環境|結果|
|---|---|---|
|JavaScript構文|Node.js v22.16.0 / node --check|8ファイル中8合格|
|モデル単体|Node.js v22.16.0 / node --test test/model.test.js|5件中5合格|
|DOM動作|Chromium headless / Playwright|8件中8合格|

8 JavaScriptファイルはGitHubのblob SHAと実行環境のgit hash-objectが一致することを確認した。ブラウザでは環境のネットワーク制限によりHTTP配信を検証できなかったため、ローカルの同一ソースを使用し、importの参照先だけをdata URLに変更して空のページで実行した。CSSの読み込み、実際のデモページの配信はこの試験には含まない。

### モデル単体
1. 数値昇降順、null末尾、元データ不変
2. 日本語・大文字小文字検索、検索対象除外
3. ページ境界、空データ、サイズ境界
4. 計算列、ソート無効
5. variantとdurationのフォールバック

### DOM動作
1. button callback / disabled
2. 文字列をHTMLとして挿入しない
3. grid filter / sort / page / empty
4. menu action / active / disabled
5. dialog FIFO / OK / cancel
6. toast終了 / banner置換
7. 並列loading / 例外時cleanup
8. destroy / 未解決dialog解放

## 未実施・マージ前の確認

- 実際のNode HTTPサーバー経由の配信（実行環境のネットワーク制限）
- npm install、Vue 3デモの実行（外部パッケージ取得不可）
- CSSの実画面確認、スマホ幅、キーボード、実機ブラウザ間差異
- 野獣大富豪に組み込んだ後のゲーム回帰（今回は組み込みなし）

手元で cd tools/macky-ui、npm run check、npm test、npm start を実行し、http://127.0.0.1:4173/ と /demo/test.html を確認。Vue版は npm install の後 /demo/vue.html を確認する。

判定: 独立部品の基本ロジック・DOM動作は合格。Vue実動作、配信、見た目の受入は保留。ゲーム本番への導入を承認するものではない。
