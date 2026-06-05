import { reactive } from 'vue'
import { wxLogin, updateProfile } from '@/api/auth'

const USER_INFO_KEY = 'userInfo'

interface UserInfo {
  openid: string
  nickname: string
  avatarUrl: string
}

// Module-level singleton — all components share this state
const authState = reactive({
  isLoggedIn: false,
  userInfo: null as UserInfo | null
})

export function useAuth() {

  function loadFromStorage() {
    const token = uni.getStorageSync('token')
    const raw = uni.getStorageSync(USER_INFO_KEY)
    if (token && raw) {
      try {
        authState.userInfo = JSON.parse(raw) as UserInfo
        authState.isLoggedIn = true
      } catch {
        logout()
      }
    }
  }

  async function login(): Promise<void> {
    return new Promise((resolve, reject) => {
      uni.login({
        provider: 'weixin',
        success: async (loginRes) => {
          try {
            const res = await wxLogin(loginRes.code)
            if (res.code === 200) {
              const { token, openid, nickname, avatarUrl } = res.data
              uni.setStorageSync('token', token)
              const info: UserInfo = { openid, nickname, avatarUrl }
              uni.setStorageSync(USER_INFO_KEY, JSON.stringify(info))
              authState.userInfo = info
              authState.isLoggedIn = true
              resolve()
            } else {
              reject(new Error(res.message))
            }
          } catch (e) {
            reject(e)
          }
        },
        fail: (err) => reject(err)
      })
    })
  }

  function logout() {
    uni.removeStorageSync('token')
    uni.removeStorageSync(USER_INFO_KEY)
    authState.isLoggedIn = false
    authState.userInfo = null
  }

  async function saveProfile(nickname: string, avatarUrl: string): Promise<void> {
    await updateProfile(nickname, avatarUrl)
    if (authState.userInfo) {
      authState.userInfo.nickname = nickname
      authState.userInfo.avatarUrl = avatarUrl
      uni.setStorageSync(USER_INFO_KEY, JSON.stringify(authState.userInfo))
    }
  }

  return { authState, login, logout, loadFromStorage, saveProfile }
}
