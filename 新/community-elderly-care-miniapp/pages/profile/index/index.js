const api = require('../../../utils/api')
const { getLabel, BINDING_STATUS_MAP, USER_STATUS_MAP, USER_TYPE_MAP } = require('../../../utils/dicts')
const { syncUserProfile } = require('../../../utils/auth')
const { requireUser } = require('../../../utils/session')

function decorateBindings(list) {
  return (list || []).map((item) => Object.assign({}, item, {
    statusLabel: getLabel(BINDING_STATUS_MAP, item.status),
  }))
}

Page({
  data: {
    currentUser: null,
    roleLabel: '',
    statusLabel: '',
    userForm: {
      realName: '',
      phone: '',
      address: '',
      emergencyContact: '',
      emergencyPhone: '',
    },
    elderlyForm: {
      age: '',
      gender: '',
      healthStatus: '',
      medicalHistory: '',
      longitude: '',
      latitude: '',
    },
    bindings: [],
    bindingForm: {
      elderlyId: '',
      relation: '',
    },
    savingUser: false,
    savingElderly: false,
    savingBinding: false,
  },

  onShow() {
    const user = requireUser(this)
    if (!user) {
      return
    }
    this.loadPage()
  },

  loadPage() {
    api.fetchCurrentUser().then((user) => {
      syncUserProfile(user)
      this.setData({
        currentUser: user,
        roleLabel: getLabel(USER_TYPE_MAP, user.userType),
        statusLabel: getLabel(USER_STATUS_MAP, user.status),
        userForm: {
          realName: user.realName || '',
          phone: user.phone || '',
          address: user.address || '',
          emergencyContact: user.emergencyContact || '',
          emergencyPhone: user.emergencyPhone || '',
        },
      })

      if (user.userType === 1) {
        return Promise.all([
          api.fetchElderlyProfile({}),
          api.fetchBindings({}),
        ]).then((results) => {
          const profile = results[0] || {}
          this.setData({
            elderlyForm: {
              age: profile.age || '',
              gender: profile.gender === 0 || profile.gender === 1 ? `${profile.gender}` : '',
              healthStatus: profile.healthStatus || '',
              medicalHistory: profile.medicalHistory || '',
              longitude: profile.longitude || '',
              latitude: profile.latitude || '',
            },
            bindings: decorateBindings(results[1] || []),
          })
        })
      }

      if (user.userType === 2) {
        return api.fetchBindings({}).then((bindings) => {
          this.setData({
            bindings: decorateBindings(bindings),
          })
        })
      }

      return null
    })
  },

  onUserInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({
      [`userForm.${field}`]: event.detail.value,
    })
  },

  onElderlyInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({
      [`elderlyForm.${field}`]: event.detail.value,
    })
  },

  onBindingInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({
      [`bindingForm.${field}`]: event.detail.value,
    })
  },

  handleSaveUser() {
    this.setData({ savingUser: true })
    api.updateCurrentUser(this.data.userForm).then(() => {
      wx.showToast({
        title: '资料已更新',
        icon: 'success',
      })
      this.loadPage()
    }).finally(() => {
      this.setData({ savingUser: false })
    })
  },

  handleSaveElderly() {
    this.setData({ savingElderly: true })
    api.saveElderlyProfile({
      age: this.data.elderlyForm.age ? Number(this.data.elderlyForm.age) : undefined,
      gender: this.data.elderlyForm.gender === '' ? undefined : Number(this.data.elderlyForm.gender),
      healthStatus: this.data.elderlyForm.healthStatus,
      medicalHistory: this.data.elderlyForm.medicalHistory,
      longitude: this.data.elderlyForm.longitude ? Number(this.data.elderlyForm.longitude) : undefined,
      latitude: this.data.elderlyForm.latitude ? Number(this.data.elderlyForm.latitude) : undefined,
    }).then(() => {
      wx.showToast({
        title: '老人资料已保存',
        icon: 'success',
      })
      this.loadPage()
    }).finally(() => {
      this.setData({ savingElderly: false })
    })
  },

  handleCreateBinding() {
    if (!this.data.bindingForm.elderlyId || !this.data.bindingForm.relation) {
      wx.showToast({
        title: '请输入老人ID和关系',
        icon: 'none',
      })
      return
    }

    this.setData({ savingBinding: true })
    api.createBinding({
      elderlyId: Number(this.data.bindingForm.elderlyId),
      relation: this.data.bindingForm.relation,
    }).then(() => {
      wx.showToast({
        title: '申请已提交',
        icon: 'success',
      })
      this.setData({
        bindingForm: {
          elderlyId: '',
          relation: '',
        },
      })
      this.loadPage()
    }).finally(() => {
      this.setData({ savingBinding: false })
    })
  },

  handleConfirmBinding(event) {
    const id = event.currentTarget.dataset.id
    api.confirmBinding(id).then(() => {
      wx.showToast({
        title: '绑定已确认',
        icon: 'success',
      })
      this.loadPage()
    })
  },

  handleLogout() {
    getApp().logout()
  },
})
