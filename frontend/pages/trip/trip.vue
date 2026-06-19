<template>
  <view class="page">
    <map
      id="trip-map"
      class="map"
      :latitude="myLat"
      :longitude="myLng"
      :scale="14"
      :markers="markers"
      :polylines="polylines"
      :show-location="false"
      @regionchange="onRegionChange"
    >
      <cover-view
        v-if="toolMode !== 'none'"
        class="draw-overlay"
        @touchstart="onDrawStart"
        @touchmove.stop="onDrawMove"
        @touchend="onDrawEnd"
      />
    </map>

    <!-- 工具栏 -->
    <view class="toolbar">
      <view
        class="tool-btn"
        :class="{ active: toolMode === 'pen' }"
        @click="toggleTool('pen')"
      >
        <text class="tool-icon">✏️</text>
        <text class="tool-label">画笔</text>
      </view>
      <view
        class="tool-btn"
        :class="{ active: toolMode === 'eraser' }"
        @click="toggleTool('eraser')"
      >
        <text class="tool-icon">🧹</text>
        <text class="tool-label">橡皮</text>
      </view>
      <view class="tool-btn" @click="clearMyStrokes">
        <text class="tool-icon">🗑️</text>
        <text class="tool-label">清除</text>
      </view>
      <view class="tool-mode-hint" v-if="toolMode !== 'none'">
        <text>{{ toolMode === 'pen' ? '绘制中' : '擦除中' }} · 再次点击关闭</text>
      </view>
    </view>

    <!-- 信息面板 -->
    <view class="panel">
      <view class="panel-row">
        <text class="panel-label">目的地</text>
        <text class="panel-value">{{ spotName }}</text>
      </view>
      <view class="panel-row">
        <text class="panel-label">搭子</text>
        <text class="panel-value">{{ partnerNickname }}</text>
      </view>
      <view class="panel-row">
        <text class="panel-label">与搭子距离</text>
        <text class="panel-value">{{ distanceText }}</text>
      </view>
      <button class="leave-btn" @click="leaveTrip">结束旅途</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { setMessageHandler, sendMatch, disconnectMatch } from '@/api/match'

const _instance = getCurrentInstance()

// ── 页面参数 ─────────────────────────────────────────────────────────────────
const spotName = ref('')
const partnerNickname = ref('')
const spotId = ref(0)

// ── 位置 ─────────────────────────────────────────────────────────────────────
const myLat = ref(29.8266)
const myLng = ref(106.422)
const partnerLat = ref<number | null>(null)
const partnerLng = ref<number | null>(null)
let locationTimer: ReturnType<typeof setInterval> | null = null

// ── 标记 ─────────────────────────────────────────────────────────────────────
const markers = computed(() => {
  const list: any[] = [
    {
      id: 1,
      latitude: myLat.value,
      longitude: myLng.value,
      title: '我',
      width: 40, height: 40,
      iconPath: '',
      callout: { content: '我', color: '#fff', bgColor: '#2196f3', padding: 8, borderRadius: 8, display: 'ALWAYS' }
    }
  ]
  if (partnerLat.value !== null && partnerLng.value !== null) {
    list.push({
      id: 2,
      latitude: partnerLat.value,
      longitude: partnerLng.value,
      title: partnerNickname.value,
      width: 40, height: 40,
      iconPath: '',
      callout: { content: partnerNickname.value, color: '#fff', bgColor: '#f44336', padding: 8, borderRadius: 8, display: 'ALWAYS' }
    })
  }
  return list
})

const distanceText = computed(() => {
  if (partnerLat.value === null || partnerLng.value === null) return '等待搭子位置…'
  const d = haversine(myLat.value, myLng.value, partnerLat.value, partnerLng.value)
  return d < 1000 ? `${Math.round(d)} 米` : `${(d / 1000).toFixed(1)} 千米`
})

// ── 涂鸦 ─────────────────────────────────────────────────────────────────────
type ToolMode = 'none' | 'pen' | 'eraser'
const toolMode = ref<ToolMode>('none')

interface StrokePoint { latitude: number; longitude: number }
interface Stroke { id: string; points: StrokePoint[] }

// 用数组确保 Vue 响应式正常追踪
const myStrokes = ref<Stroke[]>([])
const partnerStrokes = ref<Stroke[]>([])
const currentStroke = ref<Stroke | null>(null)

