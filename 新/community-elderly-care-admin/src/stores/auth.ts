import { defineStore } from 'pinia'
import { fetchCurrentUser, loginByPassword } from '@/api/auth'
import type { LoginRequest, LoginResponse, UserView } from '@/types/models'
import {
  clearAuthSession,
  readAuthSession,
  writeAuthSession,
} from '@/utils/storage'

function assertAdminUser(user: UserView | null) {
  if (!user) {
    throw new Error('未获取到管理员信息，请重新登录')
  }

  if (user.userType !== 3) {
    throw new Error('请使用管理员账号登录后台')
  }

  if (user.status !== 1) {
    throw new Error('当前管理员账号未启用')
  }
}

interface AuthState {
  token: string
  tokenType: string
  expireAt: string | null
  user: UserView | null
  hydrated: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: '',
    tokenType: 'Bearer',
    expireAt: null,
    user: null,
    hydrated: false,
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.realName || state.user?.username || '管理员',
  },

  actions: {
    hydrate() {
      const session = readAuthSession()
      if (session) {
        this.token = session.token
        this.tokenType = session.tokenType || 'Bearer'
        this.expireAt = session.expireAt ?? null
        this.user = session.user ?? null
      }
      this.hydrated = true
    },

    persist() {
      if (!this.token) {
        clearAuthSession()
        return
      }

      writeAuthSession({
        token: this.token,
        tokenType: this.tokenType,
        expireAt: this.expireAt,
        user: this.user,
      })
    },

    setSession(payload: LoginResponse) {
      this.token = payload.token
      this.tokenType = payload.tokenType
      this.expireAt = payload.expireAt
      this.user = payload.user
      this.persist()
    },

    async login(credentials: LoginRequest) {
      const response = await loginByPassword(credentials)
      assertAdminUser(response.user)
      this.setSession(response)
      return response.user
    },

    async fetchCurrentUser() {
      const user = await fetchCurrentUser()
      this.user = user
      this.persist()
      return user
    },

    async ensureAdminSession() {
      if (!this.hydrated) {
        this.hydrate()
      }

      if (!this.token) {
        return null
      }

      await this.fetchCurrentUser()

      try {
        assertAdminUser(this.user)
      } catch (error) {
        this.logout()
        throw error
      }

      return this.user
    },

    logout() {
      this.token = ''
      this.tokenType = 'Bearer'
      this.expireAt = null
      this.user = null
      this.persist()
    },
  },
})
