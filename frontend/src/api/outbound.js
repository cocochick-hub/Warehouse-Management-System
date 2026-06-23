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
