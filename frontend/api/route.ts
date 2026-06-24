import { useApi } from '@/utils/useApi'

export interface RouteSpot {
  displayName: string
  spotId: number | null
  name: string
  address: string
  latitude: number | null
  longitude: number | null
  matched: boolean
}

export interface RecommendRoute {
  id: string
  name: string
  theme: string
  description: string
  estimatedTime: string
  guideText: string
  spots: RouteSpot[]
}

export async function getRecommendRoutes(): Promise<RecommendRoute[]> {
  const { get } = useApi()
  const res = await get<RecommendRoute[]>('/api/routes/recommend', undefined, {
    withToken: false
  })

  return res.data
}