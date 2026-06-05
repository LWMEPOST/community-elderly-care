const api = require('../../../utils/api')
const { getLabel, HEALTH_RECORD_TYPE_MAP, HEALTH_WARNING_LEVEL_MAP, SERVICE_STATUS_MAP, EMERGENCY_STATUS_MAP, USER_TYPE_MAP } = require('../../../utils/dicts')
const { buildHealthMetric, formatDateTime } = require('../../../utils/format')
const { requireUser } = require('../../../utils/session')

function decorateHealthRecords(list) {
  return (list || []).map((item) => Object.assign({}, item, {
    recordTypeLabel: getLabel(HEALTH_RECORD_TYPE_MAP, item.recordType),
    warningLabel: getLabel(HEALTH_WARNING_LEVEL_MAP, item.warningLevel),
    metricText: buildHealthMetric(item),
    recordTimeText: formatDateTime(item.recordTime),
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
    summary: null,
    warnings: [],
    records: [],
    bindings: [],
    workerOrders: [],
    workerEmergency: [],
    targetBindingIndex: 0,
    bindingHint: '',
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
    this.loadPage()
  },

  loadPage() {
    const user = this.data.currentUser
    if (!user) {
      return
    }

    this.setData({ loading: true })

    if (user.userType === 4) {
      Promise.all([
        api.fetchServiceOrders({}),
        api.fetchEmergencyList({ limit: 20 }),
      ]).then((results) => {
        this.setData({
          workerOrders: decorateOrders(results[0] || []),
          workerEmergency: decorateEmergency(results[1] || []),
        })
      }).catch(() => null).finally(() => {
        this.setData({ loading: false })
      })
      return
    }

    const bindingTask = user.userType === 2 ? api.fetchBindings({ status: 1 }) : Promise.resolve([])
    bindingTask.then((bindings) => {
      const list = bindings || []
      this.setData({
        bindings: list,
        bindingHint: '',
      })
      const targetBinding = list[this.data.targetBindingIndex]

      if (user.userType === 2 && !targetBinding) {
        this.setData({
          summary: null,
          warnings: [],
          records: [],
          bindingHint: '请先在个人中心完成与老人的确认绑定，再查看健康数据。',
        })
        return null
      }

      const elderlyId = user.userType === 2 ? targetBinding.elderlyId : undefined

      return Promise.all([
        api.fetchHealthSummary({ elderlyId }),
        api.fetchHealthWarnings({ elderlyId, limit: 10 }),
        api.fetchHealthRecords({ elderlyId, limit: 20 }),
      ])
    }).then((results) => {
      if (!results) {
        return
      }

      this.setData({
        summary: results[0] || null,
        warnings: decorateHealthRecords(results[1] || []),
        records: decorateHealthRecords(results[2] || []),
      })
    }).catch(() => null).finally(() => {
      this.setData({ loading: false })
    })
  },

  onBindingChange(event) {
    this.setData({
      targetBindingIndex: Number(event.detail.value),
    })
    this.loadPage()
  },

  openCreate() {
    wx.navigateTo({
      url: '/pages/health/create/index',
    })
  },
})
