<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getEmergencyList, resolveEmergency, respondEmergency } from '@/api/emergency'
import { listUsers } from '@/api/users'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { emergencyHelpTypeOptions, emergencyStatusOptions } from '@/constants/dicts'
import type { EmergencyHelpView, UserView } from '@/types/models'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const responding = ref(false)
const list = ref<EmergencyHelpView[]>([])
const serviceUsers = ref<UserView[]>([])
const responseVisible = ref(false)
const currentEmergency = ref<EmergencyHelpView | null>(null)

const filters = reactive({
  status: undefined as number | undefined,
  elderlyId: '',
})

const responseForm = reactive({
  responseUserId: undefined as number | undefined,
})

const cards = computed(() => [
  { label: '全部求助', value: list.value.length },
  { label: '待响应', value: list.value.filter((item) => item.status === 1).length },
  { label: '已响应', value: list.value.filter((item) => item.status === 2).length },
  { label: '已解决', value: list.value.filter((item) => item.status === 3).length },
])

async function loadList() {
  loading.value = true
  try {
    list.value = await getEmergencyList({
      status: filters.status,
      elderlyId: filters.elderlyId ? Number(filters.elderlyId) : undefined,
      limit: 100,
    })
  } finally {
    loading.value = false
  }
}

async function loadServiceUsers() {
  serviceUsers.value = await listUsers({ userType: 4, status: 1 })
}

function openRespondDialog(row: EmergencyHelpView) {
  currentEmergency.value = row
  responseForm.responseUserId = row.responseUserId || undefined
  responseVisible.value = true
}

async function handleRespond() {
  if (!currentEmergency.value) {
    return
  }

  responding.value = true
  try {
    await respondEmergency(currentEmergency.value.id, {
      responseUserId: responseForm.responseUserId,
    })
    ElMessage.success('求助已响应')
    responseVisible.value = false
    await loadList()
  } finally {
    responding.value = false
  }
}

async function handleResolve(row: EmergencyHelpView) {
  await resolveEmergency(row.id)
  ElMessage.success('求助已解决')
  await loadList()
}

function resetFilters() {
  Object.assign(filters, {
    status: undefined,
    elderlyId: '',
  })
  loadList()
}

onMounted(async () => {
  await Promise.all([loadList(), loadServiceUsers()])
})
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>应急中心</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          统一管理老人和家属发起的求助工单，支持管理员指派响应人员并推动闭环解决。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.16); color: #fff">
        <div>待响应求助</div>
        <strong>{{ cards[1].value }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          已响应 {{ cards[2].value }} 条
        </div>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="card in cards" :key="card.label" class="stat-card">
        <span class="subtle-text">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <PanelCard title="求助工单列表" description="支持按照求助状态和老人用户 ID 定位重点事件。">
      <div class="page-shell">
        <div class="filter-grid">
          <ElSelect v-model="filters.status" clearable placeholder="求助状态">
            <ElOption
              v-for="item in emergencyStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
          <ElInput v-model="filters.elderlyId" clearable placeholder="老人用户 ID" />
          <div class="toolbar-actions">
            <ElButton type="primary" :loading="loading" @click="loadList">查询</ElButton>
            <ElButton @click="resetFilters">重置</ElButton>
          </div>
        </div>

        <ElTable :data="list" stripe v-loading="loading">
          <ElTableColumn prop="elderlyName" label="老人" min-width="120" />
          <ElTableColumn label="求助类型" width="120">
            <template #default="{ row }">
              <StatusTag :value="row.helpType" :options="emergencyHelpTypeOptions" />
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="110">
            <template #default="{ row }">
              <StatusTag :value="row.status" :options="emergencyStatusOptions" />
            </template>
          </ElTableColumn>
          <ElTableColumn prop="responseUserName" label="响应人员" min-width="120" />
          <ElTableColumn prop="locationAddress" label="位置" min-width="200" show-overflow-tooltip />
          <ElTableColumn label="发起时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" min-width="240" fixed="right">
            <template #default="{ row }">
              <div class="toolbar-actions">
                <ElButton type="primary" link @click="router.push(`/emergency/${row.id}`)">详情</ElButton>
                <ElButton v-if="row.status === 1" type="success" link @click="openRespondDialog(row)">
                  响应
                </ElButton>
                <ElButton v-if="row.status === 2" type="danger" link @click="handleResolve(row)">
                  解决
                </ElButton>
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </PanelCard>

    <ElDialog v-model="responseVisible" title="指派响应人员" width="420px">
      <ElForm label-position="top">
        <ElFormItem label="响应人员">
          <ElSelect
            v-model="responseForm.responseUserId"
            clearable
            placeholder="可留空，由当前管理员接手"
          >
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
        <ElButton @click="responseVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="responding" @click="handleRespond">确认响应</ElButton>
      </template>
    </ElDialog>
  </div>
</template>
