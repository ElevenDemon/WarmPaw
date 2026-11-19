import schemas from './schemas.json'
export const resolve = schema => schema?.$ref ? schemas[schema.$ref.split('/').pop()] : schema
export const schemaFor = name => schemas[name]
export function optionsFor(schema) {
 if (schema.enum) return schema.enum
 return schema.pattern && /^[a-zA-Z_]+(\|[a-zA-Z_]+)+$/.test(schema.pattern) ? schema.pattern.split('|') : []
}
export function prepare(schema, source) {
 schema = resolve(schema)
 if (schema.type === 'object') return Object.fromEntries(Object.entries(schema.properties || {}).map(([key, child]) => {
   const snake = key.replace(/[A-Z]/g, c => '_' + c.toLowerCase())
   return [key, prepare(child, source?.[key] ?? source?.[snake])]
 }))
 if (schema.type === 'array') {
   if (typeof source === 'string') { try { source = JSON.parse(source) } catch { source = [] } }
   return Array.isArray(source) ? source.map(value => prepare(schema.items, value)) : []
 }
 if (source != null) return source
 if (schema.type === 'boolean') return false
 if (schema.type === 'number' || schema.type === 'integer') return undefined
 return ''
}
export function validate(schema, value, path = '') {
 schema = resolve(schema)
 if (schema.type === 'object') {
  for (const key of schema.required || []) if (value[key] == null || value[key] === '') throw new Error(path + key + ' 为必填项')
  for (const [key, child] of Object.entries(schema.properties || {})) validate(child, value[key], path + key + '：')
  return
 }
 if (value == null || value === '') return
 if (schema.type === 'array') {
  if (schema.minItems != null && value.length < schema.minItems || schema.maxItems != null && value.length > schema.maxItems) throw new Error(path + '数量不符合要求')
  value.forEach((v, i) => validate(schema.items, v, path + (i+1) + '：')); return
 }
 if (schema.type === 'number' || schema.type === 'integer') {
  if (!Number.isFinite(value) || schema.type === 'integer' && !Number.isInteger(value)) throw new Error(path + '请输入有效数字')
  if (schema.minimum != null && value < schema.minimum || schema.maximum != null && value > schema.maximum) throw new Error(path + '超出允许范围')
 }
 if (typeof value === 'string') {
  if (schema.minLength && value.length < schema.minLength || schema.maxLength && value.length > schema.maxLength) throw new Error(path + '长度不符合要求')
  if (schema.pattern && !new RegExp('^(?:' + schema.pattern + ')$').test(value)) throw new Error(path + '格式不正确')
  if (schema.format === 'date-time' && !Number.isFinite(Date.parse(value))) throw new Error(path + '请输入有效时间')
 }
}
export function payloadFor(schema, value) {
 schema = resolve(schema)
 if (schema.type === 'object') return Object.fromEntries(Object.entries(schema.properties).map(([k,s]) => [k,payloadFor(s,value[k])]))
 if (schema.type === 'array') return value.map(v => payloadFor(schema.items,v))
 if (value === '' || value == null) return null
 if (schema.format === 'date-time') return new Date(value).toISOString()
 return value
}
