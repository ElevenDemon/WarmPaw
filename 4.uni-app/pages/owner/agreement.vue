<script setup lang="ts">
import PageShell from '../../components/PageShell.vue';import {useCartStore} from '../../store/modules/cart';import {go,toast} from '../../utils/format';const cart=useCartStore();function next(){if(!cart.accepted)return toast('请明确同意授权协议后继续');go('owner/pay')}
</script><template><PageShell title="上门服务授权" back><view class="step-number">SERVICE AUTHORIZATION · V1</view><view class="title">把信任，交代清楚。</view><view class="card" style="margin-top:24px"><view style="font-weight:700">上门服务授权与责任边界</view><view class="divider"/><view class="safe-copy">1. 您授权已指派的喂养师在预约时段进入指定地址，仅执行已约定的宠物照护事项。

2. 钥匙交接或临时门锁授权应通过双方确认的安全方式完成。请勿在公开消息中发送长期门锁密码。

3. 请如实提供宠物习性、饮食禁忌和病史。喂药等高风险项目需相应资质；平台照护不替代兽医诊断。

4. 服务过程会记录进出门时间、照护清单和媒体，仅用于履约确认与争议处理。

5. 出现异常时将联系宠主与平台。紧急送医需取得明确授权，不默认代您作出医疗决定。

6. 服务结束后确认门窗关闭，并按约定归还钥匙或撤销临时授权。</view></view><text class="label">本次具体授权范围</text><view class="field field-area"><textarea v-model="cart.scope" maxlength="500"/></view><view class="option" :class="{selected:cart.accepted}" @click="cart.accepted=!cart.accepted"><view class="row-start"><text class="checkbox">{{cart.accepted?'✓':''}}</text><text>我已阅读并同意，以当前登录账号电子确认本次授权。</text></view></view><button class="primary" @click="next">同意并确认订单</button></PageShell></template>
