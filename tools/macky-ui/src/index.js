import { buttonClass, gridView, durationMs } from "./model.js";
export { buttonClass, gridView, durationMs } from "./model.js";

const activeRoots = new WeakMap();
const tone = v => ["info","success","warning","danger"].includes(v) ? v : "info";
const text = v => String(v ?? "");
function el(tag, cls, content) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (content != null) e.textContent = text(content);
  return e;
}
export function createButton({label="",variant="primary",size="md",disabled=false,busy=false,onClick,ariaLabel}={}) {
  const b = el("button",buttonClass(variant,size),busy ? "処理中… "+label : label);
  b.type="button"; b.disabled=Boolean(disabled||busy);
  if (ariaLabel) b.setAttribute("aria-label",text(ariaLabel));
  if (busy) b.setAttribute("aria-busy","true");
  if (typeof onClick==="function") b.addEventListener("click",onClick);
  return b;
}
export function createBadge({label="",type="info"}={}) {
  return el("span","mky-badge mky-tone--"+tone(type),label);
}
export function createMenu({items=[],activeId=null,label="メニュー",orientation="horizontal",onSelect}={}) {
  const nav=el("nav","mky-menu"+(orientation==="vertical"?" mky-menu--vertical":""));
  nav.setAttribute("aria-label",text(label));
  for (const item of items) {
    const b=createButton({label:item.label,variant:item.id===activeId?"primary":"ghost",disabled:item.disabled,
      onClick:()=>{if(typeof onSelect==="function")onSelect(item.id,item);}});
    if(item.id===activeId)b.setAttribute("aria-current","true");
    nav.append(b);
  }
  return nav;
}
export function createPanel({title="",description="",content}={}) {
  const panel=el("section","mky-panel");
  if(title)panel.append(el("h2","mky-heading",title));
  if(description)panel.append(el("p","mky-muted",description));
  if(content instanceof Node)panel.append(content);
  else if(content!=null)panel.append(el("div","",content));
  return panel;
}
export function createGrid(initial={}) {
  let options={rows:[],columns:[],pageSize:10,searchable:true,label:"データ一覧",...initial};
  let query="", sortKey=null, direction="asc", page=1, disposed=false;
  const element=el("section","mky-grid");
  const toolbar=el("div","mky-grid-toolbar");
  const search=el("input","mky-input");search.type="search";search.placeholder="検索…";
  search.setAttribute("aria-label","一覧を検索");
  const count=el("span","mky-muted");count.setAttribute("aria-live","polite");
  toolbar.append(search,count);
  const viewport=el("div","mky-grid-scroll");viewport.tabIndex=0;
  viewport.setAttribute("role","region");
  const table=el("table","mky-table"), head=el("thead"),body=el("tbody");
  const footer=el("div","mky-grid-footer");
  const prev=createButton({label:"前へ",variant:"secondary",size:"sm",onClick:()=>{page--;render();}});
  const next=createButton({label:"次へ",variant:"secondary",size:"sm",onClick:()=>{page++;render();}});
  const pageLabel=el("span","mky-muted");pageLabel.setAttribute("aria-live","polite");
  table.append(head,body);viewport.append(table);footer.append(prev,pageLabel,next);
  element.append(toolbar,viewport,footer);
  search.addEventListener("input",()=>{query=search.value;page=1;render();});
  function render(){
    if(disposed)return;
    const columns=options.columns;
    if(!columns.some(c=>c.key===sortKey&&c.sortable!==false)){sortKey=null;direction="asc";}
    if(options.searchable===false){query="";search.value="";}
    search.hidden=options.searchable===false;
    table.setAttribute("aria-label",text(options.label));
    viewport.setAttribute("aria-label",text(options.label)+"（横スクロール可能）");
    const view=gridView(options.rows,columns,{query,sortKey,direction,page,pageSize:options.pageSize});
    page=view.page;
    count.textContent=view.total+" 件";
    pageLabel.textContent=page+" / "+view.pages;
    prev.disabled=page<=1;next.disabled=page>=view.pages;
    const active=document.activeElement;
    const focusedKey=head.contains(active)?active.dataset.column:null;
    const tr=el("tr");
    for(const c of columns){
      const th=el("th");th.scope="col";
      if(c.sortable!==false){
        const current=c.key===sortKey;
        if(current)th.setAttribute("aria-sort",direction==="asc"?"ascending":"descending");
        const b=createButton({label:text(c.label??c.key)+(current?(direction==="asc"?" ↑":" ↓"):""),variant:"ghost",size:"sm",onClick:()=>{
          direction=sortKey===c.key&&direction==="asc"?"desc":"asc";sortKey=c.key;page=1;render();
        }});
        b.dataset.column=text(c.key);th.append(b);
      }else th.textContent=text(c.label??c.key);
      tr.append(th);
    }
    const action=typeof options.onRowAction==="function";
    if(action){const th=el("th","","操作");th.scope="col";tr.append(th);}
    head.replaceChildren(tr);
    const fragment=document.createDocumentFragment();
    for(const row of view.rows){
      const r=el("tr");
      for(const c of columns){
        const raw=typeof c.value==="function"?c.value(row):row?.[c.key];
        const v=typeof c.format==="function"?c.format(raw,row):raw;
        r.append(el("td","",v??"—"));
      }
      if(action){
        const td=el("td");
        td.append(createButton({label:options.actionLabel??"詳細",variant:"secondary",size:"sm",onClick:()=>options.onRowAction(row)}));r.append(td);
      }
      fragment.append(r);
    }
    if(!view.rows.length){
      const r=el("tr"),td=el("td","mky-empty",options.emptyMessage??"該当するデータがありません");
      td.colSpan=Math.max(1,columns.length+(action?1:0));r.append(td);fragment.append(r);
    }
    body.replaceChildren(fragment);
    if(focusedKey!=null)Array.from(head.querySelectorAll("button")).find(b=>b.dataset.column===focusedKey)?.focus({preventScroll:true});
  }
  render();
  return {element,update(patch={}){if(disposed)return;options={...options,...patch};render();},
    destroy(){if(disposed)return;disposed=true;element.remove();}};
}

