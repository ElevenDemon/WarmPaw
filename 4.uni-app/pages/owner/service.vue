<script setup lang="ts">
import {computed,ref} from 'vue'
import {onLoad} from '@dcloudio/uni-app'
import PageShell from '../../components/PageShell.vue'
import {usePage} from '../../composables/usePage'
import {request} from '../../utils/request'
import {useUserStore} from '../../store/modules/user'
import {time,toast} from '../../utils/format'

const user=useUserStore()
const orderId=ref(0),conversationId=ref(0),messages=ref<any[]>([]),content=ref('')
const conversationStatus=ref(''),showTopics=ref(true)
const topics=computed(()=>user.sitter?[
 {title:'接单与档期',question:'我是喂养师，接单前应该核对哪些信息？'},
 {title:'服务打卡',question:'上门服务打卡和照护记录需要注意什么？'},
 {title:'收益问题',question:'喂养师遇到收益或结算问题应该如何处理？'},
 {title:'遛狗照护',question:'遛狗前需要和宠主确认哪些事项？'}
]:[
 {title:'订单与派单',question:'我的订单什么时候会匹配喂养师？'},
 {title:'服务范围',question:'你们目前可以服务哪些地区？'},
 {title:'服务价格',question:'这次上门服务怎么收费？'},
 {title:'特殊照护',question:'我的宠物比较怕生，可以安排有经验的喂养师吗？'}
])
const greeting=computed(()=>user.sitter?'我是暖爪服务助手，可以协助你了解接单、档期、服务记录和照护注意事项，也可以为你联系人工客服。':'我是暖爪服务助手，可以帮你了解服务信息，也可以为你联系人工客服。')
const {loading,error,load,busy,action}=usePage(async()=>{
 if(orderId.value){await history();return}
 const list=(await request('/support/conversations?size=100')).list
 const active=list.find((item:any)=>item.status!=='CLOSED'&&!item.order_id)
 if(active){conversationId.value=active.id;conversationStatus.value=active.status;showTopics.value=active.status==='WAITING_HUMAN';await history()}
 else {conversationId.value=0;conversationStatus.value='';showTopics.value=true;messages.value=[]}
})
onLoad(q=>orderId.value=Number(q?.orderId||0))
const title=computed(()=>orderId.value?'专属服务群':'智能客服')
async function history(){
 if(orderId.value||conversationId.value){
  const query=orderId.value?'orderId='+orderId.value:'conversationId='+conversationId.value
  messages.value=(await request('/messages?size=100&'+query)).list.reverse()
 }
}
function isAssistant(message:any){return message.assistant||message.sender_type==='AI'}
function isMine(message:any){return !isAssistant(message)&&message.sender_id===user.user?.id}
async function ensureConversation(){
 if(!conversationId.value){
  conversationId.value=await request('/support/conversations','POST',{})
  conversationStatus.value='WAITING_HUMAN'
 }
}
async function handoff(){
 await ensureConversation()
 await request(`/support/conversations/${conversationId.value}/handoff`,'POST')
 conversationStatus.value='HUMAN_REQUESTED'
 showTopics.value=false
 toast('已为你联系人工客服')
}
async function resumeAi(){
 const nextId=await request(`/support/conversations/${conversationId.value}/resume-ai`,'POST')
 conversationId.value=nextId
 conversationStatus.value='WAITING_HUMAN'
 messages.value=[]
 showTopics.value=true
 content.value=''
}
async function send(question?:string){
 const text=(question||content.value).trim()
 if(!text)return
 if(!orderId.value)await ensureConversation()
 if(!orderId.value&&conversationStatus.value==='WAITING_HUMAN'){
  const result:any=await request('/ai/chat','POST',{conversationId:conversationId.value,message:text})
  messages.value.push({...result.userMessage,local:true},{...result.assistantMessage,local:true})
 }else{
  const body={...(orderId.value?{orderId:orderId.value}:{conversationId:conversationId.value}),content:text,mediaKeys:[]}
  await request('/messages','POST',body)
  await history()
 }
 content.value=''
 showTopics.value=false
}
</script>

