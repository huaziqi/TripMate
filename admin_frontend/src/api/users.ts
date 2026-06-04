import http from './http'

export interface AdminUser {
  id: number
  username: string
  role: string
  status: number
  createdAt: string
}

export function listUsers() {
  return http.get<{ code: number; data: AdminUser[] }>('/admin/users')
}

export function createUser(data: { username: string; password: string; role: string }) {
  return http.post('/admin/users', data)
}

export function updateUser(id: number, data: { status?: number; password?: string }) {
  return http.put(`/admin/users/${id}`, data)
}

export function deleteUser(id: number) {
  return http.delete(`/admin/users/${id}`)
}
