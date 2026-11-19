import { reactive } from 'vue'
import { request, tokenStore } from '../api/client'
export const auth = reactive({ user: null })
export const can = permission => !!auth.user && (!permission || auth.user.permissions?.includes('*') || auth.user.permissions?.includes(permission))
export async function restoreSession() {
  if (!tokenStore.get()) return false
  const user = await request('/auth/me')
  if (!['ADMIN', 'SUPPORT', 'FINANCE'].includes(user.role)) {
    await logout()
    throw new Error('请使用运营后台账号登录')
  }
  auth.user = user
  return true
}
export async function login(credentials) {
  const data = await request('/auth/login', { method: 'POST', body: credentials })
  tokenStore.set(data.token)
  await restoreSession()
}
export async function logout() {
  try { if (tokenStore.get()) await request('/auth/logout', { method: 'POST' }) }
  finally { tokenStore.clear(); auth.user = null }
}
