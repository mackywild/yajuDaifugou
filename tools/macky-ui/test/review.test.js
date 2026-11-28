import test from "node:test";
import assert from "node:assert/strict";
import {planCSVImport,createGridStore,validateValue,encodeDelimited,screenVue,screenJS} from "../src/index.js";
const rows=[{id:1,name:"A",role:"admin"},{id:2,name:"B",role:"user"}],columns=[{key:"id",readonly:true},{key:"name",required:true},{key:"role",readonly:true}],opt={rows,columns,allowCSVImport:true};
test("RV-01 default deny, all headers and required values",()=>{
 assert.throws(()=>planCSVImport("id,name,role\n1,A,admin\n2,B,user",{rows,columns}));
 for(const text of ["id\n1","id,name,role\n","id,name,role\n1,,admin\n2,B,user"])assert.throws(()=>planCSVImport(text,opt));
});
test("RV-01 readonly, canEdit, canAdd and canDelete cannot be bypassed",()=>{
 for(const [text,extra] of [["id,name,role\n1,A,changed\n2,B,user",{}],["id,name,role\n1,C,admin\n2,B,user",{canEdit:()=>false}],["id,name,role\n1,A,admin",{}],["id,name,role\n1,A,admin\n2,B,user\n3,C,user",{}]])assert.throws(()=>planCSVImport(text,{...opt,...extra}));
});
test("RV-01 original typed IDs, diff and atomic model history",()=>{
 const s=createGridStore(rows);s.apply([{...rows[0],name:"X"},rows[1]]);
 assert.throws(()=>planCSVImport("id\n1",{...opt,rows:s.get()}));assert.equal(s.get()[0].name,"X");assert.ok(s.canUndo());assert.equal(s.canRedo(),false);
 const p=planCSVImport("id,name,role\n1,C,admin\n2,B,user",opt);
 assert.deepEqual(p.rows,[{id:1,name:"C",role:"admin"},rows[1]]);assert.deepEqual(p.changes,{added:[],updated:[{id:1,name:"C",role:"admin"}],deleted:[]});
 const q=planCSVImport("id,name,role\n1,A,admin\n3,C,user",{...opt,canAdd:()=>true,canDelete:()=>true});
 assert.deepEqual(q.changes.added,[{id:3,name:"C",role:"user"}]);assert.deepEqual(q.changes.deleted,[rows[1]]);
 assert.equal(rows[0].name,"A");
});
test("RV-01 reject duplicate, ambiguous and malformed IDs",()=>{
 assert.throws(()=>planCSVImport("id,name,role\n1,A,admin\n1,A,admin",opt));
 assert.throws(()=>planCSVImport("id,name,role\n1,A,admin",{...opt,rows:[rows[0],{...rows[1],id:"1"}]}));
 assert.throws(()=>planCSVImport("id,name,role\n1,A,admin\n2,B,user\n03,C,user",{...opt,canAdd:()=>true}));
});
test("RV-03 date bounds, invalid calendar date, leap year and optional value",()=>{
 const rules={min:"2026-11-01",max:"2026-11-30"};
 for(const v of ["2026-11-01","2026-11-28","2026-11-30",""])assert.equal(validateValue(v,rules),"");
 for(const v of ["2026-10-31","2026-12-01","2026-11-31"])assert.ok(validateValue(v,{type:"date",...rules}));
 assert.equal(validateValue("2028-02-29",{type:"date"}),"");assert.ok(validateValue("2026-02-29",{type:"date"}));assert.ok(validateValue("",{type:"date",required:true}));
});
test("RV-03 time bounds including midnight wrap",()=>{
 const rules={type:"time",min:"22:00",max:"02:00"};for(const v of ["22:00","23:30","00:00","02:00"])assert.equal(validateValue(v,rules),"");assert.ok(validateValue("12:00",rules));assert.ok(validateValue("24:00",{type:"time"}));assert.equal(validateValue("12:30",{type:"time",min:"12:00",max:"13:00"}),"");
});
test("RV-04 safe external TSV and raw internal TSV",()=>{
 assert.equal(encodeDelimited([["=1+1","-12",12]],"\t",{spreadsheetSafe:true}),"'=1+1\t'-12\t12");assert.equal(encodeDelimited([["=1+1"]],"\t",{spreadsheetSafe:false}),"=1+1");
});
test("RV-02 generated JS/SFC awaitable submit task hook",()=>{
 const spec={components:[{type:"form"}]};assert.match(screenVue(spec),/defineProps\(\{ submitTask: Function \}\)/);assert.match(screenVue(spec),/:submit-task=/);assert.match(screenJS(spec),/onEvent,submitTask/);
});
