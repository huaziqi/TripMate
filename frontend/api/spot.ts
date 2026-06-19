import { useApi } from '@/utils/useApi'

export interface Spot {
  id: number
  name: string
  address: string
  region: string
  category: string
  latitude: number
  longitude: number
  imageUrl?: string
}

export function useSpotApi() {
  const { get } = useApi()

  function listSpots(): Promise<Spot[]> {
    return get<Spot[]>('/api/spots').then(r => r.data ?? [])
  }

  function searchSpots(keyword: string): Promise<Spot[]> {
    return get<Spot[]>('/api/spots/search', { keyword }).then(r => r.data ?? [])
  }

  return { listSpots, searchSpots }
}
