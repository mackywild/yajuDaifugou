// Serializable design schema; no eval or executable user input.
const rowData=[{id:1,name:"サンプルA",score:12},{id:2,name:"サンプルB",score:28}];
const cols=[{key:"id",label:"ID",readonly:true},{key:"name",label:"名称"},{key:"score",label:"数量",type:"number",aggregate:"sum"}];
const choices=[{label:"選択肢A",value:"a"},{label:"選択肢B",value:"b"}];
const sampleItems=[{id:"home",label:"ホーム"},{id:"settings",label:"設定"}];
const entry=(label,api,options,group="基本")=>({label,api,options,group});
export const registry={
 button:entry("ボタン","createButton",{label:"実行",variant:"primary",size:"md",disabled:false,busy:false}),
 panel:entry("パネル","createPanel",{title:"見出し",description:"説明",content:"コンテンツ"}),
 badge:entry("バッジ","createBadge",{label:"完了",type:"success"}),
 menu:entry("メニュー","createMenu",{items:sampleItems,activeId:"home",orientation:"horizontal"}),
 grid:entry("読み取りグリッド","createGrid",{rows:rowData,columns:cols,pageSize:5},"データ"),
 virtualGrid:entry("仮想スクロールグリッド","createVirtualGrid",{rows:rowData,columns:cols,height:240},"データ"),
 advancedGrid:entry("高機能グリッド","createAdvancedGrid",{rows:rowData,columns:cols,pageSize:10},"データ"),
 popup:entry("確認ダイアログ","ui.confirm",{message:"実行しますか？",title:"確認"}),
 toast:entry("トースト","ui.toast",{message:"完了しました",type:"success",duration:3000}),
 banner:entry("バナー","ui.banner",{message:"お知らせ",title:"NOTICE",duration:1600}),
 loading:entry("ローディング","ui.withLoading",{message:"読み込み中",duration:1000}),
 field:entry("入力欄","createField",{name:"name",label:"名前",type:"text",required:true,placeholder:"入力してください"},"フォーム"),
 select:entry("選択欄","createSelect",{name:"category",label:"分類",options:choices,value:"a"},"フォーム"),
 checkbox:entry("チェックボックス","createCheckbox",{name:"agree",label:"同意する",value:false},"フォーム"),
 radio:entry("ラジオ","createRadio",{name:"choice",label:"選択",options:choices,value:"a"},"フォーム"),
 date:entry("日付","createDatePicker",{name:"date",label:"日付"},"フォーム"),
 file:entry("ファイル選択","createFilePicker",{name:"file",label:"ファイル",multiple:false},"フォーム"),
 form:entry("フォーム","createForm",{fields:[{name:"name",label:"名前",required:true},{name:"email",label:"メール",type:"email",required:true}],submitLabel:"保存"},"フォーム"),
 tabs:entry("タブ","createTabs",{items:[{id:"a",label:"概要",content:"概要の内容"},{id:"b",label:"詳細",content:"詳細の内容"}]},"ナビゲーション"),
 accordion:entry("アコーディオン","createAccordion",{items:[{label:"詳細",content:"開閉できる内容",open:true}]},"ナビゲーション"),
 sidebar:entry("サイドバー","createSidebar",{items:sampleItems,activeId:"home"},"ナビゲーション"),
 tooltip:entry("ツールチップ","createTooltip",{label:"ヘルプ",text:"補足説明です"},"ナビゲーション"),
 context:entry("右クリックメニュー","createContextMenu",{items:sampleItems},"ナビゲーション"),
 tree:entry("ツリー","createTree",{items:[{label:"フォルダ",open:true,children:[{id:"file",label:"ファイル"}]}]},"データ"),
 progress:entry("プログレス","createProgress",{value:65,max:100,label:"進捗"},"データ"),
 chart:entry("グラフ","createChart",{label:"月別集計",type:"bar",data:[{label:"4月",value:12},{label:"5月",value:28},{label:"6月",value:20}]},"データ"),
 calendar:entry("カレンダー","createCalendar",{year:2026,month:11,events:[{date:"2026-11-28",label:"予定"}]},"データ"),
 wizard:entry("ウィザード","createWizard",{steps:[{label:"入力",content:"入力内容"},{label:"確認",content:"確認内容"}]},"ナビゲーション"),
 timeline:entry("タイムライン","createTimeline",{items:[{date:"2026-11-28",title:"作成",description:"履歴を記録"}]},"データ"),
 kanban:entry("カンバン","createKanban",{columns:[{id:"todo",label:"未着手"},{id:"done",label:"完了"}],cards:[{id:"1",columnId:"todo",title:"タスクA"}]},"データ"),
 image:entry("画像ビューア","createImageViewer",{src:"",alt:"画像"},"基本"),
 richText:entry("文書編集","createRichText",{value:"テキストを編集"},"フォーム"),
 command:entry("コマンドパレット","createCommandPalette",{items:sampleItems},"ナビゲーション"),
 retry:entry("再試行パネル","createRetryPanel",{message:"処理に失敗しました"},"基本")
};
export function safeJSON(value){
 const str=JSON.stringify(value);
 if(str===undefined)throw Error("JSON value required");
 return str.replace(/[<>&\u2028\u2029]/g,ch=>"\\u"+ch.charCodeAt(0).toString(16).padStart(4,"0"));
}
export function cloneOptions(v){
 const raw=safeJSON(v);if(raw.length>250000)throw Error("設定は250KBまで");
 return JSON.parse(raw,(k,v)=>{if(["__proto__","constructor","prototype"].includes(k))throw Error("予約キーは使用できません");return v;});
}
export function getDefaults(type){if(!Object.hasOwn(registry,type))throw Error("Unknown component");return cloneOptions(registry[type].options);}
export function normalizeScreen(screen){
 if(!screen||typeof screen!=="object"||!Array.isArray(screen.components)||screen.components.length>100)throw Error("画面定義は100部品まで");
 const ids=new Set();
 const components=screen.components.map((c,i)=>{if(!Object.hasOwn(registry,c.type))throw Error("Unknown component: "+c.type);const id=String(c.id??i);if(ids.has(id))throw Error("Duplicate component id");ids.add(id);
  const options=cloneOptions(c.options??getDefaults(c.type));if(!options||Array.isArray(options)||typeof options!=="object")throw Error("options must be object");
  return {id,type:c.type,options,span:c.span===1?1:2};});
 const result={title:String(screen.title??"New app").slice(0,200),theme:["casino","business","cyberpunk","minimal","metal","ocean"].includes(screen.theme)?screen.theme:"business",components};
 if(safeJSON(result).length>1_000_000)throw Error("画面定義は1MBまで");return result;
}
export function componentCode(type,options,framework="js"){
 if(!Object.hasOwn(registry,type))throw Error("Unknown component");
 const json=safeJSON(cloneOptions(options));if(framework==="vue")return 'h(kit.MComponent, { type: '+JSON.stringify(type)+', options: '+json+', onAction: (name, value) => console.log(name, value) })';
 return 'const component = api.mountComponent(api, '+JSON.stringify(type)+', '+json+', { ui, onEvent: (name, value) => console.log(name, value) });\nroot.append(component.element);\n// 画面破棄時: component.destroy();';
}
export function screenHTML(screen,framework="js"){
 const data=safeJSON(normalizeScreen(screen));const vue=framework==="vue";
 // HTML can run in playground with /src, or after placement beside dist/ and vendor/.
 const script=vue?
 'import * as Vue from "./vendor/vue.js";\nimport * as api from "./dist/index.js";\nimport { createVueKit } from "./dist/vue.js";\nconst spec = '+data+';\nconst root=document.querySelector("#ui-root"), ui=api.createUI(root,{theme:["business","minimal"].includes(spec.theme)?"light":"yaju"}), kit=createVueKit(Vue,ui);api.applyTheme(root,spec.theme);\nconst app=Vue.createApp({setup(){return ()=>Vue.h("main",{class:"mky-screen"},[Vue.h("h1",spec.title),Vue.h("div",{class:"mky-screen-grid"},spec.components.map(c=>Vue.h("div",{key:c.id,class:c.span===1?"mky-span-1":"mky-span-2"},[Vue.h(kit.MComponent,{type:c.type,options:c.options,onAction:(name,value)=>console.log(name,value)})])))]);}});app.use(kit);app.mount("#app");':
 'import * as api from "./dist/index.js";\nconst spec = '+data+';\nconst root=document.querySelector("#ui-root"),ui=api.createUI(root,{theme:["business","minimal"].includes(spec.theme)?"light":"yaju"});api.applyTheme(root,spec.theme);api.mountScreen(api,root,spec,{ui,onEvent:(name,value)=>console.log(name,value)});';
 return '<!doctype html>\n<html lang="ja"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Macky UI App</title><link rel="stylesheet" href="./dist/style.css"></head><body style="margin:0"><div id="ui-root" style="background:var(--mky-bg);color:var(--mky-fg);min-height:100vh">'+(vue?'<div id="app"></div>':'')+'</div><script type="module">\n'+script+'\n</script></body></html>\n';
}

