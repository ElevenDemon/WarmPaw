<script setup lang="ts">
import { ref } from 'vue'
import PageShell from '../../components/PageShell.vue'
import { usePage } from '../../composables/usePage'
import { request } from '../../utils/request'
import { useUserStore } from '../../store/modules/user'
import { go, status, categories, confirm } from '../../utils/format'

const user = useUserStore()
const profile = ref<any>(null)
const { loading, error, load, action } = usePage(async () => {
  profile.value = await request('/sitter/profile')
})

async function logout() {
  if (await confirm('确认退出当前账号？')) await user.logout()
}

async function switchToOwner() {
  await user.switchRole('OWNER')
  uni.reLaunch({ url: '/pages/index/index' })
}
</script>

<template>
  <PageShell title="我的主页" active="profile" :loading="loading" :error="error" @retry="load">
    <template v-if="profile">
      <view class="row-start" style="margin:16px 0 24px">
        <view class="profile-mark">☺</view>
        <view>
          <view class="title">{{ profile.name }}</view>
          <text class="muted">{{ profile.code }} · {{ profile.grade_code }}</text>
        </view>
      </view>
      <view class="row" style="margin-bottom:20px">
        <text class="pill green">{{ status(profile.status) }}</text>
        <text class="link" @click="go('sitter/profile-edit')">编辑个人档案 ›</text>
      </view>
      <view class="grid">
        <view class="tile metric"><view class="amount">{{ profile.completed_orders }}</view><text class="muted">累计完成服务</text></view>
        <view class="tile metric"><view class="amount">{{ profile.rating }}</view><text class="muted">综合评分</text></view>
      </view>
      <view class="section">我的认证</view>
      <view class="card">
        <view v-for="q in (profile.qualifications || [])" :key="q.id" class="menu-item">
          <text>{{ categories[q.category] }}</text><text class="pill">{{ status(q.status) }}</text>
        </view>
        <text v-if="!profile.qualifications?.length" class="muted">尚未提交资质材料</text>
      </view>
      <view class="card">
        <view class="menu-item" @click="go('sitter/profile-edit')">我的档案 <text>›</text></view>
        <view class="menu-item" @click="go('sitter/schedule')">档期安排 <text>›</text></view>
        <view class="menu-item" @click="go('sitter/income')">收益明细 <text>›</text></view>
        <view class="menu-item" @click="go('owner/service')">智能客服 <text>›</text></view>
        <view class="menu-item" @click="go('sitter/apply')">提交分类资质 <text>›</text></view>
      </view>
      <button class="secondary full-width" @click="action(switchToOwner, '')">切换到宠主端</button>
      <button class="outline full-width" @click="action(logout, '')">退出登录</button>
    </template>
  </PageShell>
</template>

<style scoped>
.profile-mark{display:flex;align-items:center;justify-content:center;width:52px;height:52px;margin-right:12px;border-radius:14px;background:#e5f1e9;color:#3d7a5c;font-size:24px;flex-shrink:0}
</style>
