import request from './request'

/**
 * 分页查询可转包看板列表
 * @param {Object} params - 查询参数
 * @param {string} params.materialCode - 物料编码（可选，模糊匹配）
 * @param {string} params.supplierName - 供应商名称（可选，模糊匹配）
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页大小
 */
export function listAvailableKanbans(params) {
  return request({
    url: '/transfer/kanbans',
    method: 'get',
    params
  })
}

/**
 * 分页查询转包历史记录
 * @param {Object} params - 查询参数
 * @param {string} params.sourceKanbanNo - 源看板号（可选，模糊匹配）
 * @param {string} params.targetKanbanNo - 目标看板号（可选，模糊匹配）
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页大小
 */
export function listTransferHistory(params) {
  return request({
    url: '/transfer/history',
    method: 'get',
    params
  })
}

/**
 * 扫码查询看板信息（转包入口）
 * @param {string} kanbanNo - 看板号
 */
export function getTransferLabel(kanbanNo) {
  return request({
    url: `/transfer/label?kanbanNo=${encodeURIComponent(kanbanNo)}`,
    method: 'get'
  })
}

/**
 * 执行转包
 * @param {Object} data - 转包请求
 * @param {string} data.sourceKanbanNo - 源看板号
 * @param {string} data.targetKanbanNo - 目标看板号（可选，不填自动生成）
 * @param {number} data.transferQty - 转移数量
 */
export function executeTransfer(data) {
  return request({
    url: '/transfer/execute',
    method: 'post',
    data
  })
}
