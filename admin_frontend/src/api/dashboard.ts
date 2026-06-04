import http from './http'

export interface DashboardData {
  adminUserCount: number
  configCount: number
}

export function getDashboard() {
  return http.get<{ code: number; data: DashboardData }>('/admin/dashboard')
}
