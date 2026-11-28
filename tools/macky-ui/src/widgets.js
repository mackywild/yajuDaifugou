import {node,action,appendContent,selectOptions} from "./dom.js";
let uid=0;
export function createTabs({items=[],activeId,onChange}={}){
 const element=node("section","mky-tabs"),bar=node("div","mky-menu"),content=node("div","mky-tab-content");bar.setAttribute("role","tablist");bar.setAttribute("aria-label","タブ");
 let active=items.find(x=>x.id===activeId&&!x.disabled)?.id??items.find(x=>!x.disabled)?.id;const id="mky-tabs-"+(++uid);
 const panels=items.map((item,i)=>{const p=node("section");p.id=id+"-panel-"+i;p.setAttribute("role","tabpanel");p.setAttribute("aria-labelledby",id+"-tab-"+i);appendContent(p,typeof item.content==="function"?item.content():item.content);content.append(p);return p;});
 const buttons=items.map((item,i)=>{const b=action(item.label,()=>setActive(item.id),"ghost");b.id=id+"-tab-"+i;b.setAttribute("role","tab");b.setAttribute("aria-controls",panels[i].id);b.disabled=Boolean(item.disabled);bar.append(b);return b;});
 function render(){items.forEach((item,i)=>{const on=item.id===active;buttons[i].setAttribute("aria-selected",String(on));buttons[i].tabIndex=on?0:-1;buttons[i].classList.toggle("mky-tab-active",on);panels[i].hidden=!on;});}
 function setActive(value){if(!items.some(i=>i.id===value&&!i.disabled)||active===value)return;active=value;render();onChange?.(value);}
 bar.addEventListener("keydown",e=>{const enabled=items.filter(i=>!i.disabled),index=enabled.findIndex(i=>i.id===active);if(!enabled.length)return;const offsets={ArrowRight:1,ArrowLeft:-1};let next;if(e.key==="Home")next=0;else if(e.key==="End")next=enabled.length-1;else if(e.key in offsets)next=(index+offsets[e.key]+enabled.length)%enabled.length;else return;e.preventDefault();setActive(enabled[next].id);buttons[items.indexOf(enabled[next])].focus();});
 element.append(bar,content);render();return {element,setActive,getActive:()=>active,destroy:()=>element.remove()};
}
export function createAccordion({items=[],multiple=false}={}){
 const el=node("div","mky-accordion"),entries=[];
 for(const item of items){const detail=node("details","mky-panel"),summary=node("summary","",item.label);detail.open=Boolean(item.open);detail.append(summary);const body=node("div","mky-tab-content");appendContent(body,typeof item.content==="function"?item.content():item.content);detail.append(body);entries.push(detail);el.append(detail);detail.addEventListener("toggle",()=>{if(detail.open&&!multiple)entries.forEach(e=>{if(e!==detail)e.open=false;});});}
 return el;
}
export function createSidebar({items=[],activeId,onSelect,title="メニュー"}={}){
 const e=node("aside","mky-sidebar mky-panel");e.append(node("h2","mky-heading",title));const nav=node("nav","mky-menu mky-menu--vertical");nav.setAttribute("aria-label",title);let active=activeId;
 function render(){nav.replaceChildren();for(const item of items){const b=action(item.label,()=>{active=item.id;render();onSelect?.(active,item);},active===item.id?"primary":"ghost");b.disabled=Boolean(item.disabled);if(active===item.id)b.setAttribute("aria-current","true");nav.append(b);}}render();e.append(nav);return e;
}
export function createTooltip({label="",text=""}={}){
 const wrap=node("span","mky-tooltip"),button=action(label,null,"ghost"),tip=node("span","mky-tooltip-content",text);tip.id="mky-tip-"+(++uid);tip.setAttribute("role","tooltip");button.setAttribute("aria-describedby",tip.id);wrap.append(button,tip);button.addEventListener("keydown",e=>{if(e.key==="Escape"){tip.hidden=true;button.blur();}});button.addEventListener("focus",()=>tip.hidden=false);wrap.addEventListener("pointerenter",()=>tip.hidden=false);return wrap;
}
export function createContextMenu(target,{items=[],onSelect}={}){
 const menu=node("div","mky-context mky-panel");menu.hidden=true;menu.setAttribute("role","group");menu.setAttribute("aria-label","操作メニュー");
 const oldPosition=target.style.position;let changedPosition=false;if(getComputedStyle(target).position==="static"){target.style.position="relative";changedPosition=true;}
 const close=()=>{menu.hidden=true;};
 for(const item of items){const b=action(item.label,()=>{close();onSelect?.(item.id,item);},"ghost");b.disabled=Boolean(item.disabled);menu.append(b);}
 target.append(menu);
 const show=(x,y)=>{menu.hidden=false;const rect=target.getBoundingClientRect();menu.style.left=Math.max(0,Math.min(x-rect.left,target.clientWidth-menu.offsetWidth))+"px";menu.style.top=Math.max(0,Math.min(y-rect.top,target.clientHeight-menu.offsetHeight))+"px";menu.querySelector("button:not(:disabled)")?.focus();};
 const ctx=e=>{e.preventDefault();show(e.clientX,e.clientY);},key=e=>{if(e.key==="Escape"){close();target.focus();}else if(e.key==="ContextMenu"||e.shiftKey&&e.key==="F10"){e.preventDefault();const r=target.getBoundingClientRect();show(r.left,r.top);}},outside=e=>{if(!menu.contains(e.target))close();};
 target.addEventListener("contextmenu",ctx);target.addEventListener("keydown",key);document.addEventListener("pointerdown",outside);
 return {element:menu,close,destroy(){target.removeEventListener("contextmenu",ctx);target.removeEventListener("keydown",key);document.removeEventListener("pointerdown",outside);menu.remove();if(changedPosition)target.style.position=oldPosition;}};
}
export function createProgress({value=0,max=100,label="進捗"}={}){
 const element=node("div","mky-progress-wrap"),text=node("span","mky-muted"),bar=node("progress","mky-progress");bar.setAttribute("aria-label",label);bar.max=Number(max)>0?Number(max):100;element.append(text,bar);
 function setValue(v){if(v==null){bar.removeAttribute("value");text.textContent=label+" · 処理中";}else{bar.value=Math.max(0,Math.min(bar.max,Number(v)||0));text.textContent=label+" "+Math.round(bar.value/bar.max*100)+"%";}}setValue(value);return {element,setValue,destroy:()=>element.remove()};
}
export function createChart({type="bar",data=[],label="グラフ"}={}){
 const element=node("figure","mky-chart mky-panel");element.append(node("figcaption","mky-heading",label));
 const valid=data.filter(d=>Number.isFinite(Number(d.value))),max=Math.max(1,...valid.map(d=>Math.abs(Number(d.value))));
 if(type==="line"){
  const svg=document.createElementNS("http://www.w3.org/2000/svg","svg");svg.setAttribute("viewBox","0 0 600 180");svg.setAttribute("role","img");svg.setAttribute("aria-label",label);
  const points=valid.map((d,i)=>[30+i*540/Math.max(1,valid.length-1),90-Number(d.value)/max*65]);const path=document.createElementNS(svg.namespaceURI,"polyline");path.setAttribute("points",points.map(p=>p.join(",")).join(" "));path.setAttribute("fill","none");path.setAttribute("stroke","currentColor");path.setAttribute("stroke-width","3");svg.append(path);for(const p of points){const c=document.createElementNS(svg.namespaceURI,"circle");c.setAttribute("cx",p[0]);c.setAttribute("cy",p[1]);c.setAttribute("r","4");c.setAttribute("fill","currentColor");svg.append(c);}element.append(svg);
 }
 const list=node("div","mky-chart-list");for(const d of valid){const row=node("div","mky-chart-row");row.append(node("span","",d.label));if(type!=="line"){const track=node("div","mky-chart-track"),fill=node("div","mky-chart-fill");fill.style.width=(Math.abs(Number(d.value))/max*100)+"%";if(Number(d.value)<0)fill.classList.add("mky-negative");track.append(fill);row.append(track);}row.append(node("strong","",d.value));list.append(row);}element.append(list);return element;
}
export function createTree({items=[],onSelect}={}){
 const element=node("nav","mky-tree");element.setAttribute("aria-label","階層一覧");
 function render(nodes,parent){const ul=node("ul");for(const item of nodes){const li=node("li");if(item.children?.length){const d=node("details"),s=node("summary","",item.label);d.open=Boolean(item.open);d.append(s);render(item.children,d);li.append(d);}else li.append(action(item.label,()=>onSelect?.(item.id,item),"ghost"));ul.append(li);}parent.append(ul);}render(items,element);return element;
}
export function createCalendar({year=new Date().getFullYear(),month=new Date().getMonth()+1,events=[],onSelect}={}){
 const element=node("section","mky-calendar mky-panel"),header=node("div","mky-grid-toolbar"),title=node("h2","mky-heading"),grid=node("div","mky-calendar-grid");
 let cursor=new Date(year,month-1,1);const shift=n=>{cursor=new Date(cursor.getFullYear(),cursor.getMonth()+n,1);render();};
 header.append(action("前月",()=>shift(-1)),title,action("翌月",()=>shift(1)));element.append(header,grid);
 function render(){const y=cursor.getFullYear(),m=cursor.getMonth(),days=new Date(y,m+1,0).getDate();title.textContent=y+"年 "+(m+1)+"月";grid.replaceChildren();for(const day of ["日","月","火","水","木","金","土"])grid.append(node("span","mky-muted",day));for(let i=0;i<cursor.getDay();i++)grid.append(node("span"));for(let d=1;d<=days;d++){const date=[y,String(m+1).padStart(2,"0"),String(d).padStart(2,"0")].join("-"),ev=events.filter(x=>x.date===date),b=action(String(d),()=>onSelect?.(date,ev),"ghost");b.setAttribute("aria-label",date+(ev.length?" "+ev.length+"件":""));for(const x of ev.slice(0,2))b.append(node("small","",x.label));grid.append(b);}}render();return element;
}
export function createWizard({steps=[],onComplete}={}){
 const element=node("section","mky-panel"),status=node("div","mky-muted"),content=node("div","mky-tab-content"),nav=node("div","mky-actions");let i=0,busy=false;
 const panels=steps.map(s=>{const p=node("div");appendContent(p,typeof s.content==="function"?s.content():s.content);content.append(p);return p;});
 const prev=action("戻る",()=>{if(!busy&&i>0){i--;render();}}),next=action("次へ",async()=>{if(busy||!steps.length)return;busy=true;render();try{if(steps[i].validate&&!(await steps[i].validate()))return;if(i===steps.length-1)await onComplete?.();else i++;}catch(e){status.textContent=String(e?.message||e);return;}finally{busy=false;render();}},"primary");nav.append(prev,next);element.append(status,content,nav);
 function render(){status.textContent=steps.length?(i+1)+" / "+steps.length+" · "+steps[i].label:"ステップなし";panels.forEach((p,j)=>p.hidden=i!==j);prev.disabled=busy||i===0;next.disabled=busy||!steps.length;next.textContent=i===steps.length-1?"完了":"次へ";}render();return element;
}
export function createTimeline({items=[]}={}){const el=node("ol","mky-timeline");for(const item of items){const li=node("li");li.append(node("time","mky-muted",item.date),node("strong","",item.title),node("div","",item.description));el.append(li);}return el;}
export function createKanban({columns=[],cards=[],onMove}={}){
 const element=node("div","mky-kanban");let data=cards.map(c=>({...c}));
 function move(id,to){if(!columns.some(c=>c.id===to)||!data.some(c=>c.id===id))return;data=data.map(c=>c.id===id?{...c,columnId:to}:c);render();onMove?.(id,to,data.map(c=>({...c})));}
 function render(){element.replaceChildren();for(const col of columns){const p=node("section","mky-panel");p.append(node("h2","mky-heading",col.label));for(const card of data.filter(c=>c.columnId===col.id)){const tile=node("article","mky-kanban-card");tile.append(node("strong","",card.title),node("p","mky-muted",card.description));const select=node("select","mky-input");select.setAttribute("aria-label",card.title+" 移動先");for(const c of columns){const opt=node("option","",c.label);opt.value=String(c.id);select.append(opt);}select.value=String(col.id);select.addEventListener("change",()=>move(card.id,columns.find(c=>String(c.id)===select.value).id));tile.append(select);tile.draggable=true;tile.addEventListener("dragstart",e=>e.dataTransfer?.setData("application/x-mky-card",String(card.id)));p.append(tile);}
 p.addEventListener("dragover",e=>e.preventDefault());p.addEventListener("drop",e=>{e.preventDefault();const id=e.dataTransfer?.getData("application/x-mky-card"),card=data.find(c=>String(c.id)===id);if(card)move(card.id,col.id);});element.append(p);}}render();return {element,getCards:()=>data.map(c=>({...c})),move,destroy:()=>element.remove()};
}
export function createImageViewer({src="",alt=""}={}){
 const element=node("figure","mky-image-viewer mky-panel"),frame=node("div","mky-image-frame"),img=node("img");img.alt=alt;
 // Only explicit HTTP(S), blob, relative same-origin URLs and safe raster data types.
 try{const u=new URL(src,document.baseURI);if(["http:","https:","blob:"].includes(u.protocol)||/^data:image\/(?:png|jpeg|gif|webp);base64,/i.test(src))img.src=src;}catch{}
 frame.append(img);let zoom=1;function set(v){zoom=Math.max(.5,Math.min(4,v));img.style.width=(zoom*100)+"%";}
 const tools=node("div","mky-actions");tools.append(action("−",()=>set(zoom-.25)),action("＋",()=>set(zoom+.25)),action("リセット",()=>set(1)));element.append(tools,frame);return element;
}
export function createRichText({value="",onChange}={}){
 const element=node("div","mky-rich-text mky-panel"),toolbar=node("div","mky-actions"),editor=node("div","mky-editor");editor.contentEditable="true";editor.setAttribute("role","textbox");editor.setAttribute("aria-label","文書編集");editor.setAttribute("aria-multiline","true");editor.textContent=value;
 const emit=()=>onChange?.(editor.innerText);
 // Plain-text paste, formatting only from the local toolbar. Export is plain text.
 editor.addEventListener("paste",e=>{e.preventDefault();const text=e.clipboardData?.getData("text/plain")||"";document.execCommand("insertText",false,text);emit();});
 editor.addEventListener("input",emit);
 for(const [label,cmd] of [["太字","bold"],["斜体","italic"],["箇条書き","insertUnorderedList"]]){const b=action(label,()=>{editor.focus();document.execCommand(cmd,false,null);emit();});b.addEventListener("mousedown",e=>e.preventDefault());toolbar.append(b);}
 element.append(toolbar,editor);return {element,getValue:()=>editor.innerText,setValue:v=>editor.textContent=String(v??""),destroy:()=>element.remove()};
}
export function createCommandPalette({items=[],onSelect}={}){
 const element=node("section","mky-panel"),input=node("input","mky-input"),results=node("div","mky-command-list");input.type="search";input.placeholder="コマンド検索";input.setAttribute("aria-label","コマンド検索");
 function render(){results.replaceChildren();const q=input.value.toLowerCase();for(const item of items.filter(x=>(x.label+" "+(x.keywords||"")).toLowerCase().includes(q))){const b=action(item.label,()=>onSelect?.(item.id,item),"ghost");b.disabled=Boolean(item.disabled);results.append(b);}if(!results.children.length)results.append(node("p","mky-muted","該当なし"));}
 input.addEventListener("input",render);input.addEventListener("keydown",e=>{if(e.key==="ArrowDown"){e.preventDefault();results.querySelector("button:not(:disabled)")?.focus();}else if(e.key==="Enter")results.querySelector("button:not(:disabled)")?.click();});element.append(input,results);render();return element;
}
