const api = require('../../../utils/api')
const { buildDateTimeString } = require('../../../utils/format')
const { requireUser } = require('../../../utils/session')

Page({
  data: {
    currentUser: null,
    recordTypes: [
      { label: '血压', value: 1 },
      { label: '血糖', value: 2 },
      { label: '心率', value: 3 },
      { label: '其他', value: 4 },
    ],
    recordTypeIndex: 0,
    bindings: [],
    bindingIndex: 0,
    date: '',
    time: '',
    systolicPressure: '',
    diastolicPressure: '',
    bloodSugar: '',
    heartRate: '',
    submitting: false,
  },

  onShow() {
    const user = requireUser(this)
    if (!user) {
      return
    }

    if (user.userType === 4) {
      wx.showToast({
        title: '当前角色不可录入健康',
        icon: 'none',
      })
      wx.navigateBack()
      return
    }

    this.initDateTime()
    if (user.userType === 2) {
      api.fetchBindings({ status: 1 }).then((bindings) => {
        this.setData({
          bindings: bindings || [],
        })
      }).catch(() => null)
    }
  },

  initDateTime() {
    const now = new Date()
    const month = `${now.getMonth() + 1}`.padStart(2, '0')
    const day = `${now.getDate()}`.padStart(2, '0')
    const hours = `${now.getHours()}`.padStart(2, '0')
    const minutes = `${now.getMinutes()}`.padStart(2, '0')

    this.setData({
      date: `${now.getFullYear()}-${month}-${day}`,
      time: `${hours}:${minutes}`,
    })
  },

  onRecordTypeChange(event) {
    this.setData({
      recordTypeIndex: Number(event.detail.value),
    })
  },

  onBindingChange(event) {
    this.setData({
      bindingIndex: Number(event.detail.value),
    })
  },

  onDateChange(event) {
    this.setData({ date: event.detail.value })
  },

  onTimeChange(event) {
    this.setData({ time: event.detail.value })
  },

  onFieldInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({
      [field]: event.detail.value,
    })
  },

  handleSubmit() {
    const recordType = this.data.recordTypes[this.data.recordTypeIndex].value
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

    const payload = {
      elderlyId,
      recordType,
      recordTime: buildDateTimeString(this.data.date, this.data.time),
    }

    if (recordType === 1) {
      payload.systolicPressure = Number(this.data.systolicPressure)
      payload.diastolicPressure = Number(this.data.diastolicPressure)
    } else if (recordType === 2) {
      payload.bloodSugar = Number(this.data.bloodSugar)
    } else if (recordType === 3) {
      payload.heartRate = Number(this.data.heartRate)
    }

    this.setData({ submitting: true })
    api.createHealthRecord(payload).then(() => {
      wx.showToast({
        title: '记录已保存',
        icon: 'success',
      })
      const pages = getCurrentPages()
      const previousPage = pages.length > 1 ? pages[pages.length - 2] : null

      if (previousPage && previousPage.route === 'pages/health/index/index') {
        wx.navigateBack()
        return
      }

      wx.switchTab({
        url: '/pages/health/index/index',
      })
    }).catch(() => null).finally(() => {
      this.setData({ submitting: false })
    })
  },
})
