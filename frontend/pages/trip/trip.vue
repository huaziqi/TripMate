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
      <!-- 透明覆盖层，仅在绘图/橡皮模式下拦截触摸 -->
      <cover-view
        v-if="toolMode !== 'none'"
        class="draw-overlay"
        @touchstart="onDrawStart"
        @touchmove="onDrawMove"
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
        <text>{{ toolMode === 'pen' ? '绘制中' : '擦除中' }} · 点击再次关闭</text>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { setMessageHandler, sendMatch, disconnectMatch } from '@/api/match'

// ── 页面参数 ────────────────────────────────────────────────────────────────
const spotName = ref('')
const partnerNickname = ref('')
const spotId = ref(0)

// ── 位置 ────────────────────────────────────────────────────────────────────
const myLat = ref(29.8266)
const myLng = ref(106.422)
const partnerLat = ref<number | null>(null)
const partnerLng = ref<number | null>(null)
let locationTimer: ReturnType<typeof setInterval> | null = null

// ── 地图标记 ─────────────────────────────────────────────────────────────────
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
interface Stroke { id: string; points: StrokePoint[]; owner: 'me' | 'partner' }

const myStrokes = ref(new Map<string, Stroke>())
const partnerStrokes = ref(new Map<string, Stroke>())
const currentStroke = ref<Stroke | null>(null)

// 地图视野缓存，用于像素 → 经纬度换算
let mapCtx: any = null
let cachedRegion: { southwest: any; northeast: any } | null = null
let mapLeft = 0
let mapTop = 0
let mapWidth = 0
let mapHeight = 0

const polylines = computed(() => {
  const lines: any[] = []

  myStrokes.value.forEach(s => {
    if (s.points.length >= 2) {
      lines.push({ points: s.points, color: '#2196f3BB', width: 5, arrowLine: false })
    }
  })

  if (currentStroke.value && currentStroke.value.points.length >= 2) {
    lines.push({ points: currentStroke.value.points, color: '#2196f3BB', width: 5 })
  }

  partnerStrokes.value.forEach(s => {
    if (s.points.length >= 2) {
      lines.push({ points: s.points, color: '#f44336BB', width: 5, arrowLine: false })
    }
  })

  return lines
})

// ── 生命周期 ──────────────────────────────────────────────────────────────────
onLoad((query) => {
  spotId.value = Number(query?.spotId ?? 0)
  spotName.value = decodeURIComponent(query?.spotName ?? '')
  partnerNickname.value = decodeURIComponent(query?.partnerNickname ?? '搭子')
})

onMounted(() => {
  setMessageHandler(onWsMessage)
  updateMyLocation()
  locationTimer = setInterval(updateMyLocation, 3000)

  mapCtx = uni.createMapContext('trip-map')

  // 缓存地图容器位置和尺寸
  uni.createSelectorQuery()
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
})

onUnmounted(() => {
  if (locationTimer) clearInterval(locationTimer)
  disconnectMatch()
})

// ── WebSocket 消息处理 ─────────────────────────────────────────────────────────
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
      partnerStrokes.value.set(id, { id, points, owner: 'partner' })
      // 触发响应式更新
      partnerStrokes.value = new Map(partnerStrokes.value)
      break
    }
    case 'partnerEraseStroke':
      partnerStrokes.value.delete(msg.payload.id)
      partnerStrokes.value = new Map(partnerStrokes.value)
      break
  }
}

// ── 位置更新 ──────────────────────────────────────────────────────────────────
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

// ── 地图视野 ──────────────────────────────────────────────────────────────────
function refreshRegion() {
  mapCtx?.getRegion({
    success: (res: any) => { cachedRegion = res }
  })
}

function onRegionChange(e: any) {
  if (e.type === 'end') refreshRegion()
}

function pixelToLatLng(clientX: number, clientY: number): StrokePoint | null {
  if (!cachedRegion || !mapWidth || !mapHeight) return null
  const px = clientX - mapLeft
  const py = clientY - mapTop
  if (px < 0 || py < 0 || px > mapWidth || py > mapHeight) return null
  const { southwest, northeast } = cachedRegion
  return {
    latitude:  northeast.latitude  - (py / mapHeight) * (northeast.latitude  - southwest.latitude),
    longitude: southwest.longitude + (px / mapWidth)  * (northeast.longitude - southwest.longitude),
  }
}

// ── 工具栏 ────────────────────────────────────────────────────────────────────
function toggleTool(mode: ToolMode) {
  toolMode.value = toolMode.value === mode ? 'none' : mode
}

// ── 绘制事件 ──────────────────────────────────────────────────────────────────
function onDrawStart(e: any) {
  if (!e.touches[0]) return
  const { clientX, clientY } = e.touches[0]

  if (toolMode.value === 'pen') {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`
    currentStroke.value = { id, points: [], owner: 'me' }
    const pt = pixelToLatLng(clientX, clientY)
    if (pt) currentStroke.value.points.push(pt)
  } else if (toolMode.value === 'eraser') {
    const pt = pixelToLatLng(clientX, clientY)
    if (pt) eraseNear(pt)
  }
}

function onDrawMove(e: any) {
  if (!e.touches[0] || toolMode.value !== 'pen' || !currentStroke.value) return
  const { clientX, clientY } = e.touches[0]
  const pt = pixelToLatLng(clientX, clientY)
  if (!pt) return

  // 最小间距过滤，避免点过于密集
  const pts = currentStroke.value.points
  if (pts.length > 0) {
    const last = pts[pts.length - 1]
    const dist = Math.abs(pt.latitude - last.latitude) + Math.abs(pt.longitude - last.longitude)
    if (dist < 0.00002) return
  }
  currentStroke.value.points.push(pt)
}

function onDrawEnd() {
  if (toolMode.value !== 'pen' || !currentStroke.value) return
  const stroke = currentStroke.value
  currentStroke.value = null
  if (stroke.points.length < 2) return

  myStrokes.value.set(stroke.id, stroke)
  myStrokes.value = new Map(myStrokes.value)
  sendMatch('drawStroke', { id: stroke.id, points: stroke.points })
}

function eraseNear(pt: StrokePoint) {
  const THRESHOLD = 0.0004 // 约 44 米
  for (const [id, stroke] of myStrokes.value.entries()) {
    for (const p of stroke.points) {
      const d = Math.abs(p.latitude - pt.latitude) + Math.abs(p.longitude - pt.longitude)
      if (d < THRESHOLD) {
        myStrokes.value.delete(id)
        myStrokes.value = new Map(myStrokes.value)
        sendMatch('eraseStroke', { id })
        return
      }
    }
  }
}

function clearMyStrokes() {
  const ids = [...myStrokes.value.keys()]
  myStrokes.value.clear()
  myStrokes.value = new Map()
  ids.forEach(id => sendMatch('eraseStroke', { id }))
}

// ── 离开 ──────────────────────────────────────────────────────────────────────
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

// ── 工具函数 ──────────────────────────────────────────────────────────────────
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

/* 绘图覆盖层：全覆盖 map，capture touch 事件 */
.draw-overlay {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: transparent;
}

/* 工具栏 */
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

/* 信息面板 */
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
