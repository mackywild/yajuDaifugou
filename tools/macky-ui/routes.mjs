export const builtinPages=[
  {title:"v2 ブラウザ受入試験",description:"34部品のマウント・フォーム・高機能グリッド・仮想スクロール・画面生成",url:"/demo/test-v2.html",group:"integration"},
  {"title":"Component Builder","description":"部品設定・プレビュー・コード生成・イベントログ","url":"/demo/studio.html?mode=component","group":"studio"},
  {"title":"Screen Builder","description":"画面構築・保存・HTML/JSON出力","url":"/demo/studio.html?mode=screen","group":"studio"},
  {"title":"Theme Editor","description":"テーマ6種・CSS/JSON出力","url":"/demo/studio.html?mode=theme","group":"studio"},
  {"title":"Quality Lab","description":"性能・異常系・モック通信・ビルド情報","url":"/demo/studio.html?mode=quality","group":"studio"},
  {title:"全コンポーネント",description:"すべての基本部品をまとめて確認",url:"/demo/index.html",group:"catalog"},
  {title:"ボタン",description:"種類・無効・処理中",url:"/demo/index.html?component=buttons",group:"catalog"},
  {title:"ポップアップ・通知",description:"確認・警告・バナー・ローディング",url:"/demo/index.html?component=popup",group:"catalog"},
  {title:"ステータス",description:"バッジと状態表示",url:"/demo/index.html?component=status",group:"catalog"},
  {title:"データグリッド",description:"検索・ソート・ページ送り",url:"/demo/index.html?component=grid",group:"catalog"},
  {title:"メニュー",description:"横型・縦型・選択・無効",url:"/demo/index.html?component=menu",group:"catalog"},
  {title:"Vue 3 結合試験",description:"リアクティブ更新（npm install が必要）",url:"/demo/vue.html",group:"integration"},
  {title:"自動ブラウザ試験",description:"PASS / FAIL を確認",url:"/demo/test.html",group:"integration"},
  {title:"ビルド済み部品試験",description:"dist を使用（npm run build が必要）",url:"/demo/built.html",group:"integration"}
];
export const safeName=name=>typeof name==="string"&&/^[\p{L}\p{N}_-][\p{L}\p{N}_. -]*\.(html|js|mjs|css)$/iu.test(name)&&!name.includes("..");
export function customPages(entries){
  return entries.filter(e=>e.isFile()&&safeName(e.name)&&/\.html$/i.test(e.name)).map(e=>({
    title:e.name,description:"ローカルの独自試験画面",url:"/playground/"+encodeURIComponent(e.name),group:"custom"
  })).sort((a,b)=>a.title.localeCompare(b.title,"ja"));
}
const allowed=new Set(["demo/test-v2.html","demo/browser-test-v2.js","src/virtual-grid.js","dist/virtual-grid.js","demo/studio.html","demo/studio.js","demo/studio.css","src/dom.js","dist/dom.js","src/form.js","dist/form.js","src/grid-model.js","dist/grid-model.js","src/advanced-grid.js","dist/advanced-grid.js","src/theme.js","dist/theme.js","src/widgets.js","dist/widgets.js","src/services.js","dist/services.js","src/registry.js","dist/registry.js","src/render.js","dist/render.js","demo/launcher.html","demo/launcher.js","demo/index.html","demo/demo.js","demo/vue.html","demo/vue-demo.js","demo/demo.css","demo/test.html","demo/browser-test.js","demo/built.html","demo/built.js","src/index.js","src/model.js","src/vue.js","src/style.css","dist/index.js","dist/model.js","dist/vue.js","dist/style.css","dist/manifest.json"]);
export function resolveRoute(path){
  if(path==="/")return "demo/launcher.html";
  if(path.startsWith("/playground/dist/")&&allowed.has(path.slice("/playground/".length)))return path.slice("/playground/".length);
  if(path==="/playground/vendor/vue.js")return "node_modules/vue/dist/vue.esm-browser.js";
  if(path==="/vendor/vue.js")return "node_modules/vue/dist/vue.esm-browser.js";
  if(allowed.has(path.slice(1))&&path.startsWith("/"))return path.slice(1);
  if(path.startsWith("/playground/")){
    try{const name=decodeURIComponent(path.slice("/playground/".length));if(safeName(name))return "playground/"+name;}catch{}
  }
  return null;
}
