import type { InformationSaveRequest, InformationView } from '@/types/models'
import { request } from './client'

export function listManagedInformation(params?: {
  infoType?: number
  status?: number
  keyword?: string
  publisherId?: number
  limit?: number
}) {
  return request<InformationView[]>({
    url: '/information/manage',
    method: 'get',
    params,
  })
}

export function getInformationDetail(id: number) {
  return request<InformationView>({
    url: `/information/${id}`,
    method: 'get',
  })
}

export function createInformationDraft(data: InformationSaveRequest) {
  return request<InformationView>({
    url: '/information/draft',
    method: 'post',
    data,
  })
}

export function publishInformationImmediately(data: InformationSaveRequest) {
  return request<InformationView>({
    url: '/information/publish',
    method: 'post',
    data,
  })
}

export function updateInformation(id: number, data: InformationSaveRequest) {
  return request<InformationView>({
    url: `/information/${id}`,
    method: 'put',
    data,
  })
}

export function publishInformation(id: number) {
  return request<InformationView>({
    url: `/information/${id}/publish`,
    method: 'put',
  })
}

export function withdrawInformation(id: number) {
  return request<InformationView>({
    url: `/information/${id}/withdraw`,
    method: 'put',
  })
}
