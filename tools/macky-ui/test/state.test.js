import test from "node:test";
import assert from "node:assert/strict";
import * as api from "../src/index.js";

// A small deterministic DOM/event adapter, not a browser emulator.
// Real browser coverage is kept separately in demo/browser-review.js.
function withDOM(){
 let timers=[],seq=0;
 const win={events:new Map(),addEventListener(n,f){const a=this.events.get(n)||[];a.push(f);this.events.set(n,a);},removeEventListener(n,f){this.events.set(n,(this.events.get(n)||[]).filter(x=>x!==f));},emit(n,e={}){for(const f of this.events.get(n)||[])f(e);}};
 let doc;
 class E{
  constructor(tag){this.tagName=tag.toUpperCase();this.children=[];this.dataset={};this.style={};this.attrs={};this.events={};this.classList={toggle(){},add(){}};this.value="";this.checked=false;this.disabled=false;this.readOnly=false;this.parent=null;this._text="";this.type=tag==="input"?"text":"";}
  set textContent(v){this._text=String(v);for(const c of this.children)c.parent=null;this.children=[];}
  get textContent(){return this._text+this.children.map(x=>x.textContent).join("");}
  append(...xs){for(const x of xs){if(x.tagName==="FRAGMENT"){this.append(...x.children);continue;}this.children.push(x);x.parent=this;}}
  replaceChildren(...xs){for(const x of this.children)x.parent=null;this.children=[];this._text="";this.append(...xs);}
  addEventListener(n,f){(this.events[n]??=[]).push(f);}
  removeEventListener(n,f){this.events[n]=(this.events[n]||[]).filter(x=>x!==f);}
  emit(n,e={}){for(const f of this.events[n]||[])f({target:this,preventDefault(){},stopPropagation(){},...e});}
  setAttribute(k,v){this.attrs[k]=v;}
  removeAttribute(k){delete this.attrs[k];}
  contains(x){return this===x||this.children.some(c=>c.contains(x));}
  matches(s){return s.split(",").some(t=>t.toUpperCase()===this.tagName)||s==="td[data-r]"&&this.tagName==="TD"&&this.dataset.r!=null;}
  closest(s){return this.matches(s)?this:this.parent?.closest(s);}
  querySelectorAll(s){const match=x=>s==="[data-r]"?x.dataset.r!=null:s==="input"?x.tagName==="INPUT":s==="button"?x.tagName==="BUTTON":s.startsWith('[data-r="')?(()=>{const m=s.match(/data-r="(\d+)".*data-c="(\d+)"/);return m&&x.dataset.r===m[1]&&x.dataset.c===m[2];})():false;const a=[];for(const x of this.children){if(match(x))a.push(x);a.push(...x.querySelectorAll(s));}return a;}
  querySelector(s){return this.querySelectorAll(s)[0]||null;}
  focus(){const old=doc.activeElement;doc.activeElement=this;if(old&&old!==this)old.emit("blur");this.emit("focus");}
  select(){}
  remove(){if(this.parent)this.parent.children=this.parent.children.filter(x=>x!==this);this.parent=null;}
  click(){if(!this.disabled)this.emit("click");}
 }
 doc={activeElement:null,createElement:t=>new E(t),createDocumentFragment:()=>new E("fragment"),createTextNode:t=>{const e=new E("text");e.textContent=t;return e;}};
 const globals={document:doc,window:win,Node:E,setTimeout:fn=>{const id=++seq;timers.push({id,fn});return id;},clearTimeout:id=>{timers=timers.filter(t=>t.id!==id);}};
 const old=new Map(Object.keys(globals).map(k=>[k,Object.getOwnPropertyDescriptor(globalThis,k)]));
 for(const [k,v] of Object.entries(globals))Object.defineProperty(globalThis,k,{value:v,writable:true,configurable:true});
 return {doc,win,flush(){while(timers.length)timers.shift().fn();},restore(){timers=[];for(const [k,d] of old){if(d)Object.defineProperty(globalThis,k,d);else delete globalThis[k];}}};
}
async function scenario(fn){const env=withDOM();try{await fn(env);}finally{env.restore();}}
const rows=[{id:1,name:"A",note:"x"},{id:2,name:"B",note:"y"}],columns=[{key:"id",readonly:true},{key:"name"},{key:"note"}];
const cell=(g,r,c)=>g.element.querySelector('[data-r="'+r+'"][data-c="'+c+'"]');
function edit(g,r,c,v){g.select(r,c);cell(g,r,c).emit("dblclick");const e=cell(g,r,c).querySelector("input");assert.ok(e);e.value=v;return e;}
function blurTo(g,env,r,c){env.win.emit("pointerdown");cell(g,r,c).focus();env.win.emit("pointerup");}
test("RV-08 all submitted fields validated across access changes",()=>scenario(async env=>{
 let submitted=[];const f=api.createForm({fields:[{name:"name",required:true,value:""},{name:"other",value:"OK"}],onSubmit:v=>submitted.push(v)});
 try{assert.equal(f.validate(),false);for(const patch of [{readonly:true},{disabled:true,readonly:false}]){
  f.setAccess({fields:[{name:"name",...patch}]});assert.equal(f.validate(),false);assert.equal(await f.submit(),false);assert.match(f.element.textContent,/必須項目です/);assert.match(env.doc.activeElement.textContent,/必須項目です/);}
  assert.deepEqual(submitted,[]);f.setValues({name:"valid"});assert.equal(await f.submit(),true);assert.deepEqual(submitted,[{name:"valid",other:"OK"}]);
  f.setAccess({fields:[{name:"name",disabled:false,readonly:false}]});assert.equal(f.element.querySelector("input").disabled,false);
 }finally{f.destroy();}
}));
test("RV-08 readonly range, custom rule, optional value and whole-form lock",()=>scenario(async()=>{
 let calls=0;const f=api.createForm({fields:[{name:"date",type:"date",min:"2026-11-01",max:"2026-11-30",value:"2026-12-01",readonly:true},{name:"code",value:"no",disabled:true,validate:v=>v==="OK"||"コード不正"},{name:"optional",disabled:true,value:""}],onSubmit:()=>calls++});
 try{assert.equal(await f.submit(),false);assert.match(f.element.textContent,/以下/);assert.match(f.element.textContent,/コード不正/);f.setValues({date:"2026-11-30",code:"OK"});assert.equal(await f.submit(),true);f.setAccess({disabled:true});assert.equal(await f.submit(),false);assert.equal(calls,1);}finally{f.destroy();}
}));
test("RV-09 committed values during callback, before repaint, multi-cell and after repaint",()=>scenario(async env=>{
 let g,callback;g=api.createAdvancedGrid({rows,columns,onChange:()=>{callback=g.copy({spreadsheetSafe:false});}});
 try{edit(g,0,1,"C");env.win.emit("pointerdown");cell(g,1,1).focus();env.win.emit("pointerup");assert.equal(callback,"C");g.select(0,1);g.select(1,2,true);assert.equal(g.copy({spreadsheetSafe:false}),"C\tx\r\nB\ty");env.flush();g.select(0,1);g.select(1,2,true);assert.equal(g.copy({spreadsheetSafe:false}),"C\tx\r\nB\ty");}finally{g.destroy();}
}));
test("RV-09 pending repaint does not retarget sorted row for copy or paste",()=>scenario(async env=>{
 const g=api.createAdvancedGrid({rows,columns});try{g.element.querySelectorAll("button").find(b=>b.textContent==="name").click();edit(g,0,1,"Z");blurTo(g,env,1,1);g.select(0,1);assert.equal(g.copy(),"Z");g.paste("Q");assert.equal(g.getRows().find(r=>r.id===1).name,"Q");assert.equal(g.getRows().find(r=>r.id===2).name,"B");env.flush();g.select(0,1);assert.equal(g.copy(),"B");g.undo();assert.equal(g.getRows()[0].name,"Z");}finally{g.destroy();}
}));
test("RV-09 filtered row remains copyable until displayed order updates; deletion targets same ID",()=>scenario(async env=>{
 const g=api.createAdvancedGrid({rows,columns});try{const search=g.element.querySelector("input");search.value="A";search.emit("input");edit(g,0,1,"Z");env.win.emit("pointerdown");search.focus();env.win.emit("pointerup");g.select(0,1);assert.equal(g.copy(),"Z");env.flush();assert.equal(g.copy(),"");search.value="";search.emit("input");g.select(0,2);g.element.querySelectorAll("button").find(b=>b.textContent==="セル削除").click();assert.equal(g.getRows()[0].note,"");assert.equal(g.getRows()[1].note,"y");}finally{g.destroy();}
}));
