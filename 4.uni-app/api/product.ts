import { request } from '../utils/request'
export const productApi = { catalog: () => request('/public/catalog', 'GET', undefined, false), health: () => request('/public/health'), order: (id:number) => request('/orders/'+id), orders: (page=1, status='') => request('/orders?page='+page+'&size=10&status='+status) }
