import { useApi } from '@/utils/useApi'

export interface HistoryRecord {
  id: number
  userId: number
  type: string
  targetId: number | null
  content: string
  createTime: string
}

export function addHistory(
  type: string,
  targetId: number | null,
  content: string
) {
  const { post } = useApi()

  return post<HistoryRecord>('/api/history', {
    type,
    targetId,
    content
  })
}

export async function getHistoryList(): Promise<HistoryRecord[]> {
  const { get } = useApi()
  const res = await get<HistoryRecord[]>('/api/history')
  return res.data
}