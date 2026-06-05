const USER_TYPE_MAP = {
  1: '老人',
  2: '家属',
  3: '管理员',
  4: '服务人员',
}

const USER_STATUS_MAP = {
  0: '待审核 / 禁用',
  1: '启用',
}

const BINDING_STATUS_MAP = {
  0: '待确认',
  1: '已确认',
}

const SERVICE_STATUS_MAP = {
  1: '待接单',
  2: '已接单',
  3: '服务中',
  4: '已完成',
  5: '已取消',
}

const EMERGENCY_STATUS_MAP = {
  1: '待响应',
  2: '已响应',
  3: '已解决',
}

const EMERGENCY_HELP_TYPE_MAP = {
  1: '医疗求助',
  2: '摔倒求助',
  3: '其他求助',
}

const MESSAGE_STATUS_MAP = {
  0: '未读',
  1: '已读',
}

const MESSAGE_TYPE_MAP = {
  1: '留言',
  2: '反馈',
  3: '咨询',
  4: '系统提醒',
}

const INFORMATION_TYPE_MAP = {
  1: '政策',
  2: '活动',
  3: '通知',
  4: '动态',
}

const HEALTH_RECORD_TYPE_MAP = {
  1: '血压',
  2: '血糖',
  3: '心率',
  4: '其他',
}

const HEALTH_WARNING_LEVEL_MAP = {
  0: '正常',
  1: '低风险预警',
  2: '高风险预警',
}

function getLabel(map, value, fallback) {
  if (value === null || value === undefined) {
    return fallback || '--'
  }
  return map[value] || fallback || '--'
}

function buildHomeActions(userType) {
  if (userType === 1) {
    return [
      { label: '我要预约服务', desc: '快速预约上门照护与陪诊服务', icon: '服', tone: 'ocean', url: '/pages/service/create/index' },
      { label: '记录健康数据', desc: '录入血压、血糖和心率等指标', icon: '健', tone: 'mint', url: '/pages/health/create/index' },
      { label: '紧急求助', desc: '一键发起紧急帮助并同步家属', icon: '急', tone: 'rose', url: '/pages/emergency/create/index' },
      { label: '最新资讯', desc: '查看社区通知、活动和政策更新', icon: '讯', tone: 'gold', url: '/pages/info/list/index' },
    ]
  }

  if (userType === 2) {
    return [
      { label: '代老人预约', desc: '替已绑定老人预约社区上门服务', icon: '代', tone: 'ocean', url: '/pages/service/create/index' },
      { label: '绑定管理', desc: '查看绑定状态并维护家庭关系', icon: '绑', tone: 'violet', url: '/pages/profile/index/index' },
      { label: '求助进展', desc: '跟进老人求助状态和处置结果', icon: '援', tone: 'rose', url: '/pages/emergency/index/index' },
      { label: '资讯中心', desc: '查看通知、义诊和护理政策提醒', icon: '讯', tone: 'gold', url: '/pages/info/list/index' },
    ]
  }

  return [
    { label: '待接单任务', desc: '查看当前可接订单与服务安排', icon: '单', tone: 'ocean', url: '/pages/service/index/index' },
    { label: '求助工单', desc: '及时响应社区老人紧急求助', icon: '援', tone: 'rose', url: '/pages/emergency/index/index' },
    { label: '消息中心', desc: '接收管理员消息与系统提醒', icon: '聊', tone: 'mint', url: '/pages/message/index/index' },
    { label: '个人中心', desc: '维护账号资料与查看角色状态', icon: '我', tone: 'violet', url: '/pages/profile/index/index' },
  ]
}

module.exports = {
  USER_TYPE_MAP,
  USER_STATUS_MAP,
  BINDING_STATUS_MAP,
  SERVICE_STATUS_MAP,
  EMERGENCY_STATUS_MAP,
  EMERGENCY_HELP_TYPE_MAP,
  MESSAGE_STATUS_MAP,
  MESSAGE_TYPE_MAP,
  INFORMATION_TYPE_MAP,
  HEALTH_RECORD_TYPE_MAP,
  HEALTH_WARNING_LEVEL_MAP,
  getLabel,
  buildHomeActions,
}
