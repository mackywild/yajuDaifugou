const status=document.querySelector("#status");
try{
  const [core,adapter,response]=await Promise.all([import("/dist/index.js"),import("/dist/vue.js"),fetch("/dist/manifest.json")]);
  if(!response.ok)throw Error("manifest not found");
  const manifest=await response.json();
  if(typeof adapter.createVueKit!=="function")throw Error("Vue entry missing");
  const ui=core.createUI(document.querySelector("#root"));
  const grid=core.createGrid({rows:[{id:1,name:"配布版",state:"OK"}],columns:[{key:"id"},{key:"name",label:"名称"},{key:"state",label:"状態"}]});
  document.querySelector("#content").append(core.createButton({label:"ビルド済み通知",onClick:()=>ui.toast("dist から表示しました",{type:"success"})}),grid.element);
  status.textContent="PASS: "+manifest.name+"@"+manifest.version+" / ESM core・Vue entry 読込成功";
}catch(e){status.textContent="FAIL: npm run build を実行して再読込してください。 "+e.message;}
