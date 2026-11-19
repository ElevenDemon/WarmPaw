<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import PageShell from './PageShell.vue'
import { usePage } from '../composables/usePage'
import { useUserStore } from '../store/modules/user'
import { request } from '../utils/request'
import { categories, status, confirm, toast } from '../utils/format'

type AmapTip = { id?: string; name: string; address?: string; district?: string[] | string; location?: string }
const props = defineProps<{ apply?: boolean }>()
const user = useUserStore()
const profile = ref<any>(null)
const selected = ref<string[]>(['CAT'])
const category = ref('CAT')
const materials = ref('')
const keyword = ref('')
const tips = ref<AmapTip[]>([])
const searching = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | undefined
let searchSequence = 0
const reapplying = computed(() => props.apply && user.sitter && profile.value?.status === 'REJECTED')
const showApplicationForm = computed(() => !props.apply || !user.sitter || reapplying.value)
const pageTitle = computed(() => reapplying.value ? '重新提交入驻申请' : props.apply ? '申请入驻 / 分类资质' : '我的档案 · 编辑')
const form = reactive<Record<string, any>>({
  name: '', phone: '', emergencyContact: '', avatarUrl: '', notes: '', servicePreferences: '',
  district: '', address: '', longitude: '', latitude: '', radiusKm: '5', transport: '步行'
})
const { loading, error, load, busy, action } = usePage(async () => {
  if (!user.sitter) return
  profile.value = await request('/sitter/profile')
  const p = profile.value
  Object.assign(form, {
    name: p.name, phone: p.phone, emergencyContact: p.emergency_contact || '', avatarUrl: p.avatar_url || '',
    notes: p.notes || '', servicePreferences: p.service_preferences || '', district: p.district, address: p.address,
    longitude: p.longitude, latitude: p.latitude, radiusKm: p.radius_km, transport: p.transport
  })
  if (props.apply && p.status === 'REJECTED') {
    selected.value = Array.isArray(p.categories) ? [...p.categories] : ['CAT']
  }
})
const fields = [
  ['name', '姓名', '真实姓名'], ['phone', '手机号', '用于服务联系'], ['emergencyContact', '紧急联系人', '姓名与联系电话'],
  ['district', '所在区', '选择地点后自动回填'], ['address', '常驻地址', '选择地点后自动回填，可补充楼栋门牌'],
  ['longitude', '经度', '选择地点后回填'], ['latitude', '纬度', '选择地点后回填'], ['radiusKm', '服务半径（km）', '0.5–50'],
  ['transport', '交通方式', '步行 / 电动车 / 汽车'], ['notes', '个人备注', '60 字以内'],
  ['servicePreferences', '服务经验与偏好', '填写可照护的特殊情况']
]

function searchTips() {
  if (searchTimer) clearTimeout(searchTimer)
  const text = keyword.value.trim(), sequence = ++searchSequence
  if (text.length < 2) { tips.value = []; return }
  searchTimer = setTimeout(async () => {
    searching.value = true
    try {
      const result: any = await request('/owner/addresses/amap/tips?keywords=' + encodeURIComponent(text))
      if (sequence === searchSequence) tips.value = result.tips || []
    } catch (e: any) {
      if (sequence === searchSequence) { tips.value = []; toast(e?.message || '地点搜索失败') }
    } finally { if (sequence === searchSequence) searching.value = false }
  }, 350)
}

async function fillByLocation(location: string, placeName = '') {
  const result: any = await request('/owner/addresses/amap/regeo?location=' + encodeURIComponent(location))
  const geo = result.regeocode, component = geo?.addressComponent || {}, street = component.streetNumber || {}
  const neighborhood = component.neighborhood
  const detail = [component.township, street.street, street.number, neighborhood?.name, component.building?.name]
    .filter((part: any) => typeof part === 'string' && part.trim()).join('')
  form.district = component.district || component.city || component.province || ''
  form.address = detail || geo?.formatted_address || placeName
  form.longitude = location.split(',')[0] || ''
  form.latitude = location.split(',')[1] || ''
  keyword.value = placeName || form.address
  tips.value = []
}

async function selectTip(item: AmapTip) {
  let location = item.location
  if (!location) {
    const result: any = await request('/owner/addresses/amap/geocode?address=' + encodeURIComponent([item.district, item.address, item.name].filter(Boolean).join('')))
    location = result.geocodes?.[0]?.location
    if (!location) throw new Error('高德未能解析该地点，请换个关键词搜索')
  }
  await fillByLocation(location, item.name)
}

function locate() {
  uni.getLocation({
    type: 'gcj02',
    success: r => action(() => fillByLocation(`${r.longitude},${r.latitude}`), '已回填当前位置'),
    fail: () => toast('定位未授权或不可用，请搜索地点并选择高德结果')
  })
}

async function save() {
  if (!form.longitude || !form.latitude) return toast('请先搜索并选择高德地点，或使用当前位置回填坐标')
  if (!form.district || !form.address) return toast('请补全所在区和常驻地址')
  const isReapply = !!reapplying.value
  const prompt = isReapply
    ? '请确认已根据驳回意见修改资料。重新提交后，平台将再次审核。'
    : props.apply && !user.sitter
      ? '提交入驻后此账号将转为喂养师账号，请确认使用专用账号。'
      : '确认保存个人档案？新的地址、服务经验与偏好将影响派单匹配。'
  if (!await confirm(prompt)) return
  const personal = { ...form, longitude: Number(form.longitude), latitude: Number(form.latitude), radiusKm: Number(form.radiusKm) }
  if (props.apply && (!user.sitter || isReapply)) {
    await request('/sitter/application', 'POST', { personal, categories: selected.value })
    if (!user.sitter) {
      await user.restore()
      uni.reLaunch({ url: '/pages/sitter/profile' })
    } else {
      await load()
    }
  } else {
    await request('/sitter/profile', 'PUT', personal)
    await load()
  }
}

