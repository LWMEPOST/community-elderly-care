const api = require('../../../utils/api')
const { getLabel, SERVICE_STATUS_MAP, USER_TYPE_MAP } = require('../../../utils/dicts')
const { formatCurrency, formatDateTime } = require('../../../utils/format')
const { requireUser } = require('../../../utils/session')

function decorateItems(items) {
  return (items || []).map((item) => Object.assign({}, item, {
    priceText: formatCurrency(item.price, '待定'),
    durationText: item.duration ? `${item.duration} 分钟` : '时长待定',
  }))
}

function decorateOrders(list) {
  return (list || []).map((item) => Object.assign({}, item, {
    statusLabel: getLabel(SERVICE_STATUS_MAP, item.status),
    appointmentText: formatDateTime(item.appointmentTime, '待安排'),
  }))
}

Page({
  data: {
    currentUser: null,
    roleLabel: '',
    currentTab: 'catalog',
    categories: [],
    categoryIndex: 0,
    items: [],
    orders: [],
    loading: false,
  },

  onShow() {
    const user = requireUser(this)
    if (!user) {
      return
    }

    this.setData({
      roleLabel: getLabel(USER_TYPE_MAP, user.userType),
      currentTab: user.userType === 4 ? 'orders' : 'catalog',
    })

    this.loadPage()
  },

  loadPage() {
    this.setData({ loading: true })

    Promise.all([
      api.fetchServiceCategories(),
      api.fetchServiceOrders({}),
    ]).then((results) => {
      const categories = results[0] || []
      const orders = decorateOrders(results[1] || [])
      this.setData({
        categories,
        orders,
      })

      if (categories.length) {
        const category = categories[this.data.categoryIndex] || categories[0]
        this.loadItems(category.id)
      }
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  loadItems(categoryId) {
    api.fetchServiceItems(categoryId).then((items) => {
      this.setData({
        items: decorateItems(items),
      })
    })
  },

  switchTab(event) {
    this.setData({
      currentTab: event.currentTarget.dataset.tab,
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

  openCreate() {
    wx.navigateTo({
      url: '/pages/service/create/index',
    })
  },

  openDetail(event) {
    wx.navigateTo({
      url: `/pages/service/detail/index?id=${event.currentTarget.dataset.id}`,
    })
  },

  handleOrderAction(event) {
    const id = event.currentTarget.dataset.id
    const action = event.currentTarget.dataset.action

    let task = null
    if (action === 'accept') {
      task = api.acceptServiceOrder(id)
    } else if (action === 'start') {
      task = api.startServiceOrder(id)
    } else if (action === 'complete') {
      task = api.completeServiceOrder(id)
    } else if (action === 'cancel') {
      task = api.cancelServiceOrder(id)
    }

    if (!task) {
      return
    }

    task.then(() => {
      wx.showToast({
        title: '操作成功',
        icon: 'success',
      })
      this.loadPage()
    })
  },
})
