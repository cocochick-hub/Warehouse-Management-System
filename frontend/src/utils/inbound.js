export function inboundStatusType(status) {
  if (status === '已完成') return 'success'
  if (status === '部分完成') return 'warning'
  return 'info'
}

export function formatDateTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}
