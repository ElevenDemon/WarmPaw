import { ref } from 'vue'
import type { Page, RecordData } from '../types/api'
export function usePagination(loader: (page:number)=>Promise<Page<RecordData>>) { const rows=ref<RecordData[]>([]), page=ref(0), total=ref(0), loading=ref(false); async function load(reset=false) { if(loading.value)return; loading.value=true; try { const next=reset?1:page.value+1; const data=await loader(next); rows.value=reset?data.list:[...rows.value,...data.list]; total.value=data.total; page.value=next } finally {loading.value=false} } return {rows,total,loading,load} }
