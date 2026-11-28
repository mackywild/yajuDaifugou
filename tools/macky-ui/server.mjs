import {createServer} from "node:http";
import {readFile,readdir,lstat} from "node:fs/promises";
import {fileURLToPath} from "node:url";
import {join,extname} from "node:path";
import {builtinPages,customPages,resolveRoute} from "./routes.mjs";
const root=fileURLToPath(new URL(".",import.meta.url));
const port=Number(process.env.PORT||4173);
const types={".html":"text/html; charset=utf-8",".js":"text/javascript; charset=utf-8",".mjs":"text/javascript; charset=utf-8",".css":"text/css; charset=utf-8",".json":"application/json; charset=utf-8"};
createServer(async(req,res)=>{
  if(!["GET","HEAD"].includes(req.method)){res.writeHead(405,{Allow:"GET, HEAD"}).end();return;}
  const send=(status,type,data)=>{res.writeHead(status,{"Content-Type":type,"Cache-Control":"no-store","X-Content-Type-Options":"nosniff"});res.end(req.method==="HEAD"?undefined:data);};
  try{
    const path=new URL(req.url,"http://localhost").pathname;
    if(path==="/api/test-pages"){
      let entries=[];try{entries=await readdir(join(root,"playground"),{withFileTypes:true});}catch(e){if(e.code!=="ENOENT")throw e;}
      return send(200,types[".json"],JSON.stringify([...builtinPages,...customPages(entries)]));
    }
    const file=resolveRoute(path);
    if(!file)return send(404,"text/plain; charset=utf-8","Not found");
    if(file.startsWith("playground/")){
      const dir=await lstat(join(root,"playground")),stat=await lstat(join(root,file));
      if(!dir.isDirectory()||!stat.isFile())return send(404,"text/plain; charset=utf-8","Not found");
    }
    return send(200,types[extname(file).toLowerCase()],await readFile(join(root,file)));
  }catch(e){return send(e.code==="ENOENT"?404:500,"text/plain; charset=utf-8",e.code==="ENOENT"?"Not found. Vue: npm install / dist: npm run build":"Unable to serve request");}
}).listen(port,"127.0.0.1",()=>console.log("Macky UI Test Studio: http://127.0.0.1:"+port));
