<script setup>
import { computed, ref, watch, onBeforeUnmount, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download, Plus } from '@element-plus/icons-vue'
import { adminApi, request } from '../api/client'
import { moduleMap, fieldLabel, format, states } from '../config/modules'
import { edits, creates, assignAction, pushAction, cancelAction, refundAction } from '../config/actions'
import { can, restoreSession } from '../stores/auth'
import { schemaFor, prepare, validate, payloadFor } from '../api/forms'
import SchemaFields from '../components/SchemaFields.vue'
import RecordDetail from '../components/RecordDetail.vue'
const route = useRoute()
const demoPaymentsEnabled = ref(false)
onMounted(async () => { try { const health = await request('/public/health'); demoPaymentsEnabled.value = health.demoPaymentsEnabled === true } catch { /* List requests display connection errors. */ } })
const meta = computed(() => moduleMap[route.name])
const tabKey = ref(''), query = ref(''), status = ref(''), page = ref(1), size = ref(10)
const rows = ref([]), total = ref(0), loading = ref(false), error = ref(''), selectedRows = ref([])
const tab = computed(() => meta.value.tabs.find(t => t.key === tabKey.value) || meta.value.tabs[0])
const creation = computed(() => creates[tabKey.value])
const drawer = ref(false), detail = ref(null), detailBusy = ref(false), conversation = ref(null)
const messages = ref([]), messagePage = ref(1), messageTotal = ref(0), chatInput = ref(''), chatBusy = ref(false)
const dialog = ref(false), operation = ref(null), operationRow = ref(null), form = ref({}), saving = ref(false), formError = ref('')
const eligibility = ref(null), checking = ref(false), sitters = ref([]), sitterLoading = ref(false)
let requestVersion = 0, detailVersion = 0, messageVersion = 0, sitterVersion = 0
const statusOptions = computed(() => ({
 order:['PENDING_PAYMENT','DISPATCHING','PENDING_ACCEPT','ACCEPTED','SERVICING','COMPLETED','CANCELLED','REFUNDED'],
 audit:['PENDING','ACTIVE','REST','DISABLED','REJECTED'], qualifications:['PENDING','APPROVED','REJECTED'], finance:['PENDING','SETTLED','REVERSED'], member:['ACTIVE','DISABLED'],
 sitter:['PENDING','ACTIVE','REST','DISABLED','REJECTED'], user:['ACTIVE','BLOCKED'], service:['WAITING_HUMAN','HUMAN','CLOSED'],
 emergency:['OPEN','ACCEPTED','TRANSPORTING','RESOLVED'], complaints:['OPEN','PROCESSING','RESOLVED'], staff:['ACTIVE','BLOCKED'],
}[tabKey.value] || []))
async function load() {
 const version = ++requestVersion
 loading.value = true; error.value = ''; selectedRows.value = []; rows.value = []
 try {
  let data
  if (tabKey.value === 'dict') {
   const all = await adminApi.dictionary()
   const filtered = all.filter(row => Object.values(row).some(v => String(v).toLowerCase().includes(query.value.toLowerCase())))
   data = { list:filtered.slice((page.value-1)*size.value,page.value*size.value), total:filtered.length }
  } else data = await adminApi.list(tabKey.value, { page:page.value, size:size.value, keyword:query.value.trim(), status:status.value })
  if (version !== requestVersion) return
  rows.value = data.list; total.value = data.total
  if (!rows.value.length && total.value && page.value > 1) { page.value = Math.ceil(total.value / size.value); return load() }
 } catch (e) { if (version === requestVersion) { error.value = e.message; total.value = 0 } }
 finally { if (version === requestVersion) loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { query.value = ''; status.value = route.name === 'dispatch' ? 'DISPATCHING' : ''; search() }
function switchTab(key) { tabKey.value = key; reset() }
watch(() => route.name, () => {
 ++detailVersion; ++messageVersion; drawer.value = false; dialog.value = false; conversation.value = null
 tabKey.value = meta.value.tabs[0].key; reset()
}, { immediate:true })
onBeforeUnmount(() => { ++requestVersion; ++detailVersion; ++messageVersion; ++sitterVersion })
async function fetchAll(module, params = {}) {
 const list = []; let p = 1, data
 do { data = await adminApi.list(module,{ ...params, page:p++, size:100 }); list.push(...data.list) } while (list.length < data.total && data.list.length)
 return list
}
async function openDetail(row) {
 const version = ++detailVersion
 drawer.value = true; detailBusy.value = true; detail.value = null; conversation.value = null; messages.value = []; chatInput.value = ''; ++messageVersion
 const module = tabKey.value
 try {
  const data = module === 'order' ? await request('/orders/' + row.id) : (module === 'sitter' || module === 'audit' || module === 'qualifications') ? await request('/admin/sitters/' + (module === 'qualifications' ? row.sitter_id : row.id)) : module === 'user' ? await request('/admin/owners/' + row.id) : row
  if (module === 'order') {
   data.media = await request('/orders/' + row.id + '/media')
   if (data.checkout_at) data.report = await request('/orders/' + row.id + '/report')
  }
  if (version !== detailVersion) return
  detail.value = data
  if (module === 'service') { conversation.value = row; messagePage.value = 1; await loadMessages() }
 } catch (e) { ElMessage.error(e.message) }
 finally { if (version === detailVersion) detailBusy.value = false }
}
async function openOperation(action, row = {}) {
 formError.value = ''; eligibility.value = null; operation.value = action; operationRow.value = row
 let source = row
 try {
  if (action.schema === 'Profile') {
   source = await request('/admin/sitters/' + row.id)
   source = { ...source, personal:{ ...source, emergencyContact:source.emergency_contact, avatarUrl:source.avatar_url, servicePreferences:source.service_preferences, radiusKm:source.radius_km } }
  }
  if (action.schema === 'Permissions') {
   const permissions = await fetchAll('roles')
   source = { permissions: permissions.filter(p => p.role === row.role).map(p => p.permission) }
  }
  form.value = { ...prepare(schemaFor(action.schema),source), ...action.defaults }
  if (action.schema === 'EmergencyUpdate' && source.status === 'OPEN') form.value.status = 'ACCEPTED'
  if (action.schema === 'ComplaintUpdate' && source.status === 'OPEN') form.value.status = 'PROCESSING'
  if (action.schema === 'Grade') form.value.effectiveAt = ''
  if (action.schema === 'Audit') { form.value.note = ''; form.value.expiresAt = '' }
  if (action.schema === 'AdmissionAudit') { form.value.decision = ''; form.value.note = '' }
  if (action.schema === 'Assign') { form.value.sitterId = undefined; form.value.reason = ''; await searchSitters('') }
  dialog.value = true
 } catch (e) { ElMessage.error(e.message) }
}
async function openAdmission(row, decision) {
 await openOperation(edits.audit,row)
 if (dialog.value) form.value = { ...form.value, decision }
}
async function searchSitters(keyword) {
 const version = ++sitterVersion; sitterLoading.value = true
 try { const data = await adminApi.list('sitter',{ keyword, size:100, status:'ACTIVE' }); if (version === sitterVersion) sitters.value = data.list }
 catch (e) { ElMessage.error(e.message) }
 finally { if (version === sitterVersion) sitterLoading.value = false }
}
async function checkEligibility() {
 if (!form.value.sitterId) { formError.value = '请选择喂养师'; return }
 checking.value = true; formError.value = ''; eligibility.value = null
 const sitterId = form.value.sitterId
 try {
  const result = await request('/admin/orders/' + operationRow.value.id + '/eligibility/' + sitterId)
  if (form.value.sitterId === sitterId) eligibility.value = result
 } catch (e) { formError.value = e.message }
 finally { checking.value = false }
}
async function llmDispatch(row) {
 try {
  await ElMessageBox.confirm('将由大模型结合订单特殊备注和喂养师服务经验选择候选人，后端会再次校验资格。继续？','智能派单',{ type:'warning' })
  loading.value = true
  const result = await request('/admin/orders/' + row.id + '/llm-dispatch',{ method:'POST' })
  ElMessage.success('已派给喂养师 #' + result.sitterId + (result.reason ? '：' + result.reason : ''))
  await load()
 } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message || '智能派单失败') }
 finally { loading.value = false }
}
watch(() => form.value.sitterId, () => { eligibility.value = null })
const formSchema = computed(() => {
 const schema = schemaFor(operation.value?.schema)
 if (operation.value?.schema === 'AdmissionAudit') return { ...schema, required:['decision'] }
 if (!schema || operation.value.schema !== 'Assign') return schema
 return { ...schema, properties:{ reason:schema.properties.reason }, required:['reason'] }
})
async function save() {
 if (saving.value) return
 formError.value = ''
 try {
  const schema = schemaFor(operation.value.schema)
  validate(schema,form.value)
  if (operation.value.schema === 'AdmissionAudit' && form.value.decision === 'REJECT' && !form.value.note?.trim()) throw new Error('驳回申请时请填写需要补充或修改的内容')
  const body = payloadFor(schema,form.value)
  const admissionDecision = operation.value.schema === 'AdmissionAudit' ? form.value.decision : ''
  const admissionTitle = admissionDecision === 'REJECT' ? '驳回入驻申请' : '通过入驻申请'
  const confirmation = operation.value.schema === 'AdmissionAudit'
   ? admissionDecision === 'REJECT' ? '驳回后，申请人可根据审核意见修改资料并重新提交。确认驳回？' : '确认申请资料和分类资质已审核通过？'
   : '确认提交“' + operation.value.label + '”？'
  await ElMessageBox.confirm(confirmation, operation.value.schema === 'AdmissionAudit' ? admissionTitle : '操作确认', { type:'warning' })
  saving.value = true
  const path = operation.value.path.replace(':id',operationRow.value.id).replace(':role',encodeURIComponent(operationRow.value.role))
  await request(path,{ method:operation.value.method, body })
  ElMessage.success(operation.value.schema === 'AdmissionAudit'
   ? admissionDecision === 'REJECT' ? '申请已驳回，喂养师可修改后重新提交' : '入驻申请已通过'
   : operation.value.schema === 'Grade' ? '规则已提交，请查看生效时间及待生效规则' : '操作成功')
  dialog.value = false
  if (operation.value.schema === 'Permissions') await restoreSession()
  await load()
 } catch (e) { if (e !== 'cancel' && e !== 'close') formError.value = e.message || '操作失败' }
 finally { saving.value = false }
}
async function settle() {
 const ids = selectedRows.value.filter(row => row.status === 'PENDING').map(row => row.id)
 if (!ids.length) return ElMessage.warning('请选择待结算记录')
 try {
  await ElMessageBox.confirm('确认结算选中的 ' + ids.length + ' 条记录？','结算确认',{ type:'warning' })
  loading.value = true
  await request('/admin/settlements/batch-settle',{ method:'POST', body:{ ids } })
  ElMessage.success('结算成功'); await load()
 } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message) }
 finally { loading.value = false }
}
async function loadMessages() {
 const id = conversation.value?.id
 if (!id) return
 const version = ++messageVersion
 try {
  const data = await request('/messages',{ query:{ conversationId:id, page:messagePage.value, size:20 } })
  if (version !== messageVersion || conversation.value?.id !== id) return
  messages.value = [...data.list].reverse(); messageTotal.value = data.total
 } catch (e) { ElMessage.error(e.message) }
}
async function conversationAction(action) {
 if (chatBusy.value) return
 const id = conversation.value.id
 chatBusy.value = true
 try {
  await request('/admin/conversations/' + id + '/' + action,{ method:'POST' })
  if (conversation.value?.id === id) conversation.value = { ...conversation.value, status:action === 'takeover' ? 'HUMAN' : 'CLOSED' }
  ElMessage.success('操作成功'); await load()
 } catch (e) { ElMessage.error(e.message) }
 finally { chatBusy.value = false }
}
async function sendChat() {
 if (!chatInput.value.trim() || chatBusy.value) return
 const id = conversation.value.id, content = chatInput.value.trim()
 chatBusy.value = true
 try {
  await request('/messages',{ method:'POST', body:{ conversationId:id, content, mediaKeys:[] } })
  if (conversation.value?.id === id) { chatInput.value = ''; messagePage.value = 1; await loadMessages() }
  ElMessage.success('消息已发送')
 } catch (e) { ElMessage.error(e.message) }
 finally { chatBusy.value = false }
}
function exportPage() {
 const columns = tab.value.columns
 const cell = value => '"' + format(value).replace(/^[=+@-]/, "'$&").replaceAll('"','""') + '"'
 const csv = '\uFEFF' + [columns.map(fieldLabel), ...rows.value.map(r => columns.map(c => r[c]))].map(r => r.map(cell).join(',')).join('\r\n')
 const url = URL.createObjectURL(new Blob([csv],{ type:'text/csv;charset=utf-8' }))
 const a = document.createElement('a'); a.href = url; a.download = tab.value.title + '-第' + page.value + '页.csv'; a.click(); setTimeout(() => URL.revokeObjectURL(url),1000)
}
const assignable = row => ['PENDING_PAYMENT','DISPATCHING','PENDING_ACCEPT','ACCEPTED'].includes(row.status)
</script>

