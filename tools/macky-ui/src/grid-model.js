export const copyRows=rows=>rows.map(r=>({...r}));
export function createGridStore(initial=[],{historyLimit=50}={}){
  let rows=copyRows(initial),baseline=copyRows(initial),past=[],future=[];
  function apply(next){
    past.push(copyRows(rows));if(past.length>historyLimit)past.shift();
    rows=copyRows(next);future=[];return get();
  }
  function get(){return copyRows(rows);}
  return {get,apply,undo(){if(!past.length)return false;future.push(copyRows(rows));rows=past.pop();return true;},
    redo(){if(!future.length)return false;past.push(copyRows(rows));rows=future.pop();return true;},
    canUndo:()=>past.length>0,canRedo:()=>future.length>0,
    commit(){baseline=copyRows(rows);past=[];future=[];},reset(){rows=copyRows(baseline);past=[];future=[];},
    isDirty:()=>JSON.stringify(rows)!==JSON.stringify(baseline),
    changes(key="id"){const old=new Map(baseline.map(r=>[r[key],r])),now=new Map(rows.map(r=>[r[key],r]));return {added:rows.filter(r=>!old.has(r[key])).map(r=>({...r})),updated:rows.filter(r=>old.has(r[key])&&JSON.stringify(old.get(r[key]))!==JSON.stringify(r)).map(r=>({...r})),deleted:baseline.filter(r=>!now.has(r[key])).map(r=>({...r}))};}};
}
export function parseDelimited(text,separator=","){
  if(typeof separator!=="string"||separator.length!==1)throw Error("Invalid delimiter");
  const result=[];let row=[],field="",quoted=false,closed=false;
  const src=String(text??"").replace(/^\uFEFF/,"");
  for(let i=0;i<src.length;i++){
    const c=src[i];
    if(quoted){if(c==='"'){if(src[i+1]==='"'){field+='"';i++;}else{quoted=false;closed=true;}}else field+=c;continue;}
    if(c==='"'&&field===""&&!closed){quoted=true;continue;}
    if(c===separator){row.push(field);field="";closed=false;continue;}
    if(c==="\n"||c==="\r"){if(c==="\r"&&src[i+1]==="\n")i++;row.push(field);result.push(row);row=[];field="";closed=false;continue;}
    if(closed||c==='"')throw Error("Invalid quoted field");
    field+=c;
  }
  if(quoted)throw Error("Unclosed quoted field");
  if(row.length||field!==""||closed||!result.length){row.push(field);result.push(row);}
  return result;
}
export function encodeDelimited(rows,separator=",",{spreadsheetSafe=separator===","}={}){
  return rows.map(row=>row.map(v=>{
    let s=String(v??"");
    if(spreadsheetSafe&&typeof v==="string"&&/^[\s]*[=+\-@]|^[\t\r\n]/.test(s))s="'"+s;
    return s.includes(separator)||/["\r\n]/.test(s)?'"'+s.replaceAll('"','""')+'"':s;
  }).join(separator)).join("\r\n");
}
export function normalizeCell(raw,column,row){
  if(column.readonly||column.editable===false||typeof column.editable==="function"&&!column.editable(row))throw Error("編集不可: "+column.key);
  let v=raw;
  if(column.type==="number"){if(raw==="")v="";else{v=Number(raw);if(!Number.isFinite(v))throw Error("数値を入力してください: "+column.key);}}
  if(column.type==="boolean"){if(![true,false,"true","false","1","0"].includes(raw))throw Error("真偽値が不正です");v=raw===true||raw==="true"||raw==="1";}
  if(column.required&&(v===""||v==null))throw Error("必須: "+column.key);
  if(column.validate){const result=column.validate(v,row);if(result!==true&&result!=null&&result!=="")throw Error(typeof result==="string"?result:"不正な値: "+column.key);}
  return v;
}
