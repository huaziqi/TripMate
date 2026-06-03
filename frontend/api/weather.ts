import { useApi } from '@/utils/useApi'
import type { WeatherData } from '@/types/weather'

const { post } = useApi()

/**
 * 获取天气信息
 * 前端只上报坐标，高德调用由后端完成
 *
 * @param longitude 经度
 * @param latitude  纬度
 *
 * TODO: 后端接口 POST /api/weather 待实现
 */
export function fetchWeather(longitude: number, latitude: number) {
  return post<WeatherData>('/api/weather', { longitude, latitude })
}
