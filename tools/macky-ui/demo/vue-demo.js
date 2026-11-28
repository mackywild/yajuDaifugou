import * as Vue from "/vendor/vue.js";
import {createUI} from "../src/index.js";
import {createVueKit} from "../src/vue.js";
const ui=createUI(document.querySelector("#root")),kit=createVueKit(Vue,ui);
const app=Vue.createApp({setup(){
 const service=kit.useUI(),count=Vue.ref(0),active=Vue.ref("home");
 const menu=Vue.computed(()=>({items:[{id:"home",label:"ホーム"},{id:"record",label:"戦績"}],activeId:active.value,onSelect:id=>{active.value=id;}}));
 const rows=Vue.ref([{id:1,name:"大富豪",score:100},{id:2,name:"平民",score:30}]);
 const grid=Vue.computed(()=>({rows:rows.value,columns:[{key:"id",label:"ID"},{key:"name",label:"名前"},{key:"score",label:"スコア"}],onRowAction:r=>service.alert(r.name)}));
 return {count,active,menu,grid,add(){count.value++;rows.value.push({id:rows.value.length+1,name:"新規",score:count.value});service.toast("追加しました",{type:"success"});},turn:()=>service.banner("あなたの番です！",{title:"YOUR TURN",type:"warning"}),theme:()=>service.setTheme("light")};
 },template:'<header><div><div class="eyebrow">MACKY UI / VUE 3</div><h1>Vue Component Lab</h1><a href="/">JavaScript版に戻る</a></div><MButton label="ライトテーマ" variant="secondary" @click="theme"/></header><MMenu :options="menu"/><div class="layout"><MPanel title="リアクティブな部品"><div class="actions"><MButton @click="add">追加 {{count}}</MButton><MButton label="ターン通知" variant="secondary" @click="turn"/><MBadge :options="{label:active,type:\'success\'}"/></div></MPanel><div class="wide"><MGrid :options="grid"/></div></div>'});
app.use(kit);app.mount("#app");
