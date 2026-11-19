// Creates labelled demo business data using real APIs; never bypasses business rules.
import assert from 'node:assert/strict'
import { readFile, writeFile } from 'node:fs/promises'
const base = process.env.SEED_API_URL || 'http://localhost:8081/api'
const password = process.env.SEED_PASSWORD || 'admin123456'
const stateFile = new URL('./integration-seed-state.json', import.meta.url)
let state
try { state = JSON.parse(await readFile(stateFile,'utf8')) } catch { state = { run:Date.now().toString(36), steps:{} } }
const sessions = []
async function call(token, method, path, body, expected=200) {
 const r = await fetch(base+path,{ method, headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body === undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(20000) })
 const p = await r.json()
 assert.equal(r.status,expected,method+' '+path+': '+JSON.stringify(p))
 return p.data
}
async function login(username) { const d=await call(null,'POST','/auth/login',{username,password});sessions.push(d.token);return d }
async function step(key, fn) {
 if (key in state.steps) return state.steps[key]
 const value=await fn();state.steps[key]=value ?? true
 await writeFile(stateFile,JSON.stringify(state,null,2)+'\n');console.log('PASS',key)
 return value
}
const label = name => '联调演示-'+state.run+'-'+name
try {
 const admin=(await login('admin')).token, ownerSession=await login('owner'), owner=ownerSession.token
 const sitter=(await login('sitter')).token
 const list=async module=>(await call(admin,'GET','/admin/modules/'+module+'?size=100')).list
 const catalog=await call(null,'GET','/public/catalog')
 const profile=await call(sitter,'GET','/sitter/profile')
 const sitterId=profile.id
 const yearLater=new Date(Date.now()+365*86400000).toISOString()
 const pet=await step('pet',()=>call(owner,'POST','/owner/pets',{name:label('年糕').slice(0,32),category:'CAT',breed:'英国短毛猫',dietaryNotes:'联调演示：每日定量喂食',temperament:'温顺'}))
 const address=await step('address',()=>call(owner,'POST','/owner/addresses',{contactName:'联调演示宠主',phone:'13800000001',district:'南山区',address:label('科技园演示地址'),longitude:113.95,latitude:22.55}))
 await step('membership',async()=>{
  const id=await call(owner,'POST','/membership/purchases',{planId:catalog.plans.find(p=>p.months===12).id})
  await call(owner,'POST','/membership/purchases/'+id+'/demo-pay');return id
 })
 const couponBody={name:label('领取券'),issueType:'claim',amount:5,threshold:0,serviceCodes:[],validDays:30,active:true,audience:'ALL',perUserLimit:1,stock:50,claimStartsAt:new Date(Date.now()-86400000).toISOString(),claimEndsAt:yearLater,limitDevice:false,limitPhone:false,limitIp:false}
 const claimTemplate=await step('claim-template',()=>call(admin,'POST','/admin/coupons',couponBody))
 await step('coupon-claim',async()=>{const d=await call(owner,'POST','/coupons/'+claimTemplate+'/claim',{deviceId:label('device')});assert.equal(d.success,true);return d.couponId})
 const pushTemplate=await step('push-template',()=>call(admin,'POST','/admin/coupons',{...couponBody,name:label('直发券'),issueType:'push'}))
 await step('coupon-push',()=>call(admin,'POST','/admin/coupons/'+pushTemplate+'/push',{ownerIds:[ownerSession.user.id],quantity:1}))
 // New applicant leaves pending qualifications for the admin page, without altering existing credentials.
 const applicantName='demo_'+state.run
 await step('applicant-account',()=>call(null,'POST','/auth/register',{username:applicantName,password,name:'联调待审核',phone:'139'+String(Date.now()).slice(-8)}))
 const applicant=(await login(applicantName)).token
 const personal={name:'联调待审核',phone:'139'+String(state.steps['applicant-phone'] || Date.now()).slice(-8),district:'南山区',address:'联调演示科技园',longitude:113.95,latitude:22.55,radiusKm:5,transport:'E_BIKE'}
 const applicantId=await step('applicant-profile',()=>call(applicant,'POST','/sitter/application',{personal,categories:['CAT']}))
 const qualification=await step('qualification-approved',async()=>{
  const id=await call(applicant,'POST','/sitter/qualifications',{category:'CAT',materials:['integration/'+state.run+'/qualification-cat.jpg']})
  await call(admin,'POST','/admin/qualifications/'+id+'/audit',{approved:true,realVerified:true,trainingPassed:true,practicePassed:false,expiresAt:yearLater,note:'联调演示资质审核通过（材料为演示对象键）'});return id
 })
 await step('qualification-pending',()=>call(applicant,'POST','/sitter/qualifications',{category:'DOG',materials:['integration/'+state.run+'/qualification-dog.jpg']}))
 const hospital=await step('hospital',()=>call(admin,'POST','/admin/hospitals',{name:label('合作医院'),phone:'075500000000',address:'联调演示南山区科技园',longitude:113.95,latitude:22.55,emergency24h:true,active:true}))
 const create=async(key, offset=5)=>step('order-'+key,async()=>{
  const start=new Date(Date.now()+offset*60000)
  const d=await call(owner,'POST','/orders',{requestKey:'integration-'+state.run+'-'+key,addressId:address,petIds:[pet],serviceStart:start.toISOString(),serviceEnd:new Date(+start+20*60000).toISOString(),items:[{serviceId:catalog.services.find(s=>s.code==='BASE').id,quantity:1}],agreementVersion:'integration-v1',agreementAccepted:true,authorizationScope:'联调演示上门喂养，仅用于本地测试',notes:label(key)})
  return d.id
 })
 const paid=async id=>call(owner,'POST','/orders/'+id+'/demo-pay')
 const assign=async id=>{
  const result=await call(admin,'GET','/admin/orders/'+id+'/eligibility/'+sitterId)
  assert.equal(result.eligible,true,JSON.stringify(result))
  await call(admin,'POST','/admin/orders/'+id+'/assign',{sitterId,reason:'联调演示：已核对档期及资质'})
  await call(sitter,'POST','/orders/'+id+'/accept')
 }
 const complete=async key=>{
  const id=await create(key)
  await step('complete-'+key,async()=>{
   await paid(id);await assign(id);await call(sitter,'POST','/orders/'+id+'/checkin')
   await call(sitter,'POST','/orders/'+id+'/complete',undefined,400)
   for(const code of ['FEED','WATER','CLEAN','PLAY','DOOR']) await call(sitter,'PUT','/orders/'+id+'/checklist',{code,completed:true,quantity:10,unit:'MINUTE',notes:'联调演示清单'})
   await call(sitter,'POST','/orders/'+id+'/media',{kind:'VIDEO',objectKey:'integration/'+state.run+'/'+id+'/demo-recording.mp4'})
   await call(sitter,'POST','/orders/'+id+'/complete')
   const report=await call(admin,'GET','/orders/'+id+'/report');assert.equal(report.status,'PENDING_AI')
   await call(owner,'POST','/orders/'+id+'/review',{score:5,tags:['联调演示'],content:'联调演示：服务清单完整，已完成验收'})
   return id
  });return id
 }
 const settled=await complete('已结算')
 await step('settled',async()=>{
  const row=(await list('finance')).find(r=>r.order_id===settled);assert.ok(row)
  await call(admin,'POST','/admin/settlements/batch-settle',{ids:[row.id]})
  await call(admin,'POST','/admin/orders/'+settled+'/demo-refund',{reason:'联调验证已结算不能退款'},409);return row.id
 })
 const pending=await complete('待结算')
 const refunded=await create('已退款')
 await step('refunded',async()=>{await paid(refunded);await call(admin,'POST','/admin/orders/'+refunded+'/demo-refund',{reason:'联调演示行程取消退款'});return refunded})
 const cancelled=await create('已取消')
 await step('cancelled',()=>call(owner,'POST','/orders/'+cancelled+'/cancel',{reason:'联调演示取消未支付订单'}))
 const dispatching=await create('待派单',1440)
 await step('dispatching',()=>paid(dispatching))
 const accepted=await create('已接单',1500)
 await step('accepted',async()=>{await paid(accepted);await assign(accepted);return accepted})
 const servicing=await create('服务中')
 await step('servicing',async()=>{await paid(servicing);await assign(servicing);await call(sitter,'POST','/orders/'+servicing+'/checkin');return servicing})
 const complaint=await step('complaint',()=>call(owner,'POST','/complaints',{orderId:pending,content:label('请协助核查服务时长')}))
 await step('complaint-processing',()=>call(admin,'PUT','/admin/complaints/'+complaint,{status:'PROCESSING',resolution:'联调演示：已联系宠主核查'}))
 const closedComplaint=await step('closed-complaint',()=>call(owner,'POST','/complaints',{orderId:settled,content:label('服务咨询已确认')}))
 await step('complaint-resolved',()=>call(admin,'PUT','/admin/complaints/'+closedComplaint,{status:'RESOLVED',resolution:'联调演示：已核对记录并取得宠主确认'}))
 const emergency=await step('emergency',()=>call(owner,'POST','/emergencies',{orderId:servicing,content:label('宠物呕吐，申请协助')}))
 await step('emergency-transport',async()=>{
  await call(admin,'PUT','/admin/emergencies/'+emergency,{status:'ACCEPTED',sitterId,hospitalId:hospital})
  await call(admin,'PUT','/admin/emergencies/'+emergency,{status:'TRANSPORTING',sitterId,hospitalId:hospital},400)
  await call(owner,'POST','/emergencies/'+emergency+'/consent')
  await call(admin,'PUT','/admin/emergencies/'+emergency,{status:'TRANSPORTING',sitterId,hospitalId:hospital});return emergency
 })
 const resolvedEmergency=await step('resolved-emergency',()=>call(owner,'POST','/emergencies',{orderId:pending,content:label('已完成的异常咨询')}))
 await step('emergency-resolved',()=>call(admin,'PUT','/admin/emergencies/'+resolvedEmergency,{status:'RESOLVED',resolution:'联调演示：复盘完毕，无需送医'}))
 const conversation=await step('conversation',()=>call(owner,'POST','/support/conversations',{orderId:servicing}))
 await step('conversation-messages',async()=>{
  await call(owner,'POST','/messages',{conversationId:conversation,content:label('请问服务进度？'),mediaKeys:[]})
  await call(admin,'POST','/admin/conversations/'+conversation+'/takeover')
  await call(admin,'POST','/messages',{conversationId:conversation,content:'联调演示客服：已核查，喂养师正在服务。',mediaKeys:[]});return conversation
 })
 const totals={}
 for(const module of ['order','audit','finance','refund','member','sitter','user','coupon','coupon-logs','review','complaints','service','emergency','hospitals']) {
  const result=await call(admin,'GET','/admin/modules/'+module+'?size=10');assert.ok(result.total>0,module+' should contain data');totals[module]=result.total
 }
 await writeFile(new URL('./integration-seed-report.json',import.meta.url),JSON.stringify({verifiedAt:new Date().toISOString(),run:state.run,base,totals,orders:{settled,pending,refunded,cancelled,dispatching,accepted,servicing},applicantId,qualification,hospital,conversation,emergency},null,2)+'\n')
 console.log('COMPLETE',JSON.stringify(totals))
} finally {
 await Promise.allSettled(sessions.map(token=>call(token,'POST','/auth/logout')))
}
