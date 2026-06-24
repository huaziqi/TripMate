import { BASE_URL, useApi } from '@/utils/useApi'

export interface GuideMessageDTO {
  role: 'USER' | 'ASSISTANT' | string
  content: string
  createdAt?: string
}

export interface LocalChatMessageDTO {
  role: 'USER' | 'ASSISTANT' | string
  content: string
}

export interface ChatStreamOptions {
  message: string
  history?: LocalChatMessageDTO[]
  onDelta?: (delta: string) => void
  onDone?: () => void
  onError?: (error: string) => void
}

export interface ChatStreamTask {
  abort: () => void
}

/**
 * 获取数字人聊天历史
 */
export async function getCompanionHistory() {
  const api = useApi()

  const res = await api.get<GuideMessageDTO[]>('/api/companion/history')

  return res.data || []
}

/**
 * 清空数字人聊天历史
 */
export async function clearCompanionHistory() {
  const api = useApi()

  await api.del<void>('/api/companion/history')
}

/**
 * 数字人 SSE 流式聊天
 *
 * 后端返回格式：
 * data: {"delta":"你好"}
 * data: {"done":true}
 * data: {"error":"xxx"}
 */
export function chatWithCompanionStream(options: ChatStreamOptions): ChatStreamTask {
  const { message, history = [], onDelta, onDone, onError } = options

  const token = uni.getStorageSync('token')

  // #ifdef MP-WEIXIN
  let sseBuffer = ''
  let finished = false

  const requestTask = wx.request({
    url: `${BASE_URL}/api/companion/chat`,
    method: 'POST',
    enableChunked: true,
    responseType: 'arraybuffer',
    header: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    data: {
      message,
      history
    },
    success() {
      // 微信小程序 chunk 数据主要通过 onChunkReceived 接收
    },
    fail(err) {
      console.error('[companion] stream request fail:', err)

      if (!finished) {
        finished = true
        onError?.('网络异常，请稍后再试')
      }
    },
    complete() {
      // 如果后端已经发 done，这里不重复处理
    }
  })

  requestTask.onChunkReceived((res) => {
    try {
      const chunkText = decodeArrayBuffer(res.data)
      sseBuffer += chunkText

      const events = sseBuffer.split(/\r?\n\r?\n/)
      sseBuffer = events.pop() || ''

      for (const eventText of events) {
        handleSseEvent(eventText, {
          onDelta,
          onDone: () => {
            if (!finished) {
              finished = true
              onDone?.()
            }
          },
          onError: (error) => {
            if (!finished) {
              finished = true
              onError?.(error)
            }
          }
        })
      }
    } catch (err) {
      console.error('[companion] handle chunk error:', err)

      if (!finished) {
        finished = true
        onError?.('解析回复失败')
      }
    }
  })

  return {
    abort() {
      try {
        requestTask.abort()
      } catch (e) {}
    }
  }
  // #endif

  // #ifndef MP-WEIXIN
  uni.request({
    url: `${BASE_URL}/api/companion/chat`,
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    data: {
      message,
      history
    },
    success() {
      onError?.('当前平台暂未适配流式聊天，请在微信小程序中使用')
    },
    fail() {
      onError?.('网络异常，请稍后再试')
    }
  })

  return {
    abort() {}
  }
  // #endif
}

function handleSseEvent(
  eventText: string,
  callbacks: {
    onDelta?: (delta: string) => void
    onDone?: () => void
    onError?: (error: string) => void
  }
) {
  const lines = eventText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  for (const line of lines) {
    if (!line.startsWith('data:')) continue

    const dataText = line.replace(/^data:\s*/, '')

    if (!dataText || dataText === '[DONE]') {
      callbacks.onDone?.()
      return
    }

    try {
      const data = JSON.parse(dataText)

      if (data.delta) {
        callbacks.onDelta?.(data.delta)
      }

      if (data.done) {
        callbacks.onDone?.()
      }

      if (data.error) {
        callbacks.onError?.(data.error)
      }
    } catch (err) {
      console.warn('[companion] parse sse data failed:', dataText, err)
    }
  }
}

function decodeArrayBuffer(buffer: ArrayBuffer) {
  try {
    // 微信小程序新版基础库一般支持 TextDecoder
    // @ts-ignore
    if (typeof TextDecoder !== 'undefined') {
      // @ts-ignore
      return new TextDecoder('utf-8').decode(buffer)
    }
  } catch (e) {}

  const uint8Array = new Uint8Array(buffer)
  let result = ''

  for (let i = 0; i < uint8Array.length; i++) {
    result += String.fromCharCode(uint8Array[i])
  }

  try {
    return decodeURIComponent(escape(result))
  } catch (e) {
    return result
  }
}