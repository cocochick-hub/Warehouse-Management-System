import request from './request'

export function getInventoryStocksApi(params) {
  return request({
    url: '/inventory/stocks',
    method: 'get',
    params
  })
}
