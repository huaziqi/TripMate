<template>
  <view class="page">
    <!-- 界面语言切换栏 -->
    <view class="ui-lang-bar">
      <text class="ui-lang-label" :style="{ fontSize: rpx(22) }">界面语言</text>
      <view class="ui-lang-switcher">
        <view
          class="ui-lang-opt"
          :class="{ active: currentLocale === 'zh' }"
          @click="switchUiLang('zh')"
        >
          <text class="ui-lang-opt-text" :style="{ fontSize: rpx(22) }">🇨🇳 中文</text>
        </view>
        <view
          class="ui-lang-opt"
          :class="{ active: currentLocale === 'en' }"
          @click="switchUiLang('en')"
        >
          <text class="ui-lang-opt-text" :style="{ fontSize: rpx(22) }">🇺🇸 English</text>
        </view>
      </view>
    </view>

    <!-- 标签页导航 -->
    <view class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <text class="tab-text" :style="{ fontSize: rpx(26) }">{{ tab.label }}</text>
        <view v-if="tab.key === 'history' && history.length" class="tab-badge">
          <text class="tab-badge-text">{{ history.length }}</text>
        </view>
      </view>
    </view>

    <!-- ============ 文本翻译 ============ -->
    <scroll-view v-if="activeTab === 'text'" scroll-y class="tab-content">
      <!-- 最近使用语言对 -->
      <view v-if="recentLangPairs.length" class="recent-bar">
        <text class="recent-label" :style="{ fontSize: rpx(22) }">最近：</text>
        <scroll-view scroll-x class="recent-scroll">
          <view class="recent-list">
            <view
              v-for="pair in recentLangPairs"
              :key="pair.key"
              class="recent-chip"
              :class="{ active: fromLang === pair.from && toLang === pair.to }"
              @click="applyLangPair(pair)"
            >
              <text class="recent-chip-text" :style="{ fontSize: rpx(22) }">
                {{ pair.fromFlag }}→{{ pair.toFlag }}
              </text>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 语言选择栏 -->
      <view class="lang-bar">
        <view class="lang-select" @click="showLangPicker('from')">
          <text class="lang-flag">{{ fromLangInfo.flag }}</text>
          <text class="lang-name" :style="{ fontSize: rpx(26) }">{{ fromLangInfo.name }}</text>
          <text class="lang-arrow">▾</text>
        </view>
        <view class="swap-btn" @click="swapLang">
          <text class="swap-icon">⇌</text>
        </view>
        <view class="lang-select" @click="showLangPicker('to')">
          <text class="lang-flag">{{ toLangInfo.flag }}</text>
          <text class="lang-name" :style="{ fontSize: rpx(26) }">{{ toLangInfo.name }}</text>
          <text class="lang-arrow">▾</text>
        </view>
      </view>

      <!-- 输入区 -->
      <view class="input-card">
        <textarea
          class="input-area"
          :placeholder="t('translate.placeholder')"
          :value="inputText"
          :maxlength="500"
          :style="{ fontSize: rpx(28) }"
          @input="onInput"
        />
        <view class="input-footer">
          <text class="char-count" :class="{ warn: inputText.length > 450 }" :style="{ fontSize: rpx(22) }">
            {{ inputText.length }}/500
          </text>
          <view class="input-actions">
            <text v-if="inputText" class="action-btn clear" :style="{ fontSize: rpx(22) }" @click="clearInput">清空</text>
            <text v-if="inputText" class="action-btn paste" :style="{ fontSize: rpx(22) }" @click="pasteFromClipboard">粘贴</text>
          </view>
        </view>
      </view>

      <!-- 翻译按钮 -->
      <button
        class="translate-btn"
        :disabled="translating || !inputText.trim()"
        :class="{ disabled: translating || !inputText.trim() }"
        :style="{ fontSize: rpx(30) }"
        @click="doTranslate"
      >
        <text v-if="translating" class="btn-loading">···</text>
        {{ translating ? t('translate.translating') : t('translate.btn') }}
      </button>

      <!-- 翻译结果 -->
      <view class="result-card" :class="{ 'has-result': resultText }">
        <view class="result-header">
          <view class="result-meta">
            <text class="result-label" :style="{ fontSize: rpx(22) }">{{ t('translate.result') }}</text>
            <text v-if="detectedLang && fromLang === 'auto'" class="detected-lang" :style="{ fontSize: rpx(20) }">
              检测到：{{ langNameByCode(detectedLang) }}
            </text>
          </view>
          <view v-if="resultText" class="result-actions-row">
            <view class="icon-btn" @click="speakResult">
              <text class="icon-btn-text">🔊</text>
            </view>
            <view class="copy-btn" @click="copyResult">
              <text class="copy-text" :style="{ fontSize: rpx(22) }">
                {{ copied ? '✓ ' + t('translate.copied') : t('translate.copy') }}
              </text>
            </view>
          </view>
        </view>
        <text
          v-if="resultText"
          class="result-text"
          :style="{ fontSize: rpx(30) }"
          user-select
        >{{ resultText }}</text>
        <view v-else class="result-empty">
          <text class="result-placeholder" :style="{ fontSize: rpx(26) }">{{ t('translate.noResult') }}</text>
        </view>
      </view>

      <!-- 底部提示 + 字数统计 -->
      <view v-if="resultText" class="result-tip">
        <text class="tip-text" :style="{ fontSize: rpx(22) }">✓ 已自动保存</text>
        <text class="char-stat" :style="{ fontSize: rpx(22) }">
          {{ inputText.trim().length }} → {{ resultText.length }} 字符
        </text>
      </view>
    </scroll-view>

    <!-- ============ 常用短语 ============ -->
    <scroll-view v-if="activeTab === 'phrases'" scroll-y class="tab-content">
      <!-- 搜索框 -->
      <view class="search-bar">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          placeholder="搜索短语..."
          :value="phraseSearch"
          :style="{ fontSize: rpx(26) }"
          @input="(e: any) => phraseSearch = e.detail.value"
        />
        <text v-if="phraseSearch" class="search-clear" @click="phraseSearch = ''">✕</text>
      </view>

      <!-- 目标语言切换 -->
      <view class="phrase-lang-bar">
        <text class="phrase-lang-label" :style="{ fontSize: rpx(24) }">翻译为：</text>
        <scroll-view scroll-x class="phrase-lang-scroll">
          <view class="phrase-lang-list">
            <view
              v-for="lang in phraseTargetLangs"
              :key="lang.code"
              class="phrase-lang-chip"
              :class="{ active: phraseTargetLang === lang.code }"
              @click="onPhraseTargetChange(lang.code)"
            >
              <text class="chip-flag">{{ lang.flag }}</text>
              <text class="chip-name" :style="{ fontSize: rpx(22) }">{{ lang.name }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 无结果提示 -->
      <view v-if="phraseSearch && !phraseCategories.length && !filteredFavorites.length" class="search-empty">
        <text class="search-empty-icon">🔍</text>
        <text class="search-empty-text" :style="{ fontSize: rpx(26) }">没有找到"{{ phraseSearch }}"相关短语</text>
      </view>

      <!-- 收藏的短语 -->
      <view v-if="filteredFavorites.length" class="phrase-section">
        <view class="section-header">
          <text class="section-icon">⭐</text>
          <text class="section-title" :style="{ fontSize: rpx(26) }">我的收藏</text>
        </view>
        <view class="phrase-list">
          <view
            v-for="phrase in filteredFavorites"
            :key="phrase.zh"
            class="phrase-item"
            @click="translatePhrase(phrase)"
          >
            <view class="phrase-content">
              <text class="phrase-zh" :style="{ fontSize: rpx(28) }">{{ phrase.zh }}</text>
              <text class="phrase-en" :style="{ fontSize: rpx(24) }">{{ phrase.en }}</text>
            </view>
            <view class="phrase-fav-btn active" @click.stop="toggleFavorite({ ...phrase, category: 'fav' })">
              <text class="fav-icon">⭐</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 分类短语 -->
      <view class="phrase-tip">
        <text class="phrase-tip-text" :style="{ fontSize: rpx(24) }">💡 {{ t('translate.phrases.tapToTranslate') }}，长按收藏</text>
      </view>

      <view v-for="cat in phraseCategories" :key="cat.key" class="phrase-section">
        <view class="section-header">
          <text class="section-icon">{{ cat.icon }}</text>
          <text class="section-title" :style="{ fontSize: rpx(26) }">{{ t(`translate.phrases.${cat.key}`) }}</text>
        </view>
        <view class="phrase-list">
          <view
            v-for="phrase in cat.phrases"
            :key="phrase.zh"
            class="phrase-item"
            @click="translatePhrase(phrase)"
            @longpress="toggleFavorite({ ...phrase, category: cat.key })"
          >
            <view class="phrase-content">
              <text class="phrase-zh" :style="{ fontSize: rpx(28) }">{{ phrase.zh }}</text>
              <text class="phrase-en" :style="{ fontSize: rpx(24) }">{{ phrase.en }}</text>
            </view>
            <view
              class="phrase-fav-btn"
              :class="{ active: isFavorite(phrase.zh) }"
              @click.stop="toggleFavorite({ ...phrase, category: cat.key })"
            >
              <text class="fav-icon">{{ isFavorite(phrase.zh) ? '⭐' : '☆' }}</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- ============ 翻译历史 ============ -->
    <view v-if="activeTab === 'history'" class="tab-content history-tab">
      <view v-if="history.length === 0" class="empty-state">
        <text class="empty-icon">📝</text>
        <text class="empty-text" :style="{ fontSize: rpx(28) }">{{ t('translate.history.empty') }}</text>
        <text class="empty-sub" :style="{ fontSize: rpx(24) }">翻译过的内容会自动保存在这里</text>
      </view>
      <view v-else class="history-container">
        <view class="history-toolbar">
          <text class="history-count" :style="{ fontSize: rpx(24) }">
            {{ historyFilter ? filteredHistory.length + '/' : '' }}共 {{ history.length }} 条
          </text>
          <text class="history-clear" :style="{ fontSize: rpx(24) }" @click="onClearHistory">清空</text>
        </view>

        <!-- 语言对筛选 chips -->
        <scroll-view v-if="historyLangPairs.length > 1" scroll-x class="history-filter-scroll">
          <view class="history-filter-list">
            <view
              class="history-filter-chip"
              :class="{ active: historyFilter === '' }"
              @click="historyFilter = ''"
            >
              <text class="filter-chip-text" :style="{ fontSize: rpx(22) }">全部</text>
            </view>
            <view
              v-for="pair in historyLangPairs"
              :key="pair"
              class="history-filter-chip"
              :class="{ active: historyFilter === pair }"
              @click="historyFilter = pair"
            >
              <text class="filter-chip-text" :style="{ fontSize: rpx(22) }">{{ pair }}</text>
            </view>
          </view>
        </scroll-view>

        <scroll-view scroll-y class="history-list">
          <view v-if="filteredHistory.length === 0" class="filter-empty">
            <text class="filter-empty-text" :style="{ fontSize: rpx(26) }">该语言对暂无翻译记录</text>
          </view>
          <view
            v-for="item in filteredHistory"
            :key="item.id"
            class="history-item"
            @click="reuseHistory(item)"
            @longpress="onHistoryLongPress(item)"
          >
            <view class="history-meta">
              <view class="history-lang-tag">
                <text class="history-lang-text" :style="{ fontSize: rpx(20) }">
                  {{ langNameByCode(item.from) }} → {{ langNameByCode(item.to) }}
                </text>
              </view>
              <text class="history-time" :style="{ fontSize: rpx(20) }">{{ formatTime(item.timestamp) }}</text>
            </view>
            <text class="history-source" :style="{ fontSize: rpx(28) }">{{ item.sourceText }}</text>
            <view class="history-divider" />
            <text class="history-translated" :style="{ fontSize: rpx(26) }">{{ item.translatedText }}</text>
            <view class="history-footer">
              <text class="history-reuse" :style="{ fontSize: rpx(22) }">点击复用 ›</text>
              <text class="history-delete" :style="{ fontSize: rpx(22) }" @click.stop="removeHistory(item.id)">删除</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- ============ 短语翻译弹出层 ============ -->
    <view v-if="phraseResult" class="overlay" @click="closePhraseResult">
      <view class="bottom-sheet" @click.stop>
        <view class="sheet-handle" />
        <view class="sheet-header">
          <text class="sheet-title" :style="{ fontSize: rpx(28) }">翻译结果</text>
          <text class="sheet-close" @click="closePhraseResult">✕</text>
        </view>
        <view class="sheet-source">
          <text class="sheet-source-text" :style="{ fontSize: rpx(28) }">{{ phraseResult.source }}</text>
          <text class="sheet-lang-pair" :style="{ fontSize: rpx(22) }">
            中文 → {{ langNameByCode(phraseTargetLang) }}
          </text>
        </view>
        <view class="sheet-result">
          <text class="sheet-translated" :style="{ fontSize: rpx(34) }">{{ phraseResult.translated }}</text>
        </view>
        <view class="sheet-actions">
          <button class="sheet-btn secondary" :style="{ fontSize: rpx(26) }" @click="copyPhraseResult">
            {{ phraseCopied ? '✓ 已复制' : '📋 复制' }}
          </button>
          <button class="sheet-btn primary" :style="{ fontSize: rpx(26) }" @click="usePhraseInText">
            在翻译页使用
          </button>
        </view>
      </view>
    </view>

    <!-- ============ 语言选择器弹窗 ============ -->
    <view v-if="langPickerVisible" class="overlay" @click="langPickerVisible = false">
      <view class="bottom-sheet" @click.stop>
        <view class="sheet-handle" />
        <view class="sheet-header">
          <text class="sheet-title" :style="{ fontSize: rpx(28) }">
            {{ pickerTarget === 'from' ? '选择源语言' : '选择目标语言' }}
          </text>
          <text class="sheet-close" @click="langPickerVisible = false">✕</text>
        </view>
        <scroll-view scroll-y style="max-height: 60vh;">
          <view
            v-for="lang in pickerLangs"
            :key="lang.code"
            class="picker-item"
            :class="{ selected: currentPickerLang === lang.code }"
            @click="selectLang(lang.code)"
          >
            <text class="picker-flag">{{ lang.flag }}</text>
            <text class="picker-lang-name" :style="{ fontSize: rpx(28) }">{{ lang.name }}</text>
            <text v-if="currentPickerLang === lang.code" class="picker-check">✓</text>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="tabbar-placeholder" />
    <TabBar active="language" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import TabBar from '@/components/TabBar/TabBar.vue'
import { useElder } from '@/composables/useElder'
import { useTranslationHistory } from '@/composables/useTranslationHistory'
import { useFavoritePhrases } from '@/composables/useFavoritePhrases'
import { translateText } from '@/api/translate'
import { setLocale } from '@/i18n'

const { t, locale } = useI18n()

const currentLocale = computed(() => locale.value as 'zh' | 'en')

function switchUiLang(lang: 'zh' | 'en') {
  if (lang === locale.value) return
  setLocale(lang)
  uni.showToast({ title: lang === 'zh' ? '已切换为中文' : 'Switched to English', icon: 'none' })
}
const { rpx } = useElder()
const { history, addHistory, removeHistory, clearHistory, formatTime } = useTranslationHistory()
const { favorites, isFavorite, toggleFavorite } = useFavoritePhrases()

// ——— 标签页 ———
const activeTab = ref<'text' | 'phrases' | 'history'>('text')
const tabs = computed(() => [
  { key: 'text',    label: t('translate.tabs.text')    },
  { key: 'phrases', label: t('translate.tabs.phrases') },
  { key: 'history', label: t('translate.tabs.history') },
])

// ——— 语言列表 ———
const allLanguages = [
  { code: 'auto', name: '自动检测', flag: '🔍' },
  { code: 'zh',   name: '中文',     flag: '🇨🇳' },
  { code: 'en',   name: 'English',  flag: '🇺🇸' },
  { code: 'ja',   name: '日本語',   flag: '🇯🇵' },
  { code: 'ko',   name: '한국어',   flag: '🇰🇷' },
  { code: 'fr',   name: 'Français', flag: '🇫🇷' },
  { code: 'es',   name: 'Español',  flag: '🇪🇸' },
  { code: 'de',   name: 'Deutsch',  flag: '🇩🇪' },
  { code: 'ru',   name: 'Русский',  flag: '🇷🇺' },
  { code: 'th',   name: 'ภาษาไทย',  flag: '🇹🇭' },
  { code: 'ar',   name: 'العربية',  flag: '🇸🇦' },
]

// 目标语言不含"自动检测"
const targetLangs     = allLanguages.filter(l => l.code !== 'auto')
// 短语目标语言不含"自动检测"和"中文"（短语本身已是中文）
const phraseTargetLangs = allLanguages.filter(l => l.code !== 'auto' && l.code !== 'zh')
const fromLangs       = allLanguages                               // 含自动检测
const pickerLangs   = computed(() =>
  pickerTarget.value === 'from' ? fromLangs : targetLangs
)

function langNameByCode(code: string): string {
  return allLanguages.find(l => l.code === code)?.name ?? code
}

// MyMemory 返回 "ZH-CN"、"EN-US" 等格式，规范化到内部语言代码
function normalizeDetectedLang(raw: string): string {
  if (!raw) return ''
  const lower = raw.toLowerCase().split('-')[0]
  // 处理特殊映射
  if (lower === 'zh' || lower === 'cn') return 'zh'
  const found = allLanguages.find(l => l.code === lower)
  return found ? found.code : ''
}

// 恢复上次语言选择
const LANG_KEY = 'translate_langs'
let initFrom = 'auto'
let initTo   = 'en'
try {
  const stored = uni.getStorageSync(LANG_KEY)
  if (stored?.from) initFrom = stored.from
  if (stored?.to)   initTo   = stored.to
} catch { /* ignore */ }

const fromLang = ref(initFrom)
const toLang   = ref(initTo)

// 从历史记录中提取最近用过的语言对（最多3个，去重）
const recentLangPairs = computed(() => {
  const seen = new Set<string>()
  const result: Array<{ from: string; to: string; fromFlag: string; toFlag: string; key: string }> = []
  for (const h of history.value) {
    const key = `${h.from}|${h.to}`
    if (!seen.has(key)) {
      seen.add(key)
      const fromInfo = allLanguages.find(l => l.code === h.from)
      const toInfo   = allLanguages.find(l => l.code === h.to)
      if (fromInfo && toInfo) {
        result.push({ from: h.from, to: h.to, fromFlag: fromInfo.flag, toFlag: toInfo.flag, key })
      }
    }
    if (result.length >= 3) break
  }
  return result
})

function applyLangPair(pair: { from: string; to: string }) {
  fromLang.value     = pair.from
  toLang.value       = pair.to
  resultText.value   = ''
  detectedLang.value = ''
}

const fromLangInfo = computed(() => allLanguages.find(l => l.code === fromLang.value) ?? allLanguages[0])
const toLangInfo   = computed(() => allLanguages.find(l => l.code === toLang.value)   ?? allLanguages[2])

function swapLang() {
  if (fromLang.value === 'auto') {
    uni.showToast({ title: '自动检测模式下无法互换', icon: 'none' })
    return
  }
  ;[fromLang.value, toLang.value] = [toLang.value, fromLang.value]
  if (resultText.value) {
    inputText.value  = resultText.value
    resultText.value = ''
    detectedLang.value = ''
  }
}

// ——— 语言选择器弹窗 ———
const langPickerVisible = ref(false)
const pickerTarget      = ref<'from' | 'to'>('from')
const currentPickerLang = computed(() =>
  pickerTarget.value === 'from' ? fromLang.value : toLang.value
)

function showLangPicker(target: 'from' | 'to') {
  pickerTarget.value      = target
  langPickerVisible.value = true
}

function selectLang(code: string) {
  if (pickerTarget.value === 'from') {
    fromLang.value = code
    // 若源语言和目标语言相同（非auto）则互换目标
    if (code !== 'auto' && code === toLang.value) {
      toLang.value = code === 'zh' ? 'en' : 'zh'
    }
  } else {
    toLang.value = code
    if (fromLang.value !== 'auto' && fromLang.value === code) {
      fromLang.value = code === 'zh' ? 'en' : 'zh'
    }
  }
  langPickerVisible.value = false
  resultText.value   = ''
  detectedLang.value = ''
  // 持久化语言选择
  try { uni.setStorageSync(LANG_KEY, { from: fromLang.value, to: toLang.value }) } catch { /* ignore */ }
}

// ——— 文本翻译 ———
const inputText    = ref('')
const resultText   = ref('')
const detectedLang = ref('')
const translating  = ref(false)
const copied       = ref(false)

function onInput(e: any) {
  inputText.value  = e.detail.value ?? ''
  resultText.value = ''
  detectedLang.value = ''
}

function clearInput() {
  inputText.value    = ''
  resultText.value   = ''
  detectedLang.value = ''
}

function pasteFromClipboard() {
  uni.getClipboardData({
    success: (res) => {
      if (res.data) {
        inputText.value = res.data
        resultText.value = ''
      }
    }
  })
}

async function doTranslate() {
  const text = inputText.value.trim()
  if (!text) {
    uni.showToast({ title: t('translate.error.empty'), icon: 'none' })
    return
  }
  uni.hideKeyboard()
  translating.value = true
  try {
    const res = await translateText(text, fromLang.value, toLang.value)
    if (res.code === 200 && res.data) {
      resultText.value   = res.data.translatedText
      detectedLang.value = normalizeDetectedLang(res.data.detectedLang ?? '')
      uni.vibrateShort({ type: 'light' })
      const actualFrom = detectedLang.value || (fromLang.value === 'auto' ? 'zh' : fromLang.value)
      addHistory({
        sourceText:     text,
        translatedText: res.data.translatedText,
        from:           actualFrom,
        to:             toLang.value,
      })
    } else {
      uni.showToast({ title: res.message || t('translate.error.failed'), icon: 'none' })
    }
  } catch {
    uni.showToast({ title: t('translate.error.failed'), icon: 'none' })
  } finally {
    translating.value = false
  }
}

function copyResult() {
  uni.setClipboardData({
    data: resultText.value,
    success: () => {
      uni.vibrateShort({ type: 'light' })
      copied.value = true
      setTimeout(() => { copied.value = false }, 2000)
    }
  })
}

function speakResult() {
  // 微信小程序使用 wx.createInnerAudioContext 或文字转语音暂无免费 API
  // 这里只用 Toast 提示，预留扩展
  uni.showToast({ title: '朗读功能即将上线', icon: 'none' })
}

// ——— 历史操作 ———
const historyFilter = ref('')

const historyLangPairs = computed(() => {
  const pairs = new Set<string>()
  history.value.forEach(h => {
    pairs.add(`${langNameByCode(h.from)}→${langNameByCode(h.to)}`)
  })
  return Array.from(pairs)
})

const filteredHistory = computed(() => {
  if (!historyFilter.value) return history.value
  return history.value.filter(h => {
    const pair = `${langNameByCode(h.from)}→${langNameByCode(h.to)}`
    return pair === historyFilter.value
  })
})

function reuseHistory(item: any) {
  inputText.value    = item.sourceText
  resultText.value   = item.translatedText
  fromLang.value     = item.from
  toLang.value       = item.to
  detectedLang.value = ''
  activeTab.value    = 'text'
}

function onHistoryLongPress(item: any) {
  uni.showActionSheet({
    itemList: ['复制原文', '复制译文', '复用到翻译', '删除'],
    success: (res) => {
      switch (res.tapIndex) {
        case 0:
          uni.setClipboardData({ data: item.sourceText, success: () => uni.showToast({ title: '已复制原文', icon: 'none' }) })
          break
        case 1:
          uni.setClipboardData({ data: item.translatedText, success: () => uni.showToast({ title: '已复制译文', icon: 'none' }) })
          break
        case 2:
          reuseHistory(item)
          break
        case 3:
          removeHistory(item.id)
          break
      }
    }
  })
}

function onClearHistory() {
  uni.showModal({
    title: '清空历史',
    content: t('translate.history.confirmClear'),
    success: (res) => {
      if (res.confirm) clearHistory()
    }
  })
}

// ——— 常用短语 ———
const PHRASE_LANG_KEY = 'phrase_target_lang'
let initPhraseLang = 'en'
try {
  const stored = uni.getStorageSync(PHRASE_LANG_KEY)
  if (stored && phraseTargetLangs.find(l => l.code === stored)) initPhraseLang = stored
} catch { /* ignore */ }
const phraseTargetLang = ref(initPhraseLang)
const phraseResult     = ref<{ source: string; translated: string } | null>(null)
const phraseCopied     = ref(false)
const phraseLoading    = ref(false)
const phraseSearch     = ref('')

const allPhraseCategories = [
  {
    key: 'greeting', icon: '👋',
    phrases: [
      { zh: '你好', en: 'Hello' },
      { zh: '谢谢', en: 'Thank you' },
      { zh: '对不起', en: 'I\'m sorry' },
      { zh: '再见', en: 'Goodbye' },
      { zh: '请问', en: 'Excuse me' },
      { zh: '不客气', en: 'You\'re welcome' },
    ]
  },
  {
    key: 'dining', icon: '🍽️',
    phrases: [
      { zh: '我想要这个', en: 'I\'d like this one' },
      { zh: '菜单在哪里？', en: 'Where is the menu?' },
      { zh: '买单', en: 'Check, please' },
      { zh: '不辣', en: 'Not spicy please' },
      { zh: '素食', en: 'Vegetarian' },
      { zh: '好吃！', en: 'Delicious!' },
    ]
  },
  {
    key: 'transport', icon: '🚌',
    phrases: [
      { zh: '去机场怎么走？', en: 'How do I get to the airport?' },
      { zh: '最近的地铁站在哪里？', en: 'Where is the nearest subway station?' },
      { zh: '请载我去这里', en: 'Please take me here' },
      { zh: '这趟车去哪里？', en: 'Where does this bus go?' },
      { zh: '多少钱？', en: 'How much?' },
      { zh: '停在这里', en: 'Stop here please' },
    ]
  },
  {
    key: 'shopping', icon: '🛍️',
    phrases: [
      { zh: '这个多少钱？', en: 'How much is this?' },
      { zh: '可以便宜一点吗？', en: 'Can you give me a discount?' },
      { zh: '我只是看看', en: 'Just looking, thanks' },
      { zh: '有没有其他颜色？', en: 'Do you have other colors?' },
      { zh: '可以用信用卡吗？', en: 'Do you accept credit card?' },
      { zh: '可以退换吗？', en: 'Can I return this?' },
    ]
  },
  {
    key: 'hotel', icon: '🏨',
    phrases: [
      { zh: '我有预订', en: 'I have a reservation' },
      { zh: '退房时间是几点？', en: 'What time is checkout?' },
      { zh: '能帮我叫醒吗？', en: 'Could I have a wake-up call?' },
      { zh: '有无线网络吗？', en: 'Is there Wi-Fi?' },
      { zh: '空调坏了', en: 'The air conditioner is broken' },
      { zh: '请打扫房间', en: 'Please clean my room' },
    ]
  },
  {
    key: 'emergency', icon: '🆘',
    phrases: [
      { zh: '救命！', en: 'Help!' },
      { zh: '请叫救护车', en: 'Please call an ambulance' },
      { zh: '请叫警察', en: 'Please call the police' },
      { zh: '我迷路了', en: 'I\'m lost' },
      { zh: '我钱包被偷了', en: 'My wallet was stolen' },
      { zh: '我需要医生', en: 'I need a doctor' },
      { zh: '这里有药店吗？', en: 'Is there a pharmacy nearby?' },
      { zh: '我对这个过敏', en: 'I\'m allergic to this' },
    ]
  },
  {
    key: 'scenic',    icon: '🏛️',
    phrases: [
      { zh: '入口在哪里？', en: 'Where is the entrance?' },
      { zh: '几点开放？', en: 'What time does it open?' },
      { zh: '门票多少钱？', en: 'How much is the ticket?' },
      { zh: '可以拍照吗？', en: 'Can I take photos here?' },
      { zh: '有导游服务吗？', en: 'Is there a guided tour?' },
      { zh: '洗手间在哪里？', en: 'Where is the restroom?' },
      { zh: '这里是什么地方？', en: 'What is this place?' },
      { zh: '可以帮我拍照吗？', en: 'Could you take a photo for me?' },
    ]
  },
  {
    key: 'time',      icon: '🕐',
    phrases: [
      { zh: '现在几点？', en: 'What time is it now?' },
      { zh: '今天是几号？', en: 'What is today\'s date?' },
      { zh: '等一下', en: 'Just a moment' },
      { zh: '快点', en: 'Hurry up' },
      { zh: '明天', en: 'Tomorrow' },
      { zh: '后天', en: 'The day after tomorrow' },
      { zh: '上午', en: 'Morning' },
      { zh: '下午', en: 'Afternoon' },
    ]
  },
]

const filteredFavorites = computed(() => {
  const q = phraseSearch.value.trim().toLowerCase()
  if (!q) return favorites.value
  return favorites.value.filter(p =>
    p.zh.includes(q) || p.en.toLowerCase().includes(q)
  )
})

const phraseCategories = computed(() => {
  const q = phraseSearch.value.trim().toLowerCase()
  if (!q) return allPhraseCategories
  return allPhraseCategories
    .map(cat => ({
      ...cat,
      phrases: cat.phrases.filter(p =>
        p.zh.includes(q) || p.en.toLowerCase().includes(q)
      )
    }))
    .filter(cat => cat.phrases.length > 0)
})

function onPhraseTargetChange(code: string) {
  phraseTargetLang.value = code
  try { uni.setStorageSync(PHRASE_LANG_KEY, code) } catch { /* ignore */ }
}

async function translatePhrase(phrase: { zh: string; en: string }) {
  if (phraseLoading.value) return
  const sourceText = phrase.zh

  // 中→英直接用静态值，免去 API 调用
  if (phraseTargetLang.value === 'en') {
    phraseResult.value = { source: phrase.zh, translated: phrase.en }
    return
  }

  // 其他语言调 API
  phraseLoading.value = true
  uni.showLoading({ title: '翻译中...', mask: true })
  try {
    const res = await translateText(sourceText, 'zh', phraseTargetLang.value)
    if (res.code === 200 && res.data) {
      phraseResult.value = { source: sourceText, translated: res.data.translatedText }
      addHistory({
        sourceText,
        translatedText: res.data.translatedText,
        from: 'zh',
        to:   phraseTargetLang.value,
      })
    }
  } catch {
    uni.showToast({ title: '翻译失败', icon: 'none' })
  } finally {
    phraseLoading.value = false
    uni.hideLoading()
  }
}

function closePhraseResult() {
  phraseResult.value = null
  phraseCopied.value = false
}

function copyPhraseResult() {
  if (!phraseResult.value) return
  uni.setClipboardData({
    data: phraseResult.value.translated,
    success: () => {
      phraseCopied.value = true
      setTimeout(() => { phraseCopied.value = false }, 2000)
    }
  })
}

function usePhraseInText() {
  if (!phraseResult.value) return
  inputText.value    = phraseResult.value.source
  resultText.value   = phraseResult.value.translated
  fromLang.value     = 'zh'
  toLang.value       = phraseTargetLang.value
  detectedLang.value = ''
  closePhraseResult()
  activeTab.value = 'text'
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background-color: #f7f8fa;
}

/* ——— 界面语言切换 ——— */
.ui-lang-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  padding: 16rpx 28rpx;
}

