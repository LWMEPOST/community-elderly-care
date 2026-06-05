<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getElderlyProfile, listUsers, updateUserStatus } from '@/api/users'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { userStatusOptions, userTypeOptions } from '@/constants/dicts'
import type { ElderlyProfileView, UserView } from '@/types/models'
import { formatDateTime, formatGender } from '@/utils/format'

const loading = ref(false)
const users = ref<UserView[]>([])
const profileVisible = ref(false)
const profileLoading = ref(false)
const currentProfile = ref<ElderlyProfileView | null>(null)

const filters = reactive({
  keyword: '',
  userType: undefined as number | undefined,
  status: undefined as number | undefined,
})

const cards = computed(() => [
  { label: '全部账号', value: users.value.length },
  { label: '待审核 / 禁用', value: users.value.filter((item) => item.status === 0).length },
  { label: '启用账号', value: users.value.filter((item) => item.status === 1).length },
  { label: '老人用户', value: users.value.filter((item) => item.userType === 1).length },
])

async function loadUsers() {
  loading.value = true
  try {
    users.value = await listUsers({
      keyword: filters.keyword || undefined,
      userType: filters.userType,
      status: filters.status,
    })
  } finally {
    loading.value = false
  }
}

async function handleStatusChange(row: UserView, status: number) {
  await updateUserStatus({ userId: row.id, status })
  ElMessage.success(status === 1 ? '用户已启用' : '用户已禁用')
  await loadUsers()
}

async function openProfile(row: UserView) {
  if (row.userType !== 1) {
    return
  }

  profileVisible.value = true
  profileLoading.value = true
  try {
    currentProfile.value = await getElderlyProfile(row.id)
  } finally {
    profileLoading.value = false
  }
}

onMounted(loadUsers)
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>用户审核与账号治理</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          统一查看老人、家属、管理员和服务人员账号状态，快速完成启用、停用与老人档案核验。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.16); color: #fff">
        <div>当前筛选结果</div>
        <strong>{{ users.length }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          待审核与禁用账号 {{ cards[1].value }} 个
        </div>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="card in cards" :key="card.label" class="stat-card">
        <span class="subtle-text">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <PanelCard title="账号列表" description="优先处理待审核账号，并对老人资料进行抽检。">
      <template #actions>
        <div class="toolbar-actions">
          <ElButton @click="loadUsers">刷新数据</ElButton>
        </div>
      </template>

      <div class="page-shell">
        <div class="filter-grid">
          <ElInput v-model="filters.keyword" clearable placeholder="用户名 / 姓名 / 手机号" />
          <ElSelect v-model="filters.userType" clearable placeholder="用户类型">
            <ElOption
              v-for="item in userTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
          <ElSelect v-model="filters.status" clearable placeholder="账号状态">
            <ElOption
              v-for="item in userStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
          <div class="toolbar-actions">
            <ElButton type="primary" :loading="loading" @click="loadUsers">查询</ElButton>
            <ElButton @click="Object.assign(filters, { keyword: '', userType: undefined, status: undefined }); loadUsers()">
              重置
            </ElButton>
          </div>
        </div>

        <ElTable :data="users" stripe v-loading="loading">
          <ElTableColumn prop="username" label="用户名" min-width="130" />
          <ElTableColumn prop="realName" label="姓名" min-width="110" />
          <ElTableColumn prop="phone" label="手机号" min-width="130" />
          <ElTableColumn label="用户类型" width="110">
            <template #default="{ row }">
              <StatusTag :value="row.userType" :options="userTypeOptions" />
            </template>
          </ElTableColumn>
          <ElTableColumn label="账号状态" width="140">
            <template #default="{ row }">
              <StatusTag :value="row.status" :options="userStatusOptions" />
            </template>
          </ElTableColumn>
          <ElTableColumn prop="address" label="地址" min-width="180" show-overflow-tooltip />
          <ElTableColumn label="创建时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" min-width="230">
            <template #default="{ row }">
              <div class="toolbar-actions">
                <ElButton
                  v-if="row.status !== 1"
                  type="success"
                  link
                  @click="handleStatusChange(row, 1)"
                >
                  启用
                </ElButton>
                <ElButton
                  v-if="row.status !== 0"
                  type="danger"
                  link
                  @click="handleStatusChange(row, 0)"
                >
                  禁用
                </ElButton>
                <ElButton
                  v-if="row.userType === 1"
                  type="primary"
                  link
                  @click="openProfile(row)"
                >
                  老人资料
                </ElButton>
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </PanelCard>

    <ElDrawer v-model="profileVisible" title="老人资料详情" size="460px">
      <div v-loading="profileLoading">
        <template v-if="currentProfile">
          <div class="detail-grid">
            <div class="value-block">
              <div class="muted-label">姓名</div>
              <div>{{ currentProfile.realName || '--' }}</div>
            </div>
            <div class="value-block">
              <div class="muted-label">手机号</div>
              <div>{{ currentProfile.phone || '--' }}</div>
            </div>
            <div class="value-block">
              <div class="muted-label">年龄</div>
              <div>{{ currentProfile.age ?? '--' }}</div>
            </div>
            <div class="value-block">
              <div class="muted-label">性别</div>
              <div>{{ formatGender(currentProfile.gender) }}</div>
            </div>
            <div class="value-block">
              <div class="muted-label">健康状态</div>
              <div>{{ currentProfile.healthStatus || '未填写' }}</div>
            </div>
            <div class="value-block">
              <div class="muted-label">病史</div>
              <div>{{ currentProfile.medicalHistory || '未填写' }}</div>
            </div>
            <div class="value-block">
              <div class="muted-label">坐标</div>
              <div>{{ currentProfile.longitude ?? '--' }}, {{ currentProfile.latitude ?? '--' }}</div>
            </div>
            <div class="value-block">
              <div class="muted-label">最近更新时间</div>
              <div>{{ formatDateTime(currentProfile.updateTime) }}</div>
            </div>
          </div>
        </template>
        <div v-else class="empty-copy">
          暂无老人资料
        </div>
      </div>
    </ElDrawer>
  </div>
</template>
