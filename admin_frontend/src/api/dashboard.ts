import http from './http'

export interface DashboardData {
  adminUserCount: number
  configCount: number
}

export function getDashboard() {
  return http.get<{ code: number; data: DashboardData }>('/admin/dashboard')
}

// ==================== 数据大屏概览 ====================

export interface ServicePoint {
  date: string
  questions: number
  sessions: number
}

export interface HotQuestion {
  question: string
  count: number
}

export interface SatisfactionPoint {
  date: string
  avgSatisfaction: number
  visits: number
}

export interface DimItem {
  label: string
  value: number
}

export interface DashboardOverview {
  todayQuestions: number
  weekQuestions: number
  todaySessions: number
  weekSessions: number
  totalSessions: number
  totalUsers: number
  knowledgeCount: number
  serviceTrend: ServicePoint[]
  hotQuestions: HotQuestion[]
  visitorDataReady: boolean
  totalVisits: number
  avgSatisfaction: number
  avgSpend: number
  satisfactionTrend: SatisfactionPoint[]
  satisfactionDist: DimItem[]
  attractionTypeDist: DimItem[]
  ageDist: DimItem[]
  genderDist: DimItem[]
}

export function getOverview() {
  return http.get<{ code: number; data: DashboardOverview }>('/admin/dashboard/overview')
}

export function importVisitorData(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return http.post<{
    code: number
    message: string
    data: { totalRows: number; skippedRows: number; dateFrom: string; dateTo: string }
  }>('/admin/dashboard/import-visitors', fd)
}
