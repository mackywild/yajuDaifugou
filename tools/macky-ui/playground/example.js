import {createUI,createButton,createPanel} from "/src/index.js";
const ui=createUI(document.querySelector("#root"),{theme:"light"});
const button=createButton({label:"サンプルを実行",onClick:async()=>{if(await ui.confirm("実行しますか？"))ui.toast("完了",{type:"success"});}});
document.querySelector("#content").append(createPanel({title:"新規アプリの試作",content:button}));
