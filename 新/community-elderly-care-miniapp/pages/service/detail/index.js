const api = require('../../../utils/api')
const { getLabel, SERVICE_STATUS_MAP, USER_TYPE_MAP } = require('../../../utils/dicts')
const { formatCurrency, formatDateTime } = require('../../../utils/format')
const { requireUser } = require('../../../utils/session')

function decorateDetail(detail) {
  if (!detail) {
    return null
  }
  return Object.assign({}, detail, {
    statusLabel: getLabel(SERVICE_STATUS_MAP, detail.status),
    appointmentText: formatDateTime(detail.appointmentTime, '待安排'),
    priceText: formatCurrency(detail.servicePrice, '待定'),
    roleUserLabel: getLabel(USER_TYPE_MAP, detail.userType),
  })
}

Page({
  data: {
    currentUser: null,
    detail: null,
    loading: false,
  },

  onLoad(options) {
    this.id = options.id
  },

  onShow() {
    const user = requireUser(this)
    if (!user || !this.id) {
      return
    }
    this.loadDetail()
  },

  loadDetail() {
    this.setData({ loading: true })
    api.fetchServiceOrderDetail(this.id).then((detail) => {
      this.setData({
        detail: decorateDetail(detail),
      })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  handleAction(event) {
    const action = event.currentTarget.dataset.action
    let task = null

    if (action === 'accept') {
      task = api.acceptServiceOrder(this.id)
    } else if (action === 'start') {
      task = api.startServiceOrder(this.id)
    } else if (action === 'complete') {
      task = api.completeServiceOrder(this.id)
    } else if (action === 'cancel') {
      task = api.cancelServiceOrder(this.id)
    }

    if (!task) {
      return
    }

    task.then(() => {
      wx.showToast({
        title: '操作成功',
        icon: 'success',
      })
      this.loadDetail()
    })
  },
})
