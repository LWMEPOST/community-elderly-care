const { request } = require('./request')

module.exports = {
  login(data) {
    return request({
      url: '/user/login',
      method: 'POST',
      data,
      auth: false,
    })
  },

  register(data) {
    return request({
      url: '/user/register',
      method: 'POST',
      data,
      auth: false,
    })
  },

  fetchCurrentUser() {
    return request({
      url: '/user/me',
    })
  },

  updateCurrentUser(data) {
    return request({
      url: '/user/update',
      method: 'PUT',
      data,
    })
  },

  fetchElderlyProfile(data) {
    return request({
      url: '/user/elderly-profile',
      data,
    })
  },

  saveElderlyProfile(data) {
    return request({
      url: '/user/elderly-profile',
      method: 'PUT',
      data,
    })
  },

  fetchBindings(data) {
    return request({
      url: '/user/bindings',
      data,
    })
  },

  createBinding(data) {
    return request({
      url: '/user/binding',
      method: 'POST',
      data,
    })
  },

  confirmBinding(id) {
    return request({
      url: `/user/binding/${id}/confirm`,
      method: 'PUT',
    })
  },

  fetchInformationList(data) {
    return request({
      url: '/information/list',
      data,
    })
  },

  fetchInformationDetail(id) {
    return request({
      url: `/information/${id}`,
    })
  },

  fetchServiceCategories() {
    return request({
      url: '/service/categories',
    })
  },

  fetchServiceItems(categoryId) {
    return request({
      url: `/service/items/${categoryId}`,
    })
  },

  createServiceOrder(data) {
    return request({
      url: '/service/order',
      method: 'POST',
      data,
    })
  },

  fetchServiceOrders(data) {
    return request({
      url: '/service/orders',
      data,
    })
  },

  fetchServiceOrderDetail(id) {
    return request({
      url: `/service/order/${id}`,
    })
  },

  acceptServiceOrder(id) {
    return request({
      url: `/service/order/${id}/accept`,
      method: 'PUT',
    })
  },

  startServiceOrder(id) {
    return request({
      url: `/service/order/${id}/start`,
      method: 'PUT',
    })
  },

  completeServiceOrder(id) {
    return request({
      url: `/service/order/${id}/complete`,
      method: 'PUT',
    })
  },

  cancelServiceOrder(id) {
    return request({
      url: `/service/order/${id}/cancel`,
      method: 'PUT',
    })
  },

  createHealthRecord(data) {
    return request({
      url: '/health/record',
      method: 'POST',
      data,
    })
  },

  fetchHealthRecords(data) {
    return request({
      url: '/health/records',
      data,
    })
  },

  fetchHealthWarnings(data) {
    return request({
      url: '/health/warnings',
      data,
    })
  },

  fetchHealthSummary(data) {
    return request({
      url: '/health/summary',
      data,
    })
  },

  createEmergencyHelp(data) {
    return request({
      url: '/emergency/help',
      method: 'POST',
      data,
    })
  },

  fetchEmergencyList(data) {
    return request({
      url: '/emergency/list',
      data,
    })
  },

  fetchEmergencyDetail(id) {
    return request({
      url: `/emergency/${id}`,
    })
  },

  respondEmergency(id, data) {
    return request({
      url: `/emergency/${id}/response`,
      method: 'PUT',
      data,
    })
  },

  resolveEmergency(id) {
    return request({
      url: `/emergency/${id}/resolve`,
      method: 'PUT',
    })
  },

  fetchConversations(data) {
    return request({
      url: '/message/conversations',
      data,
    })
  },

  fetchConversationMessages(targetUserId, data) {
    return request({
      url: `/message/conversation/${targetUserId}`,
      data,
    })
  },

  sendMessage(data) {
    return request({
      url: '/message/send',
      method: 'POST',
      data,
    })
  },

  replyMessage(id, data) {
    return request({
      url: `/message/${id}/reply`,
      method: 'POST',
      data,
    })
  },

  markMessageRead(id) {
    return request({
      url: `/message/${id}/read`,
      method: 'PUT',
    })
  },

  fetchUnreadSummary() {
    return request({
      url: '/message/unread-summary',
    })
  },
}
