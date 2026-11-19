import { token, setToken, loginPage } from './auth'
import type { Envelope } from '../types/api'
import { normalize } from './normalize'
let base = import.meta.env.VITE_API_BASE_URL || '/api'
// #ifndef H5
if (base.startsWith('/')) base = 'http://localhost:8080/api'
// #endif
export const API_BASE = base.replace(/\/$/, '')
export class ApiError extends Error { constructor(public status: number, message: string) { super(message) } }
export function request<T = any>(path: string, method: 'GET' | 'POST' | 'PUT' | 'DELETE' = 'GET', data?: any, redirectOnUnauthorized = true): Promise<T> {
  return new Promise((resolve, reject) => uni.request({ url: API_BASE + path, method, data,
    header: { 'Content-Type': 'application/json', ...(token() ? { Authorization: 'Bearer ' + token() } : {}) }, timeout: path === '/ai/chat' ? 60000 : 20000,
    success(res) { const body = res.data as Envelope<T> | null; if (res.statusCode === 401 && redirectOnUnauthorized) { setToken(''); loginPage() }
      if (res.statusCode >= 200 && res.statusCode < 300 && body?.code === 0) resolve(normalize(body.data))
      else reject(new ApiError(res.statusCode, body?.message || '请求失败，请稍后重试')) },
    fail: () => reject(new ApiError(0, '连接失败，请检查网络与服务地址'))
  }))
}
export function upload(orderId: number, filePath: string) { return new Promise((resolve, reject) => uni.uploadFile({ url: API_BASE + '/orders/' + orderId + '/upload', filePath, name: 'file', header: { Authorization: 'Bearer ' + token() }, success(res) { try { const body = JSON.parse(res.data); if (res.statusCode === 200 && body.code === 0) resolve(body.data); else reject(new Error(body.message || '上传失败')) } catch { reject(new Error('上传响应异常')) } }, fail: () => reject(new Error('上传失败，请重试')) })) }
export function download(orderId: number, mediaId: number): Promise<string> { return new Promise((resolve, reject) => uni.downloadFile({ url: API_BASE + '/orders/' + orderId + '/media/' + mediaId + '/file', header: { Authorization: 'Bearer ' + token() }, success: r => r.statusCode === 200 ? resolve(r.tempFilePath) : reject(new Error('文件不存在、已过期或无访问权限')), fail: () => reject(new Error('下载失败')) })) }
