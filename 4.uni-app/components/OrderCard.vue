<script setup lang="ts">
import { money,time,status,go } from '../utils/format'
import type { RecordData } from '../types/api'
defineProps<{order:RecordData;sitter?:boolean}>()
</script>
<template><view class="card" @click="go((sitter?'sitter/execute':'detail/detail')+'?id='+order.id)"><view class="row"><text class="muted">{{time(order.service_start)}}</text><text class="pill" :class="{green:order.status==='COMPLETED'}">{{status(order.status)}}</text></view><view class="row-start" style="margin-top:16px"><view class="avatar">🐾</view><view><view style="font-weight:700">{{(order.pet_snapshot||[]).map((p:any)=>p.name).join('、') || '上门喂养服务'}}</view><view class="muted">{{order.address_snapshot?.district}} · {{order.address_snapshot?.address}}</view></view></view><view class="divider"/><view class="row"><text class="muted">{{sitter?'查看任务与服务要求':'安心照顾，每一步都看得见'}}</text><text style="font-weight:700">{{money(order.payable_amount)}} ›</text></view></view></template>
