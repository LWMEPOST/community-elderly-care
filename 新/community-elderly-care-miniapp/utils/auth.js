const SESSION_KEY = 'community-elderly-care-miniapp-session'

function isUserEnabled(user) {
  return !!(user && user.status === 1)
}

function isMiniappAllowedUser(user) {
  return !!(user && user.userType !== 3)
}

function getMiniappAccessError(user) {
  if (!user) {
    return '登录信息无效，请重新登录'
  }

  if (!isUserEnabled(user)) {
    return '账号待审核或已禁用'
  }

  if (!isMiniappAllowedUser(user)) {
    return '管理员请使用后台管理端登录'
  }

  return ''
}

function getSession() {
  try {
    return wx.getStorageSync(SESSION_KEY) || null
  } catch (error) {
    return null
  }
}

function setSession(session) {
  wx.setStorageSync(SESSION_KEY, session)
  return session
}

function saveLoginSession(payload) {
  return setSession({
    token: payload.token,
    tokenType: payload.tokenType || 'Bearer',
    expireAt: payload.expireAt || null,
    user: payload.user || null,
  })
}

function syncUserProfile(user) {
  const session = getSession()
  if (!session) {
    return null
  }

  return setSession(Object.assign({}, session, { user }))
}

function clearSession() {
  wx.removeStorageSync(SESSION_KEY)
}

module.exports = {
  getSession,
  setSession,
  saveLoginSession,
  syncUserProfile,
  clearSession,
  isUserEnabled,
  isMiniappAllowedUser,
  getMiniappAccessError,
}
