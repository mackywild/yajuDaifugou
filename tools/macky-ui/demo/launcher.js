import {createUI} from "/src/index.js";
const ui=createUI(document.querySelector("#root"));
const pages=document.querySelector("#pages"),status=document.querySelector("#status"),search=document.querySelector("#search"),refresh=document.querySelector("#refresh");
const groups={catalog:"01 / 部品別カタログ",integration:"02 / 結合・自動・配布版試験",custom:"03 / 独自HTML"};
let list=[],light=false;
function render(){
  const query=search.value.trim().toLowerCase();
  const items=list.filter(p=>(p.title+" "+p.description).toLowerCase().includes(query));
  pages.replaceChildren();
  for(const [id,label] of Object.entries(groups)){
    const section=document.createElement("section");section.className="studio-group";
    const heading=document.createElement("h2");heading.textContent=label;section.append(heading);
    const grid=document.createElement("div");grid.className="studio-grid";
    for(const p of items.filter(p=>p.group===id)){
      const a=document.createElement("a");a.className="studio-card";a.href=p.url;
      const name=document.createElement("strong"),description=document.createElement("span"),action=document.createElement("b");
      name.textContent=p.title;description.textContent=p.description;action.textContent="画面を開く →";
      a.append(name,description,action);grid.append(a);
    }
    if(!grid.children.length){const msg=document.createElement("p");msg.className="mky-muted";msg.textContent="該当する画面はありません";grid.append(msg);}
    section.append(grid);pages.append(section);
  }
  status.textContent=items.length+" 件 / "+list.length+" 画面";
}
async function load(){
  refresh.disabled=true;status.textContent="読み込み中…";
  try{const r=await fetch("/api/test-pages",{cache:"no-store"});if(!r.ok)throw Error("HTTP "+r.status);list=await r.json();render();}
  catch{status.textContent="画面一覧を取得できませんでした。再読込してください。";}
  finally{refresh.disabled=false;}
}
search.addEventListener("input",render);refresh.addEventListener("click",load);
document.querySelector("#theme").addEventListener("click",()=>{light=!light;ui.setTheme(light?"light":"yaju");});
load();