/** One service per root. No network, storage, or game-state mutation. */
export function createUI(root,{theme="yaju"}={}){
  if(!(root instanceof HTMLElement))throw new TypeError("createUI requires an HTMLElement");
  if(activeRoots.has(root))throw new Error("Macky UI is already mounted on this root");
  const hadClass=root.classList.contains("mky-ui"),oldTheme=root.getAttribute("data-mky-theme");
  root.classList.add("mky-ui");root.dataset.mkyTheme=theme==="light"?"light":"yaju";
  const layers=el("div","mky-layers"), notices=el("div","mky-notices"), banners=el("div","mky-banners"), loads=el("div","mky-loads");
  notices.setAttribute("aria-label","通知");
  layers.append(notices,banners,loads);root.append(layers);
  const handles=new Set(),queue=[],loading=new Map();
  let current=null,disposed=false;
  const ensure=()=>{if(disposed)throw new Error("Macky UI has been destroyed");};
  function timed(container,element,ms,max){
    while(container.children.length>=max){
      const first=container.firstElementChild;
      const old=[...handles].find(h=>h.element===first);
      if(old)old.close();else first.remove();
    }
    let timer=null,closed=false;
    const handle={element,close(){if(closed)return;closed=true;if(timer!==null)clearTimeout(timer);handles.delete(handle);element.remove();}};
    handles.add(handle);container.append(element);
    if(ms>0)timer=setTimeout(handle.close,ms);
    return handle;
  }
  function toast(message,{type="info",duration=3500}={}){
    ensure();
    const node=el("div","mky-toast mky-tone--"+tone(type));
    const msg=el("span","mky-notice-text",message);
    msg.setAttribute("role",type==="danger"?"alert":"status");
    const close=createButton({label:"×",variant:"ghost",size:"sm",ariaLabel:"通知を閉じる",onClick:()=>handle.close()});
    node.append(msg,close);
    const handle=timed(notices,node,durationMs(duration,3500),4);return handle;
  }
  function banner(message,{title="",type="info",duration=1600}={}){
    ensure();
    const node=el("div","mky-banner mky-tone--"+tone(type));node.setAttribute("role","status");
    if(title)node.append(el("div","mky-banner-title",title));
    node.append(el("strong","",message));
    return timed(banners,node,durationMs(duration,1600),1);
  }
  function showLoading(message="読み込み中…"){
    ensure();
    const token=Symbol();
    const node=el("div","mky-loading");node.setAttribute("role","status");
    node.append(el("span","mky-spinner"),el("span","",message));
    loading.set(token,node);loads.replaceChildren(node);
    let closed=false;
    return {close(){if(closed)return;closed=true;loading.delete(token);
      const remaining=[...loading.values()];loads.replaceChildren(...remaining.slice(-1));}};
  }
  async function withLoading(task,message){
    if(typeof task!=="function")throw new TypeError("task must be a function");
    const h=showLoading(message);
    try{return await task();}finally{h.close();}
  }
  function popup(options={}){
    ensure();
    return new Promise((resolve,reject)=>{queue.push({options:{...options},resolve,reject});nextPopup();});
  }
  function nextPopup(){
    if(current||disposed||!queue.length)return;
    const request=queue.shift(),o=request.options;
    const previous=document.activeElement;
    const dialog=el("dialog","mky-dialog");
    if(typeof dialog.showModal!=="function"){request.reject(new Error("HTML dialog is not supported"));queueMicrotask(nextPopup);return;}
    const title=el("h2","mky-heading",o.title??"お知らせ");
    const message=el("p","mky-dialog-message",o.message??"");
    dialog.setAttribute("aria-label",text(o.title??"お知らせ"));
    const actions=el("div","mky-dialog-actions");
    const cancelable=o.cancelable!==false;
    let done=false;
    function finish(result,error){
      if(done)return;done=true;
      if(dialog.open)dialog.close();
      dialog.remove();current=null;
      if(previous instanceof HTMLElement&&previous.isConnected)previous.focus({preventScroll:true});
      if(error)request.reject(error);else request.resolve(result);
      queueMicrotask(nextPopup);
    }
    if(cancelable)actions.append(createButton({label:o.cancelLabel??"キャンセル",variant:"secondary",onClick:()=>finish(false)}));
    const ok=createButton({label:o.okLabel??"OK",variant:o.type==="danger"?"danger":"primary",onClick:()=>finish(true)});
    actions.append(ok);dialog.append(title,message,actions);layers.append(dialog);
    current={finish};
    dialog.addEventListener("cancel",e=>{e.preventDefault();if(cancelable)finish(false);});
    dialog.addEventListener("close",()=>finish(false));
    try{dialog.showModal();(cancelable?actions.firstElementChild:ok).focus();}
    catch(error){finish(false,error);}
  }
  const api={
    toast,banner,popup,
    alert:(message,options={})=>popup({...options,message,cancelable:false}),
    confirm:(message,options={})=>popup({...options,message,cancelable:true}),
    showLoading,withLoading,
    setTheme(value){ensure();root.dataset.mkyTheme=value==="light"?"light":"yaju";},
    destroy(){if(disposed)return;disposed=true;
      for(const h of [...handles])h.close();
      for(const r of queue.splice(0))r.resolve(false);
      if(current)current.finish(false);
      loading.clear();layers.remove();activeRoots.delete(root);
      if(!hadClass)root.classList.remove("mky-ui");
      if(oldTheme===null)root.removeAttribute("data-mky-theme");else root.setAttribute("data-mky-theme",oldTheme);
    }
  };
  activeRoots.set(root,api);return api;
}

export * from "./form.js";
export * from "./grid-model.js";
export * from "./advanced-grid.js";
export * from "./theme.js";
export * from "./widgets.js";
export * from "./services.js";
export * from "./registry.js";
export * from "./render.js";
