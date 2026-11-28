# Macky UI 2.0.0

野獣大富豪の黒・深緑・金を継承した、ブラウザ向け独立UIライブラリ。Vue 3対応、Vanilla JS対応、Node.jsローカル開発環境。野獣大富豪は参照元のみ。現在も将来も同ゲームへの導入を前提にしない。

## 起動（Node.js 18以上、22 LTS系を推奨）

    cd tools/macky-ui
    npm run check
    npm test
    npm start

http://127.0.0.1:4173/ を開くと Test Studio が起動し、部品別・Vue・自動試験・独自HTML・配布版試験を選択できる。Vanilla版は npm install 不要。Vue版を試す場合は別途 npm install を実行して /demo/vue.html を開く。外部CDN不要。ブラウザ自動チェックは /demo/test.html。サーバーは開発専用・ループバック限定。

## 最短の使い方

CSSを読み込み、DOMの準備後に初期化する。コピー配置の場合は dist/ 以下の全ファイルを同じ構成で配置する。HTMLを file:// で直接開かずHTTPで配信する。

    <link rel="stylesheet" href="./macky-ui/style.css">
    <div id="ui-root"><div id="actions"></div></div>
    <script type="module">
      import { createUI, createButton } from "./macky-ui/index.js";
      const ui = createUI(document.querySelector("#ui-root"));
      document.querySelector("#actions").append(createButton({
        label: "保存",
        onClick: async () => {
          if (await ui.confirm("保存しますか？")) {
            // ここでアプリ側の保存処理を実行する。
            ui.toast("保存しました", { type: "success" });
          }
        }
      }));
    </script>

    ui.banner("あなたの番です！", { title: "YOUR TURN", duration: 1600 });
    ui.alert("通信状態を確認してください", { title: "お知らせ" });
    await ui.withLoading(() => fetch("/api/example"), "通信中…");
    ui.setTheme("light");

注意: fetch のHTTPエラー判定、認証、保存処理はアプリ側の責任。withLoadingは表示と後始末だけを担当する。

## Vue 3

npm環境で Vue をインポートし、ライブラリのCSSも読み込む。createUI の root は Vue マウント先の親にする（同じ要素にするとマウントでレイヤーが消える）。

    import * as Vue from "vue";
    import {createUI} from "./macky-ui/index.js";
    import {createVueKit} from "./macky-ui/vue.js";
    const ui = createUI(document.querySelector("#ui-root"));
    const kit = createVueKit(Vue, ui);
    const app = Vue.createApp({
      setup() {
        const ui = kit.useUI();
        return () => Vue.h(kit.MButton, { label: "実行", onClick: () => ui.toast("完了") });
      }
    });
    app.use(kit);
    app.mount("#app");
    // アプリ終了時は app.unmount(); ui.destroy();

MButton, MPanel, MGrid, MMenu, MBadge を登録。MGrid/MMenu/MBadge は options オブジェクトを受け取る。MButton は click を emit。MPanel は default slot に対応。

## 詳細

- DESIGN.md: 基本・詳細設計、API、移行・テスト設計
- src/index.js: DOM部品と表示サービス
- src/model.js: 純粋関数
- src/vue.js: Vueアダプター
- src/style.css: 共通テーマ
- demo/: 実行可能なカタログ、Vue例、ブラウザチェック
- test/: Node単体試験

## 独自HTMLの試験

playground/example.html と example.js をコピーして自由に開発する。playground 直下に HTML を追加して Test Studio の「一覧を再読込」を押すと自動で選択肢に追加される。HTML・JS・MJS・CSSのみ同フォルダから配信する。サブフォルダ、任意PCファイル、シンボリックリンクは対象外。日本語ファイル名も利用可能。

## ビルド・配布

    npm run build
    npm pack

build は Node 標準機能のみで dist/ に14個のネイティブESM/CSS資産とSHA-256付きmanifestを生成する。トランスパイル・minify・単一ファイルbundleではない。別アプリのVite等で最終bundle化できる。npm pack は prepack で自動ビルドし、mackywild-macky-ui-2.0.0.tgz を生成する。公開npm登録は行わない。dist と tgz はGit管理対象外。Vueデモ以外は npm install 不要。

