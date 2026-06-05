<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { assignServiceOrder, cancelServiceOrder, getServiceOrders } from '@/api/services'
import { listUsers } from '@/api/users'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { serviceOrderStatusOptions } from '@/constants/dicts'
import type { ServiceOrderView, UserView } from '@/types/models'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const assigning = ref(false)
const orders = ref<ServiceOrderView[]>([])
const serviceUsers = ref<UserView[]>([])
const assignVisible = ref(false)
const currentOrder = ref<ServiceOrderView | null>(null)

const filters = reactive({
  status: undefined as number | undefined,
  keyword: '',
})

const assignForm = reactive({
  serviceUserId: undefined as number | undefined,
})

const cards = computed(() => [
  { label: '全部订单', value: orders.value.length },
  { label: '待接单', value: orders.value.filter((item) => item.status === 1).length },
  { label: '服务中', value: orders.value.filter((item) => item.status === 3).length },
  { label: '已完成', value: orders.value.filter((item) => item.status === 4).length },
])

async function loadOrders() {
  loading.value = true
  try {
    orders.value = await getServiceOrders({
      status: filters.status,
      keyword: filters.keyword || undefined,
    })
  } finally {
    loading.value = false
  }
}

async function loadServiceUsers() {
  serviceUsers.value = await listUsers({ userType: 4, status: 1 })
}

function openAssignDialog(row: ServiceOrderView) {
  currentOrder.value = row
  assignForm.serviceUserId = row.serviceUserId || undefined
  assignVisible.value = true
}

async function confirmAssign() {
  if (!currentOrder.value || !assignForm.serviceUserId) {
    ElMessage.warning('请选择服务人员')
    return
  }

  assigning.value = true
  try {
    await assignServiceOrder(currentOrder.value.id, { serviceUserId: assignForm.serviceUserId })
    ElMessage.success('派单成功')
    assignVisible.value = false
    await loadOrders()
  } finally {
    assigning.value = false
  }
}

async function handleCancel(row: ServiceOrderView) {
  await cancelServiceOrder(row.id)
  ElMessage.success('订单已取消')
  await loadOrders()
}

function resetFilters() {
  Object.assign(filters, {
    status: undefined,
    keyword: '',
  })
  loadOrders()
}

onMounted(async () => {
  await Promise.all([loadOrders(), loadServiceUsers()])
})
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>服务调度中心</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          面向管理员完成服务订单的查询、派单、取消和详情跟进，优先处理待接单和超时服务任务。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.16); color: #fff">
        <div>待派单订单</div>
        <strong>{{ cards[1].value }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          当前服务中 {{ cards[2].value }} 单
        </div>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="card in cards" :key="card.label" class="stat-card">
        <span class="subtle-text">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <PanelCard title="服务订单列表" description="支持按照状态和关键字筛选，并快速打开详情或执行派单。">
      <div class="page-shell">
        <div class="filter-grid">
          <ElSelect v-model="filters.status" clearable placeholder="订单状态">
            <ElOption
              v-for="item in serviceOrderStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
          <ElInput v-model="filters.keyword" clearable placeholder="订单号 / 服务项目 / 姓名" />
          <div class="toolbar-actions">
            <ElButton type="primary" :loading="loading" @click="loadOrders">查询</ElButton>
            <ElButton @click="resetFilters">重置</ElButton>
          </div>
        </div>

        <ElTable :data="orders" stripe v-loading="loading">
          <ElTableColumn prop="orderNo" label="订单号" min-width="150" />
          <ElTableColumn prop="elderlyName" label="服务对象" min-width="120" />
          <ElTableColumn prop="serviceItemName" label="服务项目" min-width="160" />
          <ElTableColumn prop="serviceUserName" label="服务人员" min-width="120" />
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
          <ElTableColumn prop="serviceAddress" label="服务地址" min-width="200" show-overflow-tooltip />
          <ElTableColumn label="操作" min-width="240" fixed="right">
            <template #default="{ row }">
              <div class="toolbar-actions">
                <ElButton type="primary" link @click="router.push(`/services/${row.id}`)">详情</ElButton>
                <ElButton
                  v-if="row.status !== 4 && row.status !== 5"
                  type="success"
                  link
                  @click="openAssignDialog(row)"
                >
                  派单
                </ElButton>
                <ElButton
                  v-if="row.status !== 4 && row.status !== 5"
                  type="danger"
                  link
                  @click="handleCancel(row)"
                >
                  取消
                </ElButton>
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </PanelCard>

    <ElDialog v-model="assignVisible" title="派发服务订单" width="420px">
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
        <ElButton type="primary" :loading="assigning" @click="confirmAssign">确认派单</ElButton>
      </template>
    </ElDialog>
  </div>
</template>
