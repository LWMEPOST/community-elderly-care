<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { RefreshRight, SwitchButton } from '@element-plus/icons-vue'
import { adminMenus } from '@/router/menus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const currentTitle = computed(() => (route.meta.title as string | undefined) || '工作台')

function handleRefresh() {
  window.location.reload()
}

function handleLogout() {
  authStore.logout()
  router.replace('/login')
}
</script>

<template>
  <ElContainer class="admin-layout">
    <ElAside class="admin-layout__aside" width="260px">
      <div class="brand-card">
        <div class="brand-card__badge">社区养老服务</div>
        <h1>运营管理中枢</h1>
        <p>围绕审核、服务调度、应急处置与健康协同，构建一站式社区养老后台。</p>
      </div>

      <ElMenu :default-active="route.path" class="admin-menu" router>
        <ElMenuItem
          v-for="item in adminMenus"
          :key="item.path"
          :index="item.path"
        >
          <ElIcon><component :is="item.icon" /></ElIcon>
          <div class="admin-menu__content">
            <span>{{ item.name }}</span>
            <small>{{ item.description }}</small>
          </div>
        </ElMenuItem>
      </ElMenu>
    </ElAside>

    <ElContainer>
      <ElHeader class="admin-layout__header">
        <div>
          <div class="admin-layout__eyebrow">管理员控制台</div>
          <h2>{{ currentTitle }}</h2>
        </div>

        <div class="admin-layout__actions">
          <div class="admin-layout__user">
            <span>{{ authStore.displayName }}</span>
            <small>{{ authStore.user?.username }}</small>
          </div>
          <ElButton :icon="RefreshRight" plain @click="handleRefresh">刷新</ElButton>
          <ElButton :icon="SwitchButton" type="danger" plain @click="handleLogout">
            退出登录
          </ElButton>
        </div>
      </ElHeader>

      <ElMain class="admin-layout__main">
        <RouterView />
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: transparent;
}

.admin-layout__aside {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 18px;
  border-right: 1px solid rgba(20, 64, 96, 0.08);
  background:
    linear-gradient(180deg, rgba(248, 251, 253, 0.94), rgba(239, 246, 250, 0.96)),
    #f6fbff;
}

.brand-card {
  padding: 22px 20px;
  border-radius: 24px;
  background:
    linear-gradient(145deg, rgba(20, 88, 143, 0.97), rgba(28, 117, 188, 0.9)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), transparent);
  color: #fff;
  box-shadow: 0 20px 36px rgba(20, 88, 143, 0.2);
}

.brand-card__badge {
  display: inline-flex;
  margin-bottom: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
}

.brand-card h1 {
  margin: 0;
  font-size: 24px;
}

.brand-card p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
}

.admin-menu {
  flex: 1;
  border: 0;
  background: transparent;
}

:deep(.admin-menu .el-menu-item) {
  display: flex;
  align-items: center;
  gap: 10px;
  height: auto;
  margin-bottom: 8px;
  padding: 14px 14px 14px 16px;
  border-radius: 16px;
  line-height: 1.4;
}

:deep(.admin-menu .el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(28, 117, 188, 0.14), rgba(35, 179, 138, 0.08));
  color: var(--app-primary-dark);
}

.admin-menu__content {
  display: flex;
  flex-direction: column;
}

.admin-menu__content small {
  color: var(--app-text-soft);
}

.admin-layout__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  height: auto;
  padding: 24px 28px 12px;
}

.admin-layout__eyebrow {
  color: var(--app-text-soft);
  font-size: 13px;
}

.admin-layout__header h2 {
  margin: 6px 0 0;
  font-size: 30px;
  line-height: 1.1;
}

.admin-layout__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-layout__user {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--app-border);
}

.admin-layout__user span,
.admin-layout__user small {
  display: block;
}

.admin-layout__user small {
  color: var(--app-text-soft);
}

.admin-layout__main {
  padding: 12px 28px 28px;
}

@media (max-width: 1080px) {
  .admin-layout {
    flex-direction: column;
  }

  .admin-layout__aside {
    width: 100%;
  }

  .admin-layout__header {
    padding-top: 12px;
  }
}
</style>
