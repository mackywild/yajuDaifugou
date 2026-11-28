import {node,action,selectOptions} from "./dom.js";
let sequence=0;
function dateScalar(value){
 const m=/^(\d{4,})-(\d{2})-(\d{2})$/.exec(String(value));if(!m)return NaN;
 const [y,month,d]=m.slice(1).map(Number);if(y<1||month<1||month>12||d<1)return NaN;
 const leap=y%4===0&&(y%100!==0||y%400===0),days=[31,leap?29:28,31,30,31,30,31,31,30,31,30,31];
 return d<=days[month-1]?y*10000+month*100+d:NaN;
}
function timeScalar(value){
 const m=/^(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,3}))?)?$/.exec(String(value));if(!m)return NaN;
 const h=Number(m[1]),min=Number(m[2]),s=Number(m[3]||0),ms=Number((m[4]||"").padEnd(3,"0"));
 return h<24&&min<60&&s<60?((h*60+min)*60+s)*1000+ms:NaN;
}

export function validateValue(value,rules={},all={}){
  const empty=value==null||value===""||(Array.isArray(value)&&!value.length)||value===false;
  if(rules.required&&empty)return "必須項目です";
  if(empty)return "";
  if(rules.minLength!=null&&String(value).length<rules.minLength)return rules.minLength+"文字以上入力してください";
  if(rules.maxLength!=null&&String(value).length>rules.maxLength)return rules.maxLength+"文字以内で入力してください";
  const kind=rules.type??(/^\d{4,}-\d{2}-\d{2}$/.test(String(rules.min??rules.max??""))?"date":/^\d{2}:\d{2}/.test(String(rules.min??rules.max??""))?"time":"number");
  if(kind==="date"||kind==="time"||rules.min!=null||rules.max!=null){
    const convert=kind==="date"?dateScalar:kind==="time"?timeScalar:Number;
    const n=convert(value),lo=rules.min==null?null:convert(rules.min),hi=rules.max==null?null:convert(rules.max);
    if(!Number.isFinite(n))return "入力形式が正しくありません";
    if(lo!==null&&!Number.isFinite(lo)||hi!==null&&!Number.isFinite(hi))return "範囲設定が不正です";
    // HTML time inputs allow a range crossing midnight.
    if(kind==="time"&&lo!==null&&hi!==null&&lo>hi){if(n<lo&&n>hi)return "指定時刻範囲外です";}
    else{if(lo!==null&&hi!==null&&lo>hi)return "範囲設定が不正です";if(lo!==null&&n<lo)return rules.min+"以上を入力してください";if(hi!==null&&n>hi)return rules.max+"以下を入力してください";}
  }
  if(rules.pattern){try{if(!new RegExp(rules.pattern).test(String(value)))return rules.patternMessage||"入力形式が正しくありません";}catch{return "検証ルールが不正です";}}
  if(typeof rules.validate==="function"){const r=rules.validate(value,all);if(r!==true&&r!=null&&r!=="")return typeof r==="string"?r:"入力値を確認してください";}
  return "";
}
export function createField(options={}){
  const o={type:"text",label:"",name:"",...options},id="mky-field-"+(++sequence);
  const element=node("div","mky-field"),error=node("div","mky-field-error"),hint=node("div","mky-muted",o.hint);
  error.id=id+"-error";error.setAttribute("aria-live","polite");hint.id=id+"-hint";
  const choice=o.type==="radio"||o.type==="checkbox-group";
  let input,controls=[],intrinsicDisabled=[];
  if(choice){
    input=node("fieldset","mky-choice");input.append(node("legend","",o.label+(o.required?" *":"")));
    for(const [i,item] of (o.options||[]).entries()){
      const info=typeof item==="object"?item:{label:item,value:item};
      const l=node("label","mky-choice-item"),c=node("input");c.type=o.type==="radio"?"radio":"checkbox";c.name=id;c.id=id+"-"+i;c.value=String(info.value);c.disabled=Boolean(o.disabled||info.disabled);l.append(c,document.createTextNode(String(info.label)));input.append(l);controls.push(c);intrinsicDisabled.push(Boolean(info.disabled));
    }
  }else{
    const label=node("label","mky-field-label",o.label+(o.required?" *":""));label.htmlFor=id;element.append(label);
    input=node(o.type==="select"?"select":o.type==="textarea"?"textarea":"input","mky-input");
    if(input.tagName==="INPUT")input.type=["text","email","password","number","date","time","checkbox","file","search","url","tel","color","range"].includes(o.type)?o.type:"text";
    input.id=id;input.name=o.name||id;input.disabled=Boolean(o.disabled);input.required=Boolean(o.required);input.placeholder=o.placeholder||"";
    if(o.type==="select"){if(o.placeholder){const p=node("option","",o.placeholder);p.value="";input.append(p);}selectOptions(input,o.options||[]);input.multiple=Boolean(o.multiple);}
    if(o.type==="file"){input.multiple=Boolean(o.multiple);input.accept=o.accept||"";}
    if(o.readonly)input.readOnly=true;
    for(const k of ["min","max","step","maxLength","minLength","rows"])if(o[k]!=null&&k in input)input[k]=o[k];
    controls=[input];intrinsicDisabled=[false];
  }
  for(const c of controls)c.setAttribute("aria-describedby",id+"-hint "+id+"-error");
  element.append(input,hint,error);
  function getValue(){
    if(choice){const selected=controls.filter(c=>c.checked).map(c=>c.value);return o.type==="radio"?selected[0]??"":selected;}
    if(o.type==="checkbox")return input.checked;
    if(o.type==="file")return Array.from(input.files||[]);
    if(o.type==="select"&&o.multiple)return Array.from(input.selectedOptions,x=>x.value);
    if(o.type==="number")return input.value===""?"":input.valueAsNumber;
    return input.value;
  }
  function setValue(v){
    if(choice){const values=(Array.isArray(v)?v:[v]).map(String);controls.forEach(c=>c.checked=values.includes(c.value));}
    else if(o.type==="checkbox")input.checked=Boolean(v);
    else if(o.type==="file"){if(v==null||Array.isArray(v)&&!v.length)input.value="";}
    else if(o.type==="select"&&o.multiple){const values=(Array.isArray(v)?v:[v]).map(String);for(const opt of input.options)opt.selected=values.includes(opt.value);}
    else input.value=v??"";
  }
  let access={disabled:Boolean(o.disabled),readonly:Boolean(o.readonly)},parentAccess={disabled:false,readonly:false};
  const nativeReadonly=()=>!choice&&(input.tagName==="TEXTAREA"||input.tagName==="INPUT"&&["text","email","password","number","date","time","search","url","tel"].includes(input.type));
  const isDisabled=()=>access.disabled||parentAccess.disabled;
  const isReadonly=()=>access.readonly||parentAccess.readonly;
  function setAccess(next={},inherited){
    for(const k of ["disabled","readonly"])if(Object.hasOwn(next,k))access[k]=Boolean(next[k]);
    if(inherited)parentAccess={disabled:Boolean(inherited.disabled),readonly:Boolean(inherited.readonly)};
    const disabled=isDisabled(),readonly=isReadonly(),native=nativeReadonly();
    for(const [i,c] of controls.entries()){c.disabled=disabled||intrinsicDisabled[i]||readonly&&!native;if(native)c.readOnly=readonly;c.setAttribute("aria-disabled",String(c.disabled));}
    element.setAttribute("aria-disabled",String(disabled));if(disabled||readonly){error.textContent="";controls.forEach(c=>c.setAttribute("aria-invalid","false"));}
  }
  function validate(all={}){const value=getValue();let msg=isDisabled()||isReadonly()?"":validateValue(value,{...o,...o.rules},all);
    if(!msg&&!isDisabled()&&!isReadonly()&&controls.some(c=>c.validity&&!c.validity.valid))msg=controls.find(c=>!c.validity.valid).validationMessage;
    error.textContent=msg;controls.forEach(c=>c.setAttribute("aria-invalid",String(Boolean(msg))));return !msg;}
  if(o.value!==undefined)setValue(o.value);setAccess();
  for(const c of controls){c.addEventListener("change",()=>{if(isDisabled()||isReadonly()||c.disabled)return;if(o.validateOnChange!==false)validate();o.onChange?.(getValue());});}
  return {element,input,getValue,setValue,validate,setAccess,focus:()=>controls[0]?.focus(),destroy:()=>element.remove()};
}
export const createTextBox=o=>createField({...o,type:o?.type||"text"});
export const createSelect=o=>createField({...o,type:"select"});
export const createCheckbox=o=>createField({...o,type:"checkbox"});
export const createRadio=o=>createField({...o,type:"radio"});
export const createDatePicker=o=>createField({...o,type:"date"});
export const createFilePicker=o=>createField({...o,type:"file"});

