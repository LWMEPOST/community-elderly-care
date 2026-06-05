const { getSession, getMiniappAccessError } = require('./auth')
const { redirectToLogin } = require('./request')

function requireUser(page) {
  const session = getSession()
  if (!session || !session.token) {
    redirectToLogin()
    return null
  }

  const accessError = getMiniappAccessError(session.user)
  if (accessError) {
    redirectToLogin(accessError)
    return null
  }

  const app = getApp()
  app.globalData.session = session
  app.globalData.user = session.user
  app.applyRoleTabBar()

  if (page && typeof page.setData === 'function') {
    page.setData({
      currentUser: session.user || null,
    })
  }

  return session.user || null
}

module.exports = {
  requireUser,
}
