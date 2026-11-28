import test from "node:test";
import assert from "node:assert/strict";
import {builtinPages,customPages,resolveRoute,safeName} from "../routes.mjs";
test("launcher and all built-in pages map to approved files",()=>{
 assert.equal(resolveRoute("/"),"demo/launcher.html");
 for(const item of builtinPages)assert.ok(resolveRoute(new URL(item.url,"http://localhost").pathname));
});
test("custom pages list HTML only, with encoded names",()=>{
 const file=name=>({name,isFile:()=>true});
 assert.deepEqual(customPages([file("日本語.html"),file("script.js"),file("../bad.html"),{name:"symlink.html",isFile:()=>false}]).map(p=>p.url),["/playground/"+encodeURIComponent("日本語.html")]);
 assert.equal(resolveRoute("/playground/"+encodeURIComponent("日本語.html")),"playground/日本語.html");
});
test("no arbitrary filesystem access",()=>{
 for(const path of ["/package.json","/playground/../package.json","/playground/%2e%2e%2fsecret.js","/playground/foo%5cbar.js","/playground/.env","/playground/%ZZ","/dist/other.js"])assert.equal(resolveRoute(path),null,path);
 assert.equal(safeName("my test.css"),true);
 assert.equal(resolveRoute("/dist/manifest.json"),"dist/manifest.json");
});
