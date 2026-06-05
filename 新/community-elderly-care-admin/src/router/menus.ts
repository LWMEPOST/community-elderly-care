import type { Component } from 'vue'
import {
  Bell,
  DataAnalysis,
  DocumentCopy,
  HelpFilled,
  HomeFilled,
  Promotion,
  UserFilled,
} from '@element-plus/icons-vue'

export interface AppMenuItem {
  path: string
  name: string
  icon: Component
  description: string
}

export const adminMenus: AppMenuItem[] = [
  {
    path: '/',
    name: '工作台',
    icon: HomeFilled,
    description: '汇总待办、异常和协同消息',
  },
  {
    path: '/users',
    name: '用户审核',
    icon: UserFilled,
    description: '老人、家属、服务人员账号治理',
  },
  {
    path: '/bindings',
    name: '绑定确认',
    icon: Promotion,
    description: '老人和家属关系审核',
  },
  {
    path: '/information',
    name: '资讯治理',
    icon: DocumentCopy,
    description: '草稿编辑、发布与撤回',
  },
  {
    path: '/services',
    name: '服务调度',
    icon: DataAnalysis,
    description: '服务订单派单与进度追踪',
  },
  {
    path: '/emergency',
    name: '应急中心',
    icon: HelpFilled,
    description: '求助响应与闭环处理',
  },
  {
    path: '/messages',
    name: '消息中心',
    icon: Bell,
    description: '后台会话与提醒跟进',
  },
  {
    path: '/health',
    name: '健康概览',
    icon: DataAnalysis,
    description: '异常预警与健康记录巡检',
  },
]
