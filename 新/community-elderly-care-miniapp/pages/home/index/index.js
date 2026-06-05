const api = require('../../../utils/api')
const { buildHomeActions, getLabel, SERVICE_STATUS_MAP, EMERGENCY_STATUS_MAP, USER_TYPE_MAP } = require('../../../utils/dicts')
const { requireUser } = require('../../../utils/session')
const { formatDateTime } = require('../../../utils/format')

function decorateInfo(list) {
  return (list || []).map((item) => Object.assign({}, item, {
    publishTimeText: formatDateTime(item.publishTime, '未发布'),
  }))
}

function decorateOrders(list) {
  return (list || []).map((item) => Object.assign({}, item, {
    statusLabel: getLabel(SERVICE_STATUS_MAP, item.status),
    appointmentText: formatDateTime(item.appointmentTime),
  }))
}

function decorateEmergency(list) {
  return (list || []).map((item) => Object.assign({}, item, {
    statusLabel: getLabel(EMERGENCY_STATUS_MAP, item.status),
    createTimeText: formatDateTime(item.createTime),
  }))
}

Page({
  data: {
    currentUser: null,
    roleLabel: '',
    actions: [],
    latestInfo: [],
    serviceOrders: [],
    emergencyList: [],
    unreadSummary: null,
    bindings: [],
    healthSummary: null,
    bindingHint: '',
    loading: false,
  },

  onShow() {
    const user = requireUser(this)
    if (!user) {
      return
    }
    this.loadHome()
  },

  loadHome() {
    const user = this.data.currentUser
    if (!user) {
      return
    }

    this.setData({
      loading: true,
      actions: buildHomeActions(user.userType),
      roleLabel: getLabel(USER_TYPE_MAP, user.userType),
      bindingHint: '',
    })

    const baseJobs = Promise.all([
      api.fetchInformationList({ limit: 4 }),
      api.fetchUnreadSummary(),
      api.fetchServiceOrders({}),
      api.fetchEmergencyList({ limit: 4 }),
    ])

    const bindingJob = user.userType === 2 ? api.fetchBindings({ status: 1 }) : Promise.resolve([])

    Promise.all([baseJobs, bindingJob]).then((results) => {
      const baseResults = results[0]
      const bindings = results[1] || []
      const targetBinding = bindings[0] || null

      this.setData({
        latestInfo: decorateInfo(baseResults[0] || []),
        unreadSummary: baseResults[1] || null,
        serviceOrders: decorateOrders((baseResults[2] || []).slice(0, 4)),
        emergencyList: decorateEmergency((baseResults[3] || []).slice(0, 4)),
        bindings,
        bindingHint: user.userType === 2 && !targetBinding ? '请先完成与老人的确认绑定，才能查看健康摘要。' : '',
      })

      if (user.userType === 4) {
        return null
      }

      if (user.userType === 2) {
        if (!targetBinding) {
          this.setData({
            healthSummary: null,
          })
          return null
        }

        return api.fetchHealthSummary({ elderlyId: targetBinding.elderlyId })
      }

      return api.fetchHealthSummary({})
    }).then((healthSummary) => {
      if (healthSummary !== undefined) {
        this.setData({
          healthSummary: healthSummary || null,
        })
      }
    }).finally(() => {
      this.setData({ loading: false })
    })
  },

  openInfoDetail(event) {
    wx.navigateTo({
      url: `/pages/info/detail/index?id=${event.currentTarget.dataset.id}`,
    })
  },

  openAction(event) {
    const url = event.currentTarget.dataset.url
    if (!url) {
      return
    }

    if (
      url === '/pages/service/index/index' ||
      url === '/pages/health/index/index' ||
      url === '/pages/message/index/index' ||
      url === '/pages/profile/index/index' ||
      url === '/pages/home/index/index'
    ) {
      wx.switchTab({ url })
      return
    }

    wx.navigateTo({ url })
  },
})
