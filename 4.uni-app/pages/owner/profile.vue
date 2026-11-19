<script setup lang="ts">
import { ref } from 'vue'
import PageShell from '../../components/PageShell.vue'
import { usePage } from '../../composables/usePage'
import { userApi } from '../../api/user'
import { useUserStore } from '../../store/modules/user'
import { go, confirm } from '../../utils/format'
import UserAvatar from '../../components/UserAvatar.vue'

const user = useUserStore()
const pets = ref<any[]>([])
const { loading, error, load, action } = usePage(async () => {
  pets.value = await userApi.pets()
})
const menus = [
  ['我的订单', 'owner/orders'], ['服务地址', 'owner/addresses'], ['我的优惠券', 'owner/coupon'],
  ['领券中心', 'owner/claim'], ['会员中心', 'owner/member'], ['智能客服', 'owner/service'],
  ['申请成为喂养师', 'sitter/apply']
]

async function logout() {
  if (await confirm('确认退出当前账号？')) await user.logout()
}

async function switchToSitter() {
  await user.switchRole('SITTER')
  uni.reLaunch({ url: '/pages/sitter/tasks' })
}
</script>

<template>
  <PageShell title="我的暖爪" active="profile" :loading="loading" :error="error" @retry="load">
    <view class="row-start" style="margin:12px 0 28px" @click="go('owner/profile-edit')">
      <UserAvatar />
      <view><view class="title">{{ user.user?.name }}</view><text class="muted">和毛孩子，一起被温柔以待。</text></view>
    </view>
    <view class="dark-card" @click="go('owner/member')">
      <view class="row"><text style="font-size:19px">暖爪会员</text><text>查看权益 ›</text></view>
      <text class="muted">让长期陪伴更有温度</text>
    </view>
    <view class="section">我的毛孩子 <text class="link" @click="go('owner/petadd')">＋ 添加宠物</text></view>
    <view class="grid">
      <view v-for="pet in pets" :key="pet.id" class="tile" @click="go('owner/petdetail?id='+pet.id)">
        <view class="tile-icon">{{ pet.category==='DOG'?'🐕':'🐈' }}</view>
        <view style="font-weight:700">{{ pet.name }}</view><text class="muted">{{ pet.breed||'待完善档案' }}</text>
      </view>
    </view>
    <view class="card" style="margin-top:20px">
      <view v-for="item in menus" :key="item[1]" class="menu-item" @click="go(item[1])"><text>{{ item[0] }}</text><text class="muted">›</text></view>
    </view>
    <button v-if="user.user?.canSwitchToSitter" class="secondary full-width" @click="action(switchToSitter, '')">切换到喂养师端</button>
    <button class="outline full-width" @click="action(logout, '')">退出登录</button>
  </PageShell>
</template>
