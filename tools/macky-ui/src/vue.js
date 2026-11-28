import * as api from "./index.js";
const {buttonClass,createGrid,createMenu,createBadge}=api;
/** Runtime injection avoids bundler and Vue-version duplication. */
export function createVueKit(Vue,ui){
  const {defineComponent,h,ref,onMounted,onBeforeUnmount,watch,inject}=Vue;
  const key=Symbol("MackyUI");
  const MButton=defineComponent({name:"MButton",props:{label:String,variant:String,size:String,disabled:Boolean,busy:Boolean},
    emits:["click"],setup(p,{slots,emit}){return ()=>h("button",{type:"button",class:buttonClass(p.variant,p.size),disabled:p.disabled||p.busy,
      "aria-busy":p.busy?"true":undefined,onClick:e=>emit("click",e)},p.busy?"処理中… "+(p.label??""):(slots.default?.()??p.label));}});
  const MPanel=defineComponent({name:"MPanel",props:{title:String,description:String},setup(p,{slots}){return ()=>h("section",{class:"mky-panel"},[
    p.title?h("h2",{class:"mky-heading"},p.title):null,p.description?h("p",{class:"mky-muted"},p.description):null,slots.default?.()]);}});
  function wrap(name,create,grid=false){
    return defineComponent({name,props:{options:{type:Object,default:()=>({})}},setup(p){
      const host=ref(null);let handle=null;
      function sync(){
        if(!host.value)return;
        if(grid&&handle){handle.update(p.options);return;}
        handle=grid?create(p.options):null;
        host.value.replaceChildren(grid?handle.element:create(p.options));
      }
      onMounted(sync);watch(()=>p.options,sync,{deep:true,flush:"post"});
      onBeforeUnmount(()=>{handle?.destroy();handle=null;});
      return ()=>h("div",{ref:host});
    }});
  }
  const MGrid=wrap("MGrid",createGrid,true),MMenu=wrap("MMenu",createMenu),MBadge=wrap("MBadge",createBadge);
  const MComponent=defineComponent({name:"MComponent",props:{type:{type:String,required:true},options:{type:Object,default:()=>({})}},emits:["action"],setup(p,{emit}){
    const host=ref(null);let handle;
    function sync(){if(!host.value)return;handle?.destroy();handle=null;try{handle=api.mountComponent(api,p.type,p.options,{ui,onEvent:(name,value)=>emit("action",name,value)});host.value.replaceChildren(handle.element);}catch(e){host.value.textContent=String(e?.message||e);emit("action","error",String(e?.message||e));}}
    onMounted(sync);watch(()=>[p.type,p.options],sync,{deep:true,flush:"post"});onBeforeUnmount(()=>{handle?.destroy();handle=null;});return ()=>h("div",{ref:host});
  }});
  const named={MField:"field",MSelect:"select",MCheckbox:"checkbox",MRadio:"radio",MDatePicker:"date",MFilePicker:"file",MForm:"form",MAdvancedGrid:"advancedGrid",MTabs:"tabs",MAccordion:"accordion",MSidebar:"sidebar",MTooltip:"tooltip",MContextMenu:"context",MProgress:"progress",MChart:"chart",MTree:"tree",MCalendar:"calendar",MWizard:"wizard",MTimeline:"timeline",MKanban:"kanban",MImageViewer:"image",MRichText:"richText",MCommandPalette:"command",MRetryPanel:"retry"};
  const components={MButton,MPanel,MGrid,MMenu,MBadge,MComponent};
  for(const [name,type] of Object.entries(named))components[name]=defineComponent({name,props:{options:{type:Object,default:()=>({})}},emits:["action"],setup(p,{emit}){return ()=>h(MComponent,{type,options:p.options,onAction:(n,v)=>emit("action",n,v)});}});
  return {...components,install(app){app.provide(key,ui);for(const [name,c] of Object.entries(components))app.component(name,c);},
    useUI(){const service=inject(key);if(!service)throw new Error("Install Macky UI before useUI");return service;}};
}
