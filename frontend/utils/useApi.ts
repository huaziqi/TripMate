// 后端接口基础地址，开发时指向本地 Spring Boot 服务
export const BASE_URL = 'http://localhost:8080'

// 统一响应结构（与后端约定保持一致）
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 请求选项
interface RequestOptions {
  headers?: Record<string, string>
  // 是否携带 token，默认 true
  withToken?: boolean
}

type HttpMethod = 'GET' | 'POST' | 'DELETE' | 'PUT'

// 封装底层 uni.request，返回 Promise
function request<T = any>(
  method: HttpMethod,
  url: string,
  data?: Record<string, any>,
  options: RequestOptions = {}
): Promise<ApiResponse<T>> {
  const { headers = {}, withToken = true } = options

  // 自动携带 token
  if (withToken) {
    const token = uni.getStorageSync('token')
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...headers
      },
      success: (res) => {
        const result = res.data as ApiResponse<T>

        if (res.statusCode === 200) {
          // 后端 Result.code 不是 200 时，也认为业务失败
          if (result && typeof result.code === 'number' && result.code !== 200) {
            uni.showToast({
              title: result.message || '请求失败',
              icon: 'none'
            })
            reject(new Error(result.message || '请求失败'))
            return
          }

          resolve(result)
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.showToast({ title: '请重新登录', icon: 'none' })
          reject(new Error('未授权'))
        } else {
          uni.showToast({
            title: result?.message || '请求失败',
            icon: 'none'
          })
          reject(new Error(result?.message || '请求失败'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        reject(err)
      }
    })
  })
}

// 对外暴露的 useApi composable
export function useApi() {
  function get<T = any>(
    url: string,
    params?: Record<string, any>,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return request<T>('GET', url, params, options)
  }

  function post<T = any>(
    url: string,
    body?: Record<string, any>,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return request<T>('POST', url, body, options)
  }

  function put<T = any>(
    url: string,
    body?: Record<string, any>,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return request<T>('PUT', url, body, options)
  }

  function del<T = any>(
    url: string,
    data?: Record<string, any>,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return request<T>('DELETE', url, data, options)
  }

  return {
    get,
    post,
    put,
    del
  }
}