import { request } from './request'

/** 扫码查询出库标签（含FIFO校验） */
export function getOutboundScanLabel(kanbanNo) {
  return request({
    url: `/api/outbound/scan/labels/${encodeURIComponent(kanbanNo)}`,
    method: 'GET'
  })
}

/** 扫码执行出库（带单出库，需 outboundOrderId） */
export function issueByScan(payload) {
  return request({
    url: '/api/outbound/scan/issue',
    method: 'POST',
    data: payload
  })
}

/** 不带单出库（移动端扫码出库，自动创建出库单） */
export function orderlessIssue(payload) {
  return request({
    url: '/api/outbound/scan/orderless-issue',
    method: 'POST',
    data: payload
  })
}

/** 获取出库单列表（用于带单出库选单） */
export function getOutboundOrders(params) {
  return request({
    url: '/api/outbound/orders',
    method: 'GET',
    data: params
  })
}

/** 查询退库信息 */
export function getReturnLabel(kanbanNo) {
  return request({
    url: `/api/outbound/return/labels/${encodeURIComponent(kanbanNo)}`,
    method: 'GET'
  })
}

/** 执行退库 */
export function doReturn(kanbanNo) {
  return request({
    url: '/api/outbound/return',
    method: 'POST',
    data: { kanbanNo }
  })
}
