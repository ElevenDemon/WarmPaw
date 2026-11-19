import fs from 'node:fs';
import path from 'node:path';
import {spawn} from 'node:child_process';
import {fileURLToPath,pathToFileURL} from 'node:url';
const root=path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const out=path.join(root,'验收','powerpoint-build');fs.mkdirSync(out,{recursive:true});
const port=9956,delay=ms=>new Promise(r=>setTimeout(r,ms));
const profile=fs.mkdtempSync(path.join(out,'browser-profile-'));
if(!profile.startsWith(out+path.sep))throw Error('Profile is outside build directory');
const child=spawn('C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port='+port,'--guest','--disable-features=msImplicitSignin,msSingleSignOnOSForPrimaryAccount','--user-data-dir='+profile,'about:blank'],{windowsHide:true,stdio:'ignore'});
let ws;const keepAlive=setInterval(()=>{},1000);
try{
 let list;for(let i=0;i<100;i++){try{list=await(await fetch('http://127.0.0.1:'+port+'/json/list')).json();if(list.length)break}catch{}await delay(150)}
 const tab=list.find(t=>t.type==='page');ws=new WebSocket(tab.webSocketDebuggerUrl);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j});
 let seq=0;const pending=new Map();ws.onmessage=e=>{const p=JSON.parse(e.data);if(p.id){const h=pending.get(p.id);if(h){pending.delete(p.id);p.error?h.j(Error(p.error.message)):h.r(p.result)}}};
 const call=(method,params={})=>new Promise((r,j)=>{const id=++seq;pending.set(id,{r,j});setTimeout(()=>{if(pending.delete(id))j(Error('Timeout '+method))},20000).unref();ws.send(JSON.stringify({id,method,params}))});
 const ev=async expression=>{const r=await call('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value};
 await call('Runtime.enable');await call('Page.enable');
 await call('Emulation.setDeviceMetricsOverride',{width:1600,height:900,deviceScaleFactor:1,mobile:false});
 await call('Page.navigate',{url:pathToFileURL(path.join(root,'index.html')).href});await delay(2500);await ev('document.fonts.ready');
 await ev(`(()=>{const style=document.createElement('style');style.textContent='body> :not(#deck):not(script):not(style){visibility:hidden!important} .map-panel button,.map-zoom-hint{visibility:hidden!important}';document.head.append(style);})()`);
 const slides=[];
 for(let i=0;i<12;i++){
  await ev('go('+i+')');await delay(1750);
  const data=await ev(`(()=>{
   const slide=document.querySelectorAll('#deck>.slide')[${i}],sr=slide.getBoundingClientRect(),text=[];
   const walker=document.createTreeWalker(slide,NodeFilter.SHOW_TEXT);let n;
   while(n=walker.nextNode()){
    if(!n.textContent.trim()||n.parentElement.closest('svg,canvas,script,style,button'))continue;
    const el=n.parentElement,cs=getComputedStyle(el);let opacity=1,hidden=false;
    for(let p=el;p&&p!==slide.parentElement;p=p.parentElement){const s=getComputedStyle(p);opacity*=parseFloat(s.opacity);if(s.display==='none'||s.visibility==='hidden')hidden=true;}
    if(hidden||opacity<.01)continue;
    let line=null;
    for(let k=0;k<n.length;k++){
     const range=document.createRange();range.setStart(n,k);range.setEnd(n,k+1);const r=range.getBoundingClientRect();
     if(r.width<.01||r.height<.01)continue;
     const char=n.textContent[k];
     if(line&&Math.abs(line.y-(r.y-sr.y))<2&&Math.abs(line.right-(r.x-sr.x))<3){line.text+=char;line.right=r.right-sr.x;line.w=line.right-line.x;}
     else{line={text:char,x:r.x-sr.x,y:r.y-sr.y,w:r.width,h:r.height,right:r.right-sr.x,fontSize:parseFloat(cs.fontSize),fontFamily:cs.fontFamily,weight:parseInt(cs.fontWeight)||400,color:cs.color,letterSpacing:parseFloat(cs.letterSpacing)||0,opacity};text.push(line);}
    }
   }
   return {id:slide.dataset.slideId,width:sr.width,height:sr.height,text:text.filter(t=>t.text.trim()),notes:window.__SPEAKER_NOTES__?.[${i}]||null};
  })()`);
  slides.push(data);
  fs.writeFileSync(path.join(out,String(i+1).padStart(2,'0')+'-reference.png'),Buffer.from((await call('Page.captureScreenshot',{format:'png'})).data,'base64'));
  await ev(`(()=>{const s=document.querySelectorAll('#deck>.slide')[${i}];const w=document.createTreeWalker(s,NodeFilter.SHOW_TEXT);const ns=[];let n;while(n=w.nextNode())if(n.textContent.trim()&&!n.parentElement.closest('svg,canvas,script,style'))ns.push(n);for(const n of ns){const span=document.createElement('span');span.className='ppt-export-hidden';span.style.cssText='-webkit-text-fill-color:transparent!important;color:transparent!important;text-shadow:none!important';n.replaceWith(span);span.append(n);}})()`);
  fs.writeFileSync(path.join(out,String(i+1).padStart(2,'0')+'-background.png'),Buffer.from((await call('Page.captureScreenshot',{format:'png'})).data,'base64'));
  console.log('Extracted slide',i+1,data.text.length,'text lines');
 }
 fs.writeFileSync(path.join(out,'slides.json'),JSON.stringify(slides,null,2));
 await call('Browser.close');
}finally{
 clearInterval(keepAlive);ws?.close();child.kill();
 await delay(1500);
 if(profile.startsWith(out+path.sep))fs.rmSync(profile,{recursive:true,force:true,maxRetries:3,retryDelay:300});
}
