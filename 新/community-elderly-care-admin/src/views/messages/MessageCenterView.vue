<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getConversationMessages,
  getMessageConversations,
  getUnreadSummary,
  markMessageAsRead,
  replyMessage,
  sendMessage,
} from '@/api/messages'
import { listUsers } from '@/api/users'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { messageStatusOptions, messageTypeOptions, userTypeOptions } from '@/constants/dicts'
import { useAuthStore } from '@/stores/auth'
import type {
  MessageConversationView,
  MessageUnreadSummaryView,
  MessageView,
  UserView,
} from '@/types/models'
import { formatDateTime } from '@/utils/format'

const authStore = useAuthStore()

const loadingConversations = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const unreadOnly = ref(false)
const conversations = ref<MessageConversationView[]>([])
const messages = ref<MessageView[]>([])
const unreadSummary = ref<MessageUnreadSummaryView | null>(null)
const users = ref<UserView[]>([])
const activeTargetUserId = ref<number | null>(null)
const composeVisible = ref(false)

const replyForm = reactive({
  content: '',
})

const composeForm = reactive({
  receiverId: undefined as number | undefined,
  messageType: 1,
  content: '',
})

const activeConversation = computed(() =>
  conversations.value.find((item) => item.counterpartUserId === activeTargetUserId.value) || null,
)

async function loadUnreadSummary() {
  unreadSummary.value = await getUnreadSummary()
}

async function loadConversationList(preserveSelection = false) {
  loadingConversations.value = true
  try {
    conversations.value = await getMessageConversations({
      unreadOnly: unreadOnly.value || undefined,
    })

    if (!conversations.value.length) {
      activeTargetUserId.value = null
      messages.value = []
      return
    }

    if (
      preserveSelection &&
      activeTargetUserId.value &&
      conversations.value.some((item) => item.counterpartUserId === activeTargetUserId.value)
    ) {
      return
    }

    activeTargetUserId.value = conversations.value[0].counterpartUserId
    await loadMessages(activeTargetUserId.value)
  } finally {
    loadingConversations.value = false
  }
}

async function markUnreadMessages() {
  const unreadMessages = messages.value.filter((item) => !item.fromCurrentUser && item.status === 0)
  if (!unreadMessages.length) {
    return
  }

  await Promise.all(unreadMessages.map((item) => markMessageAsRead(item.id)))
  messages.value = messages.value.map((item) =>
    unreadMessages.some((target) => target.id === item.id)
      ? { ...item, status: 1, statusText: '已读' }
      : item,
  )
  await Promise.all([loadUnreadSummary(), loadConversationList(true)])
}

async function loadMessages(targetUserId: number) {
  loadingMessages.value = true
  try {
    messages.value = await getConversationMessages(targetUserId, { limit: 100 })
    await markUnreadMessages()
  } finally {
    loadingMessages.value = false
  }
}

async function selectConversation(targetUserId: number) {
  activeTargetUserId.value = targetUserId
  await loadMessages(targetUserId)
}

