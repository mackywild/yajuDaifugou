import {node,action} from "./dom.js";
export function singleFlight(task){
 let running=false;return async function(...args){if(running)return undefined;running=true;try{return await task.apply(this,args);}finally{running=false;}};
}
export function guardUnsaved(isDirty,{target=window}={}){
 const listener=e=>{if(isDirty()){e.preventDefault();e.returnValue="";}};target.addEventListener("beforeunload",listener);return ()=>target.removeEventListener("beforeunload",listener);
}
export function createShortcutManager(target){
 const map=new Map();
 const normalize=s=>String(s).toLowerCase().split("+").map(x=>x.trim()).sort().join("+");
 const listener=e=>{if(e.defaultPrevented||e.isComposing||e.target?.closest?.("input,textarea,select,[contenteditable=true]"))return;const key=normalize([e.ctrlKey?"ctrl":"",e.metaKey?"meta":"",e.altKey?"alt":"",e.shiftKey?"shift":"",e.key].filter(Boolean).join("+"));const fn=map.get(key);if(fn){e.preventDefault();fn(e);}};
 target.addEventListener("keydown",listener);
 return {register(key,fn){const k=normalize(key);if(map.has(k))throw Error("Shortcut conflict: "+key);map.set(k,fn);return ()=>{if(map.get(k)===fn)map.delete(k);};},destroy(){target.removeEventListener("keydown",listener);map.clear();}};
}
export function createRetryPanel({message="処理に失敗しました",task,onSuccess,onError}={}){
 const element=node("section","mky-panel"),status=node("p","mky-field-error",message);
 const retry=action("再試行",singleFlight(async()=>{retry.disabled=true;status.textContent="実行中…";try{const result=await task();status.textContent="完了";onSuccess?.(result);}catch(e){status.textContent=String(e?.message||e);onError?.(e);}finally{retry.disabled=false;}}),"primary");element.append(status,retry);return element;
}
export function createNotificationHistory(ui,{limit=100}={}){
 limit=Math.max(0,Math.min(1000,Math.floor(Number(limit)||0)));const items=[],original=ui.toast;
 function wrapped(message,options={}){const h=original.call(ui,message,options);items.unshift({message:String(message),type:options.type||"info",at:new Date().toISOString()});if(items.length>limit)items.length=limit;return h;}
 ui.toast=wrapped;return {get:()=>items.map(x=>({...x})),clear:()=>{items.length=0;},destroy(){if(ui.toast===wrapped)ui.toast=original;}};
}
export function mockRequest({delay=300,mode="success",timeout=1000,data={ok:true},signal}={}){
 return new Promise((resolve,reject)=>{let timer,deadline,done=false;const finish=(error,value)=>{if(done)return;done=true;clearTimeout(timer);clearTimeout(deadline);signal?.removeEventListener("abort",abort);error?reject(error):resolve(value);};const abort=()=>finish(new Error("Aborted"));if(signal?.aborted){abort();return;}signal?.addEventListener("abort",abort,{once:true});deadline=setTimeout(()=>finish(new Error("Timeout")),Math.max(0,timeout));if(mode!=="timeout")timer=setTimeout(()=>mode==="error"?finish(new Error("Mock communication error")):finish(null,data),Math.max(0,delay));});
}
