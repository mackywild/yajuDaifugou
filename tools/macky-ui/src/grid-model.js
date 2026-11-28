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

/** Validate a full replacement without mutating any input or history. */
export function planCSVImport(text,{rows=[],columns=[],rowKey="id",canEdit,canDelete,canAdd,allowCSVImport=false}={}){
 if(allowCSVImport!==true)throw Error("CSV取込は許可されていません");
 if(typeof text!=="string"||text.length>5_000_000)throw Error("CSVは5MBまで");
 const [header,...lines]=parseDelimited(text);
 const keys=columns.map(c=>c.key);
 if(!keys.includes(rowKey)||header.length!==keys.length||new Set(header).size!==keys.length||header.some(k=>!keys.includes(k)))throw Error("CSVには全列のヘッダーが必要です");
 if(!lines.length)throw Error("データのないCSVで全件削除することはできません");
 const existing=new Map(),seen=new Set(),next=[],added=[],updated=[],deleted=[];
 const idColumn=columns.find(c=>c.key===rowKey);
 for(const row of rows){const id=row[rowKey],k=String(id);if(!["string","number"].includes(typeof id)||id===""||typeof id==="number"&&!Number.isFinite(id)||existing.has(k))throw Error("CSVで一意に識別できない行キー");existing.set(k,row);}
 const types=new Set(rows.map(r=>typeof r[rowKey]));
 const idType=idColumn.type|| (types.size===1?[...types][0]:null);
 for(const line of lines){
  if(line.length!==header.length)throw Error("CSV列数が不一致です");
  const raw=Object.fromEntries(header.map((k,i)=>[k,line[i]]));
  const key=raw[rowKey];if(!key.trim()||seen.has(key))throw Error("行キーが空または重複しています");
  seen.add(key);const old=existing.get(key);
  let id=old?.[rowKey];
  if(!old){
   if(idType!=="string"&&idType!=="number")throw Error("新規行のID型を列typeで明示してください");
   id=idType==="number"?Number(key):key;
   if(idType==="number"&&(!Number.isFinite(id)||String(id)!==key))throw Error("数値IDの形式が不正です");
   if(existing.has(String(id)))throw Error("行キーが重複しています");
  }
  const candidate=old?{...old}:{};
  for(const col of columns){
   const k=col.key;if(k===rowKey){candidate[k]=id;continue;}
   // Untyped existing scalar values retain their type, avoiding false changes.
   const type=col.type??(typeof old?.[k]==="number"?"number":typeof old?.[k]==="boolean"?"boolean":undefined);
   const v=normalizeCell(raw[k],{...col,type,readonly:false,editable:true,validate:undefined},candidate);
   if(old&&!Object.is(old[k],v)&&(col.readonly||col.editable===false||typeof col.editable==="function"&&!col.editable({...old})||canEdit&&!canEdit({...old},col)))throw Error("編集不可: "+k);
   candidate[k]=v;
  }
  for(const col of columns){
   const value=candidate[col.key];
   if(col.required&&(value===""||value==null))throw Error("必須: "+col.key);
   if(col.validate){const r=col.validate(value,{...candidate});if(r!==true&&r!=null&&r!=="")throw Error(typeof r==="string"?r:"不正な値: "+col.key);}
  }
  if(!old){if(typeof canAdd!=="function"||!canAdd({...candidate}))throw Error("CSVからの新規行追加は許可されていません");added.push({...candidate});}
  else if(columns.some(c=>!Object.is(candidate[c.key],old[c.key])))updated.push({...candidate});
  next.push(candidate);
 }
 for(const old of rows){if(!seen.has(String(old[rowKey]))){if(typeof canDelete!=="function"||!canDelete({...old}))throw Error("CSVからの行削除は許可されていません");deleted.push({...old});}}
 return {rows:next,changes:{added,updated,deleted}};
}
