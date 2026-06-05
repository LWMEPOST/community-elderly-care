const api = require('../../../utils/api')
const { getSession, saveLoginSession, getMiniappAccessError } = require('../../../utils/auth')

Page({
  data: {
    form: {
      username: '',
      password: '',
    },
    submitting: false,
  },

  onShow() {
    const session = getSession()
    if (session && session.token && !getMiniappAccessError(session.user)) {
      wx.switchTab({
        url: '/pages/home/index/index',
      })
    }
  },

  onUsernameInput(event) {
    this.setData({
      'form.username': (event.detail.value || '').replace(/\s+/g, ''),
    })
  },

  onPasswordInput(event) {
    this.setData({
      'form.password': event.detail.value,
    })
  },

  handleSubmit() {
    const payload = {
      username: (this.data.form.username || '').trim(),
      password: this.data.form.password || '',
    }

    if (!payload.username || !payload.password) {
      wx.showToast({
        title: '请输入账号和密码',
        icon: 'none',
      })
      return
    }

    this.setData({ submitting: true })

    api.login(payload).then((result) => {
      const accessError = getMiniappAccessError(result.user)
      if (accessError) {
        wx.showToast({
          title: accessError,
          icon: 'none',
        })
        return
      }

      saveLoginSession(result)
      const app = getApp()
      app.hydrateSession()
      wx.showToast({
        title: '登录成功',
        icon: 'success',
      })
      wx.switchTab({
        url: '/pages/home/index/index',
      })
    }).finally(() => {
      this.setData({ submitting: false })
    })
  },

  goRegister() {
    wx.navigateTo({
      url: '/pages/auth/register/index',
    })
  },
})
