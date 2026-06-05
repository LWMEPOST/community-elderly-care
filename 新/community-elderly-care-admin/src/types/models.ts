export type DictTagType = '' | 'primary' | 'success' | 'warning' | 'danger' | 'info'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface DictOption<T = number> {
  value: T
  label: string
  tagType?: DictTagType
}

export interface UserView {
  id: number
  username: string
  realName: string | null
  phone: string | null
  userType: number
  avatar: string | null
  address: string | null
  emergencyContact: string | null
  emergencyPhone: string | null
  status: number
  createTime: string | null
  updateTime: string | null
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  expireAt: string | null
  user: UserView
}

export interface RegisterRequest {
  username: string
  password: string
  realName: string
  phone: string
  userType: number
  address?: string
  emergencyContact?: string
  emergencyPhone?: string
}

export interface UpdateUserRequest {
  id?: number
  realName?: string
  phone?: string
  avatar?: string
  address?: string
  emergencyContact?: string
  emergencyPhone?: string
}

export interface UserStatusUpdateRequest {
  userId: number
  status: number
}

export interface FamilyBindingView {
  id: number
  elderlyId: number
  elderlyName: string | null
  familyId: number
  familyName: string | null
  relation: string
  status: number
  createTime: string | null
}

export interface ElderlyProfileView {
  id?: number | null
  userId: number
  realName: string | null
  phone: string | null
  age: number | null
  gender: number | null
  healthStatus: string | null
  medicalHistory: string | null
  longitude: number | null
  latitude: number | null
  createTime: string | null
  updateTime: string | null
}

export interface ElderlyProfileRequest {
  userId?: number
  age?: number
  gender?: number
  healthStatus?: string
  medicalHistory?: string
  longitude?: number
  latitude?: number
}

export interface InformationView {
  id: number
  title: string
  content: string
  contentSummary: string | null
  infoType: number
  infoTypeText: string | null
  publisherId: number | null
  publisherName: string | null
  coverImage: string | null
  status: number
  statusText: string | null
  viewCount: number | null
  createTime: string | null
  publishTime: string | null
  canEdit?: boolean | null
  canPublish?: boolean | null
  canWithdraw?: boolean | null
}

export interface InformationSaveRequest {
  title: string
  content: string
  infoType: number | null
  coverImage: string
}

export interface ServiceCategory {
  id: number
  name: string
  description: string | null
  icon: string | null
  sortOrder: number | null
  status: number | null
  createTime: string | null
}

export interface ServiceItem {
  id: number
  categoryId: number
  name: string
  description: string | null
  price: number | null
  duration: number | null
  imageUrl: string | null
  status: number | null
  createTime: string | null
}

export interface ServiceOrderView {
  id: number
  orderNo: string
  elderlyId: number
  elderlyName: string | null
  familyId: number | null
  familyName: string | null
  serviceItemId: number | null
  serviceItemName: string | null
  categoryId: number | null
  categoryName: string | null
  servicePrice: number | null
  serviceDuration: number | null
  serviceUserId: number | null
  serviceUserName: string | null
  appointmentTime: string | null
  serviceAddress: string | null
  status: number
  statusText: string | null
  remark: string | null
  createTime: string | null
  updateTime: string | null
}

export interface ServiceOrderAssignRequest {
  serviceUserId: number | null
}

export interface EmergencyHelpView {
  id: number
  elderlyId: number
  elderlyName: string | null
  longitude: number | null
  latitude: number | null
  locationAddress: string | null
  helpType: number
  helpTypeText: string | null
  description: string | null
  status: number
  statusText: string | null
  responseUserId: number | null
  responseUserName: string | null
  createTime: string | null
  resolveTime: string | null
  canRespond?: boolean | null
  canResolve?: boolean | null
}

export interface EmergencyHelpRespondRequest {
  responseUserId?: number
}

export interface MessageConversationView {
  counterpartUserId: number
  counterpartUserName: string | null
  counterpartUserType: number | null
  lastMessageId: number | null
  lastMessageContent: string | null
  lastMessageType: number | null
  lastMessageTypeText: string | null
  lastMessageStatus: number | null
  lastMessageStatusText: string | null
  lastMessageTime: string | null
  lastMessageFromCurrentUser: boolean
  unreadCount: number
}

export interface MessageView {
  id: number
  rootMessageId: number | null
  parentId: number | null
  senderId: number
  senderName: string | null
  receiverId: number
  receiverName: string | null
  content: string
  messageType: number
  messageTypeText: string | null
  status: number
  statusText: string | null
  replyContent: string | null
  replyTime: string | null
  createTime: string | null
  fromCurrentUser: boolean
}

export interface MessageSendRequest {
  receiverId: number
  messageType: number
  content: string
  parentId?: number
}

export interface MessageReplyRequest {
  content: string
}

export interface MessageUnreadSummaryView {
  totalUnreadCount: number
  unreadConversationCount: number
  latestUnreadTime: string | null
}

export interface HealthRecordView {
  id: number
  elderlyId: number
  elderlyName: string | null
  recordType: number
  recordTypeText: string | null
  systolicPressure: number | null
  diastolicPressure: number | null
  bloodSugar: number | null
  heartRate: number | null
  recordTime: string | null
  warningLevel: number
  warningLevelText: string | null
  advice: string | null
  createTime: string | null
}

export interface HealthWarningSummaryView {
  elderlyId: number | null
  elderlyName: string | null
  totalRecords: number
  normalCount: number
  lowWarningCount: number
  highWarningCount: number
  abnormalCount: number
  latestRecordTime: string | null
  latestWarningRecord: HealthRecordView | null
}
