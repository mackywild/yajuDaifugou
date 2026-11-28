import {createUI,createAdvancedGrid} from "/src/index.js";
const ui=createUI(document.querySelector("#root"));
const grid=createAdvancedGrid({rows:[{id:1,name:"製品A",role:"固定"},{id:2,name:"製品B",role:"固定"}],columns:[{key:"id",readonly:true},{key:"name",required:true},{key:"role",readonly:true}],allowCSVImport:true,canAdd:()=>false,canDelete:()=>false,
 confirmCSVImport:diff=>{document.querySelector("#preview").textContent=JSON.stringify(diff,null,2);return ui.confirm("追加 "+diff.added.length+" 件 / 更新 "+diff.updated.length+" 件 / 削除 "+diff.deleted.length+" 件。反映しますか？",{title:"CSV取込確認"});}});
document.querySelector("#grid").append(grid.element);
window.addEventListener("pagehide",()=>{grid.destroy();ui.destroy();},{once:true});
