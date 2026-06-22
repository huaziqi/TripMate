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
	  timeout:500,
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
      fail: (err) => {
              console.error('搜索请求失败：', err)
              reject(err)             // 超时或网络错误都会走到这里
    }
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
}
export function getScenicSpotById(
  id: number
): Promise<ScenicSpot> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}/api/spots/${id}`,
      method: 'GET',

      success: (res) => {
        console.log('景点详情响应：', res.data)

        if (
          res.statusCode >= 200 &&
          res.statusCode < 300 &&
          res.data
        ) {
          resolve(res.data as ScenicSpot)
          return
        }

        reject(new Error(`景点详情接口异常：${res.statusCode}`))
      },

      fail: (error) => {
        console.error('景点详情请求失败：', error)
        reject(error)
      }
    })
  })
}

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
