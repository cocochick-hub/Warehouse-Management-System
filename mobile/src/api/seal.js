import { request } from './request'

/** 根据看板号查询封存信息 */
export function getSealLabel(kanbanNo) {
  return request({
    url: `/api/seal/label?kanbanNo=${encodeURIComponent(kanbanNo)}`,
    method: 'GET'
  })
}

/** 查询已封存的看板列表 */
export function getSealedLabels(params) {
  return request({
    url: '/api/seal/sealed-labels',
    method: 'GET',
    data: params
  })
}

/** 单个封存/解封 */
export function toggleSeal(data) {
  return request({
    url: '/api/seal/toggle',
    method: 'POST',
    data
  })
}