export function screenJS(screen){
 const data=safeJSON(normalizeScreen(screen));
 return 'import * as api from "@mackywild/macky-ui";\nimport "@mackywild/macky-ui/style.css";\nconst spec = '+data+';\nexport function mountApp(root,onEvent=(name,value)=>console.log(name,value),submitTask) {\n  const ui=api.createUI(root,{theme:["business","minimal"].includes(spec.theme)?"light":"yaju"});\n  const theme=api.applyTheme(root,spec.theme);\n  const screen=api.mountScreen(api,root,spec,{ui,onEvent,submitTask});\n  return {destroy(){screen.destroy();theme.restore();ui.destroy();}};\n}\n';
}
export function screenVue(screen){
 const data=safeJSON(normalizeScreen(screen));
 return '<script setup>\nimport * as Vue from "vue";\nimport * as api from "@mackywild/macky-ui";\nimport { createVueKit } from "@mackywild/macky-ui/vue";\nimport "@mackywild/macky-ui/style.css";\nconst spec = '+data+';\nconst root = Vue.ref(null), kit = Vue.shallowRef(null);\nconst emit = defineEmits(["action"]);\nconst props = defineProps({ submitTask: Function });\nlet ui, theme;\nVue.onMounted(() => {\n  ui = api.createUI(root.value, {theme:["business","minimal"].includes(spec.theme)?"light":"yaju"});\n  theme = api.applyTheme(root.value,spec.theme);\n  kit.value = createVueKit(Vue,ui);\n});\nVue.onBeforeUnmount(() => {theme?.restore();ui?.destroy();});\n</script>\n<template>\n  <div ref="root" style="background:var(--mky-bg);color:var(--mky-fg)">\n    <main v-if="kit" class="mky-screen">\n      <h1>{{ spec.title }}</h1>\n      <div class="mky-screen-grid">\n        <div v-for="c in spec.components" :key="c.id" :class="c.span === 1 ? \'mky-span-1\' : \'mky-span-2\'">\n          <component :is="kit.MComponent" :type="c.type" :options="c.options" :submit-task="values => props.submitTask?.(values,c.id)" @action="(name,value) => emit(\'action\',name,value,c.id)" />\n        </div>\n      </div>\n    </main>\n  </div>\n</template>\n';
}
