export function buttonClass(variant="primary",size="md"){return "mky-btn mky-btn--"+(["primary","secondary","danger","ghost"].includes(variant)?variant:"primary")+" mky-btn--"+(["sm","md","lg"].includes(size)?size:"md")}

export function gridView(rows,columns,{query="",sortKey=null,direction="asc",page=1,pageSize=10}={}){
  const q=String(query??"").trim().toLocaleLowerCase("ja");
  const cols=Array.isArray(columns)?columns:[];
  const source=Array.isArray(rows)?rows:[];
  const value=(r,c)=>typeof c.value==="function"?c.value(r):r?.[c.key];
  const items=source.filter(r=>!q||cols.some(c=>c.searchable!==false&&String(value(r,c)??"").toLocaleLowerCase("ja").includes(q)));
  const col=cols.find(c=>c.key===sortKey&&c.sortable!==false);
  if(col){
    const collator=new Intl.Collator("ja",{numeric:true,sensitivity:"base"});
    items.sort((a,b)=>{
      const x=value(a,col),y=value(b,col);
      if(x==null||y==null)return x==null?(y==null?0:1):-1;
      const n=typeof x==="number"&&typeof y==="number"?x-y:collator.compare(String(x),String(y));
      return direction==="desc"?-n:n;
    });
  }
  const size=Number.isFinite(Number(pageSize))?Math.max(1,Math.min(200,Math.floor(Number(pageSize)))):10;
  const pages=Math.max(1,Math.ceil(items.length/size));
  const current=Number.isFinite(Number(page))?Math.min(pages,Math.max(1,Math.floor(Number(page)))):1;
  return {rows:items.slice((current-1)*size,current*size),total:items.length,page:current,pages,pageSize:size};
}

export function durationMs(value,fallback=3000){if(value==null)return fallback;const n=Number(value);return Number.isFinite(n)?Math.max(0,Math.min(n,60000)):fallback}
