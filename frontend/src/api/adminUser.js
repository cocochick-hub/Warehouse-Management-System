import request from './request'

export function listUsersApi() {
  return request({
    url: '/admin/users',
    method: 'get'
  })
}

export function createUserApi(data) {
  return request({
    url: '/admin/users',
    method: 'post',
    data
  })
}

export function updateUserRoleApi(id, role) {
  return request({
    url: `/admin/users/${id}/role`,
    method: 'put',
    data: { role }
  })
}

export function updateUserStatusApi(id, status) {
  return request({
    url: `/admin/users/${id}/status`,
    method: 'put',
    data: { status }
  })
}