<template>
 <PageShell :title="title" :back="!!orderId" :active="orderId?'': 'service'" :loading="loading" :error="error" @retry="load">
  <view v-if="!orderId" class="assistant-head">
   <view class="assistant-mark">✦</view>
   <view class="assistant-copy"><view class="assistant-name">暖爪服务助手</view><text class="assistant-state">常见问题即时查看 · 复杂问题可转人工</text></view>
   <view class="online-dot"/>
  </view>
  <view v-else class="room-note">宠主、喂养师与平台可在这里交流。请勿发送长期门锁密码。</view>

  <scroll-view class="chat-scroll" scroll-y :scroll-into-view="messages.length?'message-end':''">
   <view class="day-label">{{orderId?'服务沟通':'今天'}}</view>
   <view v-if="!messages.length" class="message-row assistant-row">
    <view class="assistant-avatar">✦</view>
    <view class="message-content"><text class="sender-label">暖爪服务助手</text><view class="bubble assistant-bubble">你好，{{user.user?.name||'朋友'}}。{{greeting}}</view><text class="message-time">现在</text></view>
   </view>
   <view v-for="m in messages" :id="'message-'+m.id" :key="m.id" :class="['message-row',isAssistant(m)?'assistant-row':isMine(m)?'owner-row':'human-row']">
    <view v-if="isAssistant(m)" class="assistant-avatar">✦</view>
    <view v-if="!isAssistant(m)&&!isMine(m)" class="staff-avatar">客</view>
    <view class="message-content"><text class="sender-label">{{isAssistant(m)?'暖爪服务助手':isMine(m)?'我':'人工客服'}}</text><view :class="['bubble',isAssistant(m)?'assistant-bubble':isMine(m)?'owner-bubble':'human-bubble']">{{m.content}}</view><text class="message-time">{{m.local?'刚刚':time(m.created_at)}}</text></view>
    <view v-if="isMine(m)" class="owner-avatar">{{user.user?.name?.slice(0,1)||'我'}}</view>
   </view>
   <view v-if="showTopics&&!orderId" class="topic-area">
    <text class="topic-heading">常见问题</text>
    <view class="topic-list"><button v-for="topic in topics" :key="topic.title" class="topic-button" @click="action(()=>send(topic.question),'')">{{topic.title}}<text>›</text></button></view>
   </view>
   <view v-if="!orderId&&['HUMAN_REQUESTED','HUMAN'].includes(conversationStatus)" class="handoff-panel">
    <view class="handoff-copy">{{conversationStatus==='HUMAN'?'人工客服已接入':'正在等待人工客服接入'}}</view>
    <button class="resume-button" @click="action(resumeAi,'')">结束人工服务，恢复智能客服</button>
   </view>
   <view v-else-if="!orderId&&(!conversationStatus||conversationStatus==='WAITING_HUMAN')" class="handoff-line" @click="action(handoff,'')">需要进一步帮助？<text>联系人工客服 ›</text></view>
   <view id="message-end" class="scroll-end"/>
  </scroll-view>

  <view class="composer-wrap">
   <view v-if="!orderId&&['HUMAN_REQUESTED','HUMAN'].includes(conversationStatus)" class="composer-hint">{{conversationStatus==='HUMAN'?'消息将发送给人工客服':'等待人工客服接入，留言会保存在会话中'}}</view>
   <view class="composer">
    <textarea v-model="content" auto-height maxlength="2000" :placeholder="orderId?'发送服务消息…':'描述你的问题…'"/>
    <button :disabled="busy||!content.trim()" :loading="busy" @click="action(send,'')">发送</button>
   </view>
   <text class="privacy-note">请勿发送门锁密码、证件号等敏感信息</text>
  </view>
 </PageShell>
</template>

