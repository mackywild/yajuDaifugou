import {mkdir,copyFile,readFile,writeFile} from "node:fs/promises";
import {fileURLToPath,pathToFileURL} from "node:url";
import {join,resolve} from "node:path";
import {createHash} from "node:crypto";
const root=fileURLToPath(new URL(".",import.meta.url));
export const assets=["index.js","model.js","vue.js","style.css","dom.js","form.js","grid-model.js","advanced-grid.js","theme.js","widgets.js","services.js","registry.js","render.js","virtual-grid.js"];
// Native ESM distribution: no bundler or external download required.
export async function build(output=join(root,"dist")){
  await mkdir(output,{recursive:true});
  const pkg=JSON.parse(await readFile(join(root,"package.json"),"utf8"));
  const files={};
  for(const name of assets){
    const dest=join(output,name);
    await copyFile(join(root,"src",name),dest);
    const bytes=await readFile(dest);
    files[name]={bytes:bytes.length,sha256:createHash("sha256").update(bytes).digest("hex")};
  }
  const manifest={name:pkg.name,version:pkg.version,format:"esm",entries:{core:"index.js",vue:"vue.js",css:"style.css"},files};
  await writeFile(join(output,"manifest.json"),JSON.stringify(manifest,null,2)+"\n");
  return manifest;
}
if(process.argv[1]&&pathToFileURL(resolve(process.argv[1])).href===import.meta.url){
  const m=await build();
  console.log("Built "+m.name+"@"+m.version+" -> dist/ ("+Object.keys(m.files).length+" assets)");
}
