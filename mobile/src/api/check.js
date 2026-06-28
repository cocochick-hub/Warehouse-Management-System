import { request } from './request'

/** 获取进行中的盘点任务列表 */
export function getActiveTasks() {
  return request({
    url: '/api/check/tasks/active',
    method: 'GET'
  })
}

/** 获取盘点明细（扫码前查看） */
export function getCheckDetail(detailId) {
  return request({
    url: `/api/check/details/${detailId}`,
    method: 'GET'
  })
}

/** 扫码盘点 */
export function scanCheck(payload) {
  return request({
    url: '/api/check/scan',
    method: 'POST',
    data: payload
  })
}