<template>
 <div class="page module-page">
  <section class="page-head compact"><div><div class="eyebrow">WARM PAW OPERATIONS</div><h1>{{ meta.title }}</h1><p>管理与查看平台业务数据</p></div><div class="head-actions"><el-button :icon="Download" :disabled="!rows.length || loading" @click="exportPage">导出当前页</el-button><el-button v-if="creation && can(creation.permission)" type="primary" :icon="Plus" @click="openOperation(creation)">{{ creation.label }}</el-button></div></section>
  <el-alert v-if="route.name === 'dispatch'" title="系统会定时调用大模型，结合订单特殊备注与喂养师服务经验自动派单；也可对待派订单手动重试。硬性资质仍由后端校验。" type="info" :closable="false" show-icon/>
  <el-alert v-if="tabKey === 'msg'" title="通知目前仅落地站内消息，微信、短信及服务群渠道尚未接入。" type="info" :closable="false"/>
  <el-alert v-if="route.name === 'audit'" title="入驻申请可通过或驳回。驳回时请写明需修改的内容，喂养师修改后可重新提交；分类资质由独立流程审核，通过入驻前须完成所申请类目的资质审核。" type="info" :closable="false"/>
  <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon/>
  <section class="panel data-panel" v-loading="loading">
   <div class="module-tabs"><button v-for="item in meta.tabs" :key="item.key" :class="{active:tabKey===item.key}" @click="switchTab(item.key)">{{ item.title }}</button></div>
   <div class="table-tools"><el-input v-model="query" :prefix-icon="Search" clearable maxlength="100" placeholder="输入关键词，回车搜索" @keyup.enter="search" @clear="search"/><el-select v-if="statusOptions.length" v-model="status" clearable placeholder="全部状态" @change="search"><el-option v-for="s in statusOptions" :key="s" :value="s" :label="states[s] || s"/></el-select><el-button type="primary" @click="search">搜索</el-button><el-button :icon="Refresh" @click="reset">刷新 / 重置</el-button><el-button v-if="tabKey === 'finance' && can('finance:settle')" :disabled="!selectedRows.length" @click="settle">结算选中记录</el-button><small>共 {{ total }} 条</small></div>
   <el-table :data="rows" stripe @selection-change="selectedRows=$event" @row-dblclick="openDetail" empty-text="暂无数据">
    <el-table-column v-if="tabKey === 'finance'" type="selection" width="44" :selectable="row => row.status === 'PENDING'"/>
    <el-table-column v-for="col in tab.columns" :key="col" :prop="col" :label="fieldLabel(col)" :min-width="['content','description','service_start','created_at'].includes(col)?180:115" show-overflow-tooltip><template #default="scope"><el-tag v-if="col === 'status'" effect="light">{{ format(scope.row[col]) }}</el-tag><span v-else>{{ format(scope.row[col]) }}</span></template></el-table-column>
    <el-table-column label="操作" fixed="right" min-width="210"><template #default="{row}">
     <el-button link type="primary" @click="openDetail(row)">{{ tabKey === 'service' ? '会话' : '详情' }}</el-button>
     <el-button v-if="edits[tabKey] && tabKey !== 'audit' && can(edits[tabKey].permission) && !(tabKey === 'roles' && row.role === 'ADMIN')" link type="primary" @click="openOperation(edits[tabKey],row)">{{ edits[tabKey].label }}</el-button>
     <template v-if="tabKey === 'audit' && row.status === 'PENDING' && can('audit:update')"><el-button link type="success" @click="openAdmission(row,'APPROVE')">通过</el-button><el-button link type="danger" @click="openAdmission(row,'REJECT')">驳回</el-button></template>
     <template v-if="tabKey === 'order'"><el-button v-if="assignable(row) && can('dispatch:update')" link type="primary" @click="openOperation(assignAction,row)">派单 / 改派</el-button><el-button v-if="tabKey === 'order' && ['PENDING_PAYMENT','DISPATCHING'].includes(row.status) && can('dispatch:update')" link type="success" @click="llmDispatch(row)">AI 派单</el-button><el-button v-if="can('order:update') && row.status === 'PENDING_PAYMENT'" link type="danger" @click="openOperation(cancelAction,row)">取消</el-button><el-button v-if="demoPaymentsEnabled && can('finance:refund') && row.paid_at && ['DISPATCHING','PENDING_ACCEPT','ACCEPTED','COMPLETED'].includes(row.status)" link type="danger" @click="openOperation(refundAction,row)">演示退款</el-button></template>
     <el-button v-if="tabKey === 'coupon' && row.issue_type === 'push' && can('coupon:update')" link type="primary" @click="openOperation(pushAction,row)">直发</el-button>
    </template></el-table-column>
   </el-table>
   <div class="pagination"><span>第 {{ page }} 页</span><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next" background @current-change="load" @size-change="search"/></div>
  </section>
  <el-drawer v-model="drawer" :title="tab.title + '详情'" size="min(760px, 95vw)"><div v-loading="detailBusy">
   <template v-if="conversation"><div class="chat-head"><b>会话 #{{ conversation.id }} · {{ format(conversation.status) }}</b><el-button v-if="can('service:update') && conversation.status !== 'CLOSED'" :loading="chatBusy" @click="conversationAction('takeover')">人工接管</el-button><el-button v-if="can('service:update') && conversation.status !== 'CLOSED'" :loading="chatBusy" @click="conversationAction('close')">关闭会话</el-button><el-button @click="loadMessages">刷新消息</el-button></div><div class="live-chat"><el-empty v-if="!messages.length" description="暂无消息"/><article v-for="msg in messages" :key="msg.id"><small>{{ msg.sender_id }} · {{ msg.created_at }}</small><p>{{ msg.content }}</p><small v-if="msg.media_keys?.length">附件：{{ format(msg.media_keys) }}</small></article></div><el-pagination v-model:current-page="messagePage" :total="messageTotal" :page-size="20" layout="prev, pager, next" @current-change="loadMessages"/><p class="field-help">第 1 页显示最新消息</p><el-input v-model="chatInput" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="输入回复内容" :disabled="conversation.status !== 'HUMAN' || !can('service:update')"/><el-button type="primary" :loading="chatBusy" :disabled="!chatInput.trim() || conversation.status !== 'HUMAN' || !can('service:update')" @click="sendChat">发送</el-button></template>
   <RecordDetail v-if="detail" :value="detail"/>
  </div></el-drawer>
  <el-dialog v-model="dialog" :title="operation?.label" width="min(720px, 95vw)" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving">
   <el-alert v-if="formError" :title="formError" type="error" :closable="false"/>
   <el-alert v-if="operation?.schema === 'Permissions'" title="此操作替换该角色的完整权限集合，影响该角色全部账号。" type="warning" :closable="false"/>
   <el-alert v-if="operation === refundAction" title="仅在后端开启演示支付模式时可退款；当前尚未接入真实支付退款。" type="warning" :closable="false"/>
   <el-form v-if="operation" label-position="top" :disabled="saving">
    <template v-if="operation.schema === 'Assign'"><el-form-item label="喂养师" required><el-select v-model="form.sitterId" filterable remote :remote-method="searchSitters" :loading="sitterLoading" placeholder="搜索姓名或 ID"><el-option v-for="s in sitters" :key="s.id" :value="s.id" :label="s.name + ' · #' + s.id + ' · ' + s.district"/></el-select></el-form-item><el-button :loading="checking" @click="checkEligibility">校验接单条件</el-button><el-alert v-if="eligibility" :type="eligibility.eligible ? 'success' : 'warning'" :title="eligibility.eligible ? '当前符合接单条件，提交时将再次校验' : eligibility.reasons.join('；')" :closable="false"/></template>
    <SchemaFields v-model="form" :schema="formSchema"/>
   </el-form>
   <template #footer><el-button :disabled="saving" @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">确认提交</el-button></template>
  </el-dialog>
 </div>
</template>
