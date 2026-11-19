const baseURL = (import.meta.env?.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
export const tokenStore = {
  get: () => sessionStorage.getItem('warm-paw-token'),
  set: token => sessionStorage.setItem('warm-paw-token', token),
  clear: () => sessionStorage.removeItem('warm-paw-token'),
}
export async function request(path, { method = 'GET', body, query } = {}) {
  const params = new URLSearchParams(Object.entries(query || {}).filter(([, v]) => v !== '' && v != null))
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), 20000)
  try {
    const token = tokenStore.get()
    const response = await fetch(baseURL + path + (params.size ? '?' + params : ''), {
      method, signal: controller.signal,
      headers: { ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: 'Bearer ' + token } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    const payload = await response.json().catch(() => null)
    if (response.status === 401 && path !== '/auth/login') {
      tokenStore.clear()
      window.dispatchEvent(new Event('auth-expired'))
    }
    if (!response.ok || payload?.code !== 0) {
      const error = new Error(payload?.message || '请求失败（' + response.status + '）')
      error.status = response.status
      throw error
    }
    return payload.data
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('请求超时，请重试')
    if (error instanceof TypeError) throw new Error('无法连接后端，请检查服务地址与网络')
    throw error
  } finally { clearTimeout(timer) }
}
export const adminApi = {
  list: (module, query) => request('/admin/modules/' + module, { query }),
  dashboard: () => request('/admin/dashboard'),
  dictionary: () => request('/admin/dictionary'),
}
