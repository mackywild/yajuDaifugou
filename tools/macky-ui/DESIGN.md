# Macky UI 基本・詳細設計書

|項目|内容|
|---|---|
|版|1.1.0|
|目的|クライアント部品再利用・開発工数削減・デザイン統一|
|対象|Webブラウザ、Vue 3、Vanilla JavaScript|
|非対象|ゲーム進行、Spring API、認証、Androidネイティブ、iOSネイティブ|
|作成日|2026-11-28|

## 1. 現行調査・方針

main の b863de8dbb409a7c841498b819fb460bdee533b1 時点の src/main/resources/static/index.html は約2300行。CSS、HTML、JS一体型。root色は #020403 / #0d1712 / #d8b85a。toast(text,error)、turn-banner、menu-grid 等の個別実装がある。野獣大富豪は参考元のみであり、現在・将来とも導入計画の対象外とする。ゲームデータ・ルール・API・配布物・既存ビルドは変更しない。

## 2. 要求と方式

|ID|要求|方式|
|---|---|---|
|UI01|ボタン共通化|4 variant、3 size、disabled、busy、click|
|UI02|ポップアップ|ネイティブ dialog、Promise、確認/通知、FIFO|
|UI03|非モーダル演出|toast と banner、時間制御と個別終了|
|UI04|一覧|検索、昇降順、ページング、空表示、行操作|
|UI05|メニュー|横/縦、現在選択、無効、コールバック|
|UI06|共通補助|panel、badge、loading、テーマ|
|UI07|Vue対応|Vue 3 runtime を注入し、コンポーネントとして登録|
|UI08|既存影響防止|tools 配下だけ追加、mky- CSS、ルートスコープ|

層構造:
App -> Vue adapter（任意） -> DOM API / UI service -> pure model / CSS。
Nodeはブラウザ部品のサーバーサイド実行を目的としない。Nodeで開発サーバーとテストを実行する。SSR非対応。

## 3. ディレクトリ・責務

- src/index.js: DOM生成、サービス初期化、通知キュー、ライフサイクル
- src/model.js: buttonClass, gridView, durationMs。DOM非依存
- src/vue.js: createVueKit、コンポーネント登録、Vueのref/watch、unmount
- src/style.css: デザイントークン、コンポーネントの見た目
- demo/: 操作可能なカタログ、Vueとブラウザチェック
- server.mjs: ローカル専用静的HTTPサーバー。許可リスト・GET/HEAD限定
- test/: Node単体試験

## 4. 外部インターフェース

|関数|戻り値|仕様|
|---|---|---|
|createUI(root, {theme})|UI service|HTMLElement必須、rootにつき1つ|
|createButton(options)|HTMLButtonElement|label, variant, size, disabled, busy, ariaLabel, onClick(event)|
|createMenu(options)|HTMLElement|items: {id,label,disabled}[], activeId, label, orientation, onSelect(id,item)|
|createGrid(options)|{element,update,destroy}|rows, columns, searchable, pageSize, label, emptyMessage, actionLabel, onRowAction(row)|
|createPanel(options)|HTMLElement|title, description, content: Node または文字列|
|createBadge(options)|HTMLElement|label, type|
|ui.toast(message, options)|{element,close}|type, duration(ms)、既定3500、最大4件|
|ui.banner(message, options)|{element,close}|title,type,duration、既定1600、最新1件|
|ui.popup(options)|Promise boolean|title,message,okLabel,cancelLabel,cancelable,type|
|ui.alert(message,options)|Promise boolean|OKのみ|
|ui.confirm(message,options)|Promise boolean|OK=true、キャンセル/Esc=false|
|ui.showLoading(message)|{close}|表示専用、複数処理を管理、操作遮断なし|
|ui.withLoading(task,message)|Promise task result|成功・例外ともfinallyで終了、例外を再送出|
|ui.setTheme(theme)|void|yaju / light|
|ui.destroy()|void|通知、タイマー、キュー解放。未解決ダイアログはfalse|
|createVueKit(Vue,ui)|Vue plugin + components + useUI|同じUI serviceを注入|

type は info / success / warning / danger。未定義のtypeはinfo。duration=0は手動終了（bannerは次の表示でも終了）。最大60000ms。variantはprimary/secondary/danger/ghost。sizeはsm/md/lg。メニューIDは一意であること。

### 4.1 グリッド

columns は key（文字列・一意）、label、sortable（既定true）、searchable（既定true）、value(row)、format(value,row)。valueで計算列を作れる。formatは表示用文字列に変換するが検索/ソートは元値で実施。検索は前後空白除去・大小文字無視。ソートは数字は数値、その他は日本語Intl.Collator（numeric）、nullは末尾。表示配列のみソートし入力配列は変更しない。ページサイズ1〜200。更新は浅いマージ、検索・ソート・ページを維持し範囲外ページを補正。Vueではoptionsの変更を監視する。編集、コピー/ペースト、仮想スクロール、サーバー側ページングは今回対象外。大量データは別途サーバー側検索等を検討する。

### 4.2 ポップアップ・ローディング

popup request -> FIFO queue -> showModal -> OK/cancel/Esc -> close -> Promise settle -> next.
同時に表示するmodalはサービスごと1つ。破棄時はfalse。showModal非対応・表示失敗はreject。呼出側は例外処理する。Escを禁止するalertはOKで閉じる。背景クリックでは閉じない。ダイアログの直前フォーカスへ復帰。ネイティブdialogを使用するためモダンブラウザが対象。

