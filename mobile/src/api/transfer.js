import { request } from './request'

/** 扫码查询看板信息（转包入口） */
export function getTransferLabel(kanbanNo) {
  return request({
    url: `/api/transfer/label?kanbanNo=${encodeURIComponent(kanbanNo)}`,
    method: 'GET'
  })
}

/** 执行转包 */
export function executeTransfer(data) {
  return request({
    url: '/api/transfer/execute',
    method: 'POST',
    data
  })
}
