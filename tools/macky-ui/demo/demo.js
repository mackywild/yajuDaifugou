import {createUI,createButton,createPanel,createMenu,createGrid,createBadge} from "../src/index.js";
const root=document.querySelector("#root"),ui=createUI(root),content=document.querySelector("#content");
let light=false,active="home";
document.querySelector("#theme").append(createButton({label:"テーマ切替",variant:"secondary",onClick:()=>{light=!light;ui.setTheme(light?"light":"yaju");}}));
const menu=document.querySelector("#menu");
const items=[{id:"home",label:"ホーム"},{id:"records",label:"戦績"},{id:"settings",label:"設定"},{id:"locked",label:"準備中",disabled:true}];
function menuRender(){menu.replaceChildren(createMenu({items,activeId:active,onSelect:id=>{active=id;menuRender();ui.toast("メニュー: "+id);}}));}menuRender();
function panel(title,description,buttons,wide=false){
 const box=document.createElement("div");box.className="actions";box.append(...buttons);
 const p=createPanel({title,description,content:box});if(wide)p.classList.add("wide");content.append(p);return p;
}
panel("01 / Buttons","4種類のボタン・無効・処理中",[
 createButton({label:"対戦開始",onClick:()=>ui.toast("対戦開始の例",{type:"success"})}),
 createButton({label:"キャンセル",variant:"secondary",onClick:()=>ui.toast("キャンセル")}),
 createButton({label:"削除",variant:"danger",onClick:()=>ui.confirm("サンプルデータを削除しますか？",{title:"削除確認",type:"danger"}).then(v=>ui.toast(v?"削除を選択":"キャンセル"))}),
 createButton({label:"詳細",variant:"ghost",onClick:()=>ui.alert("詳細情報")}),
 createButton({label:"無効",disabled:true}),createButton({label:"保存",busy:true})
]);
panel("02 / Popup & notification","ダイアログ・通知・自分の番の演出",[
 createButton({label:"自分の番",onClick:()=>ui.banner("あなたの番です！",{title:"YOUR TURN",type:"warning"})}),
 createButton({label:"確認",variant:"secondary",onClick:async()=>{const ok=await ui.confirm("続行しますか？");ui.toast(ok?"OK":"キャンセル");}}),
 createButton({label:"エラー",variant:"danger",onClick:()=>ui.toast("通信に失敗しました",{type:"danger"})}),
 createButton({label:"ロード",variant:"secondary",onClick:()=>ui.withLoading(()=>new Promise(r=>setTimeout(r,1500)),"データ取得中…")})
]);
panel("03 / Status","状態を色と文字で伝える",["info","success","warning","danger"].map((type,i)=>createBadge({type,label:["待機中","完了","確認が必要","エラー"][i]})),true);
const rows=Array.from({length:23},(_,i)=>({id:i+1,name:["プレイヤー","CPU","ゲスト"][i%3]+" "+(i+1),score:(i*37)%500,rank:i%3===0?"大富豪":"平民"}));
const grid=createGrid({rows,columns:[{key:"id",label:"ID"},{key:"name",label:"名前"},{key:"score",label:"ポイント"},{key:"rank",label:"称号"}],pageSize:5,onRowAction:r=>ui.alert(r.name+" / "+r.score+" pt",{title:"戦績詳細"})});
const gp=createPanel({title:"04 / Data grid",description:"検索・列ソート・ページ送り・行操作",content:grid.element});gp.classList.add("wide");content.append(gp);
