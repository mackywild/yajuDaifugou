# Macky UI 1.0.0

野獣大富豪の黒・深緑・金を継承した、ブラウザ向け独立UIライブラリ。Vue 3対応、Vanilla JS対応、Node.jsローカル開発環境。既存ゲームへの組み込みはこの変更では行わない。

## 起動（Node.js 18以上、22 LTS系を推奨）

    cd tools/macky-ui
    npm run check
    npm test
    npm start

http://127.0.0.1:4173/ を開く。Vanilla版は npm install 不要。Vue版を試す場合は別途 npm install を実行して /demo/vue.html を開く。外部CDN不要。ブラウザ自動チェックは /demo/test.html。サーバーは開発専用・ループバック限定。

## 最短の使い方

CSSを読み込み、DOMの準備後に初期化する。コピー配置の場合は src/ 以下の4ファイルを同じ構成で配置する。HTMLを file:// で直接開かずHTTPで配信する。

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

公開npmパッケージではない。外部プロジェクトでは src/ コピー配置、またはローカル依存として取り込む。ゲームを直接変更せず、安全に段階導入する。
