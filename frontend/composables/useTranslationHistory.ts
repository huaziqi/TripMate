import { ref } from 'vue'

const HISTORY_KEY = 'translation_history'
const MAX_HISTORY = 30

export interface HistoryItem {
  id: string
  sourceText: string
  translatedText: string
  from: string
  to: string
  timestamp: number
}

const history = ref<HistoryItem[]>([])

// 应用启动时加载历史
try {
  const stored = uni.getStorageSync(HISTORY_KEY)
  if (Array.isArray(stored)) {
    history.value = stored
  }
} catch {
  // 忽略读取失败
}

function saveHistory() {
  try {
    uni.setStorageSync(HISTORY_KEY, history.value)
  } catch {
    // 忽略写入失败
  }
}

export function useTranslationHistory() {
  function addHistory(item: Omit<HistoryItem, 'id' | 'timestamp'>) {
    const newItem: HistoryItem = {
      ...item,
      id: Date.now().toString(),
      timestamp: Date.now()
    }
    // 去重：如果源文本和语言对相同则移除旧的
    history.value = history.value.filter(
      h => !(h.sourceText === item.sourceText && h.from === item.from && h.to === item.to)
    )
    history.value.unshift(newItem)
    if (history.value.length > MAX_HISTORY) {
      history.value = history.value.slice(0, MAX_HISTORY)
    }
    saveHistory()
  }

  function removeHistory(id: string) {
    history.value = history.value.filter(h => h.id !== id)
    saveHistory()
  }

  function clearHistory() {
    history.value = []
    saveHistory()
  }

  function formatTime(timestamp: number): string {
    const date = new Date(timestamp)
    const now = new Date()
    const diff = now.getTime() - timestamp
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    return `${month}-${day}`
  }

  return { history, addHistory, removeHistory, clearHistory, formatTime }
}
