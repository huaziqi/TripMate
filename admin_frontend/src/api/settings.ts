import http from './http'

export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  description: string
}

export function listSettings() {
  return http.get<{ code: number; data: SystemConfig[] }>('/admin/settings')
}

export function updateSetting(key: string, value: string) {
  return http.put(`/admin/settings/${key}`, { value })
}