.ui-lang-label {
  color: rgba(255,255,255,0.8);
}

.ui-lang-switcher {
  display: flex;
  background: rgba(255,255,255,0.15);
  border-radius: 32rpx;
  padding: 4rpx;
  gap: 4rpx;
}

.ui-lang-opt {
  padding: 8rpx 20rpx;
  border-radius: 28rpx;
}

.ui-lang-opt.active {
  background: #fff;
}

.ui-lang-opt-text {
  color: rgba(255,255,255,0.7);
}

.ui-lang-opt.active .ui-lang-opt-text {
  color: #1677ff;
  font-weight: 700;
}

/* ——— 标签页 ——— */
.tabs {
  display: flex;
  background-color: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.tab-item {
  flex: 1;
  padding: 28rpx 0 20rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  gap: 8rpx;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20%;
  width: 60%;
  height: 4rpx;
  background-color: #1677ff;
  border-radius: 2rpx;
}

.tab-text {
  color: #888;
  font-weight: 500;
}

.tab-item.active .tab-text {
  color: #1677ff;
  font-weight: 700;
}

.tab-badge {
  background: #f56c6c;
  border-radius: 16rpx;
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-badge-text {
  color: #fff;
  font-size: 20rpx;
  font-weight: 700;
}

/* ——— 内容区 ——— */
/* 100vh 在微信小程序不含导航栏，ui-lang-bar ~80rpx + tabs ~80rpx + tabbar ~140rpx = 300rpx */
.tab-content {
  padding: 20rpx 24rpx;
  box-sizing: border-box;
  height: calc(100vh - 300rpx);
}

/* ——— 最近使用语言对 ——— */
.recent-bar {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
  gap: 8rpx;
}

.recent-label {
  color: #bbb;
  flex-shrink: 0;
}

.recent-scroll {
  flex: 1;
}

.recent-list {
  display: flex;
  gap: 10rpx;
  white-space: nowrap;
}

.recent-chip {
  display: inline-flex;
  align-items: center;
  padding: 8rpx 16rpx;
  background: #f0f0f0;
  border-radius: 24rpx;
  flex-shrink: 0;
}

.recent-chip.active {
  background: #d6e4ff;
}

.recent-chip-text {
  color: #555;
  letter-spacing: 2rpx;
}

.recent-chip.active .recent-chip-text {
  color: #1677ff;
  font-weight: 600;
}

/* ——— 语言选择栏 ——— */
.lang-bar {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 20rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.06);
}

.lang-select {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}

.lang-flag {
  font-size: 32rpx;
}

.lang-name {
  color: #1a1a1a;
  font-weight: 600;
}

.lang-arrow {
  color: #999;
  font-size: 24rpx;
}

.swap-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8f0fe 100%);
  border-radius: 36rpx;
  margin: 0 16rpx;
}

