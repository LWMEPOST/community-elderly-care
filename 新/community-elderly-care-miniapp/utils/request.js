const { API_BASE_URL } = require('./config')
const { clearSession, getSession } = require('./auth')

function sanitizeData(data) {
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    return data
  }

  const next = {}
  Object.keys(data).forEach((key) => {
    const value = data[key]
    if (value !== undefined) {
      next[key] = value
    }
  })
  return next
}

function redirectToLogin(message) {
  clearSession()
  if (message) {
    wx.showToast({
      title: message,
      icon: 'none',
    })
  }

  const pages = getCurrentPages()
  const current = pages.length ? pages[pages.length - 1] : null
  if (!current || current.route !== 'pages/auth/login/index') {
    wx.reLaunch({
      url: '/pages/auth/login/index',
    })
  }
}

function request(options) {
  const session = getSession()
  const header = Object.assign({}, options.header || {})

  if (!header['content-type']) {
    header['content-type'] = 'application/json'
  }

  if (options.auth !== false && session && session.token) {
    header.Authorization = `Bearer ${session.token}`
  }

  return new Promise((resolve, reject) => {
    const data = sanitizeData(options.data || {})

    wx.request({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data,
      header,
      success(res) {
        const payload = res.data || {}

        if (typeof payload.code === 'number') {
          if (payload.code === 200) {
            resolve(payload.data)
            return
          }

          if (payload.code === 401) {
            redirectToLogin(payload.message || '登录已失效')
            reject(payload)
            return
          }

          console.error('[request] business error', options.method || 'GET', options.url, payload)
          wx.showToast({
            title: payload.message || '请求失败',
            icon: 'none',
          })
          reject(payload)
          return
        }

        resolve(payload)
      },
      fail(error) {
        console.error('[request] network error', options.method || 'GET', options.url, error)
        wx.showToast({
          title: '网络异常，请稍后再试',
          icon: 'none',
        })
        reject(error)
      },
    })
  })
}

module.exports = {
  request,
  redirectToLogin,
}
