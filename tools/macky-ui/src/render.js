import {node,action} from "./dom.js";
import {registry,cloneOptions,normalizeScreen} from "./registry.js";
export function mountComponent(api,type,options,{ui,onEvent=()=>{}}={}){
 if(!Object.hasOwn(registry,type))throw Error("Unknown component");
 const o=cloneOptions(options);const emit=name=>(...args)=>onEvent(name,args.length===1?args[0]:args);
 const handlers={onClick:()=>onEvent("click",o.label),onChange:emit("change"),onSelect:emit("select"),onRowAction:emit("rowAction"),onSubmit:emit("submit"),onComplete:emit("complete"),onMove:emit("move"),onError:e=>onEvent("error",String(e?.message||e))};
 let result;
 if(["popup","toast","banner","loading"].includes(type)){
   if(!ui)throw Error("UI service required");
   result=action(registry[type].label,async()=>{try{
     if(type==="popup")onEvent("confirm",await ui.confirm(o.message,o));
     else if(type==="loading")await ui.withLoading(()=>new Promise(r=>setTimeout(r,Math.max(0,Math.min(60000,Number(o.duration)||1000)))),o.message);
     else{ui[type](o.message,o);onEvent(type,o.message);}
   }catch(e){handlers.onError(e);}},"primary");
 }else if(type==="context"){
   const target=node("div","mky-context-target","右クリックまたは Shift+F10");target.tabIndex=0;
   const menu=api.createContextMenu(target,{...o,onSelect:handlers.onSelect});result={element:target,destroy(){menu.destroy();target.remove();}};
 }else{
   if(type==="advancedGrid"){let n=0;o.newRow=rows=>{const key=o.rowKey||"id";const col=o.columns.find(c=>c.key===key);let id;do{id=col?.type==="number"?Math.max(0,...rows.map(r=>Number(r[key])||0))+(++n):"new-"+(++n);}while(rows.some(r=>r[key]===id));return Object.fromEntries(o.columns.map(c=>[c.key,c.key===key?id:c.type==="number"?0:c.type==="boolean"?false:""]));};}
   if(type==="retry")o.task=()=>api.mockRequest();
   const factory=api[registry[type].api];if(typeof factory!=="function")throw Error("Component API unavailable");
   result=factory({...o,...handlers});
 }
 const element=result.element||result;
 return {element,handle:result,destroy(){if(result.destroy)result.destroy();else element.remove();}};
}
export function mountScreen(api,root,screen,{ui,onEvent=()=>{}}={}){
 const spec=normalizeScreen(screen),main=node("main","mky-screen"),grid=node("div","mky-screen-grid"),handles=[];
 main.append(node("h1","",spec.title),grid);root.append(main);
 try{for(const c of spec.components){const cell=node("div",c.span===1?"mky-span-1":"mky-span-2");const h=mountComponent(api,c.type,c.options,{ui,onEvent:(name,value)=>onEvent(name,value,c.id)});handles.push(h);cell.append(h.element);grid.append(cell);}}
 catch(e){handles.forEach(h=>h.destroy());main.remove();throw e;}
 return {element:main,destroy(){handles.forEach(h=>h.destroy());main.remove();}};
}
