<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  listManagedInformation,
  publishInformation,
  withdrawInformation,
} from '@/api/information'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { informationStatusOptions, informationTypeOptions } from '@/constants/dicts'
import type { InformationView } from '@/types/models'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const list = ref<InformationView[]>([])
const previewVisible = ref(false)
const currentInfo = ref<InformationView | null>(null)

const filters = reactive({
  infoType: undefined as number | undefined,
  status: undefined as number | undefined,
  keyword: '',
})

const cards = computed(() => [
  { label: '资讯总量', value: list.value.length },
  { label: '草稿', value: list.value.filter((item) => item.status === 0).length },
  { label: '已发布', value: list.value.filter((item) => item.status === 1).length },
])

async function loadList() {
  loading.value = true
  try {
    list.value = await listManagedInformation({
      infoType: filters.infoType,
      status: filters.status,
      keyword: filters.keyword || undefined,
      limit: 100,
    })
  } finally {
    loading.value = false
  }
}

async function handlePublish(row: InformationView) {
  await publishInformation(row.id)
  ElMessage.success('资讯已发布')
  await loadList()
}

async function handleWithdraw(row: InformationView) {
  await withdrawInformation(row.id)
  ElMessage.success('资讯已撤回')
  await loadList()
}

function openPreview(row: InformationView) {
  currentInfo.value = row
  previewVisible.value = true
}

function resetFilters() {
  Object.assign(filters, {
    infoType: undefined,
    status: undefined,
    keyword: '',
  })
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>资讯治理</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          管理社区养老平台的政策、活动、通知和动态内容，完成草稿编辑、发布和撤回闭环。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.16); color: #fff">
        <div>待处理草稿</div>
        <strong>{{ cards[1].value }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          已发布 {{ cards[2].value }} 篇
        </div>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="card in cards" :key="card.label" class="stat-card">
        <span class="subtle-text">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <PanelCard title="资讯列表" description="按类型、状态和关键字筛选，快速切换编辑与发布动作。">
      <template #actions>
        <div class="toolbar-actions">
          <ElButton type="primary" @click="router.push('/information/create')">新建资讯</ElButton>
        </div>
      </template>

      <div class="page-shell">
        <div class="filter-grid">
          <ElSelect v-model="filters.infoType" clearable placeholder="资讯类型">
            <ElOption
              v-for="item in informationTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
          <ElSelect v-model="filters.status" clearable placeholder="发布状态">
            <ElOption
              v-for="item in informationStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
          <ElInput v-model="filters.keyword" clearable placeholder="标题或内容关键字" />
          <div class="toolbar-actions">
            <ElButton type="primary" :loading="loading" @click="loadList">查询</ElButton>
            <ElButton @click="resetFilters">重置</ElButton>
          </div>
        </div>

        <ElTable :data="list" stripe v-loading="loading">
          <ElTableColumn prop="title" label="标题" min-width="180" />
          <ElTableColumn label="类型" width="110">
            <template #default="{ row }">
              <StatusTag :value="row.infoType" :options="informationTypeOptions" />
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="110">
            <template #default="{ row }">
              <StatusTag :value="row.status" :options="informationStatusOptions" />
            </template>
          </ElTableColumn>
          <ElTableColumn prop="publisherName" label="发布人" min-width="120" />
          <ElTableColumn prop="contentSummary" label="摘要" min-width="240" show-overflow-tooltip />
          <ElTableColumn prop="viewCount" label="浏览量" width="100" />
          <ElTableColumn label="发布时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.publishTime) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" min-width="250" fixed="right">
            <template #default="{ row }">
              <div class="toolbar-actions">
                <ElButton type="primary" link @click="router.push(`/information/${row.id}`)">编辑</ElButton>
                <ElButton type="info" link @click="openPreview(row)">预览</ElButton>
                <ElButton v-if="row.status === 0" type="success" link @click="handlePublish(row)">
                  发布
                </ElButton>
                <ElButton v-if="row.status === 1" type="danger" link @click="handleWithdraw(row)">
                  撤回
                </ElButton>
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </PanelCard>

    <ElDrawer v-model="previewVisible" title="资讯预览" size="560px">
      <div v-if="currentInfo" class="page-shell">
        <div class="detail-grid">
          <div class="value-block">
            <div class="muted-label">标题</div>
            <div>{{ currentInfo.title }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">类型</div>
            <div>{{ currentInfo.infoTypeText }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">发布状态</div>
            <StatusTag :value="currentInfo.status" :options="informationStatusOptions" />
          </div>
          <div class="value-block">
            <div class="muted-label">发布时间</div>
            <div>{{ formatDateTime(currentInfo.publishTime, '未发布') }}</div>
          </div>
        </div>
        <PanelCard title="正文内容">
          <div style="white-space: pre-wrap; line-height: 1.8">{{ currentInfo.content }}</div>
        </PanelCard>
      </div>
    </ElDrawer>
  </div>
</template>
