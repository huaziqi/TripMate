import { useApi } from '@/utils/useApi'

const { post: apiPost, get } = useApi()

export interface FollowStats {
  followerCount: number
  followingCount: number
  isFollowing: boolean
}

export function toggleFollow(userId: number) {
  return apiPost<{ following: boolean; followerCount: number }>(`/api/users/${userId}/follow`)
}

export function fetchFollowStats(userId: number) {
  return get<FollowStats>(`/api/users/${userId}/stats`)
}
