import type { UserView } from '@/types/models'

const AUTH_STORAGE_KEY = 'community-elderly-care-admin-auth'

export interface StoredAuthSession {
  token: string
  tokenType?: string
  expireAt?: string | null
  user?: UserView | null
}

export function readAuthSession(): StoredAuthSession | null {
  if (typeof window === 'undefined') {
    return null
  }

  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as StoredAuthSession
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export function writeAuthSession(session: StoredAuthSession) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
}

export function clearAuthSession() {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.removeItem(AUTH_STORAGE_KEY)
}