### 他のアプリでの利用

方法A: dist/ の全ファイルを別のWebアプリの静的資産フォルダに配置し、index.js / style.css を読み込む。Vue連携の場合は vue.js も利用する。

方法B: 別のNode/Vueプロジェクトでローカルパッケージを導入。

    npm install /absolute/path/to/mackywild-macky-ui-2.0.0.tgz

    import { createUI, createButton } from "@mackywild/macky-ui";
    import { createVueKit } from "@mackywild/macky-ui/vue";
    import "@mackywild/macky-ui/style.css";

Vueランタイムは導入先の Vue 3 を createVueKit に渡す（同梱しない）。WebView、Electron等のWebベースアプリにもそのアプリの資産配信方式に従って配置可能。ネイティブAndroid/iOS部品ではない。WebViewの実機互換は導入先で確認する。

### ビルド後の確認

npm start の後 Test Studio の「ビルド済み部品試験」を選択。dist から core/Vue adapter/manifest をロードする。配布版のボタン・通知・グリッドを試す。実際のVue連携はVue 3結合試験で確認する。

野獣大富豪は参考元のみ。ゲーム本体には導入しない。


## v2.0 Development Studio

起動後トップに4つのスタジオが表示される。

- Component Builder: 34種の部品を選び、スカラー引数とJSONを編集。プレビュー、イベントログ、JS/Vueコード、設定JSON・実行用HTML出力。
- Screen Builder: クリック・ドラッグで追加、並べ替え、半幅/全幅、テーマ選択、プレビュー。localStorage保存・復元、JSON入出力、HTML/JS/Vue SFC出力。業務ロジック/APIは生成しない。生成HTMLを playground 直下に保存して再読込すればテスト可能（先に npm run build、Vue は npm install）。別アプリで使う場合は HTML と同じ階層に dist/ を置く。VueスタンドアロンHTMLは vendor/vue.js にVue ESMブラウザ版を配置。JS/SFC出力は npm pack したライブラリを導入先に install。
- Theme Editor: 6プリセット、色・角丸・余白・フォント・文字サイズ・モーション。localStorage保存、CSS/JSON出力。テーマCSSは共通CSSの後に読み込む。
- Quality Lab: 100〜100000行のモデル処理・DOM生成計測、ページ型/仮想スクロール、異常値、モック通信（遅延/失敗/タイムアウト）、manifest確認、通知履歴・離脱警告・ショートカット例。描画完了時間・メモリは測定対象外。

### API追加例

    import * as api from "@mackywild/macky-ui";
    const ui = api.createUI(root, {theme:"light"});
    api.applyTheme(root,"business");
    const form=api.createForm({fields:[{name:"name",label:"名称",required:true}],onSubmit:async values=>console.log(values)});
    root.append(form.element);
    const grid=api.createAdvancedGrid({rows:[{id:1,name:"製品"}],columns:[{key:"id",readonly:true},{key:"name",required:true}],newRow:rows=>({id:Math.max(0,...rows.map(r=>r.id))+1,name:""}),onChange:(rows,changes)=>console.log(changes)});
    root.append(grid.element);
    // 画面終了時: grid.destroy(); form.destroy(); ui.destroy();

Vueは既存の MButton/MGrid 等に加え MComponent(type/options/@action) と25種の名前付き部品を利用可能（合計31登録）。MComponent の options はJSON設定専用。イベントは @action で受ける。直接APIでは独自のコールバックを指定できる。Vue options変更時は部品を再生成するため、編集途中の値を保持するにはアプリ側で管理する。

npm test は従来テストと v2.test.js を実行。ブラウザは Test Studio の「自動ブラウザ試験」と「v2 ブラウザ受入試験」を実行。

詳細は DESIGN_V2.md、API_V2.md、ROADMAP.md、TEST_REPORT_V2.md。