bannerはターン演出等、非モーダルで手札操作を止めない。同じターンの重複通知防止はアプリの責任。loadingは並行処理ごとのハンドルを返し、後発優先で表示、最後の処理終了で消える。処理のキャンセルや排他制御はしない。

### 4.3 Vue設計

MButtonはlabel/variant/size/disabled/busy、default slot、click emit。MPanelはtitle/descriptionとslot。MGrid/MMenu/MBadgeはoptions。MGridはmounted後にDOM生成、options更新を反映、beforeUnmountでdestroy。MMenu/MBadgeはoptions変更時に置換する。MMenuのactiveIdは親管理。createUIのrootはVue mount先の親にする。アプリ破棄時に明示的にui.destroyする。

## 5. 非機能・デザイン

- CSSは .mky-ui 配下の mky- クラスに限定。既存の body / button / input などのグローバル定義を上書きしない。
- dark: 黒・深緑・金、light: 白・濃緑。CSS変数で上書き可能。テーマはroot単位。
- レスポンシブ: ダイアログ最大幅460px、グリッド横スクロール、ボタン折返し。
- アクセシビリティ: ネイティブbutton/dialog、aria-label、aria-sort、aria-current、通知status/alert、フォーカス可視化、reduced-motion対応。WCAG準拠認証や支援技術実機評価は未実施。
- セキュリティ: 表示文字列はtextContent。innerHTMLやeval、外部CDNなし。createPanelのNode、コールバック、formatはアプリ提供の信頼済みコードのみ。サーバーの認証・CSRF・入力検証はアプリが実施。
- 通信、ストレージ、グローバルwindowへの登録なし。複数rootは利用可能だが、複数rootのモーダル競合を避けるため1画面1サービス推奨。
- 通知はrootがtransform/overflow等で固定配置を制限しない位置に設ける。通知のz-indexは .mky-layers の --mky-layer で調整。ネイティブdialogはtop layer。
- 外部依存: Vueデモのみnpm installが必要。Nodeサーバーは標準ライブラリ。npm公開・CJS・SSR・旧IE対応は非対象。

## 6. 独立開発・配布設計

### 6.1 Test Studio
GET / はランチャーHTML。GET /api/test-pages は固定試験画面9件と playground 直下のHTML一覧を返す。検索と再読込で選択可能。各画面は通常の別ページ遷移で実行する。部品別は component パラメータを既定の5種に限定し、対象部品のみ生成する。独自HTMLは信頼済みローカル開発者のみが配置する。ファイル名の許可文字・拡張子を制限し、ディレクトリ横断・任意ファイル・symlinkを許可しない。HTTPサーバーは127.0.0.1のみ。外部公開禁止。

### 6.2 ビルド
Node標準ライブラリで src の index.js/model.js/vue.js/style.css を dist へコピーし、manifest.jsonに版・エントリ・バイト数・SHA-256を記録する。ネイティブESM形式であり、単一ファイルbundle/minifyではない。Vueランタイムは注入型なので配布物には同梱不要。npm pack がprepackでbuildを実行し、distおよび説明文書のみ含むtgzを生成。package exports は dist に向ける。private=trueで誤publishを防止。生成物はGit対象外。開発環境はsrcを利用し、配布環境はdistを利用する。

### 6.3 他アプリへの導入
配布物を静的資産として配置するか、ローカルtgzをnpm installする。Viteなどで最終アプリ側のbundleを作成する。WebView/Electronでは各アプリの資産配信方式およびモダンブラウザ機能を確認する。Android/iOSのネイティブUIへの直接利用は非対象。野獣大富豪のソース・API・配布・ビルドには変更を加えない。

## 7. 試験計画

|ID|確認|手段|
|---|---|---|
|T01|ソート・検索・境界・不変性|npm test|
|T02|構文|npm run check|
|T03|button/disabled、文字列XSS対策|/demo/test.html|
|T04|一覧検索・ソート・ページ・空表示|/demo/test.html|
|T05|メニュー選択・disabled|/demo/test.html|
|T06|dialog FIFO・OK・キャンセル・破棄|/demo/test.html|
|T07|通知の終了・banner置換|/demo/test.html|
|T08|並行loading・例外時解放|/demo/test.html|
|T09|Vue reactive更新/ボタン/通知|/demo/vue.html 手動|
|T10|Tab/Esc、フォーカス復帰、モバイル幅、両テーマ|実ブラウザ手動|
|T11|ランチャー一覧・パス制限|npm test / 実ブラウザ|
|T12|ビルド成果物・整合性・エントリ|npm test / npm run build|
|T13|配布版の画面・npmパッケージ導入|/demo/built.html / 別アプリで確認|

実行環境のない状態では合格扱いにしない。試験記録はPRで区別する。設計上の制約としてライブラリは独立配置、他のアプリへの導入およびブラウザ実機検証は別工程。野獣大富豪への導入は行わない。

## 8. 完了・運用基準

新機能は部品APIに後方互換で追加する。破壊的変更はメジャー版。新しい部品はAPI、カタログ、試験を同時追加する。アプリ固有ロジックは部品に入れない。v1はローカル配布用であり、公開npm登録、TypeScript宣言、キーボード操作の高度なデータグリッドは次期候補。
