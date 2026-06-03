import { ref } from 'vue'

const ELDER_MODE_KEY  = 'elder_mode'
const ELDER_SCALE_KEY = 'elder_font_scale'
const ELDER_SCALE     = 1.4
const NORMAL_SCALE    = 1

// ----------------------------------------------------------------
// 模块级单例 —— 所有组件共享同一份响应式状态
// ----------------------------------------------------------------
const isElderMode = ref<boolean>(false)
const fontScale   = ref<number>(NORMAL_SCALE)

// 应用启动时从持久化存储中恢复状态
try {
  const stored = uni.getStorageSync(ELDER_MODE_KEY)
  if (stored === true) {
    isElderMode.value = true
    fontScale.value   = ELDER_SCALE
  }
} catch {
  // 读取失败时保持默认值，不影响正常使用
}

// ----------------------------------------------------------------
// 内部：同步写入持久化存储
// ----------------------------------------------------------------
function persist() {
  uni.setStorageSync(ELDER_MODE_KEY,  isElderMode.value)
  uni.setStorageSync(ELDER_SCALE_KEY, fontScale.value)
}

// ----------------------------------------------------------------
// 对外暴露的 composable
// ----------------------------------------------------------------
export function useElder() {

  function enableElderMode() {
    isElderMode.value = true
    fontScale.value   = ELDER_SCALE
    persist()
  }

  function disableElderMode() {
    isElderMode.value = false
    fontScale.value   = NORMAL_SCALE
    persist()
  }

  function toggleElderMode() {
    isElderMode.value ? disableElderMode() : enableElderMode()
  }

  /**
   * 将基准 rpx 值按当前缩放比例换算，供模板 :style 绑定使用。
   * 必须在 computed / 模板表达式中调用，Vue 才能追踪 fontScale 的变化。
   *
   * @example
   * // <text :style="{ fontSize: rpx(32) }">
   */
  function rpx(base: number): string {
    return `${Math.round(base * fontScale.value)}rpx`
  }

  return {
    isElderMode,
    fontScale,
    enableElderMode,
    disableElderMode,
    toggleElderMode,
    rpx
  }
}
