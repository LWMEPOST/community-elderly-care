const api = require('../../../utils/api')
const { getLabel, INFORMATION_TYPE_MAP } = require('../../../utils/dicts')
const { requireUser } = require('../../../utils/session')
const { formatDateTime } = require('../../../utils/format')

function decorateInfo(list) {
  return (list || []).map((item) => Object.assign({}, item, {
    typeLabel: getLabel(INFORMATION_TYPE_MAP, item.infoType),
    publishTimeText: formatDateTime(item.publishTime, '未发布'),
  }))
}

Page({
  data: {
    currentUser: null,
    list: [],
    loading: false,
    keyword: '',
    infoTypes: [
      { value: 0, label: '全部' },
      { value: 1, label: '政策' },
      { value: 2, label: '活动' },
      { value: 3, label: '通知' },
      { value: 4, label: '动态' },
    ],
    currentType: 0,
  },

  onShow() {
    const user = requireUser(this)
    if (!user) {
      return
    }
    this.loadList()
  },

  onKeywordInput(event) {
    this.setData({
      keyword: event.detail.value,
    })
  },

  changeType(event) {
    this.setData({
      currentType: Number(event.currentTarget.dataset.type),
    })
    this.loadList()
  },

  loadList() {
    this.setData({ loading: true })
    api.fetchInformationList({
      infoType: this.data.currentType || undefined,
      keyword: this.data.keyword || undefined,
      limit: 50,
    }).then((list) => {
      this.setData({ list: decorateInfo(list || []) })
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  openDetail(event) {
    wx.navigateTo({
      url: `/pages/info/detail/index?id=${event.currentTarget.dataset.id}`,
    })
  },
})
