import { useApi } from '@/utils/useApi'

export interface BadgeDTO {
  id: number
  name: string
  description: string
  type: 'SPOT' | 'ACHIEVEMENT'
  rarity: 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY'
  icon: string
  unlockCondition: string
  unlocked: boolean
  unlockedAt?: string
  note?: string
}

export function useBadgeApi() {
  const { get, post } = useApi()

  function listBadges(): Promise<{ code: number; message: string; data: BadgeDTO[] }> {
    return get<BadgeDTO[]>('/api/badges')
  }

  function unlockBadge(id: number): Promise<{ code: number; message: string; data: BadgeDTO }> {
    return post<BadgeDTO>(`/api/badges/${id}/unlock`)
  }

  return { listBadges, unlockBadge }
}
