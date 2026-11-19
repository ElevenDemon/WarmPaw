// SQL map queries may return JSONB as text; normalize only documented JSON fields.
const jsonFields = new Set(['address_snapshot','pet_snapshot','signed_agreement','source_snapshot','categories','materials','tags','media_keys','service_codes','detail','chain','content_json'])
export function normalize(value: any): any {
  if(Array.isArray(value)) return value.map(normalize)
  if(value && typeof value==='object') return Object.fromEntries(Object.entries(value).map(([key,raw])=>{
    let item=raw
    if(jsonFields.has(key) && typeof raw==='string') { try { item=JSON.parse(raw) } catch { /* Preserve malformed server text for diagnosis. */ } }
    return [key,normalize(item)]
  }))
  return value
}
