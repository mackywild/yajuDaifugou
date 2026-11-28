import {node,action,downloadText} from "./dom.js";
import {createGridStore,parseDelimited,encodeDelimited,normalizeCell} from "./grid-model.js";
export function createAdvancedGrid(options={}){
  const o={rows:[],columns:[],rowKey:"id",pageSize:50,label:"編集グリッド",...options};
  o.columns=o.columns.map(c=>({...c}));
  if(!o.columns.length||o.columns.some(c=>typeof c.key!=="string"||["__proto__","constructor","prototype"].includes(c.key))||new Set(o.columns.map(c=>c.key)).size!==o.columns.length)throw Error("columns require unique safe keys");
  const unique=rows=>{const ids=rows.map(r=>r[o.rowKey]);if(ids.some(v=>v==null)||new Set(ids).size!==ids.length)throw Error("行キーは必須かつ一意です");return rows;};
  let store=createGridStore(unique(o.rows)),query="",sort=null,desc=false,page=0,selection=null,selectedIds=new Set(),hidden=new Set(),filters=new Map(),dragging=false,disposed=false;
  const element=node("section","mky-grid mky-advanced"),bar=node("div","mky-grid-toolbar"),tools=node("div","mky-actions"),search=node("input","mky-input"),message=node("div","mky-muted"),viewport=node("div","mky-grid-scroll"),table=node("table","mky-table"),head=node("thead"),body=node("tbody"),foot=node("tfoot"),pager=node("div","mky-grid-footer"),summary=node("span","mky-muted");
  const columns=()=>o.columns.filter(c=>!hidden.has(c.key));
  const size=Math.max(1,Math.min(500,Math.floor(Number(o.pageSize)||50)));
  search.type="search";search.placeholder="全列検索";search.setAttribute("aria-label","全列検索");message.setAttribute("role","status");
  table.setAttribute("aria-label",o.label);table.append(head,body,foot);viewport.append(table);bar.append(search,tools);element.append(bar,message,viewport,pager);
  const fail=e=>{message.textContent=String(e?.message||e);o.onError?.(e);};
  function filtered(){
    const q=query.toLowerCase();
    const list=store.get().filter(r=>(!q||o.columns.some(c=>String(r[c.key]??"").toLowerCase().includes(q)))&&[...filters].every(([k,v])=>String(r[k]??"").toLowerCase().includes(v.toLowerCase())));
    if(sort)list.sort((a,b)=>{const x=a[sort],y=b[sort];if(x==null||y==null)return x==null?(y==null?0:1):-1;const n=typeof x==="number"&&typeof y==="number"?x-y:String(x).localeCompare(String(y),"ja",{numeric:true});return desc?-n:n;});return list;
  }
  let visible=[];
  const clearSelection=()=>{selection=null;selectedIds.clear();};
  function notify(){o.onChange?.(store.get(),store.changes(o.rowKey));}
  function apply(rows){store.apply(unique(rows));clearSelection();render();notify();}
  function mutate(changes){
    const rows=store.get(),byId=new Map(rows.map(r=>[r[o.rowKey],r]));
    for(const {id,key,value} of changes){const r=byId.get(id),c=o.columns.find(c=>c.key===key);if(!r||!c||key===o.rowKey||o.canEdit&&!o.canEdit(r,c))throw Error("編集できないセル");r[key]=normalizeCell(value,c,r);}
    apply(rows);
  }
  function rect(){if(!selection)return null;return {r0:Math.min(selection.a.r,selection.b.r),r1:Math.max(selection.a.r,selection.b.r),c0:Math.min(selection.a.c,selection.b.c),c1:Math.max(selection.a.c,selection.b.c)};}
  function paint(){const r=rect();for(const td of body.querySelectorAll("[data-r]")){const i=Number(td.dataset.r),j=Number(td.dataset.c);const yes=Boolean(r&&i>=r.r0&&i<=r.r1&&j>=r.c0&&j<=r.c1);td.classList.toggle("mky-cell-selected",yes);td.tabIndex=selection?.b.r===i&&selection?.b.c===j||!selection&&i===0&&j===0?0:-1;}}
  function choose(r,c,extend=false){if(!visible[r]||!columns()[c])return;selection={a:extend&&selection?selection.a:{r,c},b:{r,c}};paint();}
  function focusCell(r,c){body.querySelector('[data-r="'+r+'"][data-c="'+c+'"]')?.focus();}
  function rangeValues(){const x=rect(),cols=columns();if(!x)return [];return visible.slice(x.r0,x.r1+1).map(r=>cols.slice(x.c0,x.c1+1).map(c=>r[c.key]));}
  function paste(text){const x=rect();if(!x)throw Error("セルを選択してください");const values=parseDelimited(text,"\t"),cols=columns(),changes=[];
    if(x.r0+values.length>visible.length||values.some(a=>x.c0+a.length>cols.length))throw Error("貼り付け範囲が表示ページを超えています");
    values.forEach((line,i)=>line.forEach((value,j)=>changes.push({id:visible[x.r0+i][o.rowKey],key:cols[x.c0+j].key,value})));mutate(changes);
  }
  function erase(){const x=rect(),cols=columns();if(!x)return;const changes=[];for(let i=x.r0;i<=x.r1;i++)for(let j=x.c0;j<=x.c1;j++)changes.push({id:visible[i][o.rowKey],key:cols[j].key,value:""});mutate(changes);}
  function edit(td,r,c){
    if(td.querySelector("input"))return;const col=columns()[c],row=visible[r];if(col.key===o.rowKey||o.canEdit&&!o.canEdit(row,col)||col.readonly||col.editable===false||typeof col.editable==="function"&&!col.editable(row))return;
    const input=node("input","mky-input");input.value=String(row[col.key]??"");if(col.type==="number")input.type="number";td.replaceChildren(input);input.focus();input.select();let finished=false;
    const finish=save=>{if(finished)return;finished=true;try{if(save)mutate([{id:row[o.rowKey],key:col.key,value:input.value}]);else render();}catch(e){fail(e);render();}focusCell(r,c);};
    input.addEventListener("keydown",e=>{if(e.key==="Enter"||e.key==="Escape"){e.preventDefault();e.stopPropagation();finish(e.key==="Enter");}});
    input.addEventListener("blur",()=>finish(true));
  }
  const guarded=fn=>()=>{try{fn();}catch(e){fail(e);}};
  const undo=()=>{if(store.undo()){clearSelection();render();notify();}};
  const redo=()=>{if(store.redo()){clearSelection();render();notify();}};
  const deleteRows=()=>{if(!selectedIds.size)return;const rows=store.get().filter(r=>!selectedIds.has(r[o.rowKey])||o.canDelete&&!o.canDelete(r));clearSelection();apply(rows);};
  const addRow=()=>{if(!o.newRow)throw Error("newRow関数が必要です");const r=o.newRow(store.get());apply([...store.get(),r]);};
  const exportCSV=()=>encodeDelimited([o.columns.map(c=>c.key),...filtered().map(r=>o.columns.map(c=>r[c.key]))]);
  function importCSV(text){const [header,...lines]=parseDelimited(text);if(new Set(header).size!==header.length||header.some(k=>!o.columns.some(c=>c.key===k))||!header.includes(o.rowKey))throw Error("CSVヘッダーが不正です");
    const rows=lines.map(line=>{if(line.length!==header.length)throw Error("CSV列数が不一致です");const row=Object.create(null);header.forEach((k,i)=>{const c=o.columns.find(x=>x.key===k);row[k]=normalizeCell(line[i],{...c,editable:true,readonly:false},row);});return row;});clearSelection();apply(rows);}
  tools.append(action("追加",guarded(addRow)),action("行削除",guarded(deleteRows),"danger"),action("セル削除",guarded(erase)),action("Undo",undo),action("Redo",redo),action("CSV出力",()=>downloadText("grid.csv","\uFEFF"+exportCSV(),"text/csv;charset=utf-8")));
  const file=node("input");file.type="file";file.accept=".csv,text/csv";file.hidden=true;file.addEventListener("change",async()=>{try{if(file.files[0]){if(file.files[0].size>5_000_000)throw Error("CSVは5MBまで");importCSV(await file.files[0].text());}}catch(e){fail(e);}finally{file.value="";}});tools.append(action("CSV取込",()=>file.click()),file);
  const chooser=node("details","mky-column-settings");chooser.append(node("summary","","表示列"));
  for(const c of o.columns){const l=node("label","mky-choice-item"),cb=node("input");cb.type="checkbox";cb.checked=true;cb.disabled=c.key===o.rowKey;cb.addEventListener("change",()=>{if(cb.checked)hidden.delete(c.key);else hidden.add(c.key);clearSelection();render();});l.append(cb,document.createTextNode(c.label||c.key));chooser.append(l);}bar.append(chooser);
  const prev=action("前へ",()=>{page--;clearSelection();render();}),next=action("次へ",()=>{page++;clearSelection();render();});pager.append(prev,summary,next);
  search.addEventListener("input",()=>{query=search.value;page=0;clearSelection();render();});
  function render(){
    if(disposed)return;
    const data=filtered(),cols=columns();page=Math.max(0,Math.min(page,Math.max(0,Math.ceil(data.length/size)-1)));visible=data.slice(page*size,(page+1)*size);
    prev.disabled=page===0;next.disabled=(page+1)*size>=data.length;summary.textContent=(page+1)+" / "+Math.max(1,Math.ceil(data.length/size))+" ページ · "+data.length+"件";
    const hr=node("tr"),fr=node("tr");hr.append(node("th","","選択"));fr.append(node("th"));
    for(const c of cols){
      const th=node("th");th.scope="col";if(c.key===sort)th.setAttribute("aria-sort",desc?"descending":"ascending");
      th.append(action((c.label||c.key)+(sort===c.key?(desc?" ↓":" ↑"):""),()=>{desc=sort===c.key&&!desc;sort=c.key;page=0;clearSelection();render();},"ghost"));
      if(c.width)th.style.width=Math.max(60,Number(c.width)||120)+"px";
      if(c.fixed){th.classList.add("mky-fixed");th.style.left=50+cols.slice(0,cols.indexOf(c)).filter(x=>x.fixed).reduce((n,x)=>n+(Number(x.width)||140),0)+"px";}
      const grip=node("input","mky-input");grip.type="range";grip.min="60";grip.max="500";grip.value=String(c.width||120);grip.setAttribute("aria-label",(c.label||c.key)+" 列幅");grip.addEventListener("input",()=>{c.width=Number(grip.value);th.style.width=c.width+"px";});grip.addEventListener("change",()=>render());th.append(grip);const resize=node("span","mky-resize-grip");resize.title="ドラッグして列幅を変更";resize.addEventListener("pointerdown",e=>{e.preventDefault();e.stopPropagation();const x=e.clientX,start=th.getBoundingClientRect().width;resize.setPointerCapture(e.pointerId);const drag=ev=>{c.width=Math.max(60,Math.min(500,start+ev.clientX-x));th.style.width=c.width+"px";};const end=()=>{resize.removeEventListener("pointermove",drag);resize.removeEventListener("pointerup",end);resize.removeEventListener("pointercancel",end);render();};resize.addEventListener("pointermove",drag);resize.addEventListener("pointerup",end);resize.addEventListener("pointercancel",end);});th.append(resize);hr.append(th);
      const filter=node("input","mky-input");filter.placeholder="絞込";filter.value=filters.get(c.key)||"";filter.setAttribute("aria-label",(c.label||c.key)+" フィルター");filter.addEventListener("change",()=>{filters.set(c.key,filter.value);page=0;clearSelection();render();});const ft=node("th");ft.append(filter);fr.append(ft);
    }
    head.replaceChildren(hr,fr);body.replaceChildren();
    const fragment=document.createDocumentFragment();
    visible.forEach((r,i)=>{const tr=node("tr"),check=node("td"),cb=node("input");cb.type="checkbox";cb.checked=selectedIds.has(r[o.rowKey]);cb.setAttribute("aria-label","行 "+r[o.rowKey]+" を選択");cb.addEventListener("change",()=>{if(cb.checked)selectedIds.add(r[o.rowKey]);else selectedIds.delete(r[o.rowKey]);});check.append(cb);tr.append(check);
      cols.forEach((c,j)=>{const td=node("td","",c.format?c.format(r[c.key],r):r[c.key]??"");td.dataset.r=String(i);td.dataset.c=String(j);if(c.fixed){td.classList.add("mky-fixed");td.style.left=50+cols.slice(0,j).filter(x=>x.fixed).reduce((n,x)=>n+(Number(x.width)||140),0)+"px";}if(c.tone){const v=c.tone(r[c.key],r);if(["danger","warning","success","info"].includes(v))td.classList.add("mky-cell-"+v);}td.addEventListener("pointerdown",e=>{if(e.button!==0||e.target.tagName==="INPUT")return;dragging=true;choose(i,j,e.shiftKey);td.focus({preventScroll:true});});td.addEventListener("pointerenter",()=>{if(dragging)choose(i,j,true);});td.addEventListener("focus",()=>{if(!selection)choose(i,j);});td.addEventListener("dblclick",()=>edit(td,i,j));tr.append(td);});fragment.append(tr);});
    body.append(fragment);const total=node("tr");total.append(node("td","","集計"));for(const c of cols){const values=data.map(r=>r[c.key]).filter(v=>typeof v==="number"&&Number.isFinite(v));total.append(node("td","",c.aggregate==="sum"?values.reduce((a,b)=>a+b,0):c.aggregate==="avg"&&values.length?(values.reduce((a,b)=>a+b,0)/values.length).toFixed(2):c.aggregate==="count"?data.length:""));}foot.replaceChildren(total);paint();
  }
  // Keyboard/clipboard handlers apply only inside the grid, never document-wide.
  const stopDrag=()=>dragging=false;window.addEventListener("pointerup",stopDrag);window.addEventListener("pointercancel",stopDrag);
  element.addEventListener("keydown",e=>{if(e.target.matches("input,textarea,select,button")||!selection)return;const {r,c}=selection.b,delta={ArrowUp:[-1,0],ArrowDown:[1,0],ArrowLeft:[0,-1],ArrowRight:[0,1]}[e.key];try{
    if(delta){e.preventDefault();choose(r+delta[0],c+delta[1],e.shiftKey);focusCell(selection.b.r,selection.b.c);}
    else if(e.key==="Enter"){e.preventDefault();const td=body.querySelector('[data-r="'+r+'"][data-c="'+c+'"]');if(td)edit(td,r,c);}
    else if(e.key==="Delete"){e.preventDefault();erase();}
    else if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==="z"){e.preventDefault();e.shiftKey?redo():undo();}
    else if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==="y"){e.preventDefault();redo();}
  }catch(err){fail(err);}});
  element.addEventListener("copy",e=>{if(e.target.matches("input,textarea")||!selection)return;e.preventDefault();e.clipboardData?.setData("text/plain",encodeDelimited(rangeValues(),"\t",{spreadsheetSafe:false}));});
  element.addEventListener("paste",e=>{if(e.target.matches("input,textarea")||!selection)return;e.preventDefault();try{paste(e.clipboardData?.getData("text/plain")||"");}catch(err){fail(err);}});
  render();
  return {element,getRows:store.get,getChanges:()=>store.changes(o.rowKey),isDirty:store.isDirty,commit(){store.commit();},reset(){store.reset();clearSelection();render();notify();},undo,redo,paste,copy:()=>encodeDelimited(rangeValues(),"\t",{spreadsheetSafe:false}),exportCSV,importCSV,select:choose,editCell:(id,key,value)=>mutate([{id,key,value}]),destroy(){if(disposed)return;disposed=true;window.removeEventListener("pointerup",stopDrag);window.removeEventListener("pointercancel",stopDrag);element.remove();}};
}
