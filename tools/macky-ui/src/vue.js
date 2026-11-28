import {buttonClass,createGrid,createMenu,createBadge} from "./index.js";
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
  const components={MButton,MPanel,MGrid,MMenu,MBadge};
  return {...components,install(app){app.provide(key,ui);for(const [name,c] of Object.entries(components))app.component(name,c);},
    useUI(){const service=inject(key);if(!service)throw new Error("Install Macky UI before useUI");return service;}};
}
