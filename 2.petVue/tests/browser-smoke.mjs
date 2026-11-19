// Isolated API fixtures: this test never writes to the business backend.
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createServer } from 'vite'
const live = process.argv.includes('--live')
const edge = process.env.BROWSER_PATH || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
let price = {id:1,code:'BASE',name:'基础上门喂养',description:'喂食与换水',price:35,active:true}
const user = {id:1,name:'测试管理员',role:'ADMIN',permissions:['*']}
const mutations = [], browserErrors = []
const server = await createServer({ server:{port:4199,strictPort:true,proxy:live ? {'/api':{target:process.env.TEST_BACKEND_URL || 'http://localhost:8080',changeOrigin:true,configure:proxy=>proxy.on('proxyReq',req=>req.removeHeader('origin'))}} : {}}, plugins:[{
 name:'isolated-api-fixture',configureServer(server) { server.middlewares.use(async (req,res,next) => {
  if (live || !req.url.startsWith('/api/')) return next()
  const url = new URL(req.url,'http://localhost'); let body = ''
  for await (const chunk of req) body += chunk
  let data
  if (req.method !== 'GET') mutations.push({path:url.pathname,body:body ? JSON.parse(body) : null})
  if (url.pathname === '/api/auth/login') data = {token:'fixture-token',user}
  else if (url.pathname === '/api/auth/me') data = user
  else if (url.pathname === '/api/public/health') data = {status:'UP',demoPaymentsEnabled:false}
  else if (url.pathname === '/api/admin/dashboard') data = {orders:8,gmv:350,manualDispatchPending:2,openComplaints:1,aiEnabled:false}
  else if (url.pathname === '/api/admin/dictionary') data = [{table_name:'pet_order',column_name:'id',data_type:'bigint',is_nullable:'NO'}]
  else if (url.pathname === '/api/admin/prices/1') { price = {...price,...JSON.parse(body)}; data = 1 }
  else if (url.pathname.startsWith('/api/admin/modules/')) {
   const module = url.pathname.split('/').pop()
   const rows = module === 'price' ? [price] : module === 'service' ? [{id:2,owner_id:4,order_id:7,status:'WAITING_HUMAN'}] : []
   data = {list:rows,total:rows.length,page:1,size:10}
  } else if (url.pathname === '/api/messages' && req.method === 'POST') data = 1
  else if (url.pathname === '/api/messages') data = {list:[{id:1,sender_id:4,content:'请问服务进度？',created_at:'2026-11-16T10:00:00+08:00'}],total:1}
  else if (url.pathname === '/api/notifications') data = {list:[],total:0}
  else data = true
  res.setHeader('Content-Type','application/json');res.end(JSON.stringify({code:0,message:'ok',data}))
 }) }
}] })
const profile = await mkdtemp(join(tmpdir(),'pet-vue-browser-'))
const debugPort = 9300 + Math.floor(Math.random()*500)
let child, ws
const delay = ms => new Promise(r=>setTimeout(r,ms))
try {
 await server.listen()
 child = spawn(edge,['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port='+debugPort,'--guest','--disable-features=msImplicitSignin,msSingleSignOnOSForPrimaryAccount','--user-data-dir='+profile,'about:blank'],{windowsHide:true,stdio:'ignore'})
 child.on('error',e=>browserErrors.push(e.message))
 let tabs
 for(let i=0;i<80;i++) { try { tabs = await (await fetch('http://127.0.0.1:'+debugPort+'/json/list')).json();if(tabs.length)break } catch {} await delay(250) }
 assert.ok(tabs?.length,'Browser debugging endpoint did not start')
 ws = new WebSocket(tabs.find(t=>t.type==='page' && t.url === 'about:blank').webSocketDebuggerUrl)
 await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=reject})
 const pending = new Map(); let sequence=0
 ws.onmessage = event => {
  const packet=JSON.parse(event.data)
  if(packet.id) { const p=pending.get(packet.id); if(p){pending.delete(packet.id);packet.error?p.reject(new Error(packet.error.message)):p.resolve(packet.result)} }
  if(packet.method==='Runtime.exceptionThrown') browserErrors.push(packet.params.exceptionDetails.exception?.description || packet.params.exceptionDetails.text)
 }
 const call=(method,params={})=>new Promise((resolve,reject)=>{const id=++sequence;pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}))})
 const evaluate=async expression=>{const result=await call('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(result.exceptionDetails)throw new Error(result.exceptionDetails.text);return result.result.value}
 const until=async expression=>{for(let i=0;i<100;i++){if(await evaluate(expression))return;await delay(100)}throw new Error('Timed out: '+expression+'; '+await evaluate('document.body.innerText.slice(0,1500)'))}
 const click=async text=>{assert.ok(await evaluate('(() => { const b=[...document.querySelectorAll("button")].find(b=>b.textContent.trim()==='+JSON.stringify(text)+' && b.getClientRects().length); if(!b)return false;b.click();return true })()'),'Missing button '+text)}
 const fill=async (selector,value)=>evaluate('(() => { const el=document.querySelector('+JSON.stringify(selector)+'); const setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,"value").set;setter.call(el,'+JSON.stringify(value)+');el.dispatchEvent(new Event("input",{bubbles:true}));el.dispatchEvent(new Event("change",{bubbles:true})); })()')
 await call('Runtime.enable'); await call('Page.enable'); await call('Page.navigate',{url:'http://localhost:4199/'})
 await until('!!document.querySelector(".login-card")')
 await fill('input[autocomplete="username"]','admin');await fill('input[autocomplete="current-password"]',live ? (process.env.TEST_ADMIN_PASSWORD || 'admin123456') : 'fixture-password');await click('登录')
 await until('!!document.querySelector(".stats-grid .stat-card")')
 for(const key of ['order','dispatch','audit','service','emergency','finance','member','sitter','user','coupon','price','review','msg','system','dict']) {
  await evaluate('location.hash="/admin/'+key+'"')
  await delay(200)
  await until('!!document.querySelector(".data-panel .el-table") && !document.querySelector(".el-loading-mask")')
  assert.equal(await evaluate('document.querySelector(".el-alert--error")?.textContent || ""'),'','Error in '+key)
  const tabsCount=await evaluate('document.querySelectorAll(".module-tabs button").length')
  for(let i=1;i<tabsCount;i++) {
   await evaluate('document.querySelectorAll(".module-tabs button")['+i+'].click()')
   await delay(150);await until('!document.querySelector(".el-loading-mask")')
  }
 }
 if (live && process.argv.includes('--business')) {
  await evaluate('location.hash="/admin/user"');await delay(300);await click('详情')
  await until('document.body.textContent.includes("宠物档案") && document.body.textContent.includes("上门地址")')
  await evaluate('document.querySelector(".el-drawer__close-btn").click()');await delay(300)
  await evaluate('location.hash="/admin/order"');await delay(400)
  await evaluate('(() => {const row=[...document.querySelectorAll(".el-table__body tr")].find(r=>r.textContent.includes("已完成"));if(!row)throw Error("No completed order");[...row.querySelectorAll("button")].find(b=>b.textContent.trim()==="详情").click()})()')
  await until('document.body.textContent.includes("服务事实快照") && document.body.textContent.includes("等待 AI")')
  await evaluate('document.querySelector(".el-drawer__close-btn").click()');await delay(300)
  console.log('PASS: REAL populated owner/pet/address/membership and completed order/checklist/media/report detail pages')
 }
 await evaluate('location.hash="/admin/price"');await until('document.body.textContent.includes("基础上门喂养")')
 await click('编辑价格');await until('!!document.querySelector(".el-dialog input")')
 if (!live) await fill('.el-dialog .el-input-number input',42)
 await click('确认提交');await until('!!document.querySelector(".el-message-box")');await click('确定')
 await until('!document.querySelector(".el-overlay-dialog") || !document.querySelector(".el-overlay-dialog").getClientRects().length')
 if (!live) {
 assert.equal(price.price,42)
 assert.ok(mutations.some(m=>m.path==='/api/admin/prices/1'&&m.body.active===true))
 await evaluate('location.hash="/admin/service"');await until('document.body.textContent.includes("待人工接管")');await click('会话');await until('document.body.textContent.includes("请问服务进度？")');await click('人工接管')
 await until('!!document.querySelector("textarea:not([disabled])")')
 await evaluate('(() => {const el=document.querySelector("textarea");Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,"value").set.call(el,"正在核查服务进度");el.dispatchEvent(new Event("input",{bubbles:true}));})()')
 await click('发送');await until('document.body.textContent.includes("消息已发送")')
 assert.ok(mutations.some(m=>m.path==='/api/messages'&&m.body.conversationId===2))
 }
 await call('Page.reload');await delay(700);await until('!!document.querySelector(".user-chip")')
 await click('退出');await until('!!document.querySelector(".login-card")')
 assert.deepEqual(browserErrors,[])
 console.log(live ? 'PASS: REAL backend browser login, 15 modules/subtabs, unchanged price save, session refresh and logout' : 'PASS: fixture browser login, 15 modules/subtabs, price save, support takeover/send, session refresh and logout')
 await call('Browser.close').catch(()=>{})
} finally {
 ws?.close();child?.kill();await server.close();await delay(500);await rm(profile,{recursive:true,force:true}).catch(()=>{})
}