export function createForm({fields=[],values={},submitLabel="保存",disabled=false,readonly=false,onSubmit,onError,onChange}={}){
  const element=node("form","mky-form");element.noValidate=true;
  const map=new Map(),message=node("div","mky-field-error");message.setAttribute("role","status");
  let busy=false,disposed=false,access={disabled:Boolean(disabled),readonly:Boolean(readonly)};
  for(const config of fields){
    if(!config.name||map.has(config.name))throw Error("Form fields need unique names");
    const field=createField({...config,value:values[config.name]??config.value,validateOnChange:false,onChange:v=>{if(config.validateOnChange!==false)map.get(config.name)?.validate(getValues());config.onChange?.(v);onChange?.(getValues());}});
    map.set(config.name,field);element.append(field.element);
  }
  const submit=action(submitLabel,null,"primary");submit.type="submit";element.append(message,submit);
  function refreshSubmit(){submit.disabled=busy||access.disabled||access.readonly;element.setAttribute("aria-disabled",String(access.disabled));}
  function setAccess(next={}){
    for(const k of ["disabled","readonly"])if(Object.hasOwn(next,k))access[k]=Boolean(next[k]);
    const updates=new Map((next.fields||[]).map(f=>[f.name,f]));
    for(const [name,field] of map){const f=updates.get(name),patch={};if(f)for(const k of ["disabled","readonly"])if(Object.hasOwn(f,k))patch[k]=f[k];field.setAccess(patch,access);}
    refreshSubmit();
  }
  setAccess();
  function getValues(){return Object.fromEntries([...map].map(([k,f])=>[k,f.getValue()]));}
  function setValues(next){for(const [k,v] of Object.entries(next))map.get(k)?.setValue(v);}
  function validate(){const all=getValues();let first=null;for(const f of map.values())if(!f.validate(all)&&!first)first=f;first?.focus();return !first;}
  async function run(){
    if(busy||disposed||access.disabled||access.readonly||!validate())return false;
    busy=true;submit.disabled=true;submit.setAttribute("aria-busy","true");message.textContent="";
    try{await onSubmit?.(getValues());return true;}
    catch(e){message.textContent=String(e?.message||e);onError?.(e);return false;}
    finally{busy=false;refreshSubmit();submit.removeAttribute("aria-busy");}
  }
  element.addEventListener("submit",e=>{e.preventDefault();void run();});
  return {element,getValues,setValues,validate,setAccess,submit:run,isBusy:()=>busy,destroy(){disposed=true;element.remove();}};
}
