import {node,action} from "./dom.js";
export function virtualWindow(total,scrollTop,rowHeight,viewportHeight,overscan=5){
 const height=Math.max(1,Number(rowHeight)||36),count=Math.max(0,Math.floor(Number(total)||0)),extra=Math.max(0,Math.floor(Number(overscan)||0));
 const start=Math.max(0,Math.min(count,Math.floor(Math.max(0,Number(scrollTop)||0)/height)-extra));
 const end=Math.min(count,Math.max(start,Math.ceil((Math.max(0,Number(scrollTop)||0)+Math.max(0,Number(viewportHeight)||0))/height)+extra));
 return {start,end,before:start*height,after:(count-end)*height};
}
/** Fixed-height read-only virtual table; editing uses createAdvancedGrid. */
export function createVirtualGrid({rows=[],columns=[],rowHeight=36,height=360,overscan=5,label="大量データ一覧",onRowAction}={}){
 const element=node("section","mky-grid mky-virtual"),bar=node("div","mky-grid-toolbar"),search=node("input","mky-input"),status=node("span","mky-muted"),header=node("div","mky-virtual-row mky-virtual-head"),viewport=node("div","mky-virtual-scroll"),spacer=node("div","mky-virtual-spacer"),windowEl=node("div","mky-virtual-window");
 rowHeight=Math.max(28,Math.min(80,Number(rowHeight)||36));height=Math.max(120,Math.min(1000,Number(height)||360));
 const template="repeat("+Math.max(1,columns.length)+",minmax(100px,1fr))";
 let data=rows.slice(),source=rows.slice(),sort=null,desc=false,disposed=false;
 search.type="search";search.placeholder="検索";search.setAttribute("aria-label","仮想グリッド検索");status.setAttribute("role","status");
 header.style.gridTemplateColumns=template;header.setAttribute("role","row");viewport.style.height=height+"px";viewport.tabIndex=0;viewport.setAttribute("aria-label",label);viewport.setAttribute("role","region");
 const table=node("div","mky-virtual-table");table.setAttribute("role","table");table.setAttribute("aria-label",label);windowEl.setAttribute("role","rowgroup");spacer.append(windowEl);viewport.append(spacer);table.append(header,viewport);bar.append(search,status);element.append(bar,table);
 for(const c of columns){const cell=node("div");cell.setAttribute("role","columnheader");const b=action(c.label||c.key,()=>{desc=sort===c.key&&!desc;sort=c.key;refresh();},"ghost");cell.append(b);header.append(cell);}
 function refresh(){const q=search.value.toLowerCase();data=source.filter(r=>!q||columns.some(c=>String(r[c.key]??"").toLowerCase().includes(q)));
 if(sort)data.sort((a,b)=>{const x=a[sort],y=b[sort];const n=typeof x==="number"&&typeof y==="number"?x-y:String(x??"").localeCompare(String(y??""),"ja",{numeric:true});return desc?-n:n;});
 viewport.scrollTop=0;table.setAttribute("aria-rowcount",String(data.length+1));render();}
 function render(){if(disposed)return;const w=virtualWindow(data.length,viewport.scrollTop,rowHeight,height,overscan);
 spacer.style.height=data.length*rowHeight+"px";windowEl.style.transform="translateY("+w.before+"px)";windowEl.replaceChildren();
 for(let i=w.start;i<w.end;i++){const r=data[i],row=node("div","mky-virtual-row");row.style.gridTemplateColumns=template;row.style.height=rowHeight+"px";row.setAttribute("role","row");row.setAttribute("aria-rowindex",String(i+2));for(const c of columns){const cell=node("div","mky-virtual-cell",r[c.key]??"");cell.setAttribute("role","cell");cell.title=String(r[c.key]??"");row.append(cell);}if(onRowAction)row.addEventListener("dblclick",()=>onRowAction(r));windowEl.append(row);}
 status.textContent=data.length+" 件 / DOM "+(w.end-w.start)+" 行";}
 search.addEventListener("input",refresh);viewport.addEventListener("scroll",render);refresh();
 return {element,update(next){source=next.slice();refresh();},getRenderedCount:()=>windowEl.children.length,destroy(){disposed=true;viewport.removeEventListener("scroll",render);element.remove();}};
}
