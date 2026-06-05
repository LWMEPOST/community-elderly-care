<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { assignServiceOrder, cancelServiceOrder, getServiceOrderDetail } from '@/api/services'
import { listUsers } from '@/api/users'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { serviceOrderStatusOptions } from '@/constants/dicts'
import type { ServiceOrderView, UserView } from '@/types/models'
import { formatCurrency, formatDateTime } from '@/utils/format'

const props = defineProps<{
  id: string
}>()

const router = useRouter()

const loading = ref(false)
const assigning = ref(false)
const detail = ref<ServiceOrderView | null>(null)
const serviceUsers = ref<UserView[]>([])
const assignVisible = ref(false)

const assignForm = reactive({
  serviceUserId: undefined as number | undefined,
})

const numericId = computed(() => Number(props.id))
const stepIndex = computed(() => {
  switch (detail.value?.status) {
    case 1:
      return 0
    case 2:
      return 1
    case 3:
      return 2
    case 4:
      return 3
    case 5:
      return 3
    default:
      return 0
  }
})

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getServiceOrderDetail(numericId.value)
    assignForm.serviceUserId = detail.value.serviceUserId || undefined
  } finally {
    loading.value = false
  }
}

async function loadServiceUsers() {
  serviceUsers.value = await listUsers({ userType: 4, status: 1 })
}

async function handleAssign() {
  if (!detail.value || !assignForm.serviceUserId) {
    ElMessage.warning('请选择服务人员')
    return
  }

  assigning.value = true
  try {
    detail.value = await assignServiceOrder(detail.value.id, { serviceUserId: assignForm.serviceUserId })
    ElMessage.success('派单成功')
    assignVisible.value = false
  } finally {
    assigning.value = false
  }
}

async function handleCancel() {
  if (!detail.value) {
    return
  }

  detail.value = await cancelServiceOrder(detail.value.id)
  ElMessage.success('订单已取消')
}

onMounted(async () => {
  await Promise.all([loadDetail(), loadServiceUsers()])
})
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>服务订单详情</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          查看订单流转状态、服务信息和预约信息，必要时重新派单或取消订单。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.16); color: #fff">
        <div>当前状态</div>
        <strong>{{ detail?.statusText || '加载中' }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          订单号 {{ detail?.orderNo || '--' }}
        </div>
      </div>
    </section>

    <PanelCard title="订单状态" description="状态轨迹与当前调度动作。">
      <template #actions>
        <div class="toolbar-actions">
          <ElButton @click="router.push('/services')">返回列表</ElButton>
          <ElButton
            v-if="detail && detail.status !== 4 && detail.status !== 5"
            type="primary"
            @click="assignVisible = true"
          >
            调整派单
          </ElButton>
          <ElButton
            v-if="detail && detail.status !== 4 && detail.status !== 5"
            type="danger"
            @click="handleCancel"
          >
            取消订单
          </ElButton>
        </div>
      </template>

      <div v-loading="loading" class="page-shell">
        <ElSteps :active="stepIndex" finish-status="success" simple>
          <ElStep title="待接单" />
          <ElStep title="已接单" />
          <ElStep title="服务中" />
          <ElStep :title="detail?.status === 5 ? '已取消' : '已完成'" />
        </ElSteps>

        <div v-if="detail" class="detail-grid">
          <div class="value-block">
            <div class="muted-label">订单状态</div>
            <StatusTag :value="detail.status" :options="serviceOrderStatusOptions" />
          </div>
          <div class="value-block">
            <div class="muted-label">服务对象</div>
            <div>{{ detail.elderlyName || '--' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">家属</div>
            <div>{{ detail.familyName || '--' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">服务项目</div>
            <div>{{ detail.serviceItemName || '--' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">服务类别</div>
            <div>{{ detail.categoryName || '--' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">服务人员</div>
            <div>{{ detail.serviceUserName || '未分配' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">预约时间</div>
            <div>{{ formatDateTime(detail.appointmentTime) }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">服务时长</div>
            <div>{{ detail.serviceDuration ? `${detail.serviceDuration} 分钟` : '--' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">服务费用</div>
            <div>{{ formatCurrency(detail.servicePrice) }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">服务地址</div>
            <div>{{ detail.serviceAddress || '--' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">备注</div>
            <div>{{ detail.remark || '无备注' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">更新时间</div>
            <div>{{ formatDateTime(detail.updateTime) }}</div>
          </div>
        </div>
      </div>
    </PanelCard>

    <ElDialog v-model="assignVisible" title="调整派单" width="420px">
      <ElForm label-position="top">
        <ElFormItem label="服务人员">
          <ElSelect v-model="assignForm.serviceUserId" placeholder="请选择服务人员">
            <ElOption
              v-for="item in serviceUsers"
              :key="item.id"
              :label="`${item.realName || item.username}（${item.phone || '无手机号'}）`"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElButton @click="assignVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="assigning" @click="handleAssign">确认派单</ElButton>
      </template>
    </ElDialog>
  </div>
</template>
