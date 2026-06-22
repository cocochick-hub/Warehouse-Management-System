import request from './request'

export function getInboundOrdersApi(params) {
  return request({
    url: '/inbound/orders',
    method: 'get',
    params
  })
}

export function getInboundOrderDetailApi(id) {
  return request({
    url: `/inbound/orders/${id}`,
    method: 'get'
  })
}

export function createInboundOrderApi(data) {
  return request({
    url: '/inbound/orders',
    method: 'post',
    data
  })
}

export function receiveInboundOrderApi(id, data) {
  return request({
    url: `/inbound/orders/${id}/receive`,
    method: 'post',
    data
  })
}

export function generateInboundKanbanLabelsApi(id) {
  return request({
    url: `/inbound/orders/${id}/kanban-labels/generate`,
    method: 'post'
  })
}

export function getInboundKanbanLabelsApi(id) {
  return request({
    url: `/inbound/orders/${id}/kanban-labels`,
    method: 'get'
  })
}

export function getInboundScanLabelApi(kanbanNo) {
  return request({
    url: `/inbound/scan/labels/${encodeURIComponent(kanbanNo)}`,
    method: 'get'
  })
}

export function receiveInboundScanApi(data) {
  return request({
    url: '/inbound/scan/receive',
    method: 'post',
    data
  })
}

export function getInboundHistoryApi(params) {
  return request({
    url: '/inbound/orders/history',
    method: 'get',
    params
  })
}
