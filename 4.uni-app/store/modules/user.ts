import { defineStore } from 'pinia'
import type { User } from '../../types/api'
import { request, ApiError } from '../../utils/request'
import { setToken, token } from '../../utils/auth'
export const useUserStore = defineStore('user', { state: () => ({ user: null as User | null }), getters: { sitter: s => s.user?.role === 'SITTER' }, actions: {
  async restore() { if(token()) { try { this.user = await request('/auth/me', 'GET', undefined, false) } catch (error) { this.user = null; if (error instanceof ApiError && error.status === 401) setToken('') } } else this.user = null },
  async login(username: string, password: string) { const r = await request('/auth/login', 'POST', {username,password}); if(!['OWNER','SITTER'].includes(r.user.role)) throw new Error('请使用宠主或喂养师账号登录'); setToken(r.token); this.user = r.user },
  async switchRole(role: 'OWNER' | 'SITTER') { await request('/auth/switch-role','POST',{role}); this.user = await request('/auth/me') },
  async logout() { try { await request('/auth/logout','POST') } finally { setToken(''); this.user = null; uni.reLaunch({url:'/pages/login/index'}) } }
} })
