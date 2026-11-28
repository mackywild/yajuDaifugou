const palette=(bg,panel,panel2,primary,onPrimary,fg,muted,line)=>({bg,panel,panel2,primary,"on-primary":onPrimary,fg,muted,line,...(panel==="#ffffff"?{danger:"#a81e35",success:"#07633c",warning:"#835400",info:"#185b97"}:{danger:"#ff9ba3",success:"#8ce5b3",warning:"#ffda84",info:"#a8d7ff"})});
export const themes=Object.freeze(Object.fromEntries(Object.entries({
 casino:palette("#020403","#0d1712","#15241b","#d8b85a","#201a0a","#f7f4ea","#a7b4ac","#35483c"),
 business:palette("#edf2f7","#ffffff","#e5edf6","#245bd1","#ffffff","#14253a","#4a5d75","#afbed0"),
 cyberpunk:palette("#100c22","#211638","#302344","#ee83fb","#23082c","#f9efff","#c6b3db","#654a7c"),
 minimal:palette("#f4f4f1","#ffffff","#e9e9e5","#222222","#ffffff","#222222","#5e5e5e","#b8b8b5"),
 metal:palette("#100d0d","#201717","#352323","#ff7878","#2a0909","#f8eeee","#c2a9a9","#775354"),
 ocean:palette("#07202e","#103745","#164958","#38cbd0","#092327","#edfaff","#a5cbd4","#528493")
}).map(([k,v])=>[k,Object.freeze(v)])));
const colors=new Set(["bg","panel","panel2","primary","on-primary","fg","muted","line","danger","success","warning","info"]);
export function validateTheme(tokens){
  if(!tokens||typeof tokens!=="object"||Array.isArray(tokens))throw Error("Invalid theme");
  const out={};
  for(const [k,v] of Object.entries(tokens)){
    if(colors.has(k)&&typeof v==="string"&&/^#[0-9a-f]{6}$/i.test(v))out[k]=v.toLowerCase();
    else if(["radius","space","font-size"].includes(k)&&typeof v==="string"&&/^(?:[0-9]|[1-5][0-9]|60)px$/.test(v))out[k]=v;
    else if(k==="font"&&["system-ui","serif","sans-serif","monospace"].includes(v))out[k]=v;
    else if(k==="motion"&&["0ms","150ms","250ms"].includes(v))out[k]=v;
    else throw Error("Invalid theme token: "+k);
  }return out;
}
export function themeCSS(tokens){const safe=validateTheme(tokens);return ".mky-ui.mky-ui {\n"+Object.entries(safe).map(([k,v])=>"  --mky-"+k+": "+v+";").join("\n")+"\n}\n";}
export function applyTheme(root,nameOrTokens){
 const name=typeof nameOrTokens==="string"?nameOrTokens:null;
 const tokens=validateTheme(name?(themes[name]||(()=>{throw Error("Unknown theme: "+name);})()):nameOrTokens);
 const before=Object.keys(tokens).map(k=>[k,root.style.getPropertyValue("--mky-"+k),root.style.getPropertyPriority("--mky-"+k)]);
 for(const [k,v] of Object.entries(tokens))root.style.setProperty("--mky-"+k,v);
 let restored=false;return {restore(){if(restored)return;restored=true;for(const [k,v,p] of before){if(v)root.style.setProperty("--mky-"+k,v,p);else root.style.removeProperty("--mky-"+k);}}};
}
export function saveTheme(storage,key,tokens){const safe=validateTheme(tokens);storage.setItem(key,JSON.stringify(safe));return safe;}
export function loadTheme(storage,key){const s=storage.getItem(key);return s===null?null:validateTheme(JSON.parse(s));}