// 地图上下文 & 视野缓存
let mapCtx: any = null
let cachedRegion: { southwest: { latitude: number; longitude: number }; northeast: { latitude: number; longitude: number } } | null = null
let mapLeft = 0
let mapTop = 0
let mapWidth = 0
let mapHeight = 0

const polylines = computed(() => {
  const lines: any[] = []
  for (const s of myStrokes.value) {
    if (s.points.length >= 2) lines.push({ points: s.points, color: '#2196f3BB', width: 5 })
  }
  if (currentStroke.value && currentStroke.value.points.length >= 2) {
    lines.push({ points: currentStroke.value.points, color: '#2196f3BB', width: 5 })
  }
  for (const s of partnerStrokes.value) {
    if (s.points.length >= 2) lines.push({ points: s.points, color: '#f44336BB', width: 5 })
  }
  return lines
})

// ── 生命周期 ─────────────────────────────────────────────────────────────────
onLoad((query) => {
  spotId.value = Number(query?.spotId ?? 0)
  spotName.value = decodeURIComponent(query?.spotName ?? '')
  partnerNickname.value = decodeURIComponent(query?.partnerNickname ?? '搭子')
})

onMounted(() => {
  setMessageHandler(onWsMessage)
  updateMyLocation()
  locationTimer = setInterval(updateMyLocation, 3000)

  // 等地图渲染完成后初始化上下文
  setTimeout(() => {
    // UniApp 中必须传组件实例，否则无法找到页面内的原生组件
    mapCtx = uni.createMapContext('trip-map', _instance)

    uni.createSelectorQuery()
      .in(_instance)
      .select('#trip-map')
      .boundingClientRect((rect: any) => {
        if (rect) {
          mapLeft = rect.left
          mapTop = rect.top
          mapWidth = rect.width
          mapHeight = rect.height
        }
      })
      .exec()

    refreshRegion()
  }, 500)
})

onUnmounted(() => {
  if (locationTimer) clearInterval(locationTimer)
  disconnectMatch()
})

// ── WebSocket 消息 ────────────────────────────────────────────────────────────
function onWsMessage(msg: { type: string; payload: Record<string, any> }) {
  switch (msg.type) {
    case 'locationUpdate':
      partnerLat.value = msg.payload.latitude
      partnerLng.value = msg.payload.longitude
      break
    case 'partnerLeft':
      uni.showModal({ title: '搭子已离开', content: '搭子结束了旅途', showCancel: false })
      partnerLat.value = null
      partnerLng.value = null
      break
    case 'partnerDrawStroke': {
      const { id, points } = msg.payload
      // 过滤掉已存在的同 id（防重复）
      partnerStrokes.value = [...partnerStrokes.value.filter(s => s.id !== id), { id, points }]
      break
    }
    case 'partnerEraseStroke':
      partnerStrokes.value = partnerStrokes.value.filter(s => s.id !== msg.payload.id)
      break
  }
}

// ── 位置更新 ─────────────────────────────────────────────────────────────────
function updateMyLocation() {
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      myLat.value = res.latitude
      myLng.value = res.longitude
      sendMatch('location', { latitude: res.latitude, longitude: res.longitude })
    }
  })
}

// ── 地图视野 ─────────────────────────────────────────────────────────────────
function refreshRegion() {
  mapCtx?.getRegion({
    success: (res: any) => { cachedRegion = res },
  })
}

function onRegionChange(e: any) {
  // 地图拖拽结束后刷新缓存视野
  if (e.type === 'end') refreshRegion()
}

function pixelToLatLng(clientX: number, clientY: number): StrokePoint | null {
  if (!cachedRegion) return null
  // mapWidth/mapHeight 未初始化时从系统信息降级
  const w = mapWidth || uni.getSystemInfoSync().windowWidth
  const h = mapHeight || uni.getSystemInfoSync().windowHeight * 0.55
  const px = clientX - mapLeft
  const py = clientY - mapTop
  if (px < 0 || py < 0 || px > w || py > h) return null
  const { southwest, northeast } = cachedRegion
  return {
    latitude:  northeast.latitude  - (py / h) * (northeast.latitude  - southwest.latitude),
    longitude: southwest.longitude + (px / w) * (northeast.longitude - southwest.longitude),
  }
}

