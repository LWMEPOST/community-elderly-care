const {
  getSession,
  clearSession,
  syncUserProfile,
  getMiniappAccessError,
} = require('./utils/auth')
const api = require('./utils/api')

const TAB_BAR_PAGE_ROUTES = [
  'pages/home/index/index',
  'pages/service/index/index',
  'pages/health/index/index',
  'pages/message/index/index',
  'pages/profile/index/index',
]

App({
  globalData: {
    session: null,
    user: null,
  },

  onLaunch() {
    this.hydrateSession()
    if (this.globalData.session && this.globalData.session.token) {
      this.bootstrapCurrentUser()
    }
  },

  onShow() {
    if (this.globalData.session && this.globalData.session.token) {
      this.bootstrapCurrentUser()
      return
    }

    this.applyRoleTabBar()
  },

  hydrateSession() {
    const session = getSession()
    const user = session ? session.user : null
    const accessError = getMiniappAccessError(user)

    if (session && accessError) {
      clearSession()
      this.globalData.session = null
      this.globalData.user = null
    } else {
      this.globalData.session = session
      this.globalData.user = user
    }

    this.applyRoleTabBar()
  },

  bootstrapCurrentUser() {
    const session = getSession()
    if (!session || !session.token) {
      return Promise.resolve(null)
    }

    return api.fetchCurrentUser().then((user) => {
      const accessError = getMiniappAccessError(user)
      if (accessError) {
        this.logout(accessError)
        return null
      }

      syncUserProfile(user)
      this.globalData.session = getSession()
      this.globalData.user = user
      this.applyRoleTabBar()
      return user
    }).catch(() => null)
  },

  applyRoleTabBar() {
    const pages = getCurrentPages()
    const currentPage = pages.length ? pages[pages.length - 1] : null

    if (
      typeof wx.setTabBarItem !== 'function' ||
      !currentPage ||
      TAB_BAR_PAGE_ROUTES.indexOf(currentPage.route) === -1
    ) {
      return
    }

    const userType = this.globalData.user ? this.globalData.user.userType : null
    const thirdLabel = userType === 4 ? '工单' : '健康'
    const thirdIconPath = userType === 4 ? 'assets/tabbar/work.png' : 'assets/tabbar/health.png'
    const thirdSelectedIconPath = userType === 4 ? 'assets/tabbar/work-active.png' : 'assets/tabbar/health-active.png'

    try {
      wx.setTabBarItem({
        index: 2,
        text: thirdLabel,
        iconPath: thirdIconPath,
        selectedIconPath: thirdSelectedIconPath,
      })
    } catch (error) {
      console.error('[app] failed to update role tab bar', error)
    }
  },

  logout(message) {
    clearSession()
    this.globalData.session = null
    this.globalData.user = null
    this.applyRoleTabBar()
    if (message) {
      wx.showToast({
        title: message,
        icon: 'none',
      })
    }
    wx.reLaunch({
      url: '/pages/auth/login/index',
    })
  },
})
