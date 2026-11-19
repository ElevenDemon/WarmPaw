<script setup lang="ts">
import {computed,ref} from 'vue'
import PageShell from '../../components/PageShell.vue'
import {useCartStore} from '../../store/modules/cart'
import {usePage} from '../../composables/usePage'
import {request} from '../../utils/request'
import {go,money,toast} from '../../utils/format'

const cart=useCartStore(),catalog=ref<any>({services:[]}),created=ref<any>(null)
const {loading,error,load,busy,action}=usePage(async()=>catalog.value=await request('/public/catalog'))
const estimatedTotal=computed(()=>catalog.value.services.reduce((sum:number,s:any)=>sum+(s.code==='BASE'?Number(s.price):s.code==='EXTRA_PET'?Number(s.price)*Math.max(0,cart.petIds.length-1):cart.services.includes(s.id)?Number(s.price):0),0))
async function create(){
 if(!cart.accepted||!cart.petIds.length||!cart.addressId)return toast('请先完成预约信息和授权')
 const items=catalog.value.services.filter((s:any)=>s.code==='BASE'||(s.code==='EXTRA_PET'&&cart.petIds.length>1)||cart.services.includes(s.id)).map((s:any)=>({serviceId:s.id,quantity:s.code==='EXTRA_PET'?cart.petIds.length-1:1}))
 created.value=await request('/orders','POST',{requestKey:cart.key(),addressId:cart.addressId,petIds:cart.petIds,serviceStart:cart.date+'T'+cart.start+':00+08:00',serviceEnd:cart.date+'T'+cart.end+':00+08:00',items,agreementVersion:'v1',agreementAccepted:cart.accepted,authorizationScope:cart.scope,notes:cart.notes})
 cart.resetKey()
}
</script>
<template>
 <PageShell title="确认订单" back :loading="loading" :error="error" @retry="load">
  <view class="title">安心照顾，即将开始。</view>
  <view class="card" style="margin-top:24px">
   <view class="row"><text>服务时间</text><text>{{cart.date}}</text></view>
   <view class="row muted"><text>{{cart.petIds.length}} 只宠物</text><text>{{cart.start}} – {{cart.end}}</text></view>
   <view class="divider"/><text>{{cart.notes||'暂无额外照护要求'}}</text>
  </view>
  <template v-if="!created">
   <view class="card"><view class="row"><text>服务金额参考</text><text class="amount">{{money(estimatedTotal)}}</text></view><text class="muted">当前暂不收取费用。提交后将根据订单要求自动匹配喂养师。</text></view>
   <button class="primary" style="margin-top:20px" :loading="busy" :disabled="busy" @click="action(create,'订单已提交，正在智能匹配')">提交订单并匹配喂养师</button>
  </template>
  <template v-else>
   <view class="notice">订单已提交，系统正在结合宠物情况、服务备注和喂养师经验进行匹配。</view>
   <view class="card"><view class="row"><text>参考服务金额</text><text class="amount">{{money(created.total_amount)}}</text></view><text class="muted">本阶段不收取费用</text></view>
   <button class="primary" style="margin-top:12px" @click="go('detail/detail?id='+created.id)">查看派单进度</button>
  </template>
 </PageShell>
</template>
