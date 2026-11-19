export const money = (v: unknown) => '¥' + Number(v || 0).toFixed(2)
export const time = (v: any) => v ? String(v).replace('T', ' ').slice(0, 16) : '—'
export const statusNames: Record<string, string> = { PENDING_PAYMENT:'等待智能派单', DISPATCHING:'等待平台派单', PENDING_ACCEPT:'待接单', ACCEPTED:'待上门', SERVICING:'服务中', COMPLETED:'已完成', CANCELLED:'已取消', REFUNDED:'已退款', PENDING:'待审核', ACTIVE:'已启用', REST:'休息中', DISABLED:'已停用', REJECTED:'已驳回', APPROVED:'已认证', AVAILABLE:'可使用', USED:'已使用', EXPIRED:'已过期', LOCKED:'已锁定', PENDING_AI:'服务记录已生成', WAITING_HUMAN:'等待客服接入', HUMAN_REQUESTED:'已请求人工客服', HUMAN:'人工客服处理中', SETTLED:'已结算' }
export const status = (v: string) => statusNames[v] || v
export const categories: Record<string, string> = {CAT:'猫咪', DOG:'狗狗', SMALL:'小宠', OTHER:'其他', WALK:'遛狗', MEDICINE:'喂药', DEWORM:'驱虫'}
export const checks: Record<string, string> = {FEED:'喂食', WATER:'换水', CLEAN:'清洁猫砂与食具', PLAY:'陪伴互动', DOOR:'确认门窗关闭'}
export const go = (url: string) => uni.navigateTo({ url: '/pages/' + url })
export const toast = (title: string) => uni.showToast({title, icon:'none'})
export const confirm = (content: string): Promise<boolean> => new Promise(resolve => uni.showModal({title:'暖爪提醒', content, success:r=>resolve(r.confirm), fail:()=>resolve(false)}))
