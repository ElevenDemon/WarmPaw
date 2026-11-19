<script setup>
import { computed } from 'vue'
import { resolve, prepare, optionsFor } from '../api/forms'
import { fieldLabel, format } from '../config/modules'
const props = defineProps({ schema:Object, modelValue:Object })
const emit = defineEmits(['update:modelValue'])
const definition = computed(() => resolve(props.schema))
const set = (key, value) => emit('update:modelValue', { ...props.modelValue, [key]:value })
const add = (key, schema) => set(key, [...(props.modelValue[key] || []), prepare(schema.items)])
const updateItem = (key, index, value) => set(key, props.modelValue[key].map((v,i) => i === index ? value : v))
const remove = (key, index) => set(key, props.modelValue[key].filter((_,i) => i !== index))
</script>
<template>
 <div v-for="(raw,key) in definition.properties" :key="key" class="schema-field">
  <template v-for="schema in [resolve(raw)]" :key="key">
   <fieldset v-if="schema.type === 'object'"><legend>{{ fieldLabel(key) }}</legend><SchemaFields :schema="schema" :model-value="modelValue[key]" @update:model-value="set(key,$event)"/></fieldset>
   <el-form-item v-else :label="fieldLabel(key)" :required="definition.required?.includes(key)">
    <template v-if="schema.type === 'array'">
     <div v-if="resolve(schema.items).type === 'object'" class="array-fields"><fieldset v-for="(item,i) in modelValue[key]" :key="i"><legend>第 {{ i+1 }} 项</legend><SchemaFields :schema="resolve(schema.items)" :model-value="item" @update:model-value="updateItem(key,i,$event)"/><el-button @click="remove(key,i)">移除</el-button></fieldset><el-button :disabled="modelValue[key]?.length >= (schema.maxItems || 100)" @click="add(key,schema)">添加{{ fieldLabel(key) }}</el-button></div>
     <el-select v-else-if="optionsFor(schema.items).length" :model-value="modelValue[key]" multiple @update:model-value="set(key,$event)"><el-option v-for="v in optionsFor(schema.items)" :key="v" :label="format(v)" :value="v"/></el-select>
     <div v-else class="array-fields"><div v-for="(item,i) in modelValue[key]" :key="i" class="array-line"><el-input-number v-if="schema.items.type === 'integer'" :model-value="item" :min="1" :precision="0" @update:model-value="updateItem(key,i,$event)"/><el-input v-else :model-value="item" @update:model-value="updateItem(key,i,$event)"/><el-button @click="remove(key,i)">移除</el-button></div><el-button @click="add(key,schema)">添加{{ fieldLabel(key) }}</el-button></div>
    </template>
    <el-switch v-else-if="schema.type === 'boolean'" :model-value="modelValue[key]" @update:model-value="set(key,$event)"/>
    <el-input-number v-else-if="['integer','number'].includes(schema.type)" :model-value="modelValue[key]" :min="schema.minimum" :max="schema.maximum" :precision="schema.type === 'integer' ? 0 : undefined" @update:model-value="set(key,$event)"/>
    <el-select v-else-if="optionsFor(schema).length" :model-value="modelValue[key]" @update:model-value="set(key,$event)"><el-option v-for="v in optionsFor(schema)" :key="v" :label="format(v)" :value="v"/></el-select>
    <el-date-picker v-else-if="schema.format === 'date-time'" :model-value="modelValue[key]" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" @update:model-value="set(key,$event)"/>
    <el-time-picker v-else-if="['startTime','endTime'].includes(key)" :model-value="modelValue[key]" value-format="HH:mm:ss" @update:model-value="set(key,$event)"/>
    <el-input v-else :model-value="modelValue[key]" :type="key === 'password' ? 'password' : (schema.maxLength > 300 ? 'textarea' : 'text')" :show-password="key === 'password'" :maxlength="schema.maxLength" @update:model-value="set(key,$event)"/>
    <small v-if="schema.description" class="field-help">{{ schema.description }}</small>
   </el-form-item>
  </template>
 </div>
</template>
