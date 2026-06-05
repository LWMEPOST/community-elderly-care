const api = require('../../../utils/api')
const { getLabel, EMERGENCY_STATUS_MAP, USER_TYPE_MAP } = require('../../../utils/dicts')
const { formatDateTime } = require('../../../utils/format')
const { requireUser } = require('../../../utils/session')

function decorateDetail(detail) {
  if (!detail) {
    return null
  }
  return Object.assign({}, detail, {
    statusLabel: getLabel(EMERGENCY_STATUS_MAP, detail.status),
    createTimeText: formatDateTime(detail.createTime),
    resolveTimeText: formatDateTime(detail.resolveTime, '未解决'),
  })
}

Page({
  data: {
    currentUser: null,
    roleLabel: '',
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
    this.setData({
      roleLabel: getLabel(USER_TYPE_MAP, user.userType),
    })
    this.loadDetail()
  },

  loadDetail() {
    this.setData({ loading: true })
    api.fetchEmergencyDetail(this.id).then((detail) => {
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

    if (action === 'respond') {
      task = api.respondEmergency(this.id, {})
    } else if (action === 'resolve') {
      task = api.resolveEmergency(this.id)
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
