/**
 * 时间格式化工具
 */
export function formatTime(val) {
  if (!val) return '-'
  // 处理 LocalDateTime 格式: 2026-06-11T14:32:22.548
  const d = new Date(val.replace('T', ' '))
  if (isNaN(d.getTime())) return val
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
