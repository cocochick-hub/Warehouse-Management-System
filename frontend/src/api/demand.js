import request from './request'

/** 手工创建需求 */
export function createDemandApi(items) {
  return request.post('/demand/create', { items })
}

/** 分页查询需求列表 */
export function listDemandsApi(params) {
  return request.get('/demand/list', { params })
}

/** 查询批次详情 */
export function getDemandBatchApi(batchNo) {
  return request.get(`/demand/batch/${batchNo}`)
}
