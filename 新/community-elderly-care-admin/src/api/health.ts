import type { HealthRecordView, HealthWarningSummaryView } from '@/types/models'
import { request } from './client'

export function getHealthRecords(params?: {
  elderlyId?: number
  recordType?: number
  warningLevel?: number
  limit?: number
}) {
  return request<HealthRecordView[]>({
    url: '/health/records',
    method: 'get',
    params,
  })
}

export function getHealthWarnings(params?: {
  elderlyId?: number
  warningLevel?: number
  limit?: number
}) {
  return request<HealthRecordView[]>({
    url: '/health/warnings',
    method: 'get',
    params,
  })
}

export function getHealthSummary(params?: { elderlyId?: number }) {
  return request<HealthWarningSummaryView>({
    url: '/health/summary',
    method: 'get',
    params,
  })
}
