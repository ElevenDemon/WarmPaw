import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '../layout/AdminLayout.vue'
import Dashboard from '../views/Dashboard.vue'
import ModulePage from '../views/ModulePage.vue'
import Login from '../views/Login.vue'
import { moduleMap } from '../config/modules'
import { auth, can, restoreSession } from '../stores/auth'
import { tokenStore } from '../api/client'
const router = createRouter({ history:createWebHashHistory(), routes:[
 { path:'/login', component:Login, meta:{ title:'登录' } },
 { path:'/', redirect:'/admin/dash' },
 { path:'/admin', component:AdminLayout, children:[
  { path:'', redirect:'/admin/dash' },
  { path:'dash', name:'dash', component:Dashboard, meta:{ title:'数据看板', permission:'dash:read' } },
  ...Object.entries(moduleMap).map(([key, m]) => ({ path:key, name:key, component:ModulePage, meta:{ title:m.title, permission:m.permission } })),
 ] },
 { path:'/:pathMatch(.*)*', redirect:'/admin/dash' },
] })
router.beforeEach(async to => {
 if (to.path === '/login') return true
 try { if (!auth.user && !(await restoreSession())) return { path:'/login', query:{ redirect:to.fullPath } } }
 catch (e) { ElMessage.error(e.message); return { path:'/login', query:{ redirect:to.fullPath } } }
 if (!can(to.meta.permission)) {
   ElMessage.warning('当前账号没有此模块权限')
   const first = can('dash:read') ? '/admin/dash' : Object.entries(moduleMap).find(([,m]) => can(m.permission))?.[0]
   if (!first) return '/login'
   return first.startsWith('/') ? first : '/admin/' + first
 }
})
router.afterEach(to => { document.title = (to.meta.title || '运营后台') + ' - 暖爪' })
window.addEventListener('auth-expired', () => {
 auth.user = null; tokenStore.clear()
 if (router.currentRoute.value.path !== '/login') router.replace({ path:'/login', query:{ redirect:router.currentRoute.value.fullPath } })
})
export default router
