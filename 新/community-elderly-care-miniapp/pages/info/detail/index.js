const api = require('../../../utils/api')
const { requireUser } = require('../../../utils/session')
const { formatDateTime } = require('../../../utils/format')

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
    api.fetchInformationDetail(this.id).then((detail) => {
      this.setData({
        detail: Object.assign({}, detail, {
          publishTimeText: formatDateTime(detail.publishTime, '未发布'),
        }),
      })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },
})
