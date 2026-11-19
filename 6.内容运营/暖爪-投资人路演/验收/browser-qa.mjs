import fs from 'node:fs';
import path from 'node:path';
import {spawn} from 'node:child_process';
import {fileURLToPath,pathToFileURL} from 'node:url';
import os from 'node:os';
import assert from 'node:assert/strict';
const out=path.dirname(fileURLToPath(import.meta.url)),root=path.dirname(out);
const port=9700+Math.floor(Math.random()*300),delay=ms=>new Promise(r=>setTimeout(r,ms));
const profile=fs.mkdtempSync(path.join(os.tmpdir(),'nuanzhua-deck-'));
const child=spawn('C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--disable-popup-blocking','--remote-debugging-port='+port,'--guest','--disable-features=msImplicitSignin,msSingleSignOnOSForPrimaryAccount','--user-data-dir='+profile,'about:blank'],{windowsHide:true,stdio:'ignore'});
const sockets=[],errors=[];
const keepAlive=setInterval(()=>{},1000);
async function tabs(){return (await fetch('http://127.0.0.1:'+port+'/json/list')).json()}
async function attach(tab){
 const ws=new WebSocket(tab.webSocketDebuggerUrl);sockets.push(ws);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;setTimeout(()=>j(Error('WebSocket timeout: '+tab.webSocketDebuggerUrl)),12000).unref()});
 let seq=0;const pending=new Map();
 ws.onmessage=e=>{const p=JSON.parse(e.data);if(p.id){const h=pending.get(p.id);if(h){pending.delete(p.id);p.error?h.reject(Error(p.error.message)):h.resolve(p.result)}}if(p.method==='Runtime.exceptionThrown')errors.push(p.params.exceptionDetails.exception?.description||p.params.exceptionDetails.text)};
 const call=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});setTimeout(()=>{if(pending.delete(id))reject(Error('CDP timeout '+method))},15000).unref();ws.send(JSON.stringify({id,method,params}))});
 const ev=async expression=>{const r=await call('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value};
 console.log('WS connected');await call('Runtime.enable');console.log('Runtime enabled');await call('Page.enable');return {call,ev};
}
try{
 let list;for(let i=0;i<100;i++){try{list=await tabs();if(list.length)break}catch{}await delay(150)}
 const b=await attach(list.find(t=>t.type==='page'&&t.url==='about:blank'));
 await b.call('Emulation.setDeviceMetricsOverride',{width:1600,height:900,deviceScaleFactor:1,mobile:false});
 await b.call('Page.navigate',{url:pathToFileURL(path.join(root,'index.html')).href});await delay(2300);
 await b.ev('document.fonts.ready');
 const total=await b.ev('document.querySelectorAll("#deck>.slide").length');assert.equal(total,12);
 await b.ev('Promise.all([...document.images].map(i=>i.complete?Promise.resolve():new Promise(r=>{i.onload=r;i.onerror=r})))');
 const stats=[];
 for(let i=0;i<total;i++){
  await b.ev('go('+i+')');await delay(2100);
  stats.push(await b.ev(`(()=>{const s=document.querySelectorAll('#deck>.slide')[${i}],r=s.getBoundingClientRect();const bad=[...s.querySelectorAll('h1,h2,h3,p,li,.t-meta,.t-cat')].map(e=>{const x=e.getBoundingClientRect();return {text:e.textContent.trim(),x:x.x-r.x,y:x.y-r.y,w:x.width,h:x.height,scroll:e.scrollWidth-e.clientWidth}}).filter(x=>x.x< -2||x.y< -2||x.x+x.w>r.width+2||x.y+x.h>r.height+2||x.scroll>3);return {slide:${i+1},id:s.dataset.slideId,width:r.width,height:r.height,bad}})()`));
  const shot=await b.call('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.join(out,String(i+1).padStart(2,'0')+'.png'),Buffer.from(shot.data,'base64'));
 }
 fs.writeFileSync(path.join(out,'layout-report.json'),JSON.stringify({stats,errors},null,2));console.log(JSON.stringify({stats,errors}));
 if(process.env.DECK_VISUAL_ONLY==='1'){await b.call('Browser.close');process.exitCode=0;}
 else {
 await b.ev('go(5)');await delay(300);
 const mapResult=await b.ev(`(()=>{const p=document.querySelector(".map-panel"),m=p.querySelector(".map-static"),z=p.querySelector('[data-map-ctrl="zoom-in"]'),d=p.querySelector('[data-map-ctrl="drag"]');z.click();const zoom=m.style.transform.includes("1.1");d.click();const drag=d.getAttribute("aria-pressed")==="true";d.click();p.querySelector('[data-map-ctrl="zoom-out"]').click();return {zoom,drag,idx:window.__currentSlideIndex}})()`);assert.ok(mapResult.zoom&&mapResult.drag&&mapResult.idx===5);fs.writeFileSync(path.join(out,'map-report.json'),JSON.stringify(mapResult,null,2));
 const logos=await b.ev('[...document.querySelectorAll("#deck>.slide")].map(s=>({id:s.dataset.slideId,logos:[...s.querySelectorAll(".brand-logo img")].filter(i=>getComputedStyle(i).display!=="none").map(i=>({loaded:i.complete&&i.naturalWidth>0,src:i.getAttribute("src")}))}))');assert.ok(logos.every(s=>s.logos.length===1&&s.logos[0].loaded));fs.writeFileSync(path.join(out,'logo-report.json'),JSON.stringify(logos,null,2));
 await delay(1000);await b.ev('go(0)');await delay(1000);await b.ev('window.__pptPresenter.enterPresenter(true)');await delay(1800);
 fs.writeFileSync(path.join(out,'presenter.png'),Buffer.from((await b.call('Page.captureScreenshot',{format:'png'})).data,'base64'));
 console.log('PRESENTER',await b.ev('JSON.stringify(window.__pptPresenter.debugState())'));
 fs.writeFileSync(path.join(out,'presenter-dom.html'),await b.ev('document.querySelector("#ppt-presenter").outerHTML'));
 const passed=[];
 const state=()=>b.ev('window.__pptPresenter.debugState()');
 const click=selector=>b.ev('document.querySelector('+JSON.stringify(selector)+').click()');
 const key=k=>b.ev('document.activeElement?.blur();dispatchEvent(new KeyboardEvent("keydown",{key:'+JSON.stringify(k)+',bubbles:true}))');
 const check=(ok,msg)=>{assert.ok(ok,msg);passed.push(msg)};
 const audTab=(await tabs()).find(t=>t.type==='page'&&t.url.includes('audience'));
 check(!!audTab,'P opens presenter and audience');const a=await attach(audTab);
 check((await state()).syncState==='synced','Audience synchronized');
 await click('#ppt-next');await delay(550);check((await state()).idx===1,'Next page');
 await click('#ppt-prev');await delay(300);check((await state()).idx===0,'Previous page');
 await click('#ppt-grid');check((await state()).presenterOverviewOn,'Presenter grid opens');
 await b.ev('document.querySelectorAll(".ppt-overview-page")[4].click()');await delay(500);check((await state()).idx===4,'Grid selects page 5');
 await key('Home');await delay(250);check((await state()).idx===0,'Home');
 await key('End');await delay(250);check((await state()).idx===total-1,'End');
 await click('#ppt-last');await delay(300);check((await state()).idx===0,'Restart from last page');
 await click('#ppt-timer-toggle');await delay(1100);check((await state()).timerRunning,'Timer start');
 await click('#ppt-timer-toggle');check(!(await state()).timerRunning,'Timer pause');
 await click('#ppt-timer-reset');check(await b.ev('document.querySelector("#ppt-clock").textContent==="00:00"'),'Timer reset');
 await click('#ppt-rehearsal');await delay(250);check((await state()).rehearsalActive,'Rehearsal start');
 await click('#ppt-rehearsal');check(await b.ev('document.querySelector("#ppt-dialog-title").textContent==="排练记录"'),'Rehearsal summary');await click('#ppt-dialog-close');
 await key('b');await delay(300);check(await a.ev('getComputedStyle(document.querySelector("#ppt-audience-cover")).backgroundColor==="rgb(0, 0, 0)"'),'Audience black screen');
 await key('w');await delay(300);check(await a.ev('getComputedStyle(document.querySelector("#ppt-audience-cover")).backgroundColor==="rgb(255, 255, 255)"'),'Audience white screen');
 await key('w');await key('f');await click('#ppt-next');await delay(400);check((await state()).screenMode==='frozen'&&(await a.ev('window.__currentSlideIndex'))===0,'Freeze retains audience page');
 await key('f');await delay(500);check((await a.ev('window.__currentSlideIndex'))===1,'Unfreeze catches up');
 await key('l');check((await state()).annotationTool==='laser','Laser tool');
 await key('c');check((await state()).annotationTool==='circle','Circle tool');
 const rect=await b.ev('(()=>{const r=document.querySelector("#ppt-presenter-ink").getBoundingClientRect();return {x:r.x,y:r.y,w:r.width,h:r.height}})()');
 await b.call('Input.dispatchMouseEvent',{type:'mousePressed',x:rect.x+rect.w*.3,y:rect.y+rect.h*.3,button:'left',clickCount:1});
 await b.call('Input.dispatchMouseEvent',{type:'mouseMoved',x:rect.x+rect.w*.6,y:rect.y+rect.h*.6,button:'left',buttons:1});
 await b.call('Input.dispatchMouseEvent',{type:'mouseReleased',x:rect.x+rect.w*.6,y:rect.y+rect.h*.6,button:'left',clickCount:1});await delay(250);
 check((await state()).circles.length===1,'Circle drawing');await key('x');check((await state()).circles.length===0,'Clear annotations');await key('Escape');
 await click('#ppt-settings');await b.ev('document.querySelector("#ppt-auto-seconds-setting").value="8";document.querySelector("#ppt-auto-enabled-setting").checked=true');await click('#ppt-settings-save');await delay(300);
 check((await state()).autoEnabled,'Auto advance enabled');await click('#ppt-grid');await delay(300);check(await b.ev('document.querySelector("#ppt-auto-toggle").textContent.includes("宫格")'),'Auto pauses in grid');await click('#ppt-overview-close');await delay(350);check(await b.ev('!document.querySelector("#ppt-auto-toggle").textContent.includes("宫格")'),'Auto resumes');await click('#ppt-auto-toggle');check(!(await state()).autoEnabled,'Auto disabled');
 await b.ev('(()=>{const e=document.querySelector("#ppt-note-editor");e.value += "\\nQA临时验证";e.dispatchEvent(new Event("input",{bubbles:true}))})()');await delay(500);await click('#ppt-next');await delay(250);await click('#ppt-prev');await delay(300);check(await b.ev('document.querySelector("#ppt-note-editor").value.includes("QA临时验证")'),'Notes persist by slide ID');
 await click('#ppt-preflight');check(await b.ev('document.querySelector("#ppt-dialog-body").textContent.includes("16:9")'),'Preflight opens');await click('#ppt-dialog-close');
 const sizes=[];for(const [w,h] of [[1600,900],[1280,720],[1024,768]]){await b.call('Emulation.setDeviceMetricsOverride',{width:w,height:h,deviceScaleFactor:1,mobile:false});await delay(300);const ratios=await b.ev('[...document.querySelectorAll(".ppt-frame")].map(e=>{const r=e.getBoundingClientRect();return r.width/r.height})');check(ratios.every(r=>Math.abs(r-16/9)<.003),'16:9 previews '+w+'x'+h);sizes.push({w,h,ratios})}
 await b.call('Emulation.setDeviceMetricsOverride',{width:1600,height:900,deviceScaleFactor:1,mobile:false});await delay(400);
 await b.ev('(()=>{const e=document.querySelector("#ppt-note-editor");e.value=e.value.replace("\\nQA临时验证","");e.dispatchEvent(new Event("input",{bubbles:true}))})()');await delay(500);
 fs.writeFileSync(path.join(out,'presenter.png'),Buffer.from((await b.call('Page.captureScreenshot',{format:'png'})).data,'base64'));
 await a.call('Page.close');await delay(1200);check((await state()).syncState==='disconnected','Audience close detected');await click('#ppt-reopen');await delay(1100);check((await state()).syncState==='synced','Audience reconnect at current page');
 await click('#ppt-exit');await delay(600);check((await state()).mode==='deck','Exit presenter');check(!(await tabs()).some(t=>t.type==='page'&&t.url.includes('audience')),'Audience closes on exit');
 check(errors.length===0,'No browser JavaScript exceptions');
 fs.writeFileSync(path.join(out,'interaction-report.json'),JSON.stringify({passed,sizes,errors},null,2));console.log('INTERACTIONS',JSON.stringify(passed));
 await b.call('Browser.close');
 }
}finally{clearInterval(keepAlive);sockets.forEach(w=>w.close());child.kill()}
