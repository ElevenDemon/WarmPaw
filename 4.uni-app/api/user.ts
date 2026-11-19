import { request } from '../utils/request'
export const userApi = { pets: () => request('/owner/pets'), addresses: () => request('/owner/addresses'), profile: () => request('/sitter/profile'), income: () => request('/sitter/income'), notices: (page=1) => request('/notifications?page='+page), me: () => request('/auth/me') }
