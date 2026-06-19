<template>
  <view class="page">
    <view class="map-container">
      <map
        id="trip-map"
        class="map-layer"
        :latitude="myLat"
        :longitude="myLng"
        :scale="14"
        :markers="markers"
        :show-location="false"
        @regionchange="onRegionChange"
      />
      <!-- type="2d" canvas 通过同层渲染叠加在 map 上 -->
      <canvas
        v-if="toolMode !== 'none'"
        type="2d"
        id="draw-canvas"
        class="canvas-layer"
        @touchstart="onDrawStart"
        @touchmove.stop="onDrawMove"
        @touchend="onDrawEnd"
      />
    </view>

    <view class="toolbar">
      <view class="tool-btn" :class="{ active: toolMode === 'pen' }" @click="toggleTool('pen')">
        <text class="tool-icon">✏️</text>
        <text class="tool-label">画笔</text>
      </view>
      <view class="tool-btn" :class="{ active: toolMode === 'eraser' }" @click="toggleTool('eraser')">
        <text class="tool-icon">🧹</text>
        <text class="tool-label">橡皮</text>
      </view>
      <view class="tool-btn" @click="clearMyStrokes">
        <text class="tool-icon">🗑️</text>
        <text class="tool-label">清除</text>
      </view>
      <text v-if="toolMode !== 'none'" class="tool-hint">{{ toolMode === 'pen' ? '绘制中' : '擦除中' }}・再按关闭</text>
    </view>

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
import { ref, computed, watch, onMounted, onUnmounted, getCurrentInstance, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { setMessageHandler, sendMatch, disconnectMatch } from '@/api/match'

const _inst = getCurrentInstance()

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

const markers = computed(() => {
  const list: any[] = [{
    id: 1, latitude: myLat.value, longitude: myLng.value,
    title: '我', width: 40, height: 40, iconPath: '',
    callout: { content: '我', color: '#fff', bgColor: '#2196f3', padding: 8, borderRadius: 8, display: 'ALWAYS' }
  }]
  if (partnerLat.value !== null && partnerLng.value !== null) {
    list.push({
      id: 2, latitude: partnerLat.value, longitude: partnerLng.value,
      title: partnerNickname.value, width: 40, height: 40, iconPath: '',
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

// ── 涂鸦状态 ─────────────────────────────────────────────────────────────────
type ToolMode = 'none' | 'pen' | 'eraser'
const toolMode = ref<ToolMode>('none')

interface Pt { latitude: number; longitude: number }
interface Stroke { id: string; points: Pt[] }

const myStrokes = ref<Stroke[]>([])
const partnerStrokes = ref<Stroke[]>([])
let currentPoints: Pt[] = []

// ── Canvas 上下文 ─────────────────────────────────────────────────────────────
let ctx: any = null
let canvasW = 0
let canvasH = 0
let canvasLeft = 0
let canvasTop = 0

// ── 地图视野缓存 ──────────────────────────────────────────────────────────────
let mapCtx: any = null
let region: { sw: Pt; ne: Pt } | null = null

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

  setTimeout(() => {
    mapCtx = uni.createMapContext('trip-map', _inst)
    fetchRegion()
  }, 500)
})

onUnmounted(() => {
  if (locationTimer) clearInterval(locationTimer)
  disconnectMatch()
})

// 切到绘图模式时初始化 canvas
watch(toolMode, async (next, prev) => {
  if (next !== 'none' && prev === 'none') {
    await nextTick()
    initCanvas()
  }
})

// ── Canvas 初始化 ─────────────────────────────────────────────────────────────
function initCanvas() {
  uni.createSelectorQuery()
    .in(_inst)
    .select('#draw-canvas')
    .node()
    .exec((res: any[]) => {
      const node = res[0]?.node
      if (!node) return

      uni.createSelectorQuery()
        .in(_inst)
        .select('.map-container')
        .boundingClientRect((rect: any) => {
          if (!rect) return
          canvasW = rect.width
          canvasH = rect.height
          canvasLeft = rect.left
          canvasTop = rect.top
          node.width = rect.width
          node.height = rect.height
          ctx = node.getContext('2d')
          redrawAll()
        })
        .exec()
    })
}

// ── 地图视野 ─────────────────────────────────────────────────────────────────
function fetchRegion() {
  mapCtx?.getRegion({
    success: (r: any) => {
      region = {
        sw: { latitude: r.southwest.latitude, longitude: r.southwest.longitude },
        ne: { latitude: r.northeast.latitude, longitude: r.northeast.longitude },
      }
    },
  })
}

function onRegionChange(e: any) {
  if (e.type === 'end') {
    fetchRegion()
    // 地图移动后用新视野重绘笔迹
    setTimeout(redrawAll, 100)
  }
}

// ── 坐标转换 ─────────────────────────────────────────────────────────────────
function ptToXY(lat: number, lng: number): { x: number; y: number } | null {
  if (!region || !canvasW || !canvasH) return null
  const x = ((lng - region.sw.longitude) / (region.ne.longitude - region.sw.longitude)) * canvasW
  const y = ((region.ne.latitude - lat) / (region.ne.latitude - region.sw.latitude)) * canvasH
  return { x, y }
}

function xyToPt(x: number, y: number): Pt | null {
  if (!region || !canvasW || !canvasH) return null
  return {
    latitude:  region.ne.latitude  - (y / canvasH) * (region.ne.latitude  - region.sw.latitude),
    longitude: region.sw.longitude + (x / canvasW) * (region.ne.longitude - region.sw.longitude),
  }
}

// ── Canvas 绘制 ───────────────────────────────────────────────────────────────
function redrawAll() {
  if (!ctx || !canvasW || !canvasH) return
  ctx.clearRect(0, 0, canvasW, canvasH)
  for (const s of myStrokes.value) drawStroke(s.points, '#2196f3')
  if (currentPoints.length >= 2) drawStroke(currentPoints, '#2196f3')
  for (const s of partnerStrokes.value) drawStroke(s.points, '#f44336')
}

function drawStroke(points: Pt[], color: string) {
  if (!ctx || points.length < 2) return
  const first = ptToXY(points[0].latitude, points[0].longitude)
  if (!first) return
  ctx.beginPath()
  ctx.strokeStyle = color
  ctx.lineWidth = 4
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.moveTo(first.x, first.y)
  for (let i = 1; i < points.length; i++) {
    const p = ptToXY(points[i].latitude, points[i].longitude)
    if (p) ctx.lineTo(p.x, p.y)
  }
  ctx.stroke()
}

// ── 触摸事件 ─────────────────────────────────────────────────────────────────
function onDrawStart(e: any) {
  const t = e.touches?.[0]
  if (!t) return
  const x = t.clientX - canvasLeft
  const y = t.clientY - canvasTop

  if (toolMode.value === 'pen') {
    if (!region) { fetchRegion(); return }
    const pt = xyToPt(x, y)
    currentPoints = pt ? [pt] : []
  } else if (toolMode.value === 'eraser') {
    const pt = xyToPt(x, y)
    if (pt) eraseNear(pt)
  }
}

function onDrawMove(e: any) {
  const t = e.touches?.[0]
  if (!t || toolMode.value !== 'pen') return
  const x = t.clientX - canvasLeft
  const y = t.clientY - canvasTop
  const pt = xyToPt(x, y)
  if (!pt) return

  if (currentPoints.length > 0) {
    const last = currentPoints[currentPoints.length - 1]
    const d = Math.abs(pt.latitude - last.latitude) + Math.abs(pt.longitude - last.longitude)
    if (d < 0.00001) return
  }
  currentPoints.push(pt)
  redrawAll()
}

function onDrawEnd() {
  if (toolMode.value !== 'pen' || currentPoints.length < 2) {
    currentPoints = []
    return
  }
  const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`
  const stroke: Stroke = { id, points: [...currentPoints] }
  currentPoints = []
  myStrokes.value = [...myStrokes.value, stroke]
  sendMatch('drawStroke', { id: stroke.id, points: stroke.points })
  redrawAll()
}

function eraseNear(pt: Pt) {
  const THRESHOLD = 0.0004
  for (const stroke of myStrokes.value) {
    for (const p of stroke.points) {
      if (Math.abs(p.latitude - pt.latitude) + Math.abs(p.longitude - pt.longitude) < THRESHOLD) {
        myStrokes.value = myStrokes.value.filter(s => s.id !== stroke.id)
        sendMatch('eraseStroke', { id: stroke.id })
        redrawAll()
        return
      }
    }
  }
}

function clearMyStrokes() {
  const ids = myStrokes.value.map(s => s.id)
  myStrokes.value = []
  ids.forEach(id => sendMatch('eraseStroke', { id }))
  redrawAll()
}

// ── 工具切换 ─────────────────────────────────────────────────────────────────
function toggleTool(mode: ToolMode) {
  toolMode.value = toolMode.value === mode ? 'none' : mode
}

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
      partnerStrokes.value = [...partnerStrokes.value.filter(s => s.id !== id), { id, points }]
      redrawAll()
      break
    }
    case 'partnerEraseStroke':
      partnerStrokes.value = partnerStrokes.value.filter(s => s.id !== msg.payload.id)
      redrawAll()
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
    },
  })
}

// ── 结束旅途 ─────────────────────────────────────────────────────────────────
function leaveTrip() {
  uni.showModal({
    title: '结束旅途', content: '确定要结束本次旅途吗？',
    success: (res) => {
      if (res.confirm) {
        sendMatch('leave')
        disconnectMatch()
        uni.redirectTo({ url: '/pages/index/index' })
      }
    },
  })
}

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

.map-container {
  flex: 1;
  position: relative;
}
.map-layer {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
}
/* canvas 同层渲染叠加在 map 上 */
.canvas-layer {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: transparent;
}

.toolbar {
  display: flex; align-items: center;
  padding: 12rpx 24rpx; gap: 16rpx;
  background: #fff;
  border-top: 1rpx solid #eee;
  border-bottom: 1rpx solid #eee;
}
.tool-btn {
  display: flex; flex-direction: column; align-items: center;
  padding: 10rpx 20rpx; border-radius: 12rpx;
  background: #f5f6f7;
}
.tool-btn.active { background: #fff3ee; }
.tool-icon { font-size: 34rpx; }
.tool-label { font-size: 20rpx; color: #666; margin-top: 4rpx; }
.tool-hint { flex: 1; text-align: right; font-size: 22rpx; color: #ff6b35; }

.panel { background: #fff; padding: 20rpx 32rpx; }
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