<style scoped>
.assistant-head{display:flex;align-items:center;gap:12px;padding:12px 14px;background:#fff;border:1px solid #eae4da;border-radius:14px;margin-bottom:14px}
.assistant-mark,.assistant-avatar{display:flex;align-items:center;justify-content:center;flex-shrink:0}
.assistant-mark{width:42px;height:42px;border-radius:14px;background:#fbe9df;color:#c74e1e;font-size:22px}
.assistant-copy{display:flex;flex:1;flex-direction:column;min-width:0}
.assistant-name{font-size:15px;font-weight:700}
.assistant-state{font-size:11px;color:#8b847b}
.online-dot{width:8px;height:8px;border-radius:50%;background:#4b9b70}
.room-note{padding:11px 13px;background:#fff;border-radius:10px;color:#777168;font-size:12px;margin-bottom:12px}
.chat-scroll{height:calc(100vh - 260px);min-height:340px;padding-bottom:12px}
.day-label{text-align:center;color:#aaa298;font-size:11px;padding:8px 0 18px}
.message-row{display:flex;align-items:flex-start;gap:8px;margin:0 0 18px}
.assistant-avatar,.owner-avatar{width:30px;height:30px;border-radius:10px;font-size:15px}
.assistant-avatar{background:#fbe9df;color:#c74e1e}
.staff-avatar{display:flex;align-items:center;justify-content:center;flex-shrink:0;width:30px;height:30px;border-radius:10px;background:#e5f1e9;color:#3d7a5c;font-size:12px}
.owner-avatar{display:flex;align-items:center;justify-content:center;background:#e7eee8;color:#51705a;font-size:12px;flex-shrink:0}
.message-content{display:flex;flex-direction:column;max-width:82%;min-width:0}
.sender-label,.message-time{font-size:10px;color:#9b948a;padding:0 3px}
.bubble{margin-top:4px;padding:11px 13px;border-radius:4px 14px 14px 14px;font-size:13px;line-height:1.65;white-space:pre-wrap;word-break:break-word}
.assistant-bubble{background:#fff;border:1px solid #eae4da}
.owner-bubble{background:#fbe9df;border-radius:14px 4px 14px 14px}
.human-bubble{background:#eaf1eb;border-radius:4px 14px 14px 14px}
.message-time{margin-top:4px}
.owner-row{justify-content:flex-end}
.human-row{justify-content:flex-start}
.owner-row .message-content{align-items:flex-end}
.topic-area{margin:4px 0 18px 38px}
.topic-heading{display:block;font-size:12px;font-weight:700;color:#777168;margin-bottom:8px}
.topic-list{display:flex;flex-wrap:wrap;gap:7px}
.topic-button{width:auto;margin:0;padding:7px 11px;background:#fff;border:1px solid #eae4da;border-radius:16px;color:#514c45;font-size:11px;line-height:1.4}
.topic-button text{padding-left:5px;color:#c74e1e}
.handoff-line{text-align:center;font-size:11px;color:#8b847b;padding:4px 0 12px}
.handoff-line text{color:#c74e1e;padding-left:5px}
.handoff-panel{margin:6px 0 18px;padding:12px;background:#fff;border:1px solid #e5ded4;border-radius:10px;text-align:center}
.handoff-copy{font-size:11px;color:#777168;margin-bottom:8px}
.resume-button{height:34px;padding:0 12px;background:#eaf1eb;color:#3d7a5c;border-radius:8px;font-size:12px;line-height:34px}
.scroll-end{height:1px}
.composer-wrap{position:fixed;z-index:15;bottom:calc(68px + env(safe-area-inset-bottom));left:50%;transform:translateX(-50%);width:min(100%,600px);padding:9px 20px 5px;background:#f6f3ed}
.composer-hint,.privacy-note{display:block;text-align:center;color:#9b948a;font-size:10px;margin-bottom:6px}
.composer{display:flex;align-items:flex-end;gap:8px;padding:7px 8px 7px 13px;background:#fff;border:1px solid #e5ded4;border-radius:16px}
.composer textarea{flex:1;width:0;min-height:24px;max-height:88px;padding:3px 0;font-size:13px}
.composer button{width:58px;height:34px;padding:0;background:#e8622d;color:#fff;border-radius:10px;font-size:12px;flex-shrink:0}
.privacy-note{margin:5px 0 0}
@media(max-height:700px){.chat-scroll{height:calc(100vh - 230px);min-height:260px}}
</style>
