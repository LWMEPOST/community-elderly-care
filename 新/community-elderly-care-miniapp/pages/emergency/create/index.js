const api = require('../../../utils/api')
const { requireUser } = require('../../../utils/session')

Page({
  data: {
    currentUser: null,
    helpTypes: [
      { label: '医疗求助', value: 1 },
      { label: '摔倒求助', value: 2 },
      { label: '其他求助', value: 3 },
    ],
    helpTypeIndex: 0,
    bindings: [],
    bindingIndex: 0,
    locationAddress: '',
    longitude: '',
    latitude: '',
    description: '',
    submitting: false,
  },

  onShow() {
    const user = requireUser(this)
    if (!user) {
      return
    }

    if (user.userType === 4) {
      wx.showToast({
        title: '服务人员不可发起求助',
        icon: 'none',
      })
      wx.navigateBack()
      return
    }

    if (user.userType === 2) {
      api.fetchBindings({ status: 1 }).then((bindings) => {
        this.setData({
          bindings: bindings || [],
        })
      })
    }
  },

  onHelpTypeChange(event) {
    this.setData({
      helpTypeIndex: Number(event.detail.value),
    })
  },

  onBindingChange(event) {
    this.setData({
      bindingIndex: Number(event.detail.value),
    })
  },

  onFieldInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({
      [field]: event.detail.value,
    })
  },

  handleSubmit() {
    let elderlyId = undefined

    if (this.data.currentUser.userType === 2) {
      const binding = this.data.bindings[this.data.bindingIndex]
      if (!binding) {
        wx.showToast({
          title: '请先确认绑定老人',
          icon: 'none',
        })
        return
      }
      elderlyId = binding.elderlyId
    }

    this.setData({ submitting: true })
    api.createEmergencyHelp({
      elderlyId,
      helpType: this.data.helpTypes[this.data.helpTypeIndex].value,
      locationAddress: this.data.locationAddress,
      longitude: this.data.longitude ? Number(this.data.longitude) : undefined,
      latitude: this.data.latitude ? Number(this.data.latitude) : undefined,
      description: this.data.description,
    }).then(() => {
      wx.showToast({
        title: '求助已发起',
        icon: 'success',
      })
      wx.redirectTo({
        url: '/pages/emergency/index/index',
      })
    }).finally(() => {
      this.setData({ submitting: false })
    })
  },
})
