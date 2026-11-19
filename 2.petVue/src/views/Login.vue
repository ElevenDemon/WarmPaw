<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../stores/auth'
const form = reactive({ username:'', password:'' })
const busy = ref(false), error = ref('')
const router = useRouter(), route = useRoute()
async function submit() {
 if (!form.username.trim() || !form.password) { error.value = '请输入账号和密码'; return }
 busy.value = true; error.value = ''
 try { await login({ ...form, username:form.username.trim() }); const next = String(route.query.redirect || '/admin/dash'); await router.replace(next.startsWith('/admin/') ? next : '/admin/dash') }
 catch (e) { error.value = e.message }
 finally { busy.value = false }
}
</script>
<template><main class="login-page"><section class="panel login-card"><div class="eyebrow">WARM PAW</div><h1>暖爪运营后台</h1><p>使用管理员、客服或财务账号登录</p><el-alert v-if="error" :title="error" type="error" :closable="false"/><el-form @submit.prevent="submit" label-position="top"><el-form-item label="账号"><el-input v-model="form.username" autocomplete="username" maxlength="64"/></el-form-item><el-form-item label="密码"><el-input v-model="form.password" type="password" show-password autocomplete="current-password"/></el-form-item><el-button native-type="submit" type="primary" :loading="busy">登录</el-button></el-form></section></main></template>
