<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '../store/modules/user'
defineProps<{title:string; active?:string; back?:boolean; loading?:boolean; error?:string; hideHeader?:boolean}>()
defineEmits(['retry'])
const user=useUserStore()
const tabs=computed(()=>user.sitter ? [{key:'home',icon:'⌂',name:'任务',url:'sitter/tasks'},{key:'schedule',icon:'▦',name:'档期',url:'sitter/schedule'},{key:'income',icon:'◈',name:'收益',url:'sitter/income'},{key:'service',icon:'✦',name:'智能客服',url:'owner/service'},{key:'profile',icon:'○',name:'我的',url:'sitter/profile'}] : [{key:'home',icon:'⌂',name:'首页',url:'index/index'},{key:'orders',icon:'▤',name:'订单',url:'owner/orders'},{key:'service',icon:'✦',name:'智能客服',url:'owner/service'},{key:'notice',icon:'♧',name:'消息',url:'owner/notice'},{key:'profile',icon:'○',name:'我的',url:'owner/profile'}])
function navigate(url:string){uni.reLaunch({url:'/pages/'+url})}
function previous(){ if(getCurrentPages().length>1) uni.navigateBack(); else navigate(user.sitter?'sitter/tasks':'index/index') }
</script>
<template><view class="shell"><view v-if="!hideHeader" class="header"><view class="row-start"><text v-if="back" class="back" @click="previous">‹</text><text class="header-title">{{title}}</text></view><text class="eyebrow">WARM PAW</text></view><view v-if="error" class="error" @click="$emit('retry')">{{error}} · 点击重试</view><view v-if="loading" class="loading">正在加载…</view><slot/><view v-if="active" class="bottom-nav"><view v-for="tab in tabs" :key="tab.key" :class="['nav-item',{active:active===tab.key}]" @click="navigate(tab.url)"><text class="nav-icon">{{tab.icon}}</text>{{tab.name}}</view></view></view></template>
