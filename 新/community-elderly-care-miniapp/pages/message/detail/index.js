const api = require('../../../utils/api')
const { getLabel, MESSAGE_STATUS_MAP, MESSAGE_TYPE_MAP } = require('../../../utils/dicts')
const { formatDateTime } = require('../../../utils/format')
const { requireUser } = require('../../../utils/session')

function decorateMessages(list) {
  return (list || []).map((item) => Object.assign({}, item, {
    messageTypeLabel: getLabel(MESSAGE_TYPE_MAP, item.messageType),
    statusLabel: getLabel(MESSAGE_STATUS_MAP, item.status),
    createTimeText: formatDateTime(item.createTime),
  }))
}

Page({
  data: {
    currentUser: null,
    targetUserId: '',
    targetName: '',
    messages: [],
    replyContent: '',
    loading: false,
    sending: false,
  },

  onLoad(options) {
    this.setData({
      targetUserId: options.id || '',
      targetName: decodeURIComponent(options.name || ''),
    })
  },

  onShow() {
    const user = requireUser(this)
    if (!user || !this.data.targetUserId) {
      return
    }
    this.loadMessages()
  },

  loadMessages() {
    this.setData({ loading: true })
    api.fetchConversationMessages(this.data.targetUserId, { limit: 100 }).then((messages) => {
      const decorated = decorateMessages(messages)
      this.setData({
        messages: decorated,
      })

      const unreadMessages = decorated.filter((item) => !item.fromCurrentUser && item.status === 0)
      if (unreadMessages.length) {
        Promise.all(unreadMessages.map((item) => api.markMessageRead(item.id))).then(() => {
          this.setData({
            messages: decorateMessages(decorated.map((item) => (
              unreadMessages.some((unread) => unread.id === item.id)
                ? Object.assign({}, item, { status: 1 })
                : item
            ))),
          })
        })
      }
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  onReplyInput(event) {
    this.setData({
      replyContent: event.detail.value,
    })
  },

  handleSend() {
    if (!this.data.replyContent) {
      wx.showToast({
        title: '请输入消息内容',
        icon: 'none',
      })
      return
    }

    const latestMessage = this.data.messages[this.data.messages.length - 1]
    const task = latestMessage
      ? api.replyMessage(latestMessage.id, { content: this.data.replyContent })
      : api.sendMessage({
          receiverId: Number(this.data.targetUserId),
          messageType: 1,
          content: this.data.replyContent,
        })

    this.setData({ sending: true })
    task.then(() => {
      this.setData({ replyContent: '' })
      wx.showToast({
        title: '发送成功',
        icon: 'success',
      })
      this.loadMessages()
    }).finally(() => {
      this.setData({ sending: false })
    })
  },
})
