// frontend/api/upload.ts
const BASE_URL = 'http://localhost:8080'

export function uploadImage(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: BASE_URL + '/api/upload',
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.code === 200 && data.data?.url) {
            resolve(data.data.url)
          } else {
            uni.showToast({ title: data.message || '上传失败', icon: 'none' })
            reject(new Error(data.message))
          }
        } catch {
          reject(new Error('解析响应失败'))
        }
      },
      fail: () => {
        uni.showToast({ title: '上传失败，请重试', icon: 'none' })
        reject(new Error('上传失败'))
      }
    })
  })
}
