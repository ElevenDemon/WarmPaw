<script setup lang="ts">
import { ref, reactive } from 'vue'
import PageShell from '../../components/PageShell.vue'
import { usePage } from '../../composables/usePage'
import { request } from '../../utils/request'
import { toast } from '../../utils/format'

type AmapTip = { id?: string; name: string; address?: string; district?: string[] | string; location?: string; adcode?: string }
const rows = ref<any[]>([])
const editing = ref(false)
const id = ref(0)
const keyword = ref('')
const tips = ref<AmapTip[]>([])
const searching = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | undefined
const form = reactive({ contactName: '', phone: '', district: '', address: '', longitude: '', latitude: '' })
const { loading, error, load, busy, action } = usePage(async () => { rows.value = await request('/owner/addresses') })

function edit(a?: any) {
  id.value = a?.id || 0
  Object.assign(form, a ? {
    contactName: a.contact_name, phone: a.phone, district: a.district, address: a.address,
    longitude: String(a.longitude ?? ''), latitude: String(a.latitude ?? '')
  } : { contactName: '', phone: '', district: '', address: '', longitude: '', latitude: '' })
  keyword.value = ''
  tips.value = []
  editing.value = true
}

function searchTips() {
  if (searchTimer) clearTimeout(searchTimer)
  const text = keyword.value.trim()
  if (text.length < 2) { tips.value = []; return }
  searchTimer = setTimeout(async () => {
    searching.value = true
    try {
      const result: any = await request('/owner/addresses/amap/tips?keywords=' + encodeURIComponent(text))
      tips.value = result.tips || []
    } catch (e: any) {
      tips.value = []
      toast(e?.message || '地点搜索失败')
    } finally { searching.value = false }
  }, 350)
}

function districtName(value: any) {
  if (Array.isArray(value)) return value.filter(Boolean).join('')
  return value || ''
}

async function fillByLocation(location: string, placeName = '') {
  const result: any = await request('/owner/addresses/amap/regeo?location=' + encodeURIComponent(location))
  const geo = result.regeocode
  const component = geo?.addressComponent || {}
  const street = component.streetNumber || {}
  const neighborhood = component.neighborhood
  const detail = [component.township, street.street, street.number, neighborhood?.name, component.building?.name]
    .filter((part: any) => typeof part === 'string' && part.trim()).join('')
  const formatted = geo?.formatted_address || ''
  form.district = component.district || component.city || component.province || ''
  form.address = detail || formatted || placeName
  form.longitude = location.split(',')[0] || ''
  form.latitude = location.split(',')[1] || ''
  keyword.value = placeName || form.address
  tips.value = []
}

async function selectTip(item: AmapTip) {
  if (!item.location) {
    const result: any = await request('/owner/addresses/amap/geocode?address=' + encodeURIComponent([item.district, item.address, item.name].filter(Boolean).join('')))
    const point = result.geocodes?.[0]?.location
    if (!point) throw new Error('高德未能解析该地点，请换个关键词搜索')
    await fillByLocation(point, item.name)
    return
  }
  await fillByLocation(item.location, item.name)
}

function locate() {
  uni.getLocation({
    type: 'gcj02',
    success: r => action(() => fillByLocation(`${r.longitude},${r.latitude}`), '已回填当前位置'),
    fail: () => toast('定位未授权或不可用，请搜索地点并选择高德结果')
  })
}

async function save() {
  if (!form.longitude || !form.latitude) { toast('请先搜索并选择高德地点'); return }
  await request('/owner/addresses' + (id.value ? '/' + id.value : ''), id.value ? 'PUT' : 'POST', {
    ...form, longitude: Number(form.longitude), latitude: Number(form.latitude)
  })
  editing.value = false
  await load()
}
</script>

<template>
  <PageShell title="服务地址" back :loading="loading" :error="error" @retry="load">
    <template v-if="!editing">
      <view v-for="a in rows" :key="a.id" class="card" @click="edit(a)">
        <view style="font-weight:700">{{ a.district }} · {{ a.address }}</view>
        <text class="muted">{{ a.contact_name }} {{ a.phone }}</text>
        <view class="link">编辑地址 ›</view>
      </view>
      <button class="primary" @click="edit()">＋ 新增服务地址</button>
    </template>
    <view v-else class="card">
      <text class="label">地点搜索</text>
      <view class="field"><input v-model="keyword" placeholder="输入小区、街道或建筑名称" @input="searchTips" /></view>
      <view v-if="searching" class="muted" style="padding:8px 0">正在搜索…</view>
      <view v-for="(item, index) in tips" :key="item.id || index" class="suggestion" @click="action(() => selectTip(item), '地址信息已回填')">
        <view style="font-weight:600">{{ item.name }}</view>
        <text class="muted">{{ [districtName(item.district), item.address].filter(Boolean).join(' · ') }}</text>
      </view>
      <view class="inline-actions" style="margin:10px 0 18px">
        <button class="secondary" @click="locate">使用当前位置</button>
      </view>

      <text class="label">联系人</text>
      <view class="field"><input v-model="form.contactName" placeholder="联系人姓名" /></view>
      <text class="label">手机号码</text>
      <view class="field"><input v-model="form.phone" type="number" maxlength="11" placeholder="11 位手机号" /></view>
      <text class="label">所在区</text>
      <view class="field"><input v-model="form.district" placeholder="选择地点后自动回填" /></view>
      <text class="label">详细地址</text>
      <view class="field"><input v-model="form.address" placeholder="选择地点后自动回填，可补充楼栋门牌" /></view>
      <view class="grid" style="margin-top:14px">
        <view><text class="label">经度</text><view class="field"><input v-model="form.longitude" type="digit" placeholder="选择地点后回填" /></view></view>
        <view><text class="label">纬度</text><view class="field"><input v-model="form.latitude" type="digit" placeholder="选择地点后回填" /></view></view>
      </view>
      <view class="muted" style="margin-bottom:14px">坐标由高德地点信息回填，用于服务半径匹配；保存前请补全楼栋和门牌号。</view>
      <button class="primary" :loading="busy" :disabled="busy" @click="action(save, '地址已保存')">保存地址</button>
      <button class="outline full-width" style="margin-top:12px" @click="editing = false">返回地址列表</button>
    </view>
  </PageShell>
</template>

<style scoped>
.suggestion { padding: 12px 0; border-bottom: 1px solid var(--line); }
.suggestion:last-child { border-bottom: 0; }
</style>
