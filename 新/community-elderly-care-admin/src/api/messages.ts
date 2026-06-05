import type {
  MessageConversationView,
  MessageReplyRequest,
  MessageSendRequest,
  MessageUnreadSummaryView,
  MessageView,
} from '@/types/models'
import { request } from './client'

export function getMessageConversations(params?: { unreadOnly?: boolean }) {
  return request<MessageConversationView[]>({
    url: '/message/conversations',
    method: 'get',
    params,
  })
}

export function getConversationMessages(targetUserId: number, params?: { limit?: number }) {
  return request<MessageView[]>({
    url: `/message/conversation/${targetUserId}`,
    method: 'get',
    params,
  })
}

export function markMessageAsRead(id: number) {
  return request<MessageView>({
    url: `/message/${id}/read`,
    method: 'put',
  })
}

export function getUnreadSummary() {
  return request<MessageUnreadSummaryView>({
    url: '/message/unread-summary',
    method: 'get',
  })
}

export function sendMessage(data: MessageSendRequest) {
  return request<MessageView>({
    url: '/message/send',
    method: 'post',
    data,
  })
}

export function replyMessage(id: number, data: MessageReplyRequest) {
  return request<MessageView>({
    url: `/message/${id}/reply`,
    method: 'post',
    data,
  })
}
