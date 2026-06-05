const api = require('../../../utils/api')
const { getLabel, MESSAGE_TYPE_MAP, USER_TYPE_MAP } = require('../../../utils/dicts')
const { formatDateTime } = require('../../../utils/format')
const { requireUser } = require('../../../utils/session')

function decorateConversations(list) {
  return (list || []).map((item) => Object.assign({}, item, {
    displayName: item.counterpartUserName || `用户 #${item.counterpartUserId}`,
    counterpartRoleLabel: getLabel(USER_TYPE_MAP, item.counterpartUserType),
    lastMessageTimeText: formatDateTime(item.lastMessageTime),
    lastMessageTypeLabel: getLabel(MESSAGE_TYPE_MAP, item.lastMessageType),
  }))
}

Page({
  data: {
    currentUser: null,
    conversations: [],
    unreadSummary: null,
    loading: false,
  },

  onShow() {
    const user = requireUser(this)
    if (!user) {
      return
    }
    this.loadPage()
  },

  loadPage() {
    this.setData({ loading: true })
    Promise.all([
      api.fetchConversations({}),
      api.fetchUnreadSummary(),
    ]).then((results) => {
      this.setData({
        conversations: decorateConversations(results[0] || []),
        unreadSummary: results[1] || null,
      })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id
    const name = event.currentTarget.dataset.name || ''
    wx.navigateTo({
      url: `/pages/message/detail/index?id=${id}&name=${encodeURIComponent(name)}`,
    })
  },
})
