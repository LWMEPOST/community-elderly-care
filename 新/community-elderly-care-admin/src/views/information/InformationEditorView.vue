<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  createInformationDraft,
  getInformationDetail,
  publishInformation,
  publishInformationImmediately,
  updateInformation,
  withdrawInformation,
} from '@/api/information'
import PanelCard from '@/components/PanelCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { informationStatusOptions, informationTypeOptions } from '@/constants/dicts'
import type { InformationSaveRequest, InformationView } from '@/types/models'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const detail = ref<InformationView | null>(null)

const form = reactive<InformationSaveRequest>({
  title: '',
  content: '',
  infoType: null,
  coverImage: '',
})

const rules: FormRules<typeof form> = {
  title: [{ required: true, message: '请输入资讯标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }],
  infoType: [{ required: true, message: '请选择资讯类型', trigger: 'change' }],
}

const currentId = computed(() => {
  const raw = route.params.id
  if (!raw) {
    return null
  }
  const parsed = Number(raw)
  return Number.isNaN(parsed) ? null : parsed
})

const pageTitle = computed(() => (currentId.value ? '编辑资讯' : '新建资讯'))

async function loadDetail() {
  if (!currentId.value) {
    detail.value = null
    Object.assign(form, {
      title: '',
      content: '',
      infoType: null,
      coverImage: '',
    })
    return
  }

  loading.value = true
  try {
    const data = await getInformationDetail(currentId.value)
    detail.value = data
    Object.assign(form, {
      title: data.title,
      content: data.content,
      infoType: data.infoType,
      coverImage: data.coverImage || '',
    })
  } finally {
    loading.value = false
  }
}

async function validateForm() {
  return formRef.value?.validate().catch(() => false)
}

async function handleSave() {
  const valid = await validateForm()
  if (!valid) {
    return
  }

  saving.value = true
  try {
    if (currentId.value) {
      const updated = await updateInformation(currentId.value, form)
      detail.value = updated
      ElMessage.success('资讯内容已保存')
    } else {
      const created = await createInformationDraft(form)
      ElMessage.success('草稿已创建')
      await router.replace(`/information/${created.id}`)
    }
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  const valid = await validateForm()
  if (!valid) {
    return
  }

  saving.value = true
  try {
    if (currentId.value) {
      await updateInformation(currentId.value, form)
      const published = await publishInformation(currentId.value)
      detail.value = published
      ElMessage.success('资讯已发布')
    } else {
      const published = await publishInformationImmediately(form)
      ElMessage.success('资讯已创建并发布')
      await router.replace(`/information/${published.id}`)
    }
  } finally {
    saving.value = false
  }
}

async function handleWithdraw() {
  if (!currentId.value) {
    return
  }

  saving.value = true
  try {
    detail.value = await withdrawInformation(currentId.value)
    ElMessage.success('资讯已撤回')
  } finally {
    saving.value = false
  }
}

watch(currentId, loadDetail)
onMounted(loadDetail)
</script>

<template>
  <div class="page-shell">
    <section class="hero-banner">
      <div>
        <h1>{{ pageTitle }}</h1>
        <p style="margin-top: 14px; color: rgba(255, 255, 255, 0.78)">
          支持按后台资讯契约完成草稿保存、直接发布与已发布内容撤回。
        </p>
      </div>
      <div class="stat-card" style="background: rgba(255, 255, 255, 0.16); color: #fff">
        <div>当前状态</div>
        <strong>{{ detail?.statusText || '新建中' }}</strong>
        <div style="margin-top: 12px; color: rgba(255, 255, 255, 0.74)">
          更新时间 {{ formatDateTime(detail?.createTime, '尚未创建') }}
        </div>
      </div>
    </section>

    <section class="page-grid two-columns">
      <PanelCard title="内容编辑" description="根据后端 `InformationSaveRequest` 直接提交表单。">
        <template #actions>
          <div class="toolbar-actions">
            <ElButton @click="router.push('/information')">返回列表</ElButton>
            <ElButton :loading="saving" @click="handleSave">保存草稿</ElButton>
            <ElButton type="primary" :loading="saving" @click="handlePublish">发布</ElButton>
            <ElButton
              v-if="detail?.status === 1"
              type="danger"
              :loading="saving"
              @click="handleWithdraw"
            >
              撤回
            </ElButton>
          </div>
        </template>

        <ElForm ref="formRef" :model="form" :rules="rules" label-position="top" v-loading="loading">
          <ElFormItem label="标题" prop="title">
            <ElInput v-model="form.title" maxlength="100" show-word-limit placeholder="请输入资讯标题" />
          </ElFormItem>
          <ElFormItem label="资讯类型" prop="infoType">
            <ElSelect v-model="form.infoType" placeholder="请选择类型">
              <ElOption
                v-for="item in informationTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="封面图地址">
            <ElInput v-model="form.coverImage" placeholder="可选：输入封面图 URL" />
          </ElFormItem>
          <ElFormItem label="正文内容" prop="content">
            <ElInput
              v-model="form.content"
              type="textarea"
              :rows="18"
              maxlength="10000"
              show-word-limit
              placeholder="请输入资讯正文"
            />
          </ElFormItem>
        </ElForm>
      </PanelCard>

      <PanelCard title="发布预览" description="侧边预览帮助确认正文结构和当前元数据。">
        <div class="page-shell">
          <div class="detail-grid">
            <div class="value-block">
              <div class="muted-label">发布状态</div>
              <StatusTag
                :value="detail?.status"
                :options="informationStatusOptions"
                fallback="未创建"
              />
            </div>
            <div class="value-block">
              <div class="muted-label">发布人</div>
              <div>{{ detail?.publisherName || '创建后自动写入' }}</div>
            </div>
            <div class="value-block">
              <div class="muted-label">发布时间</div>
              <div>{{ formatDateTime(detail?.publishTime, '未发布') }}</div>
            </div>
            <div class="value-block">
              <div class="muted-label">浏览量</div>
              <div>{{ detail?.viewCount ?? 0 }}</div>
            </div>
          </div>

          <div class="value-block">
            <div class="muted-label">预览标题</div>
            <div style="font-size: 22px; font-weight: 700">{{ form.title || '请输入资讯标题' }}</div>
            <div style="margin-top: 10px">
              <StatusTag
                :value="form.infoType"
                :options="informationTypeOptions"
                fallback="未选择类型"
              />
            </div>
            <div style="margin-top: 16px; white-space: pre-wrap; line-height: 1.9">
              {{ form.content || '正文预览区域' }}
            </div>
          </div>
        </div>
      </PanelCard>
    </section>
  </div>
</template>
