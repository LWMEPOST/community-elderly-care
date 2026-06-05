import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/models'
import { clearAuthSession, readAuthSession } from '@/utils/storage'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15000,
})

let redirectingToLogin = false

function redirectToLogin() {
  if (typeof window === 'undefined' || redirectingToLogin) {
    return
  }

  redirectingToLogin = true
  clearAuthSession()

  const redirect = encodeURIComponent(`${window.location.pathname}${window.location.search}`)
  window.setTimeout(() => {
    window.location.href = `/login?redirect=${redirect}`
  }, 60)
}

export class RequestError extends Error {
  code?: number

  constructor(message: string, code?: number) {
    super(message)
    this.name = 'RequestError'
    this.code = code
  }
}

client.interceptors.request.use((config) => {
  const session = readAuthSession()
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status as number | undefined
    const message = error.response?.data?.message ?? error.message ?? '请求失败，请稍后再试'

    if (status === 401) {
      ElMessage.error('登录状态已失效，请重新登录')
      redirectToLogin()
    } else if (status === 403) {
      ElMessage.error(message || '无权限访问')
    } else if (status) {
      ElMessage.error(message)
    }

    return Promise.reject(new RequestError(message, status))
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResult<T>>(config)
  const payload = response.data

  if (payload?.code !== 200) {
    const message = payload?.message || '请求失败，请稍后再试'

    if (payload?.code === 401) {
      ElMessage.error(message)
      redirectToLogin()
    } else {
      ElMessage.error(message)
    }

    throw new RequestError(message, payload?.code)
  }

  return payload.data
}
