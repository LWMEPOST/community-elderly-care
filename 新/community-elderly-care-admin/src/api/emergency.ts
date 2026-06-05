import type { EmergencyHelpRespondRequest, EmergencyHelpView } from '@/types/models'
import { request } from './client'

export function getEmergencyList(params?: {
  elderlyId?: number
  status?: number
  limit?: number
}) {
  return request<EmergencyHelpView[]>({
    url: '/emergency/list',
    method: 'get',
    params,
  })
}

export function getEmergencyDetail(id: number) {
  return request<EmergencyHelpView>({
    url: `/emergency/${id}`,
    method: 'get',
  })
}

export function respondEmergency(id: number, data?: EmergencyHelpRespondRequest) {
  return request<EmergencyHelpView>({
    url: `/emergency/${id}/response`,
    method: 'put',
    data,
  })
}

export function resolveEmergency(id: number) {
  return request<EmergencyHelpView>({
    url: `/emergency/${id}/resolve`,
    method: 'put',
  })
}
