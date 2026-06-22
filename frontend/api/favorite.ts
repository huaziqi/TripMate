import { useApi } from '@/utils/useApi'
import type { ScenicSpot } from '@/api/spot'

export function addFavorite(spotId: number) {
  const { post } = useApi()
  return post<void>(`/api/favorites/${spotId}`)
}

export function removeFavorite(spotId: number) {
  const { del } = useApi()
  return del<void>(`/api/favorites/${spotId}`)
}

export async function checkFavorite(spotId: number): Promise<boolean> {
  const { get } = useApi()
  const res = await get<boolean>(`/api/favorites/check/${spotId}`)
  return res.data
}

export async function getFavoriteSpots(): Promise<ScenicSpot[]> {
  const { get } = useApi()
  const res = await get<ScenicSpot[]>('/api/favorites')
  return res.data
}