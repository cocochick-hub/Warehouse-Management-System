import request from './request'

export function listOrders(params) {
  return request({
    url: '/outbound/orders',
    method: 'get',
    params
  })
}

export function getOrderDetail(id) {
  return request({
    url: `/outbound/orders/${id}`,
    method: 'get'
  })
}

export function createOrder(data) {
  return request({
    url: '/outbound/orders',
    method: 'post',
    data
  })
}

export function issueOrder(id, data) {
  return request({
    url: `/outbound/orders/${id}/issue`,
    method: 'post',
    data
  })
}

export function listHistory(params) {
  return request({
    url: '/outbound/history',
    method: 'get',
    params
  })
}

export function getOutboundScanLabel(kanbanNo) {
  return request({
    url: `/outbound/scan/labels/${encodeURIComponent(kanbanNo)}`,
    method: 'get'
  })
}

export function issueByScan(data) {
  return request({
    url: '/outbound/scan/issue',
    method: 'post',
    data
  })
}

export function getReturnLabel(kanbanNo) {
  return request({
    url: `/outbound/return/labels/${encodeURIComponent(kanbanNo)}`,
    method: 'get'
  })
}

export function doReturn(data) {
  return request({
    url: '/outbound/return',
    method: 'post',
    data
  })
}

export function getAvailableKanbanLabels(orderId) {
  return request({
    url: `/outbound/orders/${orderId}/available-kanban-labels`,
    method: 'get'
  })
}

export function issueByLabels(orderId, data) {
  return request({
    url: `/outbound/orders/${orderId}/issue-by-labels`,
    method: 'post',
    data
  })
}

export function getIssuedLabels(orderId) {
  return request({
    url: `/outbound/orders/${orderId}/issued-labels`,
    method: 'get'
  })
}

export function returnByLabels(orderId, data) {
  return request({
    url: `/outbound/orders/${orderId}/return-by-labels`,
    method: 'post',
    data
  })
}
