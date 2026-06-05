export function formatDateTime(value: string | null | undefined, fallback = '--') {
  if (!value) {
    return fallback
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function formatCurrency(value: number | null | undefined, fallback = '--') {
  if (value === null || value === undefined) {
    return fallback
  }

  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2,
  }).format(value)
}

export function formatGender(value: number | null | undefined) {
  if (value === 0) {
    return '女'
  }
  if (value === 1) {
    return '男'
  }
  return '未填写'
}

export function summarizeText(value: string | null | undefined, limit = 96) {
  if (!value) {
    return ''
  }
  return value.length > limit ? `${value.slice(0, limit)}...` : value
}
