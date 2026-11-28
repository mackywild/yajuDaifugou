import {node,action,selectOptions} from "./dom.js";
let sequence=0;
export function validateValue(value,rules={},all={}){
  const empty=value==null||value===""||(Array.isArray(value)&&!value.length)||value===false;
  if(rules.required&&empty)return "必須項目です";
  if(empty)return "";
  if(rules.minLength!=null&&String(value).length<rules.minLength)return rules.minLength+"文字以上入力してください";
  if(rules.maxLength!=null&&String(value).length>rules.maxLength)return rules.maxLength+"文字以内で入力してください";
  if(rules.min!=null&&(!Number.isFinite(Number(value))||Number(value)<rules.min))return rules.min+"以上を入力してください";
  if(rules.max!=null&&(!Number.isFinite(Number(value))||Number(value)>rules.max))return rules.max+"以下を入力してください";
  if(rules.pattern){try{if(!new RegExp(rules.pattern).test(String(value)))return rules.patternMessage||"入力形式が正しくありません";}catch{return "検証ルールが不正です";}}
  if(typeof rules.validate==="function"){const r=rules.validate(value,all);if(r!==true&&r!=null&&r!=="")return typeof r==="string"?r:"入力値を確認してください";}
  return "";
}
export function createField(options={}){
  const o={type:"text",label:"",name:"",...options},id="mky-field-"+(++sequence);
  const element=node("div","mky-field"),error=node("div","mky-field-error"),hint=node("div","mky-muted",o.hint);
  error.id=id+"-error";error.setAttribute("aria-live","polite");hint.id=id+"-hint";
  const choice=o.type==="radio"||o.type==="checkbox-group";
  let input,controls=[];
  if(choice){
    input=node("fieldset","mky-choice");input.append(node("legend","",o.label+(o.required?" *":"")));
    for(const [i,item] of (o.options||[]).entries()){
      const info=typeof item==="object"?item:{label:item,value:item};
      const l=node("label","mky-choice-item"),c=node("input");c.type=o.type==="radio"?"radio":"checkbox";c.name=id;c.id=id+"-"+i;c.value=String(info.value);c.disabled=Boolean(o.disabled||info.disabled);l.append(c,document.createTextNode(String(info.label)));input.append(l);controls.push(c);
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
    controls=[input];
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
  function validate(all={}){const value=getValue();let msg=o.disabled?"":validateValue(value,{...o,...o.rules},all);
    if(!msg&&!o.disabled&&controls.some(c=>c.validity&&!c.validity.valid))msg=controls.find(c=>!c.validity.valid).validationMessage;
    error.textContent=msg;controls.forEach(c=>c.setAttribute("aria-invalid",String(Boolean(msg))));return !msg;}
  if(o.value!==undefined)setValue(o.value);
  for(const c of controls){c.addEventListener("change",()=>{if(o.validateOnChange!==false)validate();o.onChange?.(getValue());});}
  return {element,input,getValue,setValue,validate,focus:()=>controls[0]?.focus(),destroy:()=>element.remove()};
}
export const createTextBox=o=>createField({...o,type:o?.type||"text"});
export const createSelect=o=>createField({...o,type:"select"});
export const createCheckbox=o=>createField({...o,type:"checkbox"});
export const createRadio=o=>createField({...o,type:"radio"});
export const createDatePicker=o=>createField({...o,type:"date"});
export const createFilePicker=o=>createField({...o,type:"file"});

export function createForm({fields=[],values={},submitLabel="保存",onSubmit,onError,onChange}={}){
  const element=node("form","mky-form");element.noValidate=true;
  const map=new Map(),message=node("div","mky-field-error");message.setAttribute("role","status");
  let busy=false,disposed=false;
  for(const config of fields){
    if(!config.name||map.has(config.name))throw Error("Form fields need unique names");
    const field=createField({...config,value:values[config.name]??config.value,validateOnChange:false,onChange:v=>{if(config.validateOnChange!==false)map.get(config.name)?.validate(getValues());config.onChange?.(v);onChange?.(getValues());}});
    map.set(config.name,field);element.append(field.element);
  }
  const submit=action(submitLabel,null,"primary");submit.type="submit";element.append(message,submit);
  function getValues(){return Object.fromEntries([...map].map(([k,f])=>[k,f.getValue()]));}
  function setValues(next){for(const [k,v] of Object.entries(next))map.get(k)?.setValue(v);}
  function validate(){const all=getValues();let first=null;for(const f of map.values())if(!f.validate(all)&&!first)first=f;first?.focus();return !first;}
  async function run(){
    if(busy||disposed||!validate())return false;
    busy=true;submit.disabled=true;submit.setAttribute("aria-busy","true");message.textContent="";
    try{await onSubmit?.(getValues());return true;}
    catch(e){message.textContent=String(e?.message||e);onError?.(e);return false;}
    finally{busy=false;submit.disabled=false;submit.removeAttribute("aria-busy");}
  }
  element.addEventListener("submit",e=>{e.preventDefault();void run();});
  return {element,getValues,setValues,validate,submit:run,isBusy:()=>busy,destroy(){disposed=true;element.remove();}};
}