.swap-icon {
  font-size: 36rpx;
  color: #1677ff;
}

/* ——— 输入区 ——— */
.input-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.06);
}

.input-area {
  width: 100%;
  min-height: 160rpx;
  color: #1a1a1a;
  line-height: 1.7;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid #f5f5f5;
}

.char-count {
  color: #bbb;
}

.char-count.warn {
  color: #f56c6c;
}

.input-actions {
  display: flex;
  gap: 16rpx;
}

.action-btn {
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}

.action-btn.clear {
  color: #f56c6c;
  background: #fff5f5;
}

.action-btn.paste {
  color: #1677ff;
  background: #f0f4ff;
}

/* ——— 翻译按钮 ——— */
.translate-btn {
  width: 100%;
  height: 92rpx;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  color: #fff;
  border-radius: 20rpx;
  border: none;
  font-weight: 700;
  margin-bottom: 16rpx;
  letter-spacing: 2rpx;
  box-shadow: 0 4rpx 20rpx rgba(22, 119, 255, 0.3);
}

.translate-btn.disabled {
  background: #ddd;
  box-shadow: none;
}

.translate-btn::after {
  border: none;
}

/* ——— 结果区 ——— */
.result-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  min-height: 120rpx;
  box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.06);
  border: 2rpx solid #f0f0f0;
}

