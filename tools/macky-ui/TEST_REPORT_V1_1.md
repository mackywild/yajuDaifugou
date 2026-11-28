# Macky UI 1.1.0 追加試験結果

対象実装: 96ee09b4edd17d8b530e1abbaaa8e51052e3cee9
実施日: 2026-11-28

## 実施済み

Node.js v22.16.0 / npm 10.9.2。

|項目|結果|補足|
|---|---|---|
|ルート定義・組み込み9画面の許可リスト|PASS|routes.test.js|
|独自HTML・日本語ファイル名・拡張子制限|PASS|routes.test.js|
|任意ファイル参照・パス横断の拒否|PASS|routes.test.js|
|npm run build / manifest生成|PASS（fixture）|同一build.mjs、package.jsonを使用し、仮のESM/CSS資産で実行|
|npm pack --offline / prepack|PASS（fixture）|tgzにdist資産4点、manifest、package.json、設計・READMEの計8点|
|別プロジェクトへのtgz install / ESM import / CSS export解決|PASS（fixture）|ネットワーク不要、runtime依存なし|

routes.mjs、test/routes.test.js、build.mjs、package.jsonの実行用コピーとGitHub上のblob SHAが一致することを確認した。ルート試験は3件中3件合格。ビルド/配布の確認は実際の部品ソースではなく、仮資産を使用して仕組みのみ検証した。

## 未実施

ネットワーク制限によりGitHubからリポジトリ全体をローカル取得できなかったため、実装全体の npm run check / npm test（追加build.test.jsを含む）、実部品での npm run build / npm pack、HTTP配信、ランチャーのブラウザ操作、独自HTML追加、Vue実行、ビルド済み画面の実動作は未確認。手元のクローンで実施する。

## 受入手順

1. cd tools/macky-ui
2. npm run check && npm test
3. npm run build && npm pack
4. npm start、http://127.0.0.1:4173/ を開く。
5. 9種類の固定画面と独自HTML example.html を選択。playgroundに別HTMLを追加し一覧再読込。配布版画面で通知と一覧を操作。
6. npm install を行いVue 3結合試験を実施。
7. 別のWeb/Vueアプリにtgzを導入し、実部品とCSSを確認。

野獣大富豪は参考元のみで導入対象ではない。旧版の試験報告書にあるゲーム導入に関する記載は現行方針では無効。
