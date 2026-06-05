import type { LoginRequest, LoginResponse, UserView } from '@/types/models'
import { request } from './client'

export function loginByPassword(data: LoginRequest) {
  return request<LoginResponse>({
    url: '/user/login',
    method: 'post',
    data,
  })
}

export function fetchCurrentUser() {
  return request<UserView>({
    url: '/user/me',
    method: 'get',
  })
}
