// 后端接口基础地址，开发时指向本地 Spring Boot 服务
const BASE_URL = 'http://localhost:8080'

// 统一响应结构（与后端约定保持一致）
interface ApiResponse<T = any> {
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

// 封装底层 uni.request，返回 Promise
function request<T = any>(
  method: 'GET' | 'POST',
  url: string,
  data?: Record<string, any>,
  options: RequestOptions = {}
): Promise<ApiResponse<T>> {
  const { headers = {}, withToken = true } = options

  // 自动携带 token（存储在 storage 中）
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

        // HTTP 层面成功，再判断业务状态码
        if (res.statusCode === 200) {
          resolve(result)
        } else if (res.statusCode === 401) {
          // token 过期或未登录
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
  /**
   * GET 请求
   * @param url    接口路径，如 '/api/user/info'
   * @param params 查询参数（拼到 url query）
   * @param options 额外选项
   */
  function get<T = any>(
    url: string,
    params?: Record<string, any>,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    // uni.request GET 时 data 会自动序列化为 query string
    return request<T>('GET', url, params, options)
  }

  /**
   * POST 请求
   * @param url  接口路径，如 '/api/user/login'
   * @param body 请求体（JSON）
   * @param options 额外选项
   */
  function post<T = any>(
    url: string,
    body?: Record<string, any>,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    return request<T>('POST', url, body, options)
  }

  return { get, post }
}
