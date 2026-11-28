# Macky UI v2.0.0 基本・詳細設計書（追加仕様）

対象: ブラウザ向け独立部品・開発環境。野獣大富豪はデザインの参照元であり、今後も導入対象にしない。全追加ファイルは tools/macky-ui/ 内に配置する。

## 1. 設計目標

1. 部品を関数で呼び出せる。Vue 3 でも同じ部品を使用可能。
2. HTML・JS・Vue開発者が試験、設定、画面構築、配布まで行える。
3. DOM実装とモデルを分離し、アプリの業務データ・API・認証を所有しない。
4. 既存v1 APIを維持する。追加APIは opt-in。ゲーム本体への変更なし。

## 2. 層・モジュール構成

|層|モジュール|責務|
|---|---|---|
|公開|index.js|v1 API、v2再エクスポート|
|モデル|model.js, grid-model.js|検索・ソート、編集履歴、差分、CSV/TSV|
|DOM|dom.js, form.js, advanced-grid.js, virtual-grid.js, widgets.js|DOM生成、イベント、ライフサイクル|
|サービス|services.js|重複実行、離脱警告、通知履歴、ショートカット、再試行、モック|
|テーマ|theme.js, style.css|トークン、プリセット、スコープ|
|メタデータ|registry.js, render.js|部品定義、設定検査、HTML/JS/Vue生成、マウント|
|Vue|vue.js|Vue 3のruntime注入、コンポーネント登録、更新・破棄|
|開発環境|routes.mjs, server.mjs, demo/|ランチャー・Studio・試験|
|配布|build.mjs, package.json|ESM/CSS生成、manifest、npm pack|
|試験|test/, demo/test-v2.html|モデル・構文・DOM・受入|

Node.jsは開発・配布用。ブラウザUIはNodeサーバー上でレンダリングしない。Vueはランタイム注入型で外部バンドル不要。

## 3. Component Builder

34種をregistryに登録（基本、データ、フォーム、ナビゲーション、サービス型）。各項目は label/api/options/group を持ち、既定値は複製して使用する。設定はJSON互換値のみ。スカラー編集と詳細JSON編集を提供し、DOM previewを再生成する。イベントは name/value でログ化し最大60件保持する。JS/Vueコードのコピー、設定JSON、単体実行用HTMLを出力可能。API名・引数キーを画面に表示し、詳細APIは API_V2.md に記載。

信頼済みJS APIは関数・DOM Nodeを受け取れる場合があるが、BuilderのJSONに関数やHTMLコードを埋め込んで評価しない。イベントは既定のログ用関数に接続する。任意の業務イベント処理は生成コードの導入先で実装する。

## 4. フォーム

createField の戻り値は element/input/getValue/setValue/validate/focus/destroy。テキスト・メール・数値・日付・時刻・選択・チェック・ラジオ・ファイルなどに対応。required、minLength/maxLength、min/max、pattern、独自validate関数、ブラウザvalidityを利用。エラーメッセージはaria-describedby/aria-invalid、フォーム検証時は最初の不正項目へフォーカス。createFormはnameの一意性を要求し、Promise onSubmit完了まで二重送信を防止する。サーバー側の検証・認証・重複登録防止は別途必須。

## 5. グリッド設計

### 5.1 3方式

- createGrid: 既存読み取り専用ページ型。後方互換。
- createAdvancedGrid: 編集・範囲選択・行選択・履歴を持つページ型。
- createVirtualGrid: 固定行高の読み取り専用仮想スクロール。表示範囲＋overscanのDOMだけ生成。大量データ向け。編集型と仮想型は別API。

### 5.2 Advanced Grid

行は一意・非nullのrowKeyを要求。行キーは編集不可。列keyは一意・予約キー禁止。内部ストアは初期値/現在値/過去/未来を分離、行の浅いコピーでスカラー値を前提とする。履歴上限は50。変更は複数セルを検証した後、単一トランザクションとしてapplyする。1セルでも不正なら貼り付けを反映しない。Undo/Redoは操作単位。commitで基準を更新し履歴をクリア。resetで基準に戻す。getChangesは added/updated/deleted。

クリック・Shift・ドラッグ範囲選択、Enter/ダブルクリック編集、Delete、Ctrl/Cmd+C/V/Z/Y、行選択・追加・削除、列ソート、全列検索、列フィルター、表示列、列幅（スライダー・ドラッグ）、左固定、条件付き書式、集計（sum/avg/count）、CSVファイル取込・出力を提供する。canEdit(row,column)、canDelete(row)、列readonly/editable、型・必須・独自validateで制御。認可はアプリサーバーでも実施する。複数固定列は左詰めを推奨。範囲操作は現在の表示ページ内に限定し、ページを越える貼り付けは拒否する。CSVは列keyヘッダー・UTF-8・5MB以下。取込は明示的な全件置換機能で編集権限判定とは別の操作。CSV出力では式として評価されやすい文字列にアポストロフィを付加する（完全なExcel安全保証ではない）。既存APIの制約は読み取り専用版にのみ適用。

