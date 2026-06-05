<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getHealthRecords, getHealthSummary, getHealthWarnings } from '@/api/health'
import { listUsers } from '@/api/users'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { healthRecordTypeOptions, healthWarningLevelOptions } from '@/constants/dicts'
import type { HealthRecordView, HealthWarningSummaryView, UserView } from '@/types/models'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const summary = ref<HealthWarningSummaryView | null>(null)
const warnings = ref<HealthRecordView[]>([])
const records = ref<HealthRecordView[]>([])
const elderlyUsers = ref<UserView[]>([])

const filters = reactive({
  elderlyId: undefined as number | undefined,
  recordType: undefined as number | undefined,
  warningLevel: undefined as number | undefined,
})

const cards = computed(() => [
  { label: '记录总数', value: summary.value?.totalRecords ?? 0 },
  { label: '异常记录', value: summary.value?.abnormalCount ?? 0 },
  { label: '低风险预警', value: summary.value?.lowWarningCount ?? 0 },
  { label: '高风险预警', value: summary.value?.highWarningCount ?? 0 },
])

const hasElderlyUsers = computed(() => elderlyUsers.value.length > 0)

const selectedElderly = computed(() =>
  elderlyUsers.value.find((item) => item.id === filters.elderlyId) ?? null,
)

const selectedElderlyLabel = computed(() => {
  const user = selectedElderly.value
  return user ? `${user.realName || user.username}（#${user.id}）` : '请先选择老人'
})

const warningEmptyText = computed(() =>
  selectedElderly.value ? `${selectedElderlyLabel.value} 暂无预警记录` : '请先选择老人后再查询',
)

const recordEmptyText = computed(() =>
  selectedElderly.value ? `${selectedElderlyLabel.value} 暂无健康记录` : '请先选择老人后再查询',
)

function buildParams() {
  return {
    elderlyId: filters.elderlyId,
    recordType: filters.recordType,
    warningLevel: filters.warningLevel,
    limit: 100,
  }
}

function resetData() {
  summary.value = null
  warnings.value = []
  records.value = []
}

function applyDefaultElderly() {
  filters.elderlyId = elderlyUsers.value[0]?.id
}

async function loadData() {
  if (!filters.elderlyId) {
    resetData()
    return
  }

  loading.value = true
  try {
    const params = buildParams()
    const [summaryData, warningData, recordData] = await Promise.all([
      getHealthSummary({ elderlyId: filters.elderlyId }),
      getHealthWarnings({
        elderlyId: filters.elderlyId,
        warningLevel: filters.warningLevel,
        limit: 20,
      }),
      getHealthRecords(params),
    ])

    summary.value = summaryData
    warnings.value = warningData
    records.value = recordData
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, {
    elderlyId: elderlyUsers.value[0]?.id,
    recordType: undefined,
    warningLevel: undefined,
  })
  loadData()
}

function renderMetric(row: HealthRecordView) {
  if (row.recordType === 1) {
    return `${row.systolicPressure ?? '--'} / ${row.diastolicPressure ?? '--'}`
  }
  if (row.recordType === 2) {
    return `${row.bloodSugar ?? '--'} mmol/L`
  }
  if (row.recordType === 3) {
    return `${row.heartRate ?? '--'} bpm`
  }
  return '综合记录'
}

onMounted(async () => {
  elderlyUsers.value = await listUsers({ userType: 1, status: 1 })
  applyDefaultElderly()
  await loadData()
})
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>健康概览</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          汇总健康记录、预警等级和最近异常，辅助管理员对重点老人进行主动巡检。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.16); color: #fff">
        <div>最近异常记录</div>
        <strong>{{ formatDateTime(summary?.latestWarningRecord?.recordTime, '暂无') }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          {{ summary?.latestWarningRecord?.elderlyName || '暂无异常老人' }}
        </div>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="card in cards" :key="card.label" class="stat-card">
        <span class="subtle-text">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <PanelCard title="健康筛选" description="按老人、记录类型和预警级别查看健康数据。">
      <ElAlert
        title="后台健康接口当前按单个老人查询，页面已默认选中第一位启用老人，可切换查看。"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <div class="filter-grid">
        <ElSelect v-model="filters.elderlyId" filterable placeholder="选择老人" :disabled="!hasElderlyUsers">
          <ElOption
            v-for="item in elderlyUsers"
            :key="item.id"
            :label="`${item.realName || item.username}（#${item.id}）`"
            :value="item.id"
          />
        </ElSelect>
        <ElSelect v-model="filters.recordType" clearable placeholder="记录类型">
          <ElOption
            v-for="item in healthRecordTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </ElSelect>
        <ElSelect v-model="filters.warningLevel" clearable placeholder="预警级别">
          <ElOption
            v-for="item in healthWarningLevelOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </ElSelect>
        <div class="toolbar-actions">
          <ElButton type="primary" :loading="loading" :disabled="!filters.elderlyId" @click="loadData">
            查询
          </ElButton>
          <ElButton @click="resetFilters">重置</ElButton>
        </div>
      </div>
    </PanelCard>

    <ElEmpty
      v-if="!hasElderlyUsers"
      description="暂无已启用老人账号，待管理员审核通过后可查看健康记录。"
    />

    <section v-else class="page-grid two-columns">
      <PanelCard title="预警概览" description="优先处理低风险和高风险预警记录。">
        <div class="detail-grid" style="margin-bottom: 16px">
          <div class="value-block">
            <div class="muted-label">概览对象</div>
            <div>{{ summary?.elderlyName || selectedElderlyLabel }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">最近记录时间</div>
            <div>{{ formatDateTime(summary?.latestRecordTime) }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">最近异常建议</div>
            <div>{{ summary?.latestWarningRecord?.advice || '暂无异常建议' }}</div>
          </div>
        </div>

        <ElTable :data="warnings" stripe v-loading="loading" :empty-text="warningEmptyText">
          <ElTableColumn prop="elderlyName" label="老人" min-width="120" />
          <ElTableColumn prop="recordTypeText" label="类型" width="110" />
          <ElTableColumn label="指标" min-width="140">
            <template #default="{ row }">
              {{ renderMetric(row) }}
            </template>
          </ElTableColumn>
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
        </ElTable>
      </PanelCard>

      <PanelCard title="健康记录列表" description="可按老人与预警级别进行深度排查。">
        <ElTable :data="records" stripe v-loading="loading" :empty-text="recordEmptyText">
          <ElTableColumn prop="elderlyName" label="老人" min-width="120" />
          <ElTableColumn label="记录类型" width="110">
            <template #default="{ row }">
              <StatusTag :value="row.recordType" :options="healthRecordTypeOptions" />
            </template>
          </ElTableColumn>
          <ElTableColumn label="指标" min-width="140">
            <template #default="{ row }">
              {{ renderMetric(row) }}
            </template>
          </ElTableColumn>
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
          <ElTableColumn prop="advice" label="建议" min-width="200" show-overflow-tooltip />
        </ElTable>
      </PanelCard>
    </section>
  </div>
</template>
