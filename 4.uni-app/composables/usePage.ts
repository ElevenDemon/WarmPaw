import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { useAuth } from './useAuth'
import { toast } from '../utils/format'
export function usePage(loader:()=>Promise<any>, auth=true){const loading=ref(false), busy=ref(false), error=ref('');const {requireAuth}=useAuth();async function load(){loading.value=true;error.value='';try{if(auth&&!await requireAuth())return;await loader()}catch(e:any){error.value=e.message}finally{loading.value=false;uni.stopPullDownRefresh()}}async function action(fn:()=>Promise<any>,success='操作成功'){if(busy.value)return;busy.value=true;try{await fn();if(success)toast(success)}catch(e:any){toast(e.message)}finally{busy.value=false}}onShow(load);onPullDownRefresh(load);return{loading,busy,error,load,action}}

