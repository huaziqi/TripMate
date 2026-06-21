import { createI18n } from 'vue-i18n'

import zh from './locales/zh.json'
import en from './locales/en.json'

const LOCALE_KEY = 'app_locale'

// 从存储恢复上次选择的语言
let savedLocale = 'zh'
try {
  const stored = uni.getStorageSync(LOCALE_KEY)
  if (stored === 'en' || stored === 'zh') savedLocale = stored
} catch {
  // 首次启动时 uni 可能未就绪，静默忽略
}

const i18n = createI18n({
  legacy: false,
  locale: savedLocale,
  fallbackLocale: 'en',
  messages: { zh, en }
})

export function setLocale(lang: 'zh' | 'en') {
  i18n.global.locale.value = lang
  try {
    uni.setStorageSync(LOCALE_KEY, lang)
  } catch {
    // ignore
  }
}

export default i18n