const TOKEN = 'warm-paw-token'
export const token = () => uni.getStorageSync(TOKEN) || ''
export const setToken = (value: string) => value ? uni.setStorageSync(TOKEN, value) : uni.removeStorageSync(TOKEN)
export const loginPage = () => { if (!getCurrentPages()[getCurrentPages().length-1]?.route?.includes('login')) uni.reLaunch({ url: '/pages/login/index' }) }
export const deviceId = () => { let id = uni.getStorageSync('warm-paw-device'); if (!id) { id = 'device-' + Date.now() + '-' + Math.random().toString(36).slice(2); uni.setStorageSync('warm-paw-device', id) } return id }

