import {createServer} from "node:http";
import {readFile} from "node:fs/promises";
import {fileURLToPath} from "node:url";
import {join} from "node:path";
const root=fileURLToPath(new URL(".",import.meta.url));
const port=Number(process.env.PORT||4173);
const allowed=new Set(["demo/index.html","demo/demo.js","demo/vue.html","demo/vue-demo.js","demo/demo.css","demo/test.html","demo/browser-test.js","src/index.js","src/model.js","src/vue.js","src/style.css"]);
const types={".html":"text/html; charset=utf-8",".js":"text/javascript; charset=utf-8",".css":"text/css; charset=utf-8"};
createServer(async(req,res)=>{
  if(!["GET","HEAD"].includes(req.method)){res.writeHead(405,{Allow:"GET, HEAD"}).end();return;}
  const path=new URL(req.url,"http://localhost").pathname;
  const file=path==="/"?"demo/index.html":path==="/vendor/vue.js"?"node_modules/vue/dist/vue.esm-browser.js":path.slice(1);
  if(!allowed.has(file)&&file!=="node_modules/vue/dist/vue.esm-browser.js"){res.writeHead(404).end("Not found");return;}
  try{
    const content=await readFile(join(root,file));
    const ext=file.slice(file.lastIndexOf("."));
    res.writeHead(200,{"Content-Type":types[ext],"Cache-Control":"no-store","X-Content-Type-Options":"nosniff"});
    res.end(req.method==="HEAD"?undefined:content);
  }catch{res.writeHead(404).end("Not found. For Vue demo, run npm install first.");}
}).listen(port,"127.0.0.1",()=>console.log("Macky UI: http://127.0.0.1:"+port));
