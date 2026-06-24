import request from './request'

/** 获取所有物料的高低储预警阈值列表（含库存状态） */
export function getAlertThresholdsApi() {
  return request.get('/alert/thresholds')
}

/** 批量保存阈值 */
export function saveAlertThresholdsApi(items) {
  return request.post('/alert/thresholds', { items })
}
