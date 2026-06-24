<template>
  <!-- 核心修改：添加主题类名绑定 -->
  <view class="page" :class="currentTheme">
    <!-- 背景氛围 -->
    <view class="bg-layer"></view>
    <view class="glow glow-1"></view>
    <view class="glow glow-2"></view>

    <!-- 1. 顶部预置控制栏 - 新增点击事件 -->
    <view class="top-control-bar">
      <view class="control-item">设置</view>
      <view class="control-item" @tap="toggleSkinPopup">换肤</view>
      <view class="control-item" @tap="resetAll">重置</view>
    </view>

    <!-- 替换uni-popup为原生view实现的弹窗 -->
    <!-- 遮罩层 -->
    <view 
      class="skin-popup-mask" 
      v-if="isSkinPopupShow"
      @tap="closeSkinPopup"
    ></view>
    <!-- 弹窗内容 -->
    <view 
      class="skin-popup" 
      v-if="isSkinPopupShow"
    >
      <view class="popup-title">选择主题皮肤</view>
      <view class="skin-list">
        <view 
          class="skin-item" 
          :class="{ active: currentTheme === 'theme-purple' }"
          @tap="setTheme('theme-purple')"
        >
          <view class="skin-preview theme-purple"></view>
          <text class="skin-name">紫白色（默认）</text>
        </view>
        <view 
          class="skin-item" 
          :class="{ active: currentTheme === 'theme-blue' }"
          @tap="setTheme('theme-blue')"
        >
          <view class="skin-preview theme-blue"></view>
          <text class="skin-name">蓝白色</text>
        </view>
        <view 
          class="skin-item" 
          :class="{ active: currentTheme === 'theme-green' }"
          @tap="setTheme('theme-green')"
        >
          <view class="skin-preview theme-green"></view>
          <text class="skin-name">绿白色</text>
        </view>
      </view>
      <view class="skin-confirm" @tap="closeSkinPopup">确认</view>
    </view>

    <!-- 2. 聊天气泡框 - 改为可滚动容器 -->
	
	<scroll-view 
	  id="chatBubbleContainer"
	  class="chat-bubble-container"
	  scroll-y="true"
	  :scroll-top="scrollTop"
	  :scroll-with-animation="true"
	  :scroll-into-view="lastMsgId"
	>
	  <view class="chat-bubble-area">
	    <!-- 把 currentChatSession → chatHistory -->
	    <view 
	      class="chat-bubble" 
	      v-for="(msg, idx) in chatHistory" 
	      :key="msg.content + idx"
	      :class="{
	        'chat-bubble-left': !msg.isSelf,
	        'chat-bubble-right': msg.isSelf
	      }"
	      :id="idx === chatHistory.length - 1 ? 'lastMsg' : ''"
	    >
	      <text class="bubble-text">{{ msg.content }}</text>
	      <view 
	        class="voice-btn"
	        :class="{
	          'voice-btn-loading': msg.audioStatus === 'loading',
	          'voice-btn-playing': msg.audioStatus === 'playing',
	          'voice-btn-played': msg.audioStatus === 'played',
	          'voice-btn-error': msg.audioStatus === 'error'
	        }"
	        v-if="!msg.isSelf && msg.content.trim()"
	        @tap="handleVoiceTap(msg)"
	      >
	        <text class="voice-icon">
	          {{
	            msg.audioStatus === 'loading' ? '⏳' :
	            msg.audioStatus === 'playing' ? '🔊' :
	            msg.audioStatus === 'played' ? '✅' :
	            msg.audioStatus === 'error' ? '⚠️' : '🎧'
	          }}
	        </text>
	      </view>
	    </view>
	  </view>
	</scroll-view>
    

    <!-- 3. Live2D 展示区 -->
    <!-- #ifdef MP-WEIXIN -->
    <view class="live2d-stage">
      <view class="live2d-wrap">
        <live2d-view
		  ref="live2dRef"
          className="live2d-box"
          :autoInit="true"
          :stageWidth="750"
          @ready="handleReady"
          @error="handleError"
        />
      </view>
    </view>
    <!-- #endif -->

    <!-- #ifndef MP-WEIXIN -->
    <view class="placeholder">
      <text>请在微信小程序平台查看 Live2D 页面</text>
    </view>
    <!-- #endif -->

    <!-- 底部氛围层 -->
    <view class="bottom-atmosphere"></view>

    <!-- 4. 输入对话框 -->
    <view class="chat-panel">
      <view class="chat-box">
        <input
          class="chat-input"
          v-model="inputText"
          type="text"
          placeholder="和看板娘说点什么吧..."
          confirm-type="send"
          @confirm="handleSend"
        />
        <view
          class="send-btn"
          :class="{ 'send-btn-stop': isStreaming }"
          @tap="isStreaming ? handleStop() : handleSend()"
        >
          <text class="send-text">{{ isStreaming ? '停止' : '发送' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, nextTick, onBeforeUnmount, getCurrentInstance } from 'vue'
import {
  chatWithCompanionStream,
  type ChatStreamTask,
  type LocalChatMessageDTO
} from '@/api/companion'
import { synthesizeSpeech } from '@/api/tts'

const MAX_HISTORY_MESSAGES = 36
const MAX_HISTORY_CHARS = 3600

const TALKING_PRESETS = {
  happy: {
    mouthOpenBase: 0.1,
    mouthOpenRange: 0.45,
    mouthFormBase: 0.25,
    mouthFormRange: 0.06
  },
  calm: {
    mouthOpenBase: 0.06,
    mouthOpenRange: 0.32,
    mouthFormBase: 0.08,
    mouthFormRange: 0.03
  },
  sad: {
    mouthOpenBase: 0.04,
    mouthOpenRange: 0.28,
    mouthFormBase: -0.18,
    mouthFormRange: 0.04
  },
  excited: {
    mouthOpenBase: 0.12,
    mouthOpenRange: 0.58,
    mouthFormBase: 0.3,
    mouthFormRange: 0.1
  }
}
const live2dRef = ref<any>(null)
const isLive2DReady = ref(false)

function limitHistory(history: LocalChatMessageDTO[]): LocalChatMessageDTO[] {
  if (!history.length) return []

  // 先保留最近若干条
  let trimmed = history.slice(-MAX_HISTORY_MESSAGES)

  // 再限制总字符数，优先保留最新消息
  let total = 0
  const result: LocalChatMessageDTO[] = []

  for (let i = trimmed.length - 1; i >= 0; i--) {
    const item = trimmed[i]
    const len = item.content?.length || 0

    if (result.length > 0 && total + len > MAX_HISTORY_CHARS) {
      break
    }

    total += len
    result.unshift(item)
  }

  return result
}


// 当前会话历史（仅本地保留，不请求历史接口）
const localSessionMessages = ref<LocalChatMessageDTO[]>([
  { role: 'ASSISTANT', content: '你好呀，有什么想和我说的吗？' }
])


// 当前流式任务
let currentChatTask: ChatStreamTask | null = null

const isStreaming = ref(false)

const inputText = ref('')


type ChatBubbleItem = {
  id: string
  content: string
  isSelf: boolean
  audioUrl?: string
  audioStatus?: 'idle' | 'loading' | 'playing' | 'played' | 'error'
}

const audioContext = uni.createInnerAudioContext()

const currentPlayingMsgId = ref<string | null>(null)

const localAudioCache = ref<Record<string, string>>({})

// 本地缓存：messageId -> audioUrl
const audioCacheMap = ref<Record<string, string>>({})

// 聊天历史记录 - 包含消息内容和是否是自己发送的标识
const chatHistory = ref<ChatBubbleItem[]>([
  {
    id: createMsgId(),
    content: '你好呀，有什么想和我说的吗？',
    isSelf: false,
    audioStatus: 'idle'
  }
])


function createMsgId() {
  return `msg_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

audioContext.autoplay = false
audioContext.obeyMuteSwitch = false

function downloadAudioToLocal(url: string): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url,
      success: (res) => {
        if (res.statusCode === 200 && res.tempFilePath) {
          resolve(res.tempFilePath)
        } else {
          reject(new Error('语音下载失败'))
        }
      },
      fail: reject
    })
  })
}


function clearAudioCache() {
  stopCurrentAudio()

  Object.keys(localAudioCache.value).forEach((key) => {
    const filePath = localAudioCache.value[key]
    if (filePath) {
      uni.getFileSystemManager?.().unlink({
        filePath,
        success: () => {},
        fail: () => {}
      })
    }
  })

  localAudioCache.value = {}
}

audioContext.onPlay(() => {
  console.log('[tts] playing...')
})

audioContext.onEnded(() => {
  const msgId = currentPlayingMsgId.value
  if (msgId) {
    const target = chatHistory.value.find(item => item.id === msgId)
    if (target) {
      target.audioStatus = 'played'
    }
  }
  if (live2dRef.value && isLive2DReady.value) {
    live2dRef.value.stopTalking()
  }
  currentPlayingMsgId.value = null
})

audioContext.onStop(() => {
  const msgId = currentPlayingMsgId.value
  if (msgId) {
    const target = chatHistory.value.find(item => item.id === msgId)
    if (target && target.audioStatus === 'playing') {
      target.audioStatus = 'played'
    }
  }
  if (live2dRef.value && isLive2DReady.value) {
    live2dRef.value.stopTalking()
  }
  currentPlayingMsgId.value = null
})

audioContext.onError((err) => {
  console.error('[tts] audio error:', err)
  const msgId = currentPlayingMsgId.value
  if (msgId) {
    const target = chatHistory.value.find(item => item.id === msgId)
    if (target) {
      target.audioStatus = 'error'
    }
  }
  if (live2dRef.value && isLive2DReady.value) {
    live2dRef.value.stopTalking()
  }
  currentPlayingMsgId.value = null
})


function stopCurrentAudio(resetStatus = false) {
  if (currentPlayingMsgId.value) {
    const target = chatHistory.value.find(item => item.id === currentPlayingMsgId.value)
    if (target && resetStatus) {
      target.audioStatus = 'idle'
    }
  }

  audioContext.stop()

  if (live2dRef.value && isLive2DReady.value) {
    live2dRef.value.stopTalking()
  }

  currentPlayingMsgId.value = null
}


// 滚动容器底部内边距（用于适配输入框高度）
const scrollBottomPadding = ref(120)

// 新增主题相关变量
const currentTheme = ref('theme-purple') // 默认主题
const isSkinPopupShow = ref(false) // 控制原生弹窗显示/隐藏

// Live2D 就绪回调
function handleReady(e: any) {
  console.log('[live2d-page] component ready:', e)
  isLive2DReady.value = true
}

// Live2D 错误回调
function handleError(e: any) {
  console.error('[live2d-page] component error:', e)
  isLive2DReady.value = false
}

// 添加左侧气泡（机器人回复）
function addLeftBubble(content: string) {
  chatHistory.value.push({
    id: createMsgId(),
    content,
    isSelf: false,
    audioStatus: content.trim() ? 'idle' : undefined
  })
  scrollToBottom()
}

// 添加右侧气泡（用户发送）
function addRightBubble(content: string) {
  chatHistory.value.push({
    id: createMsgId(),
    content,
    isSelf: true
  })
  scrollToBottom()
}

function updateLastLeftBubble(content: string) {
  for (let i = chatHistory.value.length - 1; i >= 0; i--) {
    if (!chatHistory.value[i].isSelf) {
      chatHistory.value[i].content = content
      if (content.trim()) {
        chatHistory.value[i].audioStatus = 'idle'
      }
      break
    }
  }
  scrollToBottom(20)
}

// scroll-view 滚动绑定变量
const scrollTop = ref(0)
// 标记最后一条消息id
const lastMsgId = ref('')

// 滚动时更新scrollTop
function scrollToBottom(delay = 60) {
  nextTick(() => {
    setTimeout(() => {
      const query = uni.createSelectorQuery()
      query.select('#chatBubbleContainer').boundingClientRect()
      query.select('.chat-bubble-area').boundingClientRect()
      query.exec((res) => {
        if (!res || res.length < 2) return
        const containerRect = res[0]
        const contentRect = res[1]
        if (!containerRect || !contentRect) return
        scrollTop.value = Math.max(contentRect.height - containerRect.height, 0)
        // 兜底触发滚动到最后一条消息
        lastMsgId.value = 'lastMsg'
      })
    }, delay)
  })
}

function handleSend() {
  stopCurrentAudio()
  const text = inputText.value.trim()
  if (!text || isStreaming.value) return

  console.log('[chat] send:', text)

  if (currentChatTask) {
    currentChatTask.abort()
    currentChatTask = null
  }

  addRightBubble(text)

  localSessionMessages.value.push({
    role: 'USER',
    content: text
  })
  localSessionMessages.value = limitHistory(localSessionMessages.value)

  inputText.value = ''

  addLeftBubble('')
  let assistantReply = ''
  isStreaming.value = true

  const requestHistory = limitHistory(localSessionMessages.value)

  currentChatTask = chatWithCompanionStream({
    message: text,
    history: requestHistory,
    onDelta(delta) {
      assistantReply += delta
      updateLastLeftBubble(assistantReply)
    },
    onDone() {
      currentChatTask = null
      isStreaming.value = false

      const finalReply = assistantReply.trim() || ' '
      updateLastLeftBubble(finalReply)

      localSessionMessages.value.push({
        role: 'ASSISTANT',
        content: finalReply
      })
      localSessionMessages.value = limitHistory(localSessionMessages.value)

      scrollToBottom()
    },
    onError(error) {
      console.error('[chat] stream error:', error)
      currentChatTask = null
      isStreaming.value = false

      const errorText = error || '网络开小差了，请稍后再试～'
      updateLastLeftBubble(errorText)

      localSessionMessages.value.push({
        role: 'ASSISTANT',
        content: errorText
      })
      localSessionMessages.value = limitHistory(localSessionMessages.value)

      scrollToBottom()
    }
  })
}




function handleStop() {
  stopCurrentAudio()
  if (currentChatTask) {
    currentChatTask.abort()
    currentChatTask = null
  }

  isStreaming.value = false

  // 如果最后一条机器人回复为空，可给一个中断提示
  const lastMessage = chatHistory.value[chatHistory.value.length - 1]
  if (lastMessage && !lastMessage.isSelf && !lastMessage.content.trim()) {
    lastMessage.content = '已停止回复'
  }

  scrollToBottom()
}

async function handleVoiceTap(msg: ChatBubbleItem) {
  if (!msg.content.trim()) return

  if (currentPlayingMsgId.value === msg.id && msg.audioStatus === 'playing') {
    stopCurrentAudio()
    if (live2dRef.value && isLive2DReady.value) {
      live2dRef.value.stopTalking()
    }
    msg.audioStatus = 'played'
    return
  }

  stopCurrentAudio()
  if (live2dRef.value && isLive2DReady.value) {
    live2dRef.value.stopTalking()
  }

  try {
    msg.audioStatus = 'loading'

    let localPath = localAudioCache.value[msg.id]

    if (!localPath) {
      let audioUrl = msg.audioUrl

      if (!audioUrl) {
        const res = await synthesizeSpeech({
          text: msg.content,
          lang: 'zh'
        })
        audioUrl = res.data.audioUrl
        msg.audioUrl = audioUrl
      }

      localPath = await downloadAudioToLocal(audioUrl)
      localAudioCache.value[msg.id] = localPath
    }

    audioContext.src = localPath
    currentPlayingMsgId.value = msg.id
    msg.audioStatus = 'playing'

    // 启动 Live2D 说话
    startLive2DTalkingByAudio(localPath, msg.content)

    audioContext.play()
  } catch (error) {
    console.error('[tts] synthesize/play error:', error)
    msg.audioStatus = 'error'
    if (live2dRef.value && isLive2DReady.value) {
      live2dRef.value.stopTalking()
    }
    uni.showToast({
      title: '语音播放失败',
      icon: 'none'
    })
  }
}

function getAudioDuration(src: string): Promise<number> {
  return new Promise((resolve) => {
    const ctx = uni.createInnerAudioContext()
    let settled = false

    const done = (ms: number) => {
      if (settled) return
      settled = true
      try { ctx.destroy() } catch (e) {}
      resolve(ms)
    }

    ctx.autoplay = false
    ctx.src = src

    ctx.onCanplay(() => {
      setTimeout(() => {
        const durationSec = Number(ctx.duration || 0)
        if (durationSec > 0) {
          done(durationSec * 1000)
        }
      }, 200)
    })

    ctx.onError(() => {
      done(0)
    })

    setTimeout(() => {
      const durationSec = Number(ctx.duration || 0)
      done(durationSec > 0 ? durationSec * 1000 : 0)
    }, 1200)
  })
}

async function startLive2DTalkingByAudio(localPath: string, text = '') {
  if (!live2dRef.value || !isLive2DReady.value) return

  let duration = await getAudioDuration(localPath)

  // 拿不到时长时，用文本长度估算
  if (!duration || duration <= 0) {
    duration = Math.min(8000, Math.max(1200, text.length * 220))
  }

  try {
    live2dRef.value.startTalking(duration, {
      enableBlink: true,
      enableHeadMotion: true,
      mouthOpenBase: 0.08,
      mouthOpenRange: 0.42,
      mouthFormBase: 0.18,   // 默认偏开心
      mouthFormRange: 0.06
    })
  } catch (err) {
    console.error('[live2d] startTalking failed:', err)
  }
}

// 替换弹窗控制方法（原生view版）
function toggleSkinPopup() {
  isSkinPopupShow.value = !isSkinPopupShow.value
}

function openSkinPopup() {
  isSkinPopupShow.value = true
}

function closeSkinPopup() {
  isSkinPopupShow.value = false
}

// 新增设置主题方法
function setTheme(theme: string) {
  currentTheme.value = theme
}


function resetAll() {
  if (currentChatTask) {
    currentChatTask.abort()
    currentChatTask = null
  }

  isStreaming.value = false
  clearAudioCache()

  chatHistory.value = [
    {
      id: createMsgId(),
      content: '你好呀，有什么想和我说的吗？',
      isSelf: false,
      audioStatus: 'idle'
    }
  ]

  localSessionMessages.value = [
    { role: 'ASSISTANT', content: '你好呀，有什么想和我说的吗？' }
  ]

  inputText.value = ''
  currentTheme.value = 'theme-purple'
  closeSkinPopup()
  scrollToBottom()

  uni.showToast({
    title: '已重置',
    icon: 'success',
    duration: 1500
  })
}

onBeforeUnmount(() => {
  if (currentChatTask) {
    currentChatTask.abort()
    currentChatTask = null
  }
  clearAudioCache()
  audioContext.destroy()
})



function syncAudioCacheWithChatHistory() {
  const validIds = new Set(chatHistory.value.map(item => item.id))
  Object.keys(localAudioCache.value).forEach((id) => {
    if (!validIds.has(id)) {
      const filePath = localAudioCache.value[id]
      if (filePath) {
        uni.getFileSystemManager?.().unlink({
          filePath,
          success: () => {},
          fail: () => {}
        })
      }
      delete localAudioCache.value[id]
    }
  })
}
</script>

<style scoped>
.page {
  position: fixed; /* 固定定位，禁止整体滚动 */
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden; /* 关键：禁止页面整体滑动 */
  background: linear-gradient(180deg, #fdfcff 0%, #f7f5ff 42%, #f3f6ff 100%);
}

/* 背景层 */
.bg-layer {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 18%, rgba(255, 210, 235, 0.36), transparent 28%),
    radial-gradient(circle at 82% 26%, rgba(196, 220, 255, 0.34), transparent 30%),
    radial-gradient(circle at 50% 78%, rgba(224, 214, 255, 0.26), transparent 34%);
  pointer-events: none;
}

.glow {
  position: absolute;
  border-radius: 9999rpx;
  filter: blur(30rpx);
  opacity: 0.65;
  pointer-events: none;
}

.glow-1 {
  top: 140rpx;
  left: -40rpx;
  width: 260rpx;
  height: 260rpx;
  background: rgba(255, 205, 226, 0.45);
}

.glow-2 {
  top: 260rpx;
  right: -60rpx;
  width: 320rpx;
  height: 320rpx;
  background: rgba(194, 216, 255, 0.42);
}

/* 1. 顶部预置控制栏 */
.top-control-bar {
  position: relative;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40rpx;
  padding: 30rpx 24rpx;
}

.control-item {
  padding: 16rpx 32rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(12rpx);
  box-shadow: 0 8rpx 20rpx rgba(179, 184, 255, 0.12);
  font-size: 28rpx;
  color: #6b7280;
  font-weight: 500;
}

/* 2. 聊天气泡容器 - 核心修改：可滚动 */
.chat-bubble-container {
  position: absolute; /* 改为绝对定位固定位置 */
  top: 180rpx; /* 距离顶部固定距离 */
  left: 0;
  right: 0;
  height: 380rpx;
  z-index: 4;
  overflow-y: auto; /* 开启纵向滚动 */
  padding: 0 40rpx;
  /* 隐藏滚动条 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

/* 隐藏微信小程序滚动条 */
.chat-bubble-container::-webkit-scrollbar {
  display: none;
}

.chat-bubble-area {
  display: flex;
  flex-direction: column; /* 垂直排列多条消息 */
  gap: 20rpx; /* 消息之间的间距 */
  padding: 10rpx 0;
}

/* 通用气泡样式 */
.chat-bubble {
  max-width: 70%;
  padding: 20rpx 28rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12rpx);
  box-shadow: 0 8rpx 24rpx rgba(179, 184, 255, 0.15);
  position: relative; /* 用于语音按钮定位 */
  display: flex;
  align-items: center;
  gap: 12rpx;
}

/* 左侧气泡（机器人回复） */
.chat-bubble-left {
  align-self: flex-start;
  border-bottom-left-radius: 8rpx;
}

/* 右侧气泡（用户发送） */
.chat-bubble-right {
  align-self: flex-end;
  border-bottom-right-radius: 8rpx;
  background: linear-gradient(135deg, #8ea7ff 0%, #b59cff 100%);
  margin-right: 60rpx; /* 核心修改：添加右侧外边距往左移 */
}

/* 气泡文字样式 */
.bubble-text {
  font-size: 28rpx;
  line-height: 1.5;
  flex: 1;
}

/* 左侧气泡文字颜色 */
.chat-bubble-left .bubble-text {
  color: #333;
}

/* 右侧气泡文字颜色 */
.chat-bubble-right .bubble-text {
  color: #fff;
}

/* 语音按钮样式 */
.voice-btn {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(179, 184, 255, 0.1);
}

.voice-btn-loading {
  background: rgba(255, 243, 205, 0.95);
}

.voice-btn-playing {
  background: linear-gradient(135deg, #8ea7ff 0%, #b59cff 100%);
  box-shadow: 0 0 0 4rpx rgba(142, 167, 255, 0.15);
}

.voice-btn-played {
  background: rgba(220, 252, 231, 0.95);
}

.voice-btn-error {
  background: rgba(254, 226, 226, 0.95);
}

.voice-btn-playing .voice-icon {
  color: #fff;
}

.voice-btn-played .voice-icon {
  color: #16a34a;
}

.voice-btn-error .voice-icon {
  color: #dc2626;
}

.voice-icon {
  font-size: 24rpx;
  color: #8ea7ff;
}

/* 3. Live2D主区域 */
.live2d-stage {
  position: relative;
  z-index: 2;
  width: 100%;
  padding-top: 0rpx;
  padding-bottom: 0rpx;
  margin: 0 auto;
}

.live2d-wrap {
  width: 84%;
  height: 1170rpx;
  margin: 0 auto;
}

:deep(.live2d-box) {
  width: 100%;
  height: 100%;
  display: block;
}

/* 底部氛围 */
.bottom-atmosphere {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 180rpx;
  height: 300rpx;
  z-index: 1;
  background: radial-gradient(
    ellipse at center,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(237, 233, 255, 0.72) 42%,
    rgba(237, 233, 255, 0) 75%
  );
  pointer-events: none;
}

/* 4. 输入区 - 绝对定位固定位置 */
.chat-panel {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 5;
  padding: 20rpx 24rpx 34rpx;
  background: linear-gradient(180deg, rgba(246, 247, 251, 0) 0%, rgba(246, 247, 251, 0.92) 30%, rgba(246, 247, 251, 1) 100%);
  box-sizing: border-box;
}

.chat-box {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(12rpx);
  box-shadow: 0 12rpx 36rpx rgba(179, 184, 255, 0.18);
}

.chat-input {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
  border-radius: 24rpx;
  background: rgba(246, 247, 255, 0.95);
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.send-btn {
  flex-shrink: 0;
  height: 72rpx;
  padding: 0 28rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #8ea7ff 0%, #b59cff 100%);
  box-shadow: 0 8rpx 20rpx rgba(150, 146, 255, 0.28);
}

.send-btn-stop {
  background: linear-gradient(135deg, #ff7a7a 0%, #ff5252 100%);
  box-shadow: 0 8rpx 20rpx rgba(255, 82, 82, 0.28);
}

.send-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: 600;
}

.placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 30rpx;
}

/* 新增主题样式 */
.theme-purple .page {
  background: linear-gradient(180deg, #fdfcff 0%, #f7f5ff 42%, #f3f6ff 100%);
}
.theme-purple .glow-1 {
  background: rgba(255, 205, 226, 0.45);
}
.theme-purple .glow-2 {
  background: rgba(194, 216, 255, 0.42);
}
.theme-purple .bottom-atmosphere {
  background: radial-gradient(
    ellipse at center,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(237, 233, 255, 0.72) 42%,
    rgba(237, 233, 255, 0) 75%
  );
}

/* 蓝白色主题 */
.theme-blue .page {
  background: linear-gradient(180deg, #f8fcff 0%, #e8f4f8 42%, #e0f2f7 100%);
}
.theme-blue .glow-1 {
  background: rgba(205, 235, 255, 0.45);
}
.theme-blue .glow-2 {
  background: rgba(180, 220, 255, 0.42);
}
.theme-blue .bottom-atmosphere {
  background: radial-gradient(
    ellipse at center,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(224, 240, 255, 0.72) 42%,
    rgba(224, 240, 255, 0) 75%
  );
}
.theme-blue .chat-bubble-right {
  background: linear-gradient(135deg, #7cb3f0 0%, #91c8ff 100%);
}
.theme-blue .voice-icon {
  color: #7cb3f0;
}
.theme-blue .send-btn {
  background: linear-gradient(135deg, #7cb3f0 0%, #91c8ff 100%);
}

/* 绿白色主题 */
.theme-green .page {
  background: linear-gradient(180deg, #f9fffb 0%, #e8f8ef 42%, #e0f5e6 100%);
}
.theme-green .glow-1 {
  background: rgba(205, 255, 226, 0.45);
}
.theme-green .glow-2 {
  background: rgba(180, 255, 210, 0.42);
}
.theme-green .bottom-atmosphere {
  background: radial-gradient(
    ellipse at center,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(224, 255, 235, 0.72) 42%,
    rgba(224, 255, 235, 0) 75%
  );
}
.theme-green .chat-bubble-right {
  background: linear-gradient(135deg, #7cf0b3 0%, #91ffc8 100%);
}
.theme-green .voice-icon {
  color: #7cf0b3;
}
.theme-green .send-btn {
  background: linear-gradient(135deg, #7cf0b3 0%, #91ffc8 100%);
}

/* 原生弹窗样式 - 遮罩层 */
.skin-popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4rpx);
  z-index: 9998;
}

/* 换肤弹窗样式 - 顶部窄版 */
.skin-popup {
  position: fixed; /* 固定定位，不受页面滚动影响 */
  top: 20rpx;      /* 屏幕顶部显示 */
  left: 50%;       /* 水平居中 */
  transform: translateX(-50%); /* 仅水平居中，保持顶部位置 */
  width: 240rpx;   /* 宽度限制为240rpx（不超过要求） */
  height: auto;    /* 高度自适应内容 */
  padding: 20rpx 15rpx; /* 紧凑内边距 */
  border-radius: 16rpx; /* 适配窄宽度，缩小圆角 */
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12rpx);
  z-index: 9999;   /* 最高层级，避免被遮挡 */
  box-sizing: border-box; /* 内边距计入宽度，防止超240rpx */
}
.popup-title {
  font-size: 24rpx; /* 缩小标题字号 */
  font-weight: 600;
  color: #333;
  text-align: center;
  margin-bottom: 15rpx; /* 减少间距 */
  white-space: nowrap; /* 标题不换行 */
  overflow: hidden;
  text-overflow: ellipsis; /* 超长标题省略 */
}
.skin-list {
  display: flex;
  flex-direction: column;
  gap: 10rpx; /* 极小间距，适配窄宽度 */
  margin-bottom: 15rpx;
}
.skin-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 15rpx; /* 紧凑内边距 */
  border-radius: 12rpx;
  cursor: pointer;
}
.skin-item.active {
  background: rgba(240, 245, 255, 0.8);
  border: 2rpx solid #8ea7ff;
}
.skin-preview {
  width: 40rpx;  /* 大幅缩小预览图，适配窄宽度 */
  height: 40rpx;
  border-radius: 8rpx;
}
.skin-name {
  font-size: 22rpx; /* 缩小文字 */
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* 文字超长省略 */
}
.skin-confirm {
  width: 100%;
  height: 60rpx; /* 缩小按钮 */
  line-height: 60rpx;
  text-align: center;
  background: linear-gradient(135deg, #8ea7ff 0%, #b59cff 100%);
  color: #fff;
  font-size: 24rpx; /* 缩小按钮文字 */
  border-radius: 12rpx;
}
</style>