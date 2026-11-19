<script setup>
import { computed } from 'vue'
import { fieldLabel, format } from '../config/modules'
const props = defineProps({ value:null })
const parsed = computed(() => {
 if (typeof props.value === 'string' && /^[\[{]/.test(props.value)) { try { return JSON.parse(props.value) } catch {} }
 return props.value
})
</script>
<template>
 <div v-if="Array.isArray(parsed)" class="related-records"><el-empty v-if="!parsed.length" description="暂无记录" :image-size="45"/><div v-for="(item,index) in parsed" :key="index" class="related-record"><RecordDetail :value="item"/></div></div>
 <el-descriptions v-else-if="parsed && typeof parsed === 'object'" :column="1" border><el-descriptions-item v-for="(item,key) in parsed" :key="key" :label="fieldLabel(key)"><RecordDetail :value="item"/></el-descriptions-item></el-descriptions>
 <span v-else class="detail-value">{{ format(parsed) }}</span>
</template>
