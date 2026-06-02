import request from './request'

export function getDashboardDataApi() {
  return request({
    url: '/dashboard/data',
    method: 'get'
  })
}
