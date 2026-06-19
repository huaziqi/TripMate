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
        :polylines="mapPolylines"
        :show-location="false"
        @regionchange="onRegionChange"
      />
      <!--
        画笔/橡皮模式时 canvas 叠加地图（同层渲染）。
        此时地图无法拖拽，切回"无"模式后恢复。
        已完成的笔迹保存在 mapPolylines（lat/lng），
        随地图移动/缩放自动跟随，canvas 只用于当前笔的实时预览。
      -->
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
      <text v-if="toolMode !== 'none'" class="tool-hint">绘制中・点按钮退出可移动地图</text>
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

// ── 笔画数据（lat/lng 存储，随地图自动跟随）─────────────────────────────────
interface Pt { latitude: number; longitude: number }
interface Stroke { id: string; points: Pt[] }

const myStrokes = ref<Stroke[]>([])
const partnerStrokes = ref<Stroke[]>([])

// mapPolylines 是地图组件的 :polylines，lat/lng 驱动，移动/缩放自动跟随
// 使用 ref + 显式更新，避免 UniApp 编译后 computed 对原生组件 prop 响应不稳定
const mapPolylines = ref<any[]>([])

function updatePolylines() {
  mapPolylines.value = [
    ...myStrokes.value
      .filter(s => s.points.length >= 2)
      .map(s => ({ points: s.points, color: '#2196f3', width: 5 })),
    ...partnerStrokes.value
      .filter(s => s.points.length >= 2)
      .map(s => ({ points: s.points, color: '#f44336', width: 5 })),
  ]
}

// ── 工具 ─────────────────────────────────────────────────────────────────────
type ToolMode = 'none' | 'pen' | 'eraser'
const toolMode = ref<ToolMode>('none')

function toggleTool(mode: ToolMode) {
  toolMode.value = toolMode.value === mode ? 'none' : mode
}

// ── Canvas（实时预览当前笔画，完成后交给 mapPolylines）──────────────────────
let ctx: any = null
let canvasW = 0
let canvasH = 0
let canvasLeft = 0
let canvasTop = 0
let currentPoints: Pt[] = []

// 进入绘图模式时初始化 canvas 上下文
watch(toolMode, async (val, prev) => {
  if (val !== 'none' && prev === 'none') {
    await nextTick()
    initCanvas()
  }
  if (val === 'none') {
    ctx = null
    currentPoints = []
  }
})

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
        })
        .exec()
    })
}

// ── 地图视野 ─────────────────────────────────────────────────────────────────
let mapCtx: any = null
let region: { sw: Pt; ne: Pt } | null = null

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
  if (e.type === 'end') fetchRegion()
}

// ── 坐标转换 ─────────────────────────────────────────────────────────────────
function xyToPt(x: number, y: number): Pt | null {
  if (!region || !canvasW || !canvasH) return null
  return {
    latitude:  region.ne.latitude  - (y / canvasH) * (region.ne.latitude  - region.sw.latitude),
    longitude: region.sw.longitude + (x / canvasW) * (region.ne.longitude - region.sw.longitude),
  }
}

function ptToXY(p: Pt): { x: number; y: number } | null {
  if (!region || !canvasW || !canvasH) return null
  return {
    x: ((p.longitude - region.sw.longitude) / (region.ne.longitude - region.sw.longitude)) * canvasW,
    y: ((region.ne.latitude - p.latitude)  / (region.ne.latitude  - region.sw.latitude))  * canvasH,
  }
}

// ── Canvas 实时预览 ───────────────────────────────────────────────────────────
function redrawCanvas() {
  if (!ctx || !canvasW || !canvasH || currentPoints.length < 2) return
  ctx.clearRect(0, 0, canvasW, canvasH)
  const first = ptToXY(currentPoints[0])
  if (!first) return
  ctx.beginPath()
  ctx.strokeStyle = '#2196f3BB'
  ctx.lineWidth = 5
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.moveTo(first.x, first.y)
  for (let i = 1; i < currentPoints.length; i++) {
    const p = ptToXY(currentPoints[i])
    if (p) ctx.lineTo(p.x, p.y)
  }
  ctx.stroke()
}

// ── 触摸事件 ─────────────────────────────────────────────────────────────────
function onDrawStart(e: any) {
  const t = e.touches?.[0]
  if (!t) return
  if (!region) { fetchRegion(); return }
  const x = t.clientX - canvasLeft
  const y = t.clientY - canvasTop

  if (toolMode.value === 'pen') {
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
  const pt = xyToPt(t.clientX - canvasLeft, t.clientY - canvasTop)
  if (!pt) return
  if (currentPoints.length > 0) {
    const last = currentPoints[currentPoints.length - 1]
    if (Math.abs(pt.latitude - last.latitude) + Math.abs(pt.longitude - last.longitude) < 0.00001) return
  }
  currentPoints.push(pt)
  redrawCanvas()
}

function onDrawEnd() {
  // 清除 canvas 预览层
  if (ctx) ctx.clearRect(0, 0, canvasW, canvasH)

  if (toolMode.value !== 'pen' || currentPoints.length < 2) {
    currentPoints = []
    return
  }

  // 把这一笔提交到 mapPolylines（lat/lng 驱动，永久跟随地图）
  const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`
  const stroke: Stroke = { id, points: [...currentPoints] }
  currentPoints = []
  myStrokes.value = [...myStrokes.value, stroke]
  updatePolylines()
  sendMatch('drawStroke', { id: stroke.id, points: stroke.points })
}

function eraseNear(pt: Pt) {
  const THRESHOLD = 0.0004
  for (const stroke of myStrokes.value) {
    for (const p of stroke.points) {
      if (Math.abs(p.latitude - pt.latitude) + Math.abs(p.longitude - pt.longitude) < THRESHOLD) {
        myStrokes.value = myStrokes.value.filter(s => s.id !== stroke.id)
        updatePolylines()
        sendMatch('eraseStroke', { id: stroke.id })
        return
      }
    }
  }
}

function clearMyStrokes() {
  const ids = myStrokes.value.map(s => s.id)
  myStrokes.value = []
  updatePolylines()
  ids.forEach(id => sendMatch('eraseStroke', { id }))
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
      updatePolylines()
      break
    }
    case 'partnerEraseStroke':
      partnerStrokes.value = partnerStrokes.value.filter(s => s.id !== msg.payload.id)
      updatePolylines()
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

.map-container { flex: 1; position: relative; }
.map-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.canvas-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: transparent; }

.toolbar {
  display: flex; align-items: center;
  padding: 12rpx 24rpx; gap: 16rpx;
  background: #fff; border-top: 1rpx solid #eee; border-bottom: 1rpx solid #eee;
}
.tool-btn {
  display: flex; flex-direction: column; align-items: center;
  padding: 10rpx 20rpx; border-radius: 12rpx; background: #f5f6f7;
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
  background: #1a1a2e; color: #fff; border-radius: 40rpx; font-size: 28rpx; border: none;
}
</style>
