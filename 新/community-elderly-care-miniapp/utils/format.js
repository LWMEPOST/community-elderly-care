function pad(value) {
  return value < 10 ? `0${value}` : `${value}`
}

function formatDateTime(value, fallback) {
  if (!value) {
    return fallback || '--'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatCurrency(value, fallback) {
  if (value === null || value === undefined || value === '') {
    return fallback || '--'
  }
  return `￥${Number(value).toFixed(2)}`
}

function buildDateTimeString(date, time) {
  if (!date || !time) {
    return ''
  }
  return `${date}T${time}:00`
}

function buildHealthMetric(record) {
  if (record.recordType === 1) {
    return `${record.systolicPressure || '--'} / ${record.diastolicPressure || '--'}`
  }
  if (record.recordType === 2) {
    return `${record.bloodSugar || '--'} mmol/L`
  }
  if (record.recordType === 3) {
    return `${record.heartRate || '--'} bpm`
  }
  return '综合记录'
}

module.exports = {
  formatDateTime,
  formatCurrency,
  buildDateTimeString,
  buildHealthMetric,
}
