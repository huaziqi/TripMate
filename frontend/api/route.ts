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

// ---------- 个性化推荐 ----------

/** 兴趣问卷选项（后端下发） */
export interface InterestOption {
  key: string
  label: string
  description: string
  icon: string
}

/** 游客问卷画像 */
export interface PersonalizeRequest {
  interests: string[]
  /** half(≤3小时) / most(4-5小时) / full(6小时以上) */
  duration: string
  /** solo / partner / kids / elder */
  companions: string
  /** low / medium / high */
  stamina: string
  /** 自由描述，可选 */
  freeText?: string
}

export interface PersonalizedRouteSpot extends RouteSpot {
  /** 按兴趣挑选的讲解重点 */
  focusText: string
  /** 讲解重点对应的兴趣维度名，如 历史文化 */
  focusLabel: string | null
}

export interface PersonalizedRoute {
  id: string
  name: string
  theme: string
  description: string
  estimatedTime: string
  guideText: string
  matchScore: number
  matchReasons: string[]
  tags: string[]
  suitableFor: string
  spots: PersonalizedRouteSpot[]
}

export interface PersonalizedRecommendation {
  profileSummary: string[]
  interestWeights: Record<string, number>
  routes: PersonalizedRoute[]
}

export async function getRecommendRoutes(): Promise<RecommendRoute[]> {
  const { get } = useApi()
  const res = await get<RecommendRoute[]>('/api/routes/recommend', undefined, {
    withToken: false
  })

  return res.data
}

export async function getInterestOptions(): Promise<InterestOption[]> {
  const { get } = useApi()
  const res = await get<InterestOption[]>('/api/routes/recommend/options', undefined, {
    withToken: false
  })

  return res.data
}

/** 个性化推荐：携带 token 时后端会叠加收藏/浏览/对话行为画像 */
export async function getPersonalizedRoutes(
  profile: PersonalizeRequest
): Promise<PersonalizedRecommendation> {
  const { post } = useApi()
  const res = await post<PersonalizedRecommendation>(
    '/api/routes/recommend/personalized',
    profile
  )

  return res.data
}
