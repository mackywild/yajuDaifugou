import test from "node:test";
import assert from "node:assert/strict";
import {gridView,buttonClass,durationMs} from "../src/model.js";
const rows=[{id:1,name:"東京",score:10},{id:2,name:"大阪",score:2},{id:3,name:"東京",score:null}];
const columns=[{key:"name"},{key:"score"},{key:"id",searchable:false}];
test("numeric asc/desc and null last; input not mutated",()=>{
 assert.deepEqual(gridView(rows,columns,{sortKey:"score"}).rows.map(r=>r.id),[2,1,3]);
 assert.deepEqual(gridView(rows,columns,{sortKey:"score",direction:"desc"}).rows.map(r=>r.id),[1,2,3]);
 assert.deepEqual(rows.map(r=>r.id),[1,2,3]);
});
test("filter Japanese, case insensitive, search exclusions",()=>{
 assert.equal(gridView(rows,columns,{query:" 東京 "}).total,2);
 assert.equal(gridView(rows,[{key:"id",searchable:false}],{query:"1"}).total,0);
 assert.equal(gridView([{name:"ABC"}],columns,{query:"abc"}).total,1);
});
test("paging, empty state, invalid input",()=>{
 assert.equal(gridView(rows,columns,{page:99,pageSize:2}).page,2);
 assert.equal(gridView(rows,columns,{page:99,pageSize:2}).rows[0].id,3);
 assert.equal(gridView([],columns,{page:99}).pages,1);
 assert.equal(gridView(rows,columns,{pageSize:0}).pageSize,1);
 assert.equal(gridView(rows,columns,{pageSize:999}).pageSize,200);
 assert.equal(gridView(rows,columns,{pageSize:"bad"}).pageSize,10);
});
test("computed value and disabled sort",()=>{
 const cols=[{key:"double",value:r=>r.id*2,sortable:false}];
 assert.equal(gridView(rows,cols,{query:"4"}).rows[0].id,2);
 assert.equal(gridView(rows,cols,{sortKey:"double",direction:"desc"}).rows[0].id,1);
});
test("safe class and duration fallback",()=>{
 assert.equal(buttonClass("bad","bad"),"mky-btn mky-btn--primary mky-btn--md");
 assert.equal(durationMs(0),0);assert.equal(durationMs(-1),0);
 assert.equal(durationMs(999999),60000);assert.equal(durationMs("bad"),3000);
});
