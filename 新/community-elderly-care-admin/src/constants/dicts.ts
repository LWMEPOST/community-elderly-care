import type { DictOption } from '@/types/models'

export const userTypeOptions: DictOption[] = [
  { value: 1, label: '老人', tagType: 'warning' },
  { value: 2, label: '家属', tagType: 'success' },
  { value: 3, label: '管理员', tagType: 'primary' },
  { value: 4, label: '服务人员', tagType: 'info' },
]

export const userStatusOptions: DictOption[] = [
  { value: 0, label: '待审核 / 禁用', tagType: 'warning' },
  { value: 1, label: '启用', tagType: 'success' },
]

export const bindingStatusOptions: DictOption[] = [
  { value: 0, label: '待确认', tagType: 'warning' },
  { value: 1, label: '已确认', tagType: 'success' },
]

export const serviceOrderStatusOptions: DictOption[] = [
  { value: 1, label: '待接单', tagType: 'warning' },
  { value: 2, label: '已接单', tagType: 'primary' },
  { value: 3, label: '服务中', tagType: 'info' },
  { value: 4, label: '已完成', tagType: 'success' },
  { value: 5, label: '已取消', tagType: 'danger' },
]

export const emergencyStatusOptions: DictOption[] = [
  { value: 1, label: '待响应', tagType: 'warning' },
  { value: 2, label: '已响应', tagType: 'primary' },
  { value: 3, label: '已解决', tagType: 'success' },
]

export const emergencyHelpTypeOptions: DictOption[] = [
  { value: 1, label: '医疗求助', tagType: 'danger' },
  { value: 2, label: '摔倒求助', tagType: 'warning' },
  { value: 3, label: '其他求助', tagType: 'info' },
]

export const messageStatusOptions: DictOption[] = [
  { value: 0, label: '未读', tagType: 'warning' },
  { value: 1, label: '已读', tagType: 'success' },
]

export const messageTypeOptions: DictOption[] = [
  { value: 1, label: '留言', tagType: 'primary' },
  { value: 2, label: '反馈', tagType: 'warning' },
  { value: 3, label: '咨询', tagType: 'success' },
  { value: 4, label: '系统提醒', tagType: 'info' },
]

export const informationStatusOptions: DictOption[] = [
  { value: 0, label: '未发布', tagType: 'warning' },
  { value: 1, label: '已发布', tagType: 'success' },
]

export const informationTypeOptions: DictOption[] = [
  { value: 1, label: '政策', tagType: 'primary' },
  { value: 2, label: '活动', tagType: 'success' },
  { value: 3, label: '通知', tagType: 'warning' },
  { value: 4, label: '动态', tagType: 'info' },
]

export const healthRecordTypeOptions: DictOption[] = [
  { value: 1, label: '血压', tagType: 'warning' },
  { value: 2, label: '血糖', tagType: 'danger' },
  { value: 3, label: '心率', tagType: 'primary' },
  { value: 4, label: '其他', tagType: 'info' },
]

export const healthWarningLevelOptions: DictOption[] = [
  { value: 0, label: '正常', tagType: 'success' },
  { value: 1, label: '低风险预警', tagType: 'warning' },
  { value: 2, label: '高风险预警', tagType: 'danger' },
]

export function getDictOption<T extends string | number>(
  options: DictOption<T>[],
  value: T | null | undefined,
) {
  return options.find((item) => item.value === value)
}

export function getDictLabel<T extends string | number>(
  options: DictOption<T>[],
  value: T | null | undefined,
  fallback = '未知',
) {
  return getDictOption(options, value)?.label ?? fallback
}
