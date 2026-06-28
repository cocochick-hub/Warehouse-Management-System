import { request } from './request'

export function loginApi(username, password) {
  return request({
    url: '/api/auth/login',
    method: 'POST',
    data: { username, password }
  })
}

export function getUserInfoApi() {
  return request({
    url: '/api/auth/userInfo',
    method: 'GET'
  })
}
