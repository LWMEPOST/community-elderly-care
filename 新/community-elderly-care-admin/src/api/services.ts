import type {
  ServiceOrderAssignRequest,
  ServiceOrderView,
} from '@/types/models'
import { request } from './client'

export function getServiceOrders(params?: {
  status?: number
  keyword?: string
}) {
  return request<ServiceOrderView[]>({
    url: '/service/orders',
    method: 'get',
    params,
  })
}

export function getServiceOrderDetail(id: number) {
  return request<ServiceOrderView>({
    url: `/service/order/${id}`,
    method: 'get',
  })
}

export function assignServiceOrder(id: number, data: ServiceOrderAssignRequest) {
  return request<ServiceOrderView>({
    url: `/service/order/${id}/assign`,
    method: 'put',
    data,
  })
}

export function cancelServiceOrder(id: number) {
  return request<ServiceOrderView>({
    url: `/service/order/${id}/cancel`,
    method: 'put',
  })
}
