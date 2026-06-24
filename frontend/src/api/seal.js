import request from './request'

// 封存管理 API
export function getSealLabelApi(kanbanNo) {
  return request({
    url: '/seal/label',
    method: 'get',
    params: { kanbanNo }
  })
}

export function getSealedLabelsApi(params) {
  return request({
    url: '/seal/sealed-labels',
    method: 'get',
    params
  })
}

export function toggleSealApi(data) {
  return request({
    url: '/seal/toggle',
    method: 'post',
    data
  })
}

export function toggleSealBatchApi(data) {
  return request({
    url: '/seal/toggle-batch',
    method: 'post',
    data
  })
}
