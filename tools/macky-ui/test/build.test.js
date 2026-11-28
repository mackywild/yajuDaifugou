import test from "node:test";
import assert from "node:assert/strict";
import {mkdtemp,rm,readFile,writeFile} from "node:fs/promises";
import {tmpdir} from "node:os";
import {join} from "node:path";
import {pathToFileURL} from "node:url";
import {createHash} from "node:crypto";
import {build,assets} from "../build.mjs";
test("dist copies all ESM assets, integrity and exports",async()=>{
 const dir=await mkdtemp(join(tmpdir(),"macky-ui-"));
 try{
  const m=await build(dir);
  assert.equal(m.format,"esm");assert.equal(Object.keys(m.files).length,4);
  const onDisk=JSON.parse(await readFile(join(dir,"manifest.json"),"utf8"));assert.deepEqual(onDisk,m);
  for(const name of assets){
   const bytes=await readFile(join(dir,name));assert.equal(bytes.length,m.files[name].bytes);
   assert.equal(createHash("sha256").update(bytes).digest("hex"),m.files[name].sha256);
   assert.deepEqual(bytes,await readFile(new URL("../src/"+name,import.meta.url)));
  }
  await writeFile(join(dir,"package.json"),'{"type":"module"}');
  const core=await import(pathToFileURL(join(dir,"index.js")).href);
  const vue=await import(pathToFileURL(join(dir,"vue.js")).href);
  assert.equal(typeof core.createUI,"function");assert.equal(typeof vue.createVueKit,"function");
  assert.equal(core.gridView([{n:2},{n:1}],[{key:"n"}],{sortKey:"n"}).rows[0].n,1);
 }finally{await rm(dir,{recursive:true,force:true});}
});