### 5.3 Virtual Grid

virtualWindow(total,scrollTop,rowHeight,viewportHeight,overscan) は start/end/before/after を算出する。データ100000行でもDOMは可視範囲＋前後overscanのみ。検索・ソートは全件に対して実行。DOM生成時間と実レンダリング時間は別。可変行高、仮想化中のセル編集、サーバーページングは非対応。スクロール位置を前提とする支援技術評価は受入時に行う。

## 6. テーマ

casino/business/cyberpunk/minimal/metal/ocean 6種。色（#RRGGBB）、radius/space/font-size（px範囲）、font（許可リスト）、motion（許可リスト）。applyThemeはrootのCSS変数に適用しrestoreで以前の値に戻す。themeCSSは共通CSS後に読む。Theme Editorは色・余白・フォント等を変更し、localStorageの単一スロット保存・復元、CSS/JSON出力。プリセット・CSS変数を変更する方式で、個別部品の全CSSを生成しない。

## 7. 追加部品・UXサービス

タブ、アコーディオン、サイドバー、ツールチップ、コンテキストメニュー、プログレス、棒/折線グラフ、ツリー、月カレンダー、ウィザード、タイムライン、カンバン、画像ビューア、文書編集、コマンドパレット、再試行を独立APIとして提供。タブのキーボード操作、カンバンの選択メニューによる代替移動、ネイティブ入力を優先する。リッチテキストは簡易装飾とプレーンテキスト出力のみ。ブラウザの旧execCommandを利用するため互換性確認が必要。画像ビューアは明示URLを利用し、任意HTMLを挿入しない。

singleFlightは並行呼出を抑止するが、通信先での冪等性は保証しない。guardUnsavedはbeforeunloadの解除関数を返す（SPA内遷移はアプリで制御）。通知履歴はメモリ保持・上限あり。ショートカットは指定rootに限定し入力欄では発火しない。再試行はユーザー明示操作。mockRequestはテスト用Promiseで実通信をしない。

## 8. Screen Builder

画面スキーマ: {title,theme,components:[{id,type,options,span}]}。部品ID一意、最大100部品、全体1MB、予約キー禁止、既知のtypeのみ。span=1/2（半幅/全幅）。クリック・ドラッグ追加、リスト並べ替え、削除、設定編集、テーマ、幅375/768/autoのプレビュー、イベントログ。localStorage単一スロット保存・復元、JSONインポート/エクスポート。幅切替はレイアウト試験用で実機エミュレーションではない。完全な自由座標やネスト編集は対象外。

HTML出力は独立ESM版またはVueブラウザ版。生成HTMLと同階層のdist/を参照する。ローカルplaygroundでは許可リストによる同等の配信エイリアスを利用。JS出力は npm package を import する mountApp(root,onEvent) 関数。Vue SFC出力は package import・script setup・templateを含む。JSONをscriptへ埋め込む際は < > & U+2028/2029 をエスケープし、script終了タグの注入を防ぐ。画面生成はUIのみであり、業務フロー・認証・永続化・バックエンドは導入先の責務。

## 9. Quality Lab

100/1000/10000/100000行のモックデータ生成、JSON出力、検索・ソート時間・DOM生成時間の実測、ページ型と仮想型の比較、null/空文字/長文/HTML文字列/境界数値、モック通信成功・失敗・タイムアウト・遅延、manifest表示/保存/旧manifestとの差分（追加・削除・SHA変更）、UXサービス例を提供する。測定は端末上で実行し、ブラウザ描画完了やメモリを測るものではない。

## 10. 配布・互換

npm run build は14個のESM/CSS資産をdistへ配置し、SHA-256 manifestを生成。npm pack はprepackでビルド、package files許可リストにより配布資産と文書だけを収録。Node18以上、Vue3.5系のデモはnpm installが必要。Node22推奨。ブラウザは現行Chrome/Edge/Firefox/Safariを対象候補とするが実機互換性評価は未完了。バンドル、minify、TypeScript宣言、ネイティブAndroid/iOS部品、SSRは非対象。

## 11. 安全・品質・運用

- mky- スコープ、textContent、JSONのサイズ・キー検証、テーマトークン許可リスト。
- 開発HTTPはlocalhost専用。固定許可ルートとplayground直下の許可拡張子。サブフォルダ/シンボリックリンク/任意ファイルは拒否（dist/vendorは固定エイリアス）。
- APIで外部入力を受け取るときはアプリ側でも検証する。信頼済み開発者が生成・保存したHTMLを実行する。未信頼HTMLの実行サンドボックスではない。
- DOMイベントの解除が必要な部品はdestroyを返す。VueラッパーはbeforeUnmountでdestroyする。Vue options更新時は再生成し、状態保持は親側で管理する。
- テストは旧版、v2モデル、v2 DOM、Vue、配布版、実機・アクセシビリティを分離。実際に実行できた項目のみ合格とする。
- 本ライブラリの保管場所はtools/macky-ui。野獣大富豪のJava/Android/iOS/ゲームHTML/既存ビルドに変更なし。
