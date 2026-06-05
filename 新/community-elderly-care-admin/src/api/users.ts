import type {
  ElderlyProfileView,
  FamilyBindingView,
  UserStatusUpdateRequest,
  UserView,
} from '@/types/models'
import { request } from './client'

export function listUsers(params?: {
  keyword?: string
  userType?: number
  status?: number
}) {
  return request<UserView[]>({
    url: '/user/admin/users',
    method: 'get',
    params,
  })
}

export function updateUserStatus(data: UserStatusUpdateRequest) {
  return request<UserView>({
    url: '/user/admin/status',
    method: 'put',
    data,
  })
}

export function listBindings(params?: {
  status?: number
  elderlyId?: number
  familyId?: number
}) {
  return request<FamilyBindingView[]>({
    url: '/user/bindings',
    method: 'get',
    params,
  })
}

export function confirmBinding(id: number) {
  return request<FamilyBindingView>({
    url: `/user/binding/${id}/confirm`,
    method: 'put',
  })
}

export function getElderlyProfile(userId: number) {
  return request<ElderlyProfileView>({
    url: '/user/elderly-profile',
    method: 'get',
    params: { userId },
  })
}
