<script setup lang="ts">
import { ref } from 'vue'
import PageShell from '../../components/PageShell.vue'
import { usePage } from '../../composables/usePage'
import { request, API_BASE } from '../../utils/request'
import { token } from '../../utils/auth'
import { useUserStore } from '../../store/modules/user'
import { toast } from '../../utils/format'
const name=ref(''),phone=ref(''),avatar=ref(''),user=useUserStore()
const {loading,error,load,busy,action}=usePage(async()=>{const p=await request('/user/profile');name.value=p.name;phone.value=p.phone;if(p.has_avatar)uni.downloadFile({url:API_BASE+'/user/profile/avatar',header:{Authorization:'Bearer '+token()},success:r=>{if(r.statusCode===200)avatar.value=r.tempFilePath}})})
function chooseAvatar(){uni.chooseImage({count:1,sizeType:['compressed'],sourceType:['album','camera'],success:r=>{const path=r.tempFilePaths[0];if(!path)return;uni.showLoading({title:'上传中'});uni.uploadFile({url:API_BASE+'/user/profile/avatar',filePath:path,name:'file',header:{Authorization:'Bearer '+token()},success:res=>{try{const body=JSON.parse(res.data);if(res.statusCode===200&&body.code===0){avatar.value=path;toast('头像已更新')}else toast(body.message||'上传失败')}catch{toast('上传失败')}},fail:()=>toast('上传失败，请检查网络'),complete:()=>uni.hideLoading()})}})}
async function save(){if(!name.value.trim())return toast('请填写姓名');if(!/^1[3-9]\d{9}$/.test(phone.value))return toast('请输入正确的手机号');await action(async()=>{await request('/user/profile','PUT',{name:name.value.trim(),phone:phone.value});await user.restore();toast('资料已保存');uni.navigateBack()},'')}
</script>
<template><PageShell title="个人资料" back :loading="loading" :error="error" @retry="load"><view class="card"><view class="menu-item row" @click="chooseAvatar"><text>头像</text><view class="row-start"><view class="avatar"><image v-if="avatar" :src="avatar" mode="aspectFill" style="width:100%;height:100%;border-radius:inherit"/><text v-else>☺</text></view><text class="muted">更换 ›</text></view></view><text class="label" style="margin-top:18px">姓名</text><view class="field"><input v-model="name" maxlength="32" placeholder="请输入姓名"/></view><text class="label">手机号码</text><view class="field"><input v-model="phone" type="number" maxlength="11" placeholder="请输入手机号"/></view></view><button class="primary" :disabled="busy" :loading="busy" @click="save">保存</button></PageShell></template>
