<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getEmergencyList } from '@/api/emergency'
import { getHealthSummary, getHealthWarnings } from '@/api/health'
import { listManagedInformation } from '@/api/information'
import { getUnreadSummary } from '@/api/messages'
import { getServiceOrders } from '@/api/services'
import { listBindings, listUsers } from '@/api/users'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  emergencyStatusOptions,
  healthWarningLevelOptions,
  serviceOrderStatusOptions,
} from '@/constants/dicts'
import type {
  EmergencyHelpView,
  HealthRecordView,
  MessageUnreadSummaryView,
  ServiceOrderView,
  UserView,
} from '@/types/models'
import { formatDateTime, summarizeText } from '@/utils/format'

const loading = ref(false)
const pendingUsers = ref(0)
const pendingBindings = ref(0)
const informationDrafts = ref(0)
const pendingOrders = ref(0)
const activeOrders = ref(0)
const pendingEmergency = ref(0)
const summary = ref<MessageUnreadSummaryView | null>(null)
const highWarnings = ref<HealthRecordView[]>([])
const recentOrders = ref<ServiceOrderView[]>([])
const recentEmergency = ref<EmergencyHelpView[]>([])
const healthFocusUser = ref<UserView | null>(null)
const healthOverviewTitle = ref('重点老人健康巡检')
const healthAbnormalCount = ref(0)

function getUserDisplayName(user?: UserView | null) {
  return user?.realName || user?.username || '重点老人'
}

const healthOverviewDescription = computed(() =>
  healthFocusUser.value
    ? `当前默认巡检 ${getUserDisplayName(healthFocusUser.value)} 的高风险记录，后端暂不支持全站健康聚合。`
    : '暂无已启用老人账号，健康看板会在老人审核通过后显示。',
)

const healthWarningsEmptyText = computed(() =>
  healthFocusUser.value
    ? `${getUserDisplayName(healthFocusUser.value)} 暂无高风险预警`
    : '暂无可巡检的老人账号',
)

const cards = computed(() => [
  { label: '待审核用户', value: pendingUsers.value, accent: 'rgba(227, 139, 44, 0.16)' },
  { label: '待确认绑定', value: pendingBindings.value, accent: 'rgba(35, 179, 138, 0.16)' },
  { label: '资讯草稿', value: informationDrafts.value, accent: 'rgba(28, 117, 188, 0.16)' },
  { label: '待派单服务', value: pendingOrders.value, accent: 'rgba(207, 74, 74, 0.12)' },
  { label: '服务进行中', value: activeOrders.value, accent: 'rgba(20, 88, 143, 0.12)' },
  { label: '待响应求助', value: pendingEmergency.value, accent: 'rgba(227, 139, 44, 0.16)' },
  { label: '未读消息', value: summary.value?.totalUnreadCount ?? 0, accent: 'rgba(35, 179, 138, 0.12)' },
  { label: '重点老人异常数', value: healthAbnormalCount.value, accent: 'rgba(207, 74, 74, 0.12)' },
])

