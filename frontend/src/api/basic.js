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

// 库区管理 API
export function getWarehouseAreasApi() {
  return request({
    url: '/basic/warehouse-areas',
    method: 'get'
  })
}

export function createWarehouseAreaApi(data) {
  return request({
    url: '/basic/warehouse-areas',
    method: 'post',
    data
  })
}

export function updateWarehouseAreaApi(id, data) {
  return request({
    url: `/basic/warehouse-areas/${id}`,
    method: 'put',
    data
  })
}

export function deleteWarehouseAreaApi(id) {
  return request({
    url: `/basic/warehouse-areas/${id}`,
    method: 'delete'
  })
}