.result-card.has-result {
  border-color: #d6e4ff;
  background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16rpx;
}

.result-meta {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.result-label {
  color: #999;
  font-weight: 500;
}

.detected-lang {
  color: #1677ff;
  background: #f0f4ff;
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
}

.result-actions-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.icon-btn {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 28rpx;
}

.icon-btn-text {
  font-size: 28rpx;
}

.copy-btn {
  background: #f0f4ff;
  padding: 10rpx 20rpx;
  border-radius: 24rpx;
}

.copy-text {
  color: #1677ff;
}

.result-text {
  color: #1a1a1a;
  line-height: 1.8;
  word-break: break-all;
}

.result-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80rpx;
}

.result-placeholder {
  color: #ccc;
}

.result-tip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 4rpx 0;
}

.tip-text {
  color: #52c41a;
}

.char-stat {
  color: #bbb;
}

/* ——— 短语搜索框 ——— */
.search-bar {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.06);
  gap: 12rpx;
}

.search-icon {
  font-size: 28rpx;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  color: #1a1a1a;
}

.search-clear {
  color: #bbb;
  font-size: 28rpx;
  padding: 4rpx 8rpx;
}

/* ——— 搜索无结果 ——— */
.search-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0 40rpx;
  gap: 16rpx;
}

.search-empty-icon {
  font-size: 64rpx;
}

