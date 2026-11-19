<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as Icons from '@element-plus/icons-vue'
import { navGroups as allGroups } from '../config/navigation'
import { moduleMap, states } from '../config/modules'
import { auth, can, logout } from '../stores/auth'
import { request } from '../api/client'
import { ElMessage } from 'element-plus'
const currentUser = computed(() => ({ ...auth.user, avatar:auth.user?.name?.[0] || '暖', role:states[auth.user?.role] || auth.user?.role }))
const navGroups = computed(() => allGroups.map(group => ({ ...group, items:group.items.filter(item => can(item[0] === 'dash' ? 'dash:read' : moduleMap[item[0]].permission)) })).filter(group => group.items.length))
const noticesOpen = ref(false), notices = ref([]), noticesTotal = ref(0), noticePage = ref(1), noticeBusy = ref(false)
async function loadNotices() {
 noticeBusy.value = true
 try { const data = await request('/notifications',{query:{page:noticePage.value,size:20}}); notices.value = data.list; noticesTotal.value = data.total; noticesOpen.value = true }
 catch (e) { ElMessage.error(e.message) }
 finally { noticeBusy.value = false }
}
async function markRead(id) {
 try { await request(id ? '/notifications/' + id + '/read' : '/notifications/read-all',{method:'POST'}); await loadNotices() }
 catch (e) { ElMessage.error(e.message) }
}
async function signOut() {
 try { await logout() } catch (e) { ElMessage.warning(e.message) }
 finally { router.replace('/login') }
}

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const drawer = ref(false)
const active = computed(() => route.name)
const currentTitle = computed(() => route.meta.title || '运营后台')
const go = (key) => { router.push(`/admin/${key}`); drawer.value = false }
</script>

<template>
  <div class="admin-shell" :class="{ collapsed }">
    <header class="topbar">
      <div class="topbar-left">
        <el-button class="mobile-menu" text circle @click="drawer = true"><el-icon><component :is="Icons.Menu" /></el-icon></el-button>
        <button class="collapse-btn" @click="collapsed = !collapsed" aria-label="折叠侧栏">
          <el-icon><component :is="collapsed ? Icons.Expand : Icons.Fold" /></el-icon>
        </button>
        <div class="breadcrumb"><span>暖爪运营</span><el-icon><component :is="Icons.ArrowRight" /></el-icon><b>{{ currentTitle }}</b></div>
      </div>
      <div class="topbar-actions">
        
        <button class="icon-btn notification" title="消息通知" @click="loadNotices"><el-icon><component :is="Icons.Bell" /></el-icon></button>
        <div class="user-chip">
          <span class="user-avatar">{{ currentUser.avatar }}</span>
          <span class="user-copy"><b>{{ currentUser.name }}</b><small>{{ currentUser.role }}</small></span>
          <el-button text @click="signOut">退出</el-button>
        </div>
      </div>
    </header>

    <aside class="sidebar">
      <div class="logo-wrap">
        <div class="logo-mark"><span>●</span><i>●</i><b>●</b><em>●</em><strong>♥</strong></div>
        <div class="logo-copy"><b>暖爪</b><small>运营管理后台</small></div>
      </div>
      <nav>
        <div v-for="group in navGroups" :key="group.label" class="nav-group">
          <div class="nav-label">{{ group.label }}</div>
          <button v-for="item in group.items" :key="item[0]" :class="['nav-item', { active: active === item[0] }]" @click="go(item[0])">
            <el-icon><component :is="Icons[item[2]]" /></el-icon><span>{{ item[1] }}</span>
            
          </button>
        </div>
      </nav>
      <div class="sidebar-foot"><span class="env-dot"></span><div><b>运营管理</b><small>暖爪 · API</small></div></div>
    </aside>

    <main class="main"><router-view /></main>
    <el-drawer v-model="noticesOpen" title="站内通知" size="min(560px, 95vw)"><div v-loading="noticeBusy"><el-button @click="markRead()">全部标为已读</el-button><el-empty v-if="!notices.length" description="暂无通知"/><article v-for="notice in notices" :key="notice.id" class="notice-item"><h3>{{ notice.title }}</h3><p>{{ notice.content }}</p><small>{{ notice.created_at }}</small><el-button v-if="!notice.read_at" link type="primary" @click="markRead(notice.id)">标为已读</el-button><el-tag v-else type="info">已读</el-tag></article><el-pagination v-model:current-page="noticePage" :total="noticesTotal" :page-size="20" layout="prev, pager, next" @current-change="loadNotices"/></div></el-drawer>

    <el-drawer v-model="drawer" direction="ltr" size="280px" :with-header="false" class="mobile-drawer">
      <div class="mobile-logo">暖爪 · 运营后台</div>
      <div v-for="group in navGroups" :key="group.label" class="nav-group">
        <div class="nav-label">{{ group.label }}</div>
        <button v-for="item in group.items" :key="item[0]" :class="['nav-item', { active: active === item[0] }]" @click="go(item[0])">
          <el-icon><component :is="Icons[item[2]]" /></el-icon><span>{{ item[1] }}</span>
        </button>
      </div>
    </el-drawer>
  </div>
</template>