function toggle(c: string) {
  selected.value = selected.value.includes(c) ? selected.value.filter(x => x !== c) : [...selected.value, c]
}

async function qualification() {
  if (!materials.value.trim()) return toast('请填写证明材料对象键')
  await request('/sitter/qualifications', 'POST', {
    category: category.value, materials: materials.value.split('\n').map(s => s.trim()).filter(Boolean)
  })
  materials.value = ''
  await load()
}
</script>

<template>
  <PageShell :title="pageTitle" back :loading="loading" :error="error" @retry="load">
    <view class="title">让热爱，成为专业。</view>
    <view class="subtitle" style="margin-bottom:20px">实名与培训审核通过后，才可正式接单。</view>

    <template v-if="showApplicationForm">
      <view class="notice">{{ reapplying ? '请根据上次审核意见补充资料，重新提交后将进入待审核。' : '基本信息与常驻地址可编辑。等级、抽成、账号状态、档期由平台维护。' }}</view>
      <view v-if="reapplying && profile.admission_review_note" class="notice warning">
        上次驳回意见：{{ profile.admission_review_note }}
      </view>
      <view class="card">
        <template v-for="f in fields" :key="f[0]">
          <text class="label">{{ f[1] }}</text>
          <template v-if="f[0] === 'district'">
            <view class="field"><input v-model="keyword" placeholder="搜索小区、街道或建筑名称" @input="searchTips" /></view>
            <view v-if="searching" class="muted" style="padding:8px 0">正在搜索…</view>
            <view v-for="(item, index) in tips" :key="item.id || index" class="address-tip" @click="action(() => selectTip(item), '地址信息已回填')">
              <view style="font-weight:600">{{ item.name }}</view>
              <text class="muted">{{ [Array.isArray(item.district) ? item.district.join('') : item.district, item.address].filter(Boolean).join(' · ') }}</text>
            </view>
            <button class="secondary" style="margin:8px 0 14px" @click="locate">使用当前位置</button>
          </template>
          <view v-if="f[0] === 'servicePreferences'" class="field field-area">
            <textarea v-model="form[f[0]]" placeholder="例如：可照护大型犬，熟悉怕生宠物；不接攻击性强且无口套管理的犬只" maxlength="1000" />
          </view>
          <view v-else class="field"><input v-model="form[f[0]]" :placeholder="f[2]" :maxlength="f[0] === 'notes' ? 60 : 300" /></view>
        </template>
        <text class="muted">坐标由高德地点信息自动回填；常驻地址请补全楼栋和门牌号。服务经验与偏好会用于匹配特殊订单，不替代平台资质审核。</text>
      </view>

      <template v-if="apply">
        <view class="section">申请照护类目</view>
        <view class="tabs"><view v-for="c in ['CAT','DOG','SMALL','OTHER']" :key="c" :class="['tab',{active:selected.includes(c)}]" @click="toggle(c)">{{ categories[c] }}</view></view>
      </template>
      <view v-if="profile && !reapplying" class="card">
        <view class="row"><text>认证等级</text><text>{{ profile.grade_code }}</text></view>
        <view class="row"><text>平台抽成</text><text>{{ profile.commission_rate }}%</text></view>
        <view class="row"><text>账号状态</text><text>{{ status(profile.status) }}</text></view>
        <text class="muted">保存个人资料不会覆盖平台设置的每日档期。</text>
      </view>
      <button class="primary" :loading="busy" :disabled="busy" @click="action(save, reapplying ? '已重新提交，等待平台审核' : apply ? '入驻申请已提交' : '档案已保存')">
        {{ reapplying ? '重新提交申请' : apply ? '提交入驻申请' : '保存个人档案' }}
      </button>
    </template>

    <template v-if="apply && user.sitter && !reapplying">
      <view class="card">
        <view class="section" style="margin-top:0">分类资质材料</view>
        <view class="tabs" style="flex-wrap:wrap"><view v-for="(name,c) in categories" :key="c" :class="['tab',{active:category===c}]" @click="category=c">{{ name }}</view></view>
        <view class="notice warning">当前材料对接使用已存入平台存储的对象键。请向运营取得上传渠道；不要填写本地文件路径或伪造认证结果。</view>
        <view class="field field-area"><textarea v-model="materials" placeholder="每行一个材料对象键" /></view>
        <button class="primary" :disabled="busy" @click="action(qualification, '材料已提交，等待审核')">提交审核</button>
      </view>
      <view v-for="q in profile?.qualifications" :key="q.id" class="card">
        <view class="row"><text>{{ categories[q.category] }}</text><text class="pill">{{ status(q.status) }}</text></view>
        <text class="muted">{{ q.review_note || '审核结果将由平台更新' }}</text>
      </view>
    </template>
  </PageShell>
</template>

<style scoped>
.address-tip { padding: 12px 0; border-bottom: 1px solid var(--line); }
.address-tip:last-child { border-bottom: 0; }
</style>
