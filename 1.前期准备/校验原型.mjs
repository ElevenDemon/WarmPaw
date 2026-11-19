import {createServer} from 'node:http';
import {readFile,mkdtemp,writeFile} from 'node:fs/promises';
import {spawn} from 'node:child_process';
import {tmpdir} from 'node:os';
import path from 'node:path';
import assert from 'node:assert/strict';
const root=path.resolve('1.前期准备');
const server=createServer(async(req,res)=>{try{const name=decodeURIComponent(new URL(req.url,'http://localhost').pathname).slice(1);if(path.basename(name)!==name)throw Error('bad path');res.setHeader('Content-Type',name.endsWith('.js')?'text/javascript; charset=utf-8':'text/html; charset=utf-8');res.end(await readFile(path.join(root,name)));}catch{res.statusCode=404;res.end();}});
await new Promise(r=>server.listen(4297,'127.0.0.1',r));
const profile=await mkdtemp(path.join(tmpdir(),'warm-paw-prototype-'));
const child=spawn('C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--guest','--remote-debugging-port=9497','--user-data-dir='+profile,'about:blank'],{windowsHide:true,stdio:'ignore'});
const delay=ms=>new Promise(r=>setTimeout(r,ms));let ws;
try{
 let tabs;for(let i=0;i<60;i++){try{tabs=await(await fetch('http://127.0.0.1:9497/json/list')).json();if(tabs.length)break;}catch{}await delay(250);}
 assert.ok(tabs?.length,'Browser debugging did not start');
 ws=new WebSocket(tabs.find(t=>t.type==='page').webSocketDebuggerUrl);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});
 let seq=0;const pending=new Map(),errors=[];
 ws.onmessage=e=>{const p=JSON.parse(e.data);if(p.id){const f=pending.get(p.id);pending.delete(p.id);p.error?f.reject(Error(p.error.message)):f.resolve(p.result);}if(p.method==='Runtime.exceptionThrown')errors.push(p.params.exceptionDetails.exception?.description||p.params.exceptionDetails.text);};
 const call=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}));});
 const run=async expression=>{const r=await call('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value;};
 await call('Runtime.enable');await call('Page.enable');await call('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});
 for(const name of ['暖爪-原型图.html','暖爪-原型图-01-官网.html','暖爪-原型图-02-宠主端.html','暖爪-原型图-03-喂养师端.html','暖爪-原型图-04-运营后台.html']){
  await call('Page.navigate',{url:'http://127.0.0.1:4297/'+encodeURIComponent(name)});
  for(let i=0;i<80;i++){if(await run('document.readyState==="complete" && !!document.querySelector(".sync-badge")'))break;await delay(100);}
  await delay(1100);assert.ok(await run('!!document.querySelector(".sync-badge")'),name);
  if(await run('!!document.querySelector("#page-owner")')){
   assert.equal(await run('document.querySelectorAll("#page-owner .phone").length'),21);
   assert.ok(await run(`(()=>{const p=document.querySelector('#ow-service');p.querySelector('.sync-topics button').click();return p.querySelector('.sync-chat').textContent.includes('参考：');})()`));
   assert.ok(await run(`(()=>{const p=document.querySelector('#ow-service');[...p.querySelectorAll('button')].find(b=>b.textContent==='联系人工客服').click();p.querySelector('input').value='请人工核对';p.querySelector('.sync-stack button').click();return p.querySelector('.sync-status').textContent.includes('留言');})()`));
   assert.ok(await run(`(()=>{const p=document.querySelector('#ow-service');[...p.querySelectorAll('button')].find(b=>b.textContent.includes('恢复智能客服')).click();return p.querySelector('.sync-chat').children.length===1;})()`));
   assert.ok(await run(`(()=>{const p=document.querySelector('#ow-addresses');[...p.querySelectorAll('button')].find(b=>b.textContent.includes('新增')).click();p.querySelector('[data-f="name"]').value='测试宠主';p.querySelector('[data-f="phone"]').value='13800000000';p.querySelector('[data-f="search"]').value='科技园';p.querySelector('[data-f="search"]').dispatchEvent(new Event('input'));p.querySelector('[data-results] button').click();[...p.querySelectorAll('button')].find(b=>b.textContent==='保存地址').click();return p.querySelector('.body').textContent.includes('科技园示例小区');})()`));
   await run(`owShow('service')`);
   if(name.includes('02-')){await delay(250);const shot=await call('Page.captureScreenshot',{format:'png'});await writeFile(path.join(root,'暖爪-v1.5-客服预览.png'),Buffer.from(shot.data,'base64'));}
  }
  if(await run('!!document.querySelector("#page-sitter")')){
   assert.equal(await run('document.querySelectorAll("#page-sitter .phone").length'),9);
   assert.ok(await run(`document.querySelector('#st-service .sync-topics').textContent.includes('接单与档期')`));
   assert.ok(await run(`(()=>{const p=document.querySelector('#st-profileedit .sync-preferences');p.querySelector('textarea').value='可照护怕生宠物';p.querySelector('button').click();return p.textContent.includes('已保存');})()`));
  }
  if(await run('!!document.querySelector("#wp-ai-top")')){
   assert.ok(await run(`(()=>{const row=[...document.querySelectorAll('#wp-ai-result-body tr')].find(r=>r.dataset.aiOk==='1');if(!row)return true;const id=row.dataset.aiId;const s=document.querySelector('[data-scenario="lowStarId"]');s.value=id;s.dispatchEvent(new Event('change'));const updated=[...document.querySelectorAll('#wp-ai-result-body tr')].find(r=>r.dataset.aiId===id);return updated.dataset.aiOk==='0'&&updated.textContent.includes('1–2 星');})()`));
   assert.ok(await run(`(()=>{const low=document.querySelector('[data-scenario="lowStarId"]');low.value='';low.dispatchEvent(new Event('change'));const row=[...document.querySelectorAll('#wp-ai-result-body tr')].find(r=>r.dataset.aiOk==='1');if(!row)return true;const id=row.dataset.aiId;const s=document.querySelector('[data-scenario="familiarId"]');s.value=id;s.dispatchEvent(new Event('change'));const updated=[...document.querySelectorAll('#wp-ai-result-body tr')].find(r=>r.dataset.aiId===id);return document.querySelector('#wp-ai-explain').textContent.includes('同宠 40');})()`));
  }
  const duplicates=await run(`(()=>{const ids=[...document.querySelectorAll('[id]')].map(n=>n.id);return ids.filter((id,i)=>ids.indexOf(id)!==i);})()`);
  assert.equal(duplicates.length,0,'Duplicate IDs: '+duplicates.join(','));
  console.log('PASS '+name);
 }
 assert.deepEqual(errors,[]);console.log('PASS no runtime exceptions; owner/sitter counts, chat, handoff/resume, address save, preferences, unique IDs');
}finally{ws?.close();child.kill();server.close();}
