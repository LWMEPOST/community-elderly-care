const api = require('../../../utils/api')
const { buildDateTimeString } = require('../../../utils/format')
const { requireUser } = require('../../../utils/session')

Page({
  data: {
    currentUser: null,
    categories: [],
    categoryIndex: 0,
    items: [],
    itemIndex: 0,
    bindings: [],
    bindingIndex: 0,
    date: '',
    time: '09:00',
    serviceAddress: '',
    remark: '',
    submitting: false,
  },

  onShow() {
    const user = requireUser(this)
    if (!user) {
      return
    }

    if (user.userType === 4) {
      wx.showToast({
        title: '服务人员不可发起预约',
        icon: 'none',
      })
      wx.navigateBack()
      return
    }

    this.initDefaultDate()
    this.loadBaseData()
  },

  initDefaultDate() {
    const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000)
    const month = `${tomorrow.getMonth() + 1}`.padStart(2, '0')
    const day = `${tomorrow.getDate()}`.padStart(2, '0')
    this.setData({
      date: `${tomorrow.getFullYear()}-${month}-${day}`,
    })
  },

  loadBaseData() {
    Promise.all([
      api.fetchServiceCategories(),
      this.data.currentUser.userType === 2 ? api.fetchBindings({ status: 1 }) : Promise.resolve([]),
    ]).then((results) => {
      const categories = results[0] || []
      this.setData({
        categories,
        bindings: results[1] || [],
      })

      if (categories.length) {
        this.loadItems(categories[0].id)
      }
    })
  },

  loadItems(categoryId) {
    api.fetchServiceItems(categoryId).then((items) => {
      this.setData({
        items: items || [],
        itemIndex: 0,
      })
    })
  },

  onCategoryChange(event) {
    const categoryIndex = Number(event.detail.value)
    const category = this.data.categories[categoryIndex]
    this.setData({ categoryIndex })
    if (category) {
      this.loadItems(category.id)
    }
  },

  onItemChange(event) {
    this.setData({
      itemIndex: Number(event.detail.value),
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

  onDateChange(event) {
    this.setData({ date: event.detail.value })
  },

  onTimeChange(event) {
    this.setData({ time: event.detail.value })
  },

  handleSubmit() {
    const category = this.data.categories[this.data.categoryIndex]
    const item = this.data.items[this.data.itemIndex]

    if (!category || !item || !this.data.date || !this.data.time || !this.data.serviceAddress) {
      wx.showToast({
        title: '请完整填写预约信息',
        icon: 'none',
      })
      return
    }

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

    api.createServiceOrder({
      elderlyId,
      serviceItemId: item.id,
      appointmentTime: buildDateTimeString(this.data.date, this.data.time),
      serviceAddress: this.data.serviceAddress,
      remark: this.data.remark,
    }).then(() => {
      wx.showToast({
        title: '预约成功',
        icon: 'success',
      })
      wx.switchTab({
        url: '/pages/service/index/index',
      })
    }).finally(() => {
      this.setData({ submitting: false })
    })
  },
})
