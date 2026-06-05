<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmBinding, listBindings } from '@/api/users'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { bindingStatusOptions } from '@/constants/dicts'
import type { FamilyBindingView } from '@/types/models'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const bindings = ref<FamilyBindingView[]>([])

const filters = reactive({
  status: undefined as number | undefined,
  elderlyId: '',
  familyId: '',
})

const cards = computed(() => [
  { label: '全部申请', value: bindings.value.length },
  { label: '待确认', value: bindings.value.filter((item) => item.status === 0).length },
  { label: '已确认', value: bindings.value.filter((item) => item.status === 1).length },
])

function buildParams() {
  return {
    status: filters.status,
    elderlyId: filters.elderlyId ? Number(filters.elderlyId) : undefined,
    familyId: filters.familyId ? Number(filters.familyId) : undefined,
  }
}

async function loadBindings() {
  loading.value = true
  try {
    bindings.value = await listBindings(buildParams())
  } finally {
    loading.value = false
  }
}

async function handleConfirm(row: FamilyBindingView) {
  await confirmBinding(row.id)
  ElMessage.success('绑定关系已确认')
  await loadBindings()
}

function resetFilters() {
  Object.assign(filters, {
    status: undefined,
    elderlyId: '',
    familyId: '',
  })
  loadBindings()
}

onMounted(loadBindings)
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>家属绑定确认</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          管理员可查看老人和家属的绑定申请，帮助处理久未确认或需要人工介入的关系审核。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.16); color: #fff">
        <div>待确认申请</div>
        <strong>{{ cards[1].value }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          已确认 {{ cards[2].value }} 条
        </div>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="card in cards" :key="card.label" class="stat-card">
        <span class="subtle-text">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <PanelCard title="绑定申请列表" description="支持按老人 ID、家属 ID 和状态进行筛选。">
      <div class="page-shell">
        <div class="filter-grid">
          <ElSelect v-model="filters.status" clearable placeholder="绑定状态">
            <ElOption
              v-for="item in bindingStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
          <ElInput v-model="filters.elderlyId" clearable placeholder="老人用户 ID" />
          <ElInput v-model="filters.familyId" clearable placeholder="家属用户 ID" />
          <div class="toolbar-actions">
            <ElButton type="primary" :loading="loading" @click="loadBindings">查询</ElButton>
            <ElButton @click="resetFilters">重置</ElButton>
          </div>
        </div>

        <ElTable :data="bindings" stripe v-loading="loading">
          <ElTableColumn prop="id" label="申请ID" width="90" />
          <ElTableColumn label="老人" min-width="140">
            <template #default="{ row }">
              {{ row.elderlyName || '--' }}（#{{ row.elderlyId }}）
            </template>
          </ElTableColumn>
          <ElTableColumn label="家属" min-width="140">
            <template #default="{ row }">
              {{ row.familyName || '--' }}（#{{ row.familyId }}）
            </template>
          </ElTableColumn>
          <ElTableColumn prop="relation" label="关系" min-width="110" />
          <ElTableColumn label="状态" width="120">
            <template #default="{ row }">
              <StatusTag :value="row.status" :options="bindingStatusOptions" />
            </template>
          </ElTableColumn>
          <ElTableColumn label="申请时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <ElButton
                v-if="row.status === 0"
                type="primary"
                link
                @click="handleConfirm(row)"
              >
                确认绑定
              </ElButton>
              <span v-else class="subtle-text">已完成</span>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </PanelCard>
  </div>
</template>