.search-empty-text {
  color: #bbb;
}

/* ——— 常用短语语言切换 ——— */
.phrase-lang-bar {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.06);
  overflow: hidden;
}

.phrase-lang-label {
  color: #666;
  flex-shrink: 0;
  margin-right: 12rpx;
}

.phrase-lang-scroll {
  flex: 1;
}

.phrase-lang-list {
  display: flex;
  gap: 12rpx;
  white-space: nowrap;
}

.phrase-lang-chip {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 20rpx;
  border-radius: 32rpx;
  background: #f5f5f5;
  flex-shrink: 0;
}

.phrase-lang-chip.active {
  background: #1677ff;
}

.chip-flag {
  font-size: 24rpx;
}

.chip-name {
  color: #666;
}

.phrase-lang-chip.active .chip-name {
  color: #fff;
  font-weight: 600;
}

/* ——— 常用短语 ——— */
.phrase-tip {
  background: linear-gradient(135deg, #fff9e6 0%, #fff7d6 100%);
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 20rpx;
}

.phrase-tip-text {
  color: #b8860b;
}

.phrase-section {
  margin-bottom: 24rpx;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 12rpx;
  padding-left: 4rpx;
}

.section-icon {
  font-size: 32rpx;
}

.section-title {
  color: #1a1a1a;
  font-weight: 700;
}

.phrase-list {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
}

.phrase-item {
  padding: 24rpx 24rpx 24rpx 28rpx;
  border-bottom: 1rpx solid #f5f5f5;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.phrase-item:last-child {
  border-bottom: none;
}

.phrase-item:active {
  background: #f0f4ff;
}

.phrase-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.phrase-zh {
  color: #1a1a1a;
  font-weight: 500;
}

.phrase-en {
  color: #888;
}

.phrase-fav-btn {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 28rpx;
  flex-shrink: 0;
}

.phrase-fav-btn.active {
  background: #fff9e6;
}

.fav-icon {
  font-size: 32rpx;
}

/* ——— 翻译历史 ——— */
.history-tab {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 300rpx);
  overflow: hidden;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
  gap: 20rpx;
}

