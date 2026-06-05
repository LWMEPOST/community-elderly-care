<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: 'admin',
  password: 'admin123',
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入管理员用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功，欢迎进入管理端')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-page__panel">
      <section class="login-page__intro">
        <div class="login-page__badge">Vue 3 + Element Plus</div>
        <h1>社区养老服务后台</h1>
        <p>
          统一管理社区养老服务的用户审核、亲属绑定、资讯发布、服务派单、应急处置与消息协同。
        </p>

        <div class="login-page__highlights">
          <article>
            <strong>用户审核</strong>
            <span>快速筛选待审核账号，及时启用或停用</span>
          </article>
          <article>
            <strong>应急闭环</strong>
            <span>对求助工单统一响应、指派与完结归档</span>
          </article>
          <article>
            <strong>服务调度</strong>
            <span>围绕服务人员完成派单和进度追踪</span>
          </article>
        </div>
      </section>

      <section class="login-page__form-card">
        <div>
          <div class="login-page__title">管理员登录</div>
          <p class="subtle-text">请使用已审核通过的管理员账号进入后台。</p>
        </div>

        <ElForm
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="handleSubmit"
        >
          <ElFormItem label="用户名" prop="username">
            <ElInput v-model="form.username" :prefix-icon="User" placeholder="请输入管理员用户名" />
          </ElFormItem>

          <ElFormItem label="密码" prop="password">
            <ElInput
              v-model="form.password"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="请输入登录密码"
            />
          </ElFormItem>

          <ElButton type="primary" size="large" :loading="loading" @click="handleSubmit">
            登录进入控制台
          </ElButton>
        </ElForm>

        <div class="login-page__tips">
          <div>开发环境默认引导账号通常为：admin / admin123</div>
          <div>若提示待审核或禁用，请在后台账号表检查管理员状态。</div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 32px;
  display: grid;
  place-items: center;
}

.login-page__panel {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  width: min(1180px, 100%);
  border-radius: 32px;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(18, 57, 84, 0.16);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(16px);
}

.login-page__intro {
  padding: 56px 52px;
  color: #fff;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.16), transparent 28%),
    linear-gradient(155deg, rgba(17, 67, 112, 0.98), rgba(28, 117, 188, 0.92)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.1), transparent);
}

.login-page__badge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 13px;
}

.login-page__intro h1 {
  margin: 20px 0 12px;
  font-size: 42px;
  line-height: 1.05;
}

.login-page__intro p {
  margin: 0;
  max-width: 520px;
  color: rgba(255, 255, 255, 0.84);
  font-size: 15px;
}

.login-page__highlights {
  display: grid;
  gap: 14px;
  margin-top: 34px;
}

.login-page__highlights article {
  padding: 18px 18px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
}

.login-page__highlights strong,
.login-page__highlights span {
  display: block;
}

.login-page__highlights span {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.74);
  font-size: 13px;
}

.login-page__form-card {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 52px 44px;
  background: rgba(255, 255, 255, 0.94);
}

.login-page__title {
  font-size: 28px;
  font-weight: 700;
}

.login-page__tips {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(244, 248, 251, 0.9);
  color: var(--app-text-soft);
  font-size: 13px;
}

.login-page__tips div + div {
  margin-top: 8px;
}

:deep(.el-button) {
  width: 100%;
}

@media (max-width: 960px) {
  .login-page {
    padding: 18px;
  }

  .login-page__panel {
    grid-template-columns: 1fr;
  }

  .login-page__intro,
  .login-page__form-card {
    padding: 28px 24px;
  }

  .login-page__intro h1 {
    font-size: 32px;
  }
}
</style>
