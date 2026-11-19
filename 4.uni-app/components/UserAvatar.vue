<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request, API_BASE } from '../utils/request'
import { token } from '../utils/auth'
const src=ref('')
onShow(async()=>{ try { const p=await request('/user/profile'); if(p.has_avatar) uni.downloadFile({url:API_BASE+'/user/profile/avatar',header:{Authorization:'Bearer '+token()},success:r=>{if(r.statusCode===200)src.value=r.tempFilePath}}); else src.value='' } catch { src.value='' } })
</script>
<template><view class="avatar"><image v-if="src" :src="src" mode="aspectFill" style="width:100%;height:100%;border-radius:inherit"/><text v-else>☺</text></view></template>
