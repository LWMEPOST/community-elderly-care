const api = require('../../../utils/api')

Page({
  data: {
    roleOptions: [
      { label: '老人', value: 1 },
      { label: '家属', value: 2 },
      { label: '服务人员', value: 4 },
    ],
    roleIndex: 0,
    submitting: false,
    form: {
      username: '',
      password: '',
      realName: '',
      phone: '',
      address: '',
      emergencyContact: '',
      emergencyPhone: '',
    },
  },

  updateField(field, value) {
    this.setData({
      [`form.${field}`]: value,
    })
  },

  onRoleChange(event) {
    this.setData({
      roleIndex: Number(event.detail.value),
    })
  },

  onInput(event) {
    const field = event.currentTarget.dataset.field
    const rawValue = event.detail.value || ''
    let value = rawValue

    if (field === 'username' || field === 'phone' || field === 'emergencyPhone') {
      value = rawValue.replace(/\s+/g, '')
    } else {
      value = rawValue.replace(/^\s+/, '')
    }

    this.updateField(field, value)
  },

  handleSubmit() {
    const { form, roleOptions, roleIndex } = this.data
    const payload = Object.assign({}, form, {
      username: (form.username || '').trim(),
      password: form.password || '',
      realName: (form.realName || '').trim(),
      phone: (form.phone || '').trim(),
      address: (form.address || '').trim(),
      emergencyContact: (form.emergencyContact || '').trim(),
      emergencyPhone: (form.emergencyPhone || '').trim(),
      userType: roleOptions[roleIndex].value,
    })

    if (!payload.username || !payload.password || !payload.realName || !payload.phone) {
      wx.showToast({
        title: '请完整填写必填项',
        icon: 'none',
      })
      return
    }

    this.setData({ submitting: true })

    api.register(payload).then(() => {
      wx.showModal({
        title: '注册成功',
        content: '注册申请已提交，请等待管理员审核。',
        showCancel: false,
        success: () => {
          wx.navigateBack()
        },
      })
    }).finally(() => {
      this.setData({ submitting: false })
    })
  },

  goLogin() {
    wx.navigateBack()
  },
})
