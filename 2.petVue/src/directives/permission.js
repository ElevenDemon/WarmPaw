import { can } from '../stores/auth'
function update(el, binding) {
  const permissions = Array.isArray(binding.value) ? binding.value : [binding.value]
  el.style.display = permissions.some(can) ? '' : 'none'
}
export const permission = { mounted: update, updated: update }