// ── 工具切换 ─────────────────────────────────────────────────────────────────
function toggleTool(mode: ToolMode) {
  if (toolMode.value === mode) {
    toolMode.value = 'none'
  } else {
    toolMode.value = mode
    // 切换到绘图模式时立即刷新视野
    if (!cachedRegion) refreshRegion()
  }
}

// ── 触摸绘制 ─────────────────────────────────────────────────────────────────
function onDrawStart(e: any) {
  if (!e.touches[0]) return
  const { clientX, clientY } = e.touches[0]

  // 若视野尚未缓存，先拉取再等下次触摸
  if (!cachedRegion) {
    refreshRegion()
    return
  }

  if (toolMode.value === 'pen') {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`
    const pt = pixelToLatLng(clientX, clientY)
    currentStroke.value = { id, points: pt ? [pt] : [] }
  } else if (toolMode.value === 'eraser') {
    const pt = pixelToLatLng(clientX, clientY)
    if (pt) eraseNear(pt)
  }
}

function onDrawMove(e: any) {
  if (!e.touches[0] || toolMode.value !== 'pen' || !currentStroke.value || !cachedRegion) return
  const { clientX, clientY } = e.touches[0]
  const pt = pixelToLatLng(clientX, clientY)
  if (!pt) return

  // 最小间距去重，避免点过密
  const pts = currentStroke.value.points
  if (pts.length > 0) {
    const last = pts[pts.length - 1]
    if (Math.abs(pt.latitude - last.latitude) + Math.abs(pt.longitude - last.longitude) < 0.00001) return
  }
  currentStroke.value.points.push(pt)
}

function onDrawEnd() {
  if (toolMode.value !== 'pen' || !currentStroke.value) return
  const stroke = currentStroke.value
  currentStroke.value = null
  if (stroke.points.length < 2) return

  myStrokes.value = [...myStrokes.value, stroke]
  sendMatch('drawStroke', { id: stroke.id, points: stroke.points })
}

function eraseNear(pt: StrokePoint) {
  const THRESHOLD = 0.0004
  for (const stroke of myStrokes.value) {
    for (const p of stroke.points) {
      if (Math.abs(p.latitude - pt.latitude) + Math.abs(p.longitude - pt.longitude) < THRESHOLD) {
        myStrokes.value = myStrokes.value.filter(s => s.id !== stroke.id)
        sendMatch('eraseStroke', { id: stroke.id })
        return
      }
    }
  }
}

function clearMyStrokes() {
  const ids = myStrokes.value.map(s => s.id)
  myStrokes.value = []
  ids.forEach(id => sendMatch('eraseStroke', { id }))
}

// ── 离开 ─────────────────────────────────────────────────────────────────────
function leaveTrip() {
  uni.showModal({
    title: '结束旅途',
    content: '确定要结束本次旅途吗？',
    success: (res) => {
      if (res.confirm) {
        sendMatch('leave')
        disconnectMatch()
        uni.redirectTo({ url: '/pages/index/index' })
      }
    }
  })
}

// ── 工具函数 ─────────────────────────────────────────────────────────────────
function haversine(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}
</script>

<style scoped>
.page { height: 100vh; display: flex; flex-direction: column; }
.map { width: 100%; flex: 1; }

.draw-overlay {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: transparent;
}

.toolbar {
  display: flex; flex-direction: row; align-items: center;
  padding: 12rpx 24rpx; background: #fff;
  border-top: 1rpx solid #eee; border-bottom: 1rpx solid #eee;
  gap: 16rpx;
}
.tool-btn {
  display: flex; flex-direction: column; align-items: center;
  padding: 10rpx 20rpx; border-radius: 12rpx;
  background: #f5f6f7;
}
.tool-btn.active { background: #fff3ee; }
.tool-icon { font-size: 34rpx; }
.tool-label { font-size: 20rpx; color: #666; margin-top: 4rpx; }
.tool-mode-hint {
  flex: 1; text-align: right;
  font-size: 22rpx; color: #ff6b35;
}

.panel {
  background: #fff;
  padding: 20rpx 32rpx;
  border-top: 1rpx solid #eee;
}
.panel-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.panel-label { font-size: 26rpx; color: #888; }
.panel-value { font-size: 28rpx; color: #222; font-weight: 500; }

.leave-btn {
  margin-top: 20rpx; width: 100%; height: 80rpx; line-height: 80rpx;
  background: #1a1a2e; color: #fff;
  border-radius: 40rpx; font-size: 28rpx; border: none;
}
</style>
