# Macky UI v2.0.0 試験結果報告書

実施日: 2026-11-28
実装確認対象: 7947a51f0a391c2467c042a6331de69f9c736ec9

## 1. 実施済み（GitHubソースの読み戻しによる検証）

実行環境のDNS制限によりGitHubのgit cloneを実施できなかった。GitHub連携で対象ファイルの実コードを取得し、JavaScript実行環境でモジュールのimport/export宣言を変換して評価した。以下の検証は実コードのロジック・接続を対象とするが、Node.jsのネイティブESM/npmや実ブラウザでの実行結果ではない。

|試験|結果|
|---|---|
|必須/数値/文字数/形式|PASS|
|独自入力検証|PASS|
|グリッド履歴・Undo/Redo・commit/reset・差分|PASS|
|履歴上限・Redo無効化|PASS|
|CSV/TSVの引用・改行・往復|PASS|
|CSV式文字列の緩和|PASS|
|セル編集権限・型変換|PASS|
|6テーマ・トークン制限|PASS|
|テーマ保存・復元（メモリstorage）|PASS|
|34部品の既定値・コード生成|PASS|
|画面スキーマ・ID・予約キー・サイズ制限|PASS|
|HTML生成・script終了タグのエスケープ|PASS|
|仮想ウィンドウ境界|PASS|
|singleFlightの並行抑制|PASS|
|通知履歴・上限・restore|PASS|
|スタジオルート・パス制限|PASS（初回は検証環境にURL APIがなく再試験）|

上記16観点は最終的に全合格。ルート検証はURL APIの代わりに固定ルートのクエリを文字列で分割して再実施。追加後の固定14画面のルートも再確認済み。

### モジュール・配布設定

- 13個のソースJSモジュールを変換評価し、公開エクスポート64件を確認。
- 34種のregistry全項目に対応関数あり（サービス型を除く関数照合）。Vueのスタブruntimeで31個のコンポーネント登録を確認。実Vueの描画試験ではない。
- HTMLに加えJS moduleとVue SFCの生成、script終了タグのエスケープを確認。
- 更新した主要11ファイルの構文を、import/export等を除去したAsyncFunctionで確認。最終studio.jsも再確認済み。
- build.mjsの14個の配布資産がpackage.jsonのfiles許可リストに存在することを確認。実際のnpm packの内容は未検証。
- 100000件のgridView検索・ソート: 全100000件を取得、ページ50件、元配列順序を維持。JavaScript隔離実行環境で約201ms（単回参考値。実機ブラウザ性能保証ではない）。

## 2. 受入試験として追加したコード

- test/v2.test.js: Node test 14件（入力検証、グリッド、CSV、テーマ、コード生成、仮想表示、UX、モック通信、Vue登録、ルート）。
- demo/test-v2.html: 実ブラウザ用。34種のマウント/破棄、フォームの二重送信、Advanced Gridの貼付・Undo/Redo・権限、100000行仮想化、タブキー操作・テーマ復元、画面生成/破棄。
- 既存 test/model.test.js、test/routes.test.js、test/build.test.js、demo/test.html を維持し、ビルド試験の配布対象を更新。

## 3. 未実施・制約

- リポジトリ全体を取得できないため、実際の npm run check / npm test / npm run build / npm pack は未実施。
- ローカルHTTP、ブラウザDOM/CSS、Clipboard、ドラッグ、キーボード、Vue3実行、生成HTML/SFCの実ビルド、他アプリへの配布導入、実機モバイルは未実施。
- モック通信のタイマーとAbortController試験はNode testに追加したが、今回のJavaScript検証環境で未実施。
- v1.4の編集グリッドと仮想グリッドは別API。編集状態を保った仮想化は未対応。
- 画面生成は2列レイアウト・UIのみ。業務処理や任意座標/ネストは生成しない。
- リッチテキストは簡易装飾とプレーンテキスト。アクセシビリティ・互換性・本番負荷は別途評価。

## 4. 受入手順（手元のクローン）

    cd tools/macky-ui
    npm run check
    npm test
    npm run build
    npm pack
    npm start

http://127.0.0.1:4173/ を開き、旧・v2ブラウザチェック、全スタジオ、配布版試験、独自HTMLを実行する。Vueは npm install 後に確認。Screen BuilderでHTML/JS/Vue SFCを出力し別アプリで利用する。スマホ幅・キーボード・タッチ・支援技術・ブラウザ差分を確認する。

判定: ロードマップ全段階の実装を追加済み。ロジック確認は合格、実動作・配布・実機の最終受入は保留。PRはドラフトとし、マージや公開は行わない。野獣大富豪への導入は対象外。


> v2.0.1のレビュー修正後の検証結果は REVIEW_RESPONSE.md を参照。本書はv2.0.0時点の記録。
