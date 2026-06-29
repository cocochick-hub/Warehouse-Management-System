import request from './request'

/** 创建盘点任务 */
export function createTaskApi(data) {
  return request({
    url: '/check/tasks',
    method: 'POST',
    data
  })
}

/** 盘点任务列表 */
export function listTasksApi() {
  return request({
    url: '/check/tasks',
    method: 'GET'
  })
}

/** 任务详情 */
export function getTaskDetailsApi(taskId) {
  return request({
    url: `/check/tasks/${taskId}`,
    method: 'GET'
  })
}

/** 完成任务 */
export function completeTaskApi(taskId) {
  return request({
    url: `/check/tasks/${taskId}/complete`,
    method: 'POST'
  })
}

/** 差异调整 */
export function adjustDetailApi(detailId, adjustQty, username) {
  return request({
    url: `/check/details/${detailId}/adjust`,
    method: 'POST',
    params: { adjustQty, username }
  })
}