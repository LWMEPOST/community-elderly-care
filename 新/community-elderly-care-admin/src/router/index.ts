import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { title: '管理员登录', guestOnly: true, requiresAuth: false },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/users/UserListView.vue'),
          meta: { title: '用户审核' },
        },
        {
          path: 'bindings',
          name: 'bindings',
          component: () => import('@/views/bindings/BindingListView.vue'),
          meta: { title: '绑定确认' },
        },
        {
          path: 'information',
          name: 'information',
          component: () => import('@/views/information/InformationListView.vue'),
          meta: { title: '资讯治理' },
        },
        {
          path: 'information/create',
          name: 'information-create',
          component: () => import('@/views/information/InformationEditorView.vue'),
          meta: { title: '新建资讯' },
        },
        {
          path: 'information/:id',
          name: 'information-edit',
          component: () => import('@/views/information/InformationEditorView.vue'),
          props: true,
          meta: { title: '编辑资讯' },
        },
        {
          path: 'services',
          name: 'services',
          component: () => import('@/views/services/ServiceOrderListView.vue'),
          meta: { title: '服务调度' },
        },
        {
          path: 'services/:id',
          name: 'service-detail',
          component: () => import('@/views/services/ServiceOrderDetailView.vue'),
          props: true,
          meta: { title: '服务订单详情' },
        },
        {
          path: 'emergency',
          name: 'emergency',
          component: () => import('@/views/emergency/EmergencyListView.vue'),
          meta: { title: '应急中心' },
        },
        {
          path: 'emergency/:id',
          name: 'emergency-detail',
          component: () => import('@/views/emergency/EmergencyDetailView.vue'),
          props: true,
          meta: { title: '求助详情' },
        },
        {
          path: 'messages',
          name: 'messages',
          component: () => import('@/views/messages/MessageCenterView.vue'),
          meta: { title: '消息中心' },
        },
        {
          path: 'health',
          name: 'health',
          component: () => import('@/views/health/HealthOverviewView.vue'),
          meta: { title: '健康概览' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (!authStore.hydrated) {
    authStore.hydrate()
  }

  if (to.meta.guestOnly) {
    if (!authStore.token) {
      return true
    }

    try {
      await authStore.ensureAdminSession()
      return (to.query.redirect as string | undefined) || '/'
    } catch {
      return true
    }
  }

  if (to.meta.requiresAuth === false) {
    return true
  }

  if (!authStore.token) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  try {
    await authStore.ensureAdminSession()
    return true
  } catch {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }
})

router.afterEach((to) => {
  const title = (to.meta.title as string | undefined) || '管理端'
  document.title = `${title} - 社区养老服务平台`
})

export default router