.empty-icon {
  font-size: 96rpx;
}

.empty-text {
  color: #bbb;
}

.empty-sub {
  color: #ddd;
}

.history-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.history-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4rpx 12rpx;
}

.history-filter-scroll {
  margin-bottom: 16rpx;
}

.history-filter-list {
  display: flex;
  gap: 12rpx;
  white-space: nowrap;
}

.history-filter-chip {
  display: inline-flex;
  padding: 8rpx 20rpx;
  border-radius: 32rpx;
  background: #f0f0f0;
  flex-shrink: 0;
}

.history-filter-chip.active {
  background: #1677ff;
}

.filter-chip-text {
  color: #666;
}

.history-filter-chip.active .filter-chip-text {
  color: #fff;
  font-weight: 600;
}

.filter-empty {
  text-align: center;
  padding: 60rpx 0;
}

.filter-empty-text {
  color: #bbb;
}

.history-count {
  color: #999;
}

.history-clear {
  color: #f56c6c;
}

.history-list {
  flex: 1;
  overflow: hidden;
}

.history-item {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
}

.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.history-lang-tag {
  background: #f0f4ff;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
}

.history-lang-text {
  color: #1677ff;
  font-weight: 500;
}

.history-time {
  color: #bbb;
}

.history-source {
  color: #1a1a1a;
  font-weight: 500;
  line-height: 1.6;
}

