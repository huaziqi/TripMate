import http from './http'

export interface LoginResponse {
  token: string
  role: string
  username: string
}

export function login(username: string, password: string) {
  return http.post<{ code: number; data: LoginResponse }>('/admin/login', { username, password })
}
