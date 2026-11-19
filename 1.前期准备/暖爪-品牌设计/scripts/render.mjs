import {spawn} from 'node:child_process';
import {mkdtemp,writeFile,readFile,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join,dirname} from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const root=dirname(dirname(fileURLToPath(import.meta.url)));
const profile=await mkdtemp(join(tmpdir(),'warm-paw-logo-'));
const port=9700+Math.floor(Math.random()*200);
const delay=ms=>new Promise(r=>setTimeout(r,ms));
let browser,ws;
try{
 browser=spawn('C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',['--headless=new','--disable-gpu','--in-process-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port='+port,'--disable-features=msImplicitSignin,msSingleSignOnOSForPrimaryAccount','--user-data-dir='+profile,'about:blank'],{windowsHide:true,stdio:['ignore','ignore','pipe']});

 let tabs;for(let i=0;i<80;i++){try{tabs=await(await fetch('http://127.0.0.1:'+port+'/json/list')).json();if(tabs.length)break}catch{}await delay(250)}
 if(!tabs?.length)throw Error('Browser did not start');

 const target=tabs.find(t=>t.type==='page' && t.url==='about:blank') || tabs.find(t=>t.type==='page');
 ws=new WebSocket(target.webSocketDebuggerUrl);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j});
 const pending=new Map();let seq=0;ws.onmessage=e=>{const p=JSON.parse(e.data);if(p.id){const t=pending.get(p.id);if(t){pending.delete(p.id);clearTimeout(t.timer);p.error?t.reject(Error(p.error.message)):t.resolve(p.result)}}};
 ws.onclose=e=>{for(const t of pending.values()){clearTimeout(t.timer);t.reject(Error('Browser connection closed: '+e.code+' '+e.reason))}pending.clear()};
 const call=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;const timer=setTimeout(()=>{pending.delete(id);reject(Error('CDP timed out: '+method))},15000);pending.set(id,{resolve,reject,timer});ws.send(JSON.stringify({id,method,params}))});
 const evaluate=async expression=>{const r=await call('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.text);return r.result.value};
 await call('Page.enable');
 await call('Emulation.setDeviceMetricsOverride',{width:1440,height:1100,deviceScaleFactor:1,mobile:false});
 await call('Page.navigate',{url:pathToFileURL(join(root,'index.html')).href});
 for(let i=0;i<80;i++){if(await evaluate('document.readyState === "complete" && document.querySelectorAll("img").length === 7 && [...document.images].every(i=>i.complete && i.naturalWidth>0)'))break;if(i===79)throw Error('Images did not load');await delay(100)}
 await evaluate('document.fonts.ready');
 const board=await evaluate('({width:document.querySelector(".board").offsetWidth,height:document.querySelector(".board").offsetHeight})');
 const preview=await call('Page.captureScreenshot',{format:'png',clip:{x:0,y:0,width:board.width,height:board.height,scale:1},captureBeyondViewport:true});
 await writeFile(join(root,'暖爪-Logo设计展示.png'),Buffer.from(preview.data,'base64'));
 const jobs=[['logo-horizontal.svg','logo-horizontal.png',1704,768],['logo-mark.svg','logo-mark.png',1024,1024],['logo-horizontal-reverse.svg','logo-horizontal-reverse.png',1704,768],['logo-stacked.svg','logo-stacked.png',720,860],['app-icon.svg','app-icon-1024.png',1024,1024],['app-icon.svg','app-icon-512.png',512,512],['logo-mark-micro.svg','favicon-32.png',32,32],['logo-mark-micro.svg','favicon-16.png',16,16]];
 for(const [source,target,width,height]of jobs){
  const sourceSVG=await readFile(join(root,'assets',source),'utf8');
  const result=await evaluate(`(async()=>{const image=new Image();image.src='data:image/svg+xml;charset=utf-8,'+encodeURIComponent(${JSON.stringify(sourceSVG)});await image.decode();const canvas=document.createElement('canvas');canvas.width=${width};canvas.height=${height};canvas.getContext('2d').drawImage(image,0,0,canvas.width,canvas.height);return canvas.toDataURL('image/png').split(',')[1]})()`);
  await writeFile(join(root,'assets',target),Buffer.from(result,'base64'));
 }
 const audit=await evaluate(`({imageCount:document.images.length,loaded:[...document.images].every(i=>i.complete&&i.naturalWidth>0),overflow:document.documentElement.scrollWidth>innerWidth})`);
 await call('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:false});
 const mobile=await evaluate('({width:innerWidth,scrollWidth:document.documentElement.scrollWidth})');
 if(mobile.scrollWidth>mobile.width)throw Error('Mobile preview overflows');
 console.log(JSON.stringify({board,audit,mobile,exports:jobs.length},null,2));
 await call('Browser.close');
}finally{ws?.close();if(browser && browser.exitCode===null)browser.kill();await delay(500);await rm(profile,{recursive:true,force:true,maxRetries:4,retryDelay:300}).catch(()=>{});}


