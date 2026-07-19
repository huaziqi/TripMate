import { BASE_URL } from '@/utils/useApi'

export type AsrResultDTO = {
  text: string
  sid: string
}

export type ApiResponse<T = any> = {
  code: number
  message: string
  data: T
}

/**
 * 上传录音文件进行语音转文字
 */
export function recognizeAudio(params: {
  filePath: string
  format?: 'pcm' | 'wav' | 'mp3'
  language?: 'zh_cn' | 'en_us'
}): Promise<ApiResponse<AsrResultDTO>> {
  const { filePath, format = 'wav', language = 'zh_cn' } = params

  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')

    uni.uploadFile({
      url: `${BASE_URL}/api/asr/recognize`,
      filePath,
      name: 'file',
      formData: {
        format,
        language
      },
      header: token
        ? {
            Authorization: `Bearer ${token}`
          }
        : {},
      success: (res) => {
        try {
          const data = JSON.parse(res.data) as ApiResponse<AsrResultDTO>

          if (res.statusCode === 200) {
            if (typeof data.code === 'number' && data.code !== 200) {
              uni.showToast({
                title: data.message || '语音识别失败',
                icon: 'none'
              })
              reject(new Error(data.message || '语音识别失败'))
              return
            }

            resolve(data)
          } else if (res.statusCode === 401) {
            uni.removeStorageSync('token')
            uni.showToast({
              title: '请重新登录',
              icon: 'none'
            })
            reject(new Error('未授权'))
          } else {
            uni.showToast({
              title: data?.message || '语音识别失败',
              icon: 'none'
            })
            reject(new Error(data?.message || '语音识别失败'))
          }
        } catch (e) {
          reject(new Error('语音识别响应解析失败'))
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '网络异常，请稍后重试',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}