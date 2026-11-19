<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../api/client'
import { auth, can } from '../stores/auth'
const router = useRouter(), data = ref(null), error = ref(''), loading = ref(false)
const stats = computed(() => data.value ? [
 { label:'累计订单', value:data.value.orders, tone:'orange', note:'平台累计订单数量' },
 { label:'累计 GMV', value:'¥ ' + Number(data.value.gmv).toLocaleString('zh-CN',{minimumFractionDigits:2}), tone:'green', note:'已支付且未退款订单金额' },
 { label:'待人工派单', value:data.value.manualDispatchPending, tone:'purple', note:'等待运营人员处理' },
 { label:'未解决投诉', value:data.value.openComplaints, tone:'blue', note:'待处理与处理中投诉' },
] : [])
async function load() {
 loading.value = true; error.value = ''
 try { data.value = await adminApi.dashboard() } catch (e) { error.value = e.message; data.value = null }
 finally { loading.value = false }
}
onMounted(load)
</script>
<template><div class="page dashboard-page" v-loading="loading"><section class="page-head"><div><div class="eyebrow">OPERATION OVERVIEW</div><h1>你好，{{ auth.user?.name }}</h1><p>平台业务概览 · 累计数据</p></div><el-button @click="load">刷新数据</el-button></section><el-alert v-if="error" :title="error" type="error" :closable="false"/><section class="stats-grid"><article v-for="stat in stats" :key="stat.label" class="stat-card" :class="stat.tone"><div class="stat-top">{{ stat.label }}</div><strong>{{ stat.value }}</strong><small>{{ stat.note }}</small></article></section><section v-if="data" class="panel integration-panel"><h2>运营待办</h2><p>按业务状态查看并处理待办事项。</p><div class="quick-actions"><el-button v-if="can('order:read')" @click="router.push('/admin/dispatch')">待派订单 {{ data.manualDispatchPending }}</el-button><el-button v-if="can('review:read')" @click="router.push('/admin/review')">评价与投诉 {{ data.openComplaints }}</el-button><el-button v-if="can('audit:read')" @click="router.push('/admin/audit')">资质审核</el-button><el-button v-if="can('finance:read')" @click="router.push('/admin/finance')">财务结算</el-button></div><el-alert v-if="!data.aiEnabled" title="AI 功能暂未开放，派单使用人工校验与分配。" type="info" :closable="false"/></section></div></template>
