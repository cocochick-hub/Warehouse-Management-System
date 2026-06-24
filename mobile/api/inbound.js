import { request } from './request'

/** 扫码查询看板标签 */
export function getScanLabel(kanbanNo) {
  return request({
    url: `/api/inbound/scan/labels/${encodeURIComponent(kanbanNo)}`,
    method: 'GET'
  })
}

/** 扫码执行入库收货 */
export function receiveByScan(kanbanNo) {
  return request({
    url: '/api/inbound/scan/receive',
    method: 'POST',
    data: { kanbanNo }
  })
}
