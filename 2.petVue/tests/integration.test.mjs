import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { createServer } from 'vite'
const server = await createServer({ server:{ middlewareMode:true }, optimizeDeps:{noDiscovery:true,include:[]}, appType:'custom' })
try {
 const { moduleMap } = await server.ssrLoadModule('/src/config/modules.js')
 const actions = await server.ssrLoadModule('/src/config/actions.js')
 const { schemaFor,prepare,validate,payloadFor } = await server.ssrLoadModule('/src/api/forms.js')
 const spec = JSON.parse(await readFile('../3.petSpringboot/docs/openapi.json','utf8'))
 const model = JSON.parse(await readFile('../3.petSpringboot/tools/schema-model.json','utf8'))
 const sources = {order:'pet_order',audit:'sitter_profile',qualifications:'sitter_qualification',finance:'settlement',refund:'refund_record',member:'membership',plans:'membership_plan',benefits:'membership_benefit',sitter:'sitter_profile',grades:'sitter_grade','grade-revisions':'sitter_grade_revision',user:'app_user',coupon:'coupon_template','coupon-logs':'coupon_claim_log',price:'service_item',review:'review',complaints:'complaint',msg:'notification_template',system:'operation_log',roles:'role_permission',staff:'app_user',service:'support_conversation',emergency:'emergency_ticket',hospitals:'hospital'}
 let count = 0
 for (const m of Object.values(moduleMap)) for (const tab of m.tabs) {
  if (tab.key === 'dict') continue
  const table = model.tables.find(t => t.table_name === sources[tab.key])
  assert.ok(table,'Missing source for ' + tab.key)
  const fields = new Set(table.columns.map(c => c.name))
  if (['sitter','audit','qualifications'].includes(tab.key)) ['name','phone','grade_name','commission_rate'].forEach(c => fields.add(c))
  for (const c of tab.columns) assert.ok(fields.has(c),tab.key + ' has unknown column ' + c)
  count++
 }
 const all = [...Object.values(actions.edits),...Object.values(actions.creates),actions.assignAction,actions.pushAction,actions.cancelAction,actions.refundAction]
 for (const a of all) {
  const path = '/api' + a.path.replace(/:([a-z]+)/g,'{$1}')
  const operation = spec.paths[path]?.[a.method.toLowerCase()]
  assert.ok(operation,a.method + ' ' + path)
  assert.equal(operation.requestBody.content['application/json'].schema.$ref.split('/').pop(),a.schema)
 }
 const coupon = prepare(schemaFor('Coupon'), {name:'测试券',issue_type:'claim',amount:10,threshold:0,service_codes:[],valid_days:30,active:true,audience:'ALL',per_user_limit:1,stock:50,claim_starts_at:'2026-11-16T10:00:00+08:00',claim_ends_at:'2026-12-16T10:00:00+08:00'})
 validate(schemaFor('Coupon'),coupon)
 assert.deepEqual(payloadFor(schemaFor('Coupon'),coupon).serviceCodes,[])
 assert.equal(payloadFor(schemaFor('Coupon'),coupon).claimStartsAt,'2026-11-16T02:00:00.000Z')
 assert.throws(() => validate(schemaFor('Assign'),{sitterId:1,reason:''}))
 assert.throws(() => validate(schemaFor('Price'),{name:'服务',price:-1,active:true}))
 const profile = prepare(schemaFor('Profile'),{ personal:{name:'喂养师',radius_km:5}, grade_id:2,categories:'["CAT"]',schedule:[{weekday:1,start_time:'09:00:00',end_time:'18:00:00'}] })
 assert.equal(profile.personal.radiusKm,5); assert.equal(profile.gradeId,2); assert.equal(profile.schedule[0].startTime,'09:00:00'); assert.deepEqual(profile.categories,['CAT'])
 const memory = new Map()
 globalThis.sessionStorage = { getItem:k=>memory.get(k),setItem:(k,v)=>memory.set(k,v),removeItem:k=>memory.delete(k) }
 let expired = 0
 globalThis.window = { dispatchEvent:()=>expired++ }
 const { request,tokenStore } = await server.ssrLoadModule('/src/api/client.js')
 tokenStore.set('test-token')
 globalThis.fetch = async (url, options) => {
  assert.equal(url,'/api/admin/modules/order?page=2&keyword=%E7%8C%AB')
  assert.equal(options.headers.Authorization,'Bearer test-token')
  return new Response(JSON.stringify({code:0,data:{list:[],total:0}}))
 }
 assert.equal((await request('/admin/modules/order',{query:{page:2,keyword:'猫',status:''}})).total,0)
 globalThis.fetch = async () => new Response(JSON.stringify({code:401,message:'登录已失效'}),{status:401})
 await assert.rejects(request('/auth/me'),/登录已失效/)
 assert.equal(tokenStore.get(),undefined); assert.equal(expired,1)
 globalThis.fetch = async () => new Response(JSON.stringify({code:400,message:'库存不足'}),{status:400})
 await assert.rejects(request('/admin/coupons/1/push',{method:'POST',body:{ownerIds:[1],quantity:1}}),/库存不足/)
 globalThis.fetch = async () => { throw new TypeError('fetch failed') }
 await assert.rejects(request('/admin/dashboard'),/无法连接后端/)
 console.log('PASS: ' + count + ' list contracts, ' + all.length + ' write contracts, nested DTO mapping, form validation, auth expiry and request errors')
} finally { await server.close() }