async function handleReply() {
  if (!activeConversation.value || !replyForm.content.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  sending.value = true
  try {
    const latestMessage = messages.value[messages.value.length - 1]
    if (latestMessage) {
      await replyMessage(latestMessage.id, { content: replyForm.content.trim() })
    } else {
      await sendMessage({
        receiverId: activeConversation.value.counterpartUserId,
        messageType: 1,
        content: replyForm.content.trim(),
      })
    }

    replyForm.content = ''
    await Promise.all([loadMessages(activeConversation.value.counterpartUserId), loadConversationList(true), loadUnreadSummary()])
    ElMessage.success('消息已发送')
  } finally {
    sending.value = false
  }
}

async function openComposeDialog() {
  if (!users.value.length) {
    const list = await listUsers({ status: 1 })
    users.value = list.filter((item) => item.id !== authStore.user?.id && item.userType !== 3)
  }
  composeVisible.value = true
}

async function handleCompose() {
  if (!composeForm.receiverId || !composeForm.content.trim()) {
    ElMessage.warning('请完善收件人和消息内容')
    return
  }

  sending.value = true
  try {
    await sendMessage({
      receiverId: composeForm.receiverId,
      messageType: composeForm.messageType,
      content: composeForm.content.trim(),
    })

    composeVisible.value = false
    composeForm.receiverId = undefined
    composeForm.messageType = 1
    composeForm.content = ''

    await Promise.all([loadUnreadSummary(), loadConversationList()])
    ElMessage.success('新消息已发送')
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadUnreadSummary(), loadConversationList()])
})
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>消息中心</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          管理后台对接老人、家属和服务人员的消息沟通，跟进未读会话和重点提醒。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.16); color: #fff">
        <div>未读消息</div>
        <strong>{{ unreadSummary?.totalUnreadCount ?? 0 }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          未读会话 {{ unreadSummary?.unreadConversationCount ?? 0 }} 个
        </div>
      </div>
    </section>

    <section class="stat-grid">
      <article class="stat-card">
        <span class="subtle-text">未读消息总数</span>
        <strong>{{ unreadSummary?.totalUnreadCount ?? 0 }}</strong>
      </article>
      <article class="stat-card">
        <span class="subtle-text">未读会话数</span>
        <strong>{{ unreadSummary?.unreadConversationCount ?? 0 }}</strong>
      </article>
      <article class="stat-card">
        <span class="subtle-text">最近未读时间</span>
        <strong style="font-size: 20px">{{ formatDateTime(unreadSummary?.latestUnreadTime, '暂无') }}</strong>
      </article>
    </section>

    <section class="page-grid two-columns" style="grid-template-columns: minmax(320px, 0.8fr) minmax(0, 1.2fr)">
      <PanelCard title="会话列表" description="优先跟进未读会话与系统提醒。">
        <template #actions>
          <div class="toolbar-actions">
            <ElSwitch v-model="unreadOnly" inline-prompt active-text="未读" inactive-text="全部" @change="loadConversationList()" />
            <ElButton type="primary" @click="openComposeDialog">发起会话</ElButton>
          </div>
        </template>

        <div v-loading="loadingConversations" class="page-shell">
          <button
            v-for="item in conversations"
            :key="item.counterpartUserId"
            class="conversation-item"
            :class="{ 'conversation-item--active': item.counterpartUserId === activeTargetUserId }"
            @click="selectConversation(item.counterpartUserId)"
          >
            <div class="conversation-item__top">
              <strong>{{ item.counterpartUserName || `用户 #${item.counterpartUserId}` }}</strong>
              <span>{{ formatDateTime(item.lastMessageTime) }}</span>
            </div>
            <div class="conversation-item__meta">
              <StatusTag
                :value="item.counterpartUserType"
                :options="userTypeOptions"
                fallback="未知角色"
              />
              <ElBadge :value="item.unreadCount" :hidden="!item.unreadCount" />
            </div>
            <div class="conversation-item__summary">{{ item.lastMessageContent || '暂无消息' }}</div>
          </button>

          <div v-if="!conversations.length" class="empty-copy">
            当前没有匹配的会话
          </div>
        </div>
      </PanelCard>

      <PanelCard title="会话内容" description="查看消息内容并从当前会话直接回复。">
        <div v-if="activeConversation" class="page-shell">
          <div class="toolbar-row">
            <div>
              <strong style="font-size: 18px">
                {{ activeConversation.counterpartUserName || `用户 #${activeConversation.counterpartUserId}` }}
              </strong>
              <div class="subtle-text" style="margin-top: 6px">
                最近消息时间 {{ formatDateTime(activeConversation.lastMessageTime) }}
              </div>
            </div>
            <StatusTag
              :value="activeConversation.counterpartUserType"
              :options="userTypeOptions"
              fallback="未知角色"
            />
          </div>

          <div class="message-panel" v-loading="loadingMessages">
            <div v-for="item in messages" :key="item.id" class="page-shell" style="gap: 8px">
              <div
                class="chat-bubble"
                :class="item.fromCurrentUser ? 'outbound' : 'inbound'"
              >
                <div style="display: flex; justify-content: space-between; gap: 12px; margin-bottom: 8px">
                  <strong>{{ item.senderName || '未知发送者' }}</strong>
                  <span style="font-size: 12px; opacity: 0.78">{{ formatDateTime(item.createTime) }}</span>
                </div>
                <div>{{ item.content }}</div>
                <div style="margin-top: 10px; display: flex; gap: 8px; align-items: center">
                  <StatusTag :value="item.messageType" :options="messageTypeOptions" />
                  <StatusTag :value="item.status" :options="messageStatusOptions" />
                </div>
              </div>
            </div>

            <div v-if="!messages.length" class="empty-copy">
              当前会话还没有消息记录
            </div>
          </div>

          <div class="page-shell" style="gap: 12px">
            <ElInput
              v-model="replyForm.content"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-word-limit
              placeholder="输入回复内容"
            />
            <div class="toolbar-actions">
              <ElButton type="primary" :loading="sending" @click="handleReply">发送回复</ElButton>
            </div>
          </div>
        </div>

        <div v-else class="empty-copy">
          请先选择左侧会话
        </div>
      </PanelCard>
    </section>

    <ElDialog v-model="composeVisible" title="发起新会话" width="480px">
      <ElForm label-position="top">
        <ElFormItem label="收件人">
          <ElSelect v-model="composeForm.receiverId" filterable placeholder="请选择收件人">
            <ElOption
              v-for="item in users"
              :key="item.id"
              :label="`${item.realName || item.username}（${item.phone || '无手机号'}）`"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="消息类型">
          <ElSelect v-model="composeForm.messageType">
            <ElOption
              v-for="item in messageTypeOptions.filter((option) => option.value !== 4)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="消息内容">
          <ElInput
            v-model="composeForm.content"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
          />
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElButton @click="composeVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="sending" @click="handleCompose">发送消息</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.conversation-item {
  width: 100%;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(250, 252, 253, 0.9);
  text-align: left;
  cursor: pointer;
}

.conversation-item--active {
  border-color: rgba(28, 117, 188, 0.32);
  box-shadow: 0 16px 32px rgba(28, 117, 188, 0.12);
}

.conversation-item__top,
.conversation-item__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.conversation-item__top span,
.conversation-item__summary {
  color: var(--app-text-soft);
  font-size: 13px;
}

.conversation-item__summary {
  margin-top: 10px;
}

.message-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 520px;
  overflow: auto;
  padding-right: 6px;
}
</style>
