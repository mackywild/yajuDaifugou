import test from "node:test";
import assert from "node:assert/strict";
import * as api from "../src/index.js";
import {createVueKit} from "../src/vue.js";
import {builtinPages,resolveRoute} from "../routes.mjs";
test("form required/range/pattern/custom validation",()=>{
 assert.ok(api.validateValue("",{required:true}));assert.ok(api.validateValue(false,{required:true}));assert.equal(api.validateValue(0,{required:true}),"");
 assert.ok(api.validateValue(8,{min:10}));assert.ok(api.validateValue("aa",{minLength:3}));assert.equal(api.validateValue("ABC",{pattern:"^[A-Z]+$"}),"");
 assert.equal(api.validateValue("a",{validate:(v,all)=>v===all.confirm||"mismatch"},{confirm:"b"}),"mismatch");
});
test("grid store immutable row data, undo redo and change set",()=>{
 const source=[{id:1,n:1},{id:2,n:2}],s=api.createGridStore(source);
 s.apply([{id:1,n:10},{id:3,n:3}]);assert.deepEqual(s.changes(),{added:[{id:3,n:3}],updated:[{id:1,n:10}],deleted:[{id:2,n:2}]});
 assert.equal(s.undo(),true);assert.deepEqual(s.get(),source);assert.equal(s.redo(),true);assert.equal(s.isDirty(),true);
 s.commit();assert.equal(s.isDirty(),false);s.apply([]);s.reset();assert.deepEqual(s.get(),[{id:1,n:10},{id:3,n:3}]);assert.deepEqual(source,[{id:1,n:1},{id:2,n:2}]);
});
test("history limit",()=>{const s=api.createGridStore([{id:1,v:0}],{historyLimit:1});s.apply([{id:1,v:1}]);s.apply([{id:1,v:2}]);assert.ok(s.undo());assert.equal(s.undo(),false);s.apply([{id:1,v:3}]);assert.equal(s.canRedo(),false);});
test("CSV/TSV roundtrip and formula escaping",()=>{
 const rows=[["a,b",'a"b',"a\nb"],["","","z"]];assert.deepEqual(api.parseDelimited(api.encodeDelimited(rows)),rows);
 assert.deepEqual(api.parseDelimited("a\tb\r\nc\td","\t"),[["a","b"],["c","d"]]);assert.throws(()=>api.parseDelimited('"unclosed'));
 assert.equal(api.encodeDelimited([["=1+1","-12",12]]),"'=1+1,'-12,12");
});
test("cell validation and permissions",()=>{
 assert.equal(api.normalizeCell("42",{key:"n",type:"number"},{}),42);assert.equal(api.normalizeCell("false",{key:"n",type:"boolean"},{}),false);
 assert.throws(()=>api.normalizeCell("x",{key:"n",type:"number"},{}));assert.throws(()=>api.normalizeCell("x",{key:"n",readonly:true},{}));assert.throws(()=>api.normalizeCell("x",{key:"n",editable:()=>false},{}));
});
test("six themes, safe tokens and storage",()=>{
 assert.equal(Object.keys(api.themes).length,6);for(const t of Object.values(api.themes))assert.ok(api.themeCSS(t).includes("--mky-primary"));
 assert.throws(()=>api.validateTheme({primary:"url(x)"}));assert.throws(()=>api.validateTheme({radius:"calc(1px)"}));
 const m=new Map(),storage={setItem:(k,v)=>m.set(k,v),getItem:k=>m.get(k)??null};api.saveTheme(storage,"t",api.themes.business);assert.deepEqual(api.loadTheme(storage,"t"),api.themes.business);
});
test("all registry factories and default copies",()=>{
 for(const [type,item] of Object.entries(api.registry)){const o=api.getDefaults(type);assert.deepEqual(o,item.options);assert.notEqual(o,item.options);if(!item.api.startsWith("ui."))assert.equal(typeof api[item.api],"function");assert.match(api.componentCode(type,o,"vue"),/kit\.MComponent/);assert.match(api.componentCode(type,o,"js"),/api\.mountComponent/);}
});
test("screen definitions, size/IDs and reserved keys",()=>{
 assert.throws(()=>api.cloneOptions(JSON.parse('{"__proto__":{"x":1}}')));assert.throws(()=>api.normalizeScreen({components:[{id:1,type:"button"},{id:1,type:"button"}]}));assert.throws(()=>api.normalizeScreen({components:[{type:"unknown"}]}));assert.throws(()=>api.normalizeScreen({components:Array.from({length:101},()=>({type:"button"}))}));
});
test("HTML, JS, SFC export escapes script terminators",()=>{
 const spec={title:"</script><img src=x>",components:[{type:"button",options:{label:"</script>"}}]};
 for(const type of ["js","vue"]){const html=api.screenHTML(spec,type);assert.equal((html.match(/<\/script>/g)||[]).length,1);assert.ok(html.includes("\\u003c/script\\u003e"));assert.ok(html.includes("./dist/index.js"));}
 assert.match(api.screenJS(spec),/export function mountApp/);const sfc=api.screenVue(spec);assert.equal((sfc.match(/<\/script>/g)||[]).length,1);assert.ok(sfc.includes("\\u003c/script\\u003e"));assert.match(sfc,/<template>/);
});
test("virtual window bounds",()=>{
 assert.deepEqual(api.virtualWindow(100000,0,36,360,5),{start:0,end:15,before:0,after:99985*36});
 const w=api.virtualWindow(100000,36000,36,360,5);assert.equal(w.start,995);assert.equal(w.end,1015);assert.deepEqual(api.virtualWindow(0,100,36,360),{start:0,end:0,before:0,after:0});
});
test("single flight, history and release on rejected task",async()=>{
 let release,calls=0;const p=new Promise(r=>release=r),fn=api.singleFlight(async()=>{calls++;await p;return 7;});const first=fn();assert.equal(await fn(),undefined);release();assert.equal(await first,7);assert.equal(await fn(),7);assert.equal(calls,2);
 let failed=0;const err=api.singleFlight(async()=>{if(!failed++)throw Error("fail");return 1;});await assert.rejects(err());assert.equal(await err(),1);
 const ui={toast:()=>({close(){}})},original=ui.toast,h=api.createNotificationHistory(ui,{limit:2});ui.toast("a");ui.toast("b");ui.toast("c");assert.deepEqual(h.get().map(x=>x.message),["c","b"]);h.clear();assert.deepEqual(h.get(),[]);h.destroy();assert.equal(ui.toast,original);
});
test("mock communication success, failure, timeout and abort",async()=>{
 assert.deepEqual(await api.mockRequest({delay:1,timeout:100,data:{n:1}}),{n:1});
 await assert.rejects(api.mockRequest({mode:"error",delay:1,timeout:100}),/communication error/);await assert.rejects(api.mockRequest({mode:"timeout",timeout:5}),/Timeout/);
 const c=new AbortController();c.abort();await assert.rejects(api.mockRequest({signal:c.signal}),/Aborted/);
});
test("Vue component registrations without DOM",()=>{
 const Vue={defineComponent:o=>o,h(){},ref(){},onMounted(){},onBeforeUnmount(){},watch(){},inject(){}},kit=createVueKit(Vue,{}),names=[];
 kit.install({provide(){},component(name){names.push(name)}});for(const n of ["MButton","MForm","MComponent","MAdvancedGrid","MVirtualGrid"])assert.ok(names.includes(n));assert.equal(names.length,new Set(names).size);
});
test("studio routes and exported playground dependencies",()=>{
 for(const p of builtinPages)assert.ok(resolveRoute(new URL(p.url,"http://localhost").pathname));
 assert.equal(resolveRoute("/playground/dist/virtual-grid.js"),"dist/virtual-grid.js");assert.equal(resolveRoute("/playground/vendor/vue.js"),"node_modules/vue/dist/vue.esm-browser.js");assert.equal(resolveRoute("/playground/%2e%2e%2fsecret.js"),null);assert.equal(resolveRoute("/playground/dist/other.js"),null);
});
