import request from './request'

// ==================== 供应商管理 ====================

export function getSuppliersApi() {
  return request({
    url: '/basic/suppliers',
    method: 'get'
  })
}

export function createSupplierApi(data) {
  return request({
    url: '/basic/suppliers',
    method: 'post',
    data
  })
}

export function updateSupplierApi(id, data) {
  return request({
    url: `/basic/suppliers/${id}`,
    method: 'put',
    data
  })
}

export function deleteSupplierApi(id) {
  return request({
    url: `/basic/suppliers/${id}`,
    method: 'delete'
  })
}

// ==================== 物料管理 ====================

export function getMaterialsApi(params) {
  return request({
    url: '/basic/materials',
    method: 'get',
    params
  })
}

export function createMaterialApi(data) {
  return request({
    url: '/basic/materials',
    method: 'post',
    data
  })
}

export function updateMaterialApi(id, data) {
  return request({
    url: `/basic/materials/${id}`,
    method: 'put',
    data
  })
}

export function deleteMaterialApi(id) {
  return request({
    url: `/basic/materials/${id}`,
    method: 'delete'
  })
}

// ==================== 包装管理 ====================

export function getPackagingApi() {
  return request({
    url: '/basic/packaging',
    method: 'get'
  })
}

export function createPackagingApi(data) {
  return request({
    url: '/basic/packaging',
    method: 'post',
    data
  })
}

export function updatePackagingApi(id, data) {
  return request({
    url: `/basic/packaging/${id}`,
    method: 'put',
    data
  })
}

export function deletePackagingApi(id) {
  return request({
    url: `/basic/packaging/${id}`,
    method: 'delete'
  })
}

// ==================== 库区管理 ====================

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