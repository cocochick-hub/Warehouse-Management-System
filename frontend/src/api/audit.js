import request from './request'

/** 分页查询操作日志 */
export function getAuditLogs(params) {
  return request({
    url: '/audit/list',
    method: 'get',
    params
  })
}
