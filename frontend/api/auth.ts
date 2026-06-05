import { useApi } from '@/utils/useApi'

export interface WxLoginResponse {
  token: string
  openid: string
  nickname: string
  avatarUrl: string
}

export function wxLogin(code: string) {
  const { post } = useApi()
  return post<WxLoginResponse>('/api/wx/login', { code }, { withToken: false })
}

export function updateProfile(nickname: string, avatarUrl: string) {
  const { post } = useApi()
  return post<void>('/api/wx/profile', { nickname, avatarUrl })
}