.history-divider {
  height: 1rpx;
  background: linear-gradient(to right, #1677ff22, transparent);
  margin: 12rpx 0;
}

.history-translated {
  color: #555;
  line-height: 1.6;
}

.history-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid #f5f5f5;
}

.history-reuse {
  color: #1677ff;
}

.history-delete {
  color: #f56c6c;
}

/* ——— 通用遮罩 + 底部弹窗 ——— */
.overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: flex-end;
  z-index: 200;
}

.bottom-sheet {
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  width: 100%;
  box-sizing: border-box;
  padding: 0 32rpx 48rpx;
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
}

.sheet-handle {
  width: 80rpx;
  height: 8rpx;
  background: #e0e0e0;
  border-radius: 4rpx;
  margin: 16rpx auto 24rpx;
}

.sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28rpx;
}

.sheet-title {
  color: #1a1a1a;
  font-weight: 700;
}

.sheet-close {
  font-size: 36rpx;
  color: #bbb;
  padding: 8rpx;
}

/* 短语翻译结果 */
.sheet-source {
  background: #f7f8fa;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 16rpx;
}

.sheet-source-text {
  color: #555;
  display: block;
  margin-bottom: 8rpx;
}

.sheet-lang-pair {
  color: #aaa;
}

.sheet-result {
  padding: 20rpx 4rpx;
  margin-bottom: 28rpx;
}

.sheet-translated {
  color: #1677ff;
  font-weight: 700;
  line-height: 1.6;
}

.sheet-actions {
  display: flex;
  gap: 16rpx;
}

.sheet-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 16rpx;
  border: none;
  font-weight: 600;
}

.sheet-btn::after {
  border: none;
}

.sheet-btn.secondary {
  background: #f0f4ff;
  color: #1677ff;
}

.sheet-btn.primary {
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  color: #fff;
}

/* 语言选择器 */
.picker-item {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  gap: 20rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.picker-item:last-child {
  border-bottom: none;
}

.picker-item.selected {
  background: #f0f4ff;
  margin: 0 -32rpx;
  padding-left: 32rpx;
  padding-right: 32rpx;
}

.picker-flag {
  font-size: 40rpx;
}

.picker-lang-name {
  flex: 1;
  color: #1a1a1a;
}

.picker-check {
  color: #1677ff;
  font-size: 32rpx;
  font-weight: 700;
}

/* ——— 占位 ——— */
.tabbar-placeholder {
  height: 140rpx;
}
</style>
