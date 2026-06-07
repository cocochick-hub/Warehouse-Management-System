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
