import request from './request'

export function getSuppliersApi() {
  return request({
    url: '/basic/suppliers',
    method: 'get'
  })
}

export function getMaterialsApi(params) {
  return request({
    url: '/basic/materials',
    method: 'get',
    params
  })
}

export function getPackagingApi() {
  return request({
    url: '/basic/packaging',
    method: 'get'
  })
}