async function loadDashboard() {
  loading.value = true
  try {
    const [
      pendingUsersList,
      pendingBindingsList,
      draftInformationList,
      pendingServiceOrders,
      serviceInProgressOrders,
      pendingEmergencyList,
      unreadSummary,
      serviceOrders,
      emergencyList,
      enabledElderlyUsers,
    ] = await Promise.all([
      listUsers({ status: 0 }),
      listBindings({ status: 0 }),
      listManagedInformation({ status: 0, limit: 50 }),
      getServiceOrders({ status: 1 }),
      getServiceOrders({ status: 3 }),
      getEmergencyList({ status: 1, limit: 20 }),
      getUnreadSummary(),
      getServiceOrders(),
      getEmergencyList({ limit: 6 }),
      listUsers({ userType: 1, status: 1 }),
    ])

    pendingUsers.value = pendingUsersList.length
    pendingBindings.value = pendingBindingsList.length
    informationDrafts.value = draftInformationList.length
    pendingOrders.value = pendingServiceOrders.length
    activeOrders.value = serviceInProgressOrders.length
    pendingEmergency.value = pendingEmergencyList.length
    summary.value = unreadSummary
    recentOrders.value = serviceOrders.slice(0, 5)
    recentEmergency.value = emergencyList.slice(0, 5)

    const defaultHealthUser = enabledElderlyUsers[0] ?? null
    healthFocusUser.value = defaultHealthUser

    if (!defaultHealthUser) {
      highWarnings.value = []
      healthOverviewTitle.value = '重点老人健康巡检'
      healthAbnormalCount.value = 0
      return
    }

    const [warningSummary, warningList] = await Promise.all([
      getHealthSummary({ elderlyId: defaultHealthUser.id }),
      getHealthWarnings({ elderlyId: defaultHealthUser.id, warningLevel: 2, limit: 6 }),
    ])

    highWarnings.value = warningList
    healthOverviewTitle.value = `${warningSummary.elderlyName || getUserDisplayName(defaultHealthUser)}健康巡检`
    healthAbnormalCount.value = warningSummary.abnormalCount
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>今日运营态势</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          管理端首版优先围绕审核、调度、应急和沟通闭环展开。这里聚合关键待办，帮助值班管理员快速进入操作节点。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.18); color: #fff">
        <div>最新未读提醒时间</div>
        <strong>{{ formatDateTime(summary?.latestUnreadTime, '暂无未读消息') }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          {{ summary?.unreadConversationCount ?? 0 }} 个会话待跟进
        </div>
      </div>
    </section>

    <ElSkeleton :loading="loading" animated :rows="6">
      <template #template>
        <div class="stat-grid">
          <ElSkeletonItem
            v-for="index in 8"
            :key="index"
            variant="rect"
            style="height: 120px; border-radius: 20px"
          />
        </div>
      </template>

      <template #default>
        <section class="stat-grid">
          <article
            v-for="card in cards"
            :key="card.label"
            class="stat-card"
            :style="{ background: `linear-gradient(180deg, rgba(255,255,255,0.94), ${card.accent})` }"
          >
            <span class="subtle-text">{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </article>
        </section>

        <section class="page-grid two-columns">
          <PanelCard
            title="近期服务订单"
            description="聚焦待派单、服务中和近期创建的服务需求。"
          >
            <ElTable :data="recentOrders" stripe>
              <ElTableColumn prop="orderNo" label="订单号" min-width="150" />
              <ElTableColumn prop="elderlyName" label="服务对象" min-width="120" />
              <ElTableColumn prop="serviceItemName" label="服务项目" min-width="160" />
              <ElTableColumn label="状态" width="120">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :options="serviceOrderStatusOptions" />
                </template>
              </ElTableColumn>
              <ElTableColumn label="预约时间" min-width="160">
                <template #default="{ row }">
                  {{ formatDateTime(row.appointmentTime) }}
                </template>
              </ElTableColumn>
            </ElTable>
          </PanelCard>

          <PanelCard
            title="近期求助工单"
            description="优先关注待响应和已响应未解决的求助。"
          >
            <ElTable :data="recentEmergency" stripe>
              <ElTableColumn prop="elderlyName" label="老人" min-width="120" />
              <ElTableColumn prop="helpTypeText" label="类型" width="110" />
              <ElTableColumn label="状态" width="120">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :options="emergencyStatusOptions" />
                </template>
              </ElTableColumn>
              <ElTableColumn label="位置" min-width="180">
                <template #default="{ row }">
                  {{ summarizeText(row.locationAddress, 22) || '未填写位置' }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="发起时间" min-width="160">
                <template #default="{ row }">
                  {{ formatDateTime(row.createTime) }}
                </template>
              </ElTableColumn>
            </ElTable>
          </PanelCard>
        </section>

        <section class="page-grid two-columns">
          <PanelCard :title="healthOverviewTitle" :description="healthOverviewDescription">
            <ElTable :data="highWarnings" stripe :empty-text="healthWarningsEmptyText">
              <ElTableColumn prop="elderlyName" label="老人" min-width="120" />
              <ElTableColumn prop="recordTypeText" label="记录类型" width="110" />
              <ElTableColumn label="预警等级" width="120">
                <template #default="{ row }">
                  <StatusTag :value="row.warningLevel" :options="healthWarningLevelOptions" />
                </template>
              </ElTableColumn>
              <ElTableColumn label="记录时间" min-width="160">
                <template #default="{ row }">
                  {{ formatDateTime(row.recordTime) }}
                </template>
              </ElTableColumn>
              <ElTableColumn prop="advice" label="建议" min-width="200" />
            </ElTable>
          </PanelCard>

          <PanelCard title="消息与待办提醒" description="帮助管理员快速识别需要跟进的沟通事项。">
            <div class="detail-grid">
              <div class="value-block">
                <div class="muted-label">未读消息总数</div>
                <strong style="font-size: 30px">{{ summary?.totalUnreadCount ?? 0 }}</strong>
              </div>
              <div class="value-block">
                <div class="muted-label">未读会话数</div>
                <strong style="font-size: 30px">{{ summary?.unreadConversationCount ?? 0 }}</strong>
              </div>
              <div class="value-block">
                <div class="muted-label">高风险预警记录</div>
                <strong style="font-size: 30px">{{ highWarnings.length }}</strong>
              </div>
              <div class="value-block">
                <div class="muted-label">待响应求助</div>
                <strong style="font-size: 30px">{{ pendingEmergency }}</strong>
              </div>
            </div>
          </PanelCard>
        </section>
      </template>
    </ElSkeleton>
  </div>
</template>
