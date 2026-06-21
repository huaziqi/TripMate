const BASE_URL = 'http://127.0.0.1:8080'

export interface ScenicSpot {
  id: number
  name: string
  address: string
  description: string
  latitude: number
  longitude: number
  imageUrl: string | null
  audioUrl: string | null
  region: string
  category: string
  distance?: number 
}

export interface NearbySpot {
  id: number
  name: string
  address: string
  category: string
  latitude: number
  longitude: number
  distance: number
}

export function searchScenicSpots(
  keyword: string
): Promise<ScenicSpot[]> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}/api/spots/search`,
      method: 'GET',
      data: {
        keyword
      },
      success: (res) => {
        if (
          res.statusCode >= 200 &&
          res.statusCode < 300 &&
          Array.isArray(res.data)
        ) {
          resolve(res.data as ScenicSpot[])
          return
        }

        reject(new Error(`搜索接口异常：${res.statusCode}`))
      },
      fail: reject
    })
  })
}

// 新增：查询附近景点
export function getNearbySpots(
  latitude: number,
  longitude: number,
  limit = 3
): Promise<NearbySpot[]> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}/api/spots/nearby`,
      method: 'GET',
      data: {
        latitude,
        longitude,
        limit
      },
      success: (res) => {
        console.log('附近景点响应：', res.data)

        if (
          res.statusCode >= 200 &&
          res.statusCode < 300 &&
          Array.isArray(res.data)
        ) {
          resolve(res.data as NearbySpot[])
          return
        }

        reject(new Error(`附近景点接口异常：${res.statusCode}`))
      },
      fail: (error) => {
        console.error('附近景点请求失败：', error)
        reject(error)
      }
    })
  })
}import { useApi } from '@/utils/useApi'

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
