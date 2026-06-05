<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getEmergencyDetail, resolveEmergency, respondEmergency } from '@/api/emergency'
import { listUsers } from '@/api/users'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { emergencyHelpTypeOptions, emergencyStatusOptions } from '@/constants/dicts'
import type { EmergencyHelpView, UserView } from '@/types/models'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  id: string
}>()

const router = useRouter()

const loading = ref(false)
const responding = ref(false)
const detail = ref<EmergencyHelpView | null>(null)
const serviceUsers = ref<UserView[]>([])
const responseVisible = ref(false)

const numericId = computed(() => Number(props.id))
const responseForm = reactive({
  responseUserId: undefined as number | undefined,
})

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getEmergencyDetail(numericId.value)
    responseForm.responseUserId = detail.value.responseUserId || undefined
  } finally {
    loading.value = false
  }
}

async function loadServiceUsers() {
  serviceUsers.value = await listUsers({ userType: 4, status: 1 })
}

async function handleRespond() {
  if (!detail.value) {
    return
  }

  responding.value = true
  try {
    detail.value = await respondEmergency(detail.value.id, {
      responseUserId: responseForm.responseUserId,
    })
    ElMessage.success('求助已响应')
    responseVisible.value = false
  } finally {
    responding.value = false
  }
}

async function handleResolve() {
  if (!detail.value) {
    return
  }

  detail.value = await resolveEmergency(detail.value.id)
  ElMessage.success('求助已解决')
}

onMounted(async () => {
  await Promise.all([loadDetail(), loadServiceUsers()])
})
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>求助详情</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          查看求助位置、类型、描述与响应状态，并完成人工响应或闭环解决。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.16); color: #fff">
        <div>当前状态</div>
        <strong>{{ detail?.statusText || '加载中' }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          发起时间 {{ formatDateTime(detail?.createTime) }}
        </div>
      </div>
    </section>

    <PanelCard title="求助工单详情" description="帮助管理员查看完整位置和响应流转信息。">
      <template #actions>
        <div class="toolbar-actions">
          <ElButton @click="router.push('/emergency')">返回列表</ElButton>
          <ElButton v-if="detail?.status === 1" type="primary" @click="responseVisible = true">响应求助</ElButton>
          <ElButton v-if="detail?.status === 2" type="danger" @click="handleResolve">解决求助</ElButton>
        </div>
      </template>

      <div v-loading="loading">
        <div v-if="detail" class="detail-grid">
          <div class="value-block">
            <div class="muted-label">老人</div>
            <div>{{ detail.elderlyName || '--' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">求助类型</div>
            <StatusTag :value="detail.helpType" :options="emergencyHelpTypeOptions" />
          </div>
          <div class="value-block">
            <div class="muted-label">工单状态</div>
            <StatusTag :value="detail.status" :options="emergencyStatusOptions" />
          </div>
          <div class="value-block">
            <div class="muted-label">响应人员</div>
            <div>{{ detail.responseUserName || '未分配' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">位置地址</div>
            <div>{{ detail.locationAddress || '未填写' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">坐标</div>
            <div>{{ detail.longitude ?? '--' }}, {{ detail.latitude ?? '--' }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">发起时间</div>
            <div>{{ formatDateTime(detail.createTime) }}</div>
          </div>
          <div class="value-block">
            <div class="muted-label">解决时间</div>
            <div>{{ formatDateTime(detail.resolveTime, '未解决') }}</div>
          </div>
        </div>

        <div v-if="detail" style="margin-top: 16px" class="value-block">
          <div class="muted-label">求助描述</div>
          <div>{{ detail.description || '暂无描述' }}</div>
        </div>
      </div>
    </PanelCard>

    <ElDialog v-model="responseVisible" title="响应求助" width="420px">
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
