const api = require('../../../utils/api')
const { getLabel, EMERGENCY_STATUS_MAP, USER_TYPE_MAP } = require('../../../utils/dicts')
const { formatDateTime } = require('../../../utils/format')
const { requireUser } = require('../../../utils/session')

function decorateList(list) {
  return (list || []).map((item) => Object.assign({}, item, {
    statusLabel: getLabel(EMERGENCY_STATUS_MAP, item.status),
    createTimeText: formatDateTime(item.createTime),
  }))
}

Page({
  data: {
    currentUser: null,
    roleLabel: '',
    list: [],
    loading: false,
  },

  onShow() {
    const user = requireUser(this)
    if (!user) {
      return
    }
    this.setData({
      roleLabel: getLabel(USER_TYPE_MAP, user.userType),
    })
    this.loadList()
  },

  loadList() {
    this.setData({ loading: true })
    api.fetchEmergencyList({ limit: 30 }).then((list) => {
      this.setData({
        list: decorateList(list),
      })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  openCreate() {
    wx.navigateTo({
      url: '/pages/emergency/create/index',
    })
  },

  openDetail(event) {
    wx.navigateTo({
      url: `/pages/emergency/detail/index?id=${event.currentTarget.dataset.id}`,
    })
  },

  handleAction(event) {
    const id = event.currentTarget.dataset.id
    const action = event.currentTarget.dataset.action

    let task = null
    if (action === 'respond') {
      task = api.respondEmergency(id, {})
    } else if (action === 'resolve') {
      task = api.resolveEmergency(id)
    }

    if (!task) {
      return
    }

    task.then(() => {
      wx.showToast({
        title: '操作成功',
        icon: 'success',
      })
      this.loadList()
    })
  },
})
