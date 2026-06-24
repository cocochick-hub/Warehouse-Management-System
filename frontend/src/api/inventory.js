import request from './request'

export function getInventoryStocksApi(params) {
  return request({
    url: '/inventory/stocks',
    method: 'get',
    params
  })
}

export function getInventoryLabelsApi(params) {
  return request({
    url: '/inventory/stocks/labels',
    method: 'get',
    params
  })
}
