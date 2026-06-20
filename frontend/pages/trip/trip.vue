<template>
  <view class="page">
    <!-- ── 任务完成弹窗 ───────────────────────────────────────────── -->
    <view v-if="completedChallenge" class="challenge-popup">
      <view class="popup-inner">
        <text class="popup-star">✨</text>
        <view class="popup-body">
          <text class="popup-tag">任务完成！</text>
          <text class="popup-icon">{{ completedChallenge.icon }}</text>
          <text class="popup-title">{{ completedChallenge.title }}</text>
          <text class="popup-desc">{{ completedChallenge.desc }}</text>
        </view>
      </view>
    </view>

    <!-- ── 地图 + 画布 ──────────────────────────────────────────── -->
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
      <canvas
        type="2d"
        id="draw-canvas"
        class="canvas-layer"
        :style="toolMode !== 'none' ? 'pointer-events:auto;' : 'pointer-events:none;'"
        @touchstart="onDrawStart"
        @touchmove.stop="onDrawMove"
        @touchend="onDrawEnd"
      />
    </view>

    <!-- ── 工具栏 ───────────────────────────────────────────────── -->
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
      <text v-if="toolMode !== 'none'" class="tool-hint">绘制中・再次点击按钮退出</text>
    </view>

    <!-- ── 底部面板 ──────────────────────────────────────────────── -->
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

      <!-- 挑战任务折叠区 -->
      <view class="challenge-header" @click="showChallenges = !showChallenges">
        <view class="challenge-progress-bar">
          <view
            class="challenge-progress-fill"
            :style="{ width: (completedCount / challenges.length * 100) + '%' }"
          />
        </view>
        <text class="challenge-summary">
          🏆 挑战任务 {{ completedCount }}/{{ challenges.length }}
        </text>
        <text class="challenge-toggle">{{ showChallenges ? '▲' : '▼' }}</text>
      </view>

      <view v-if="showChallenges" class="challenge-list">
        <view
          v-for="c in challenges"
          :key="c.id"
          class="challenge-item"
          :class="{ done: c.completed }"
        >
          <text class="ci-icon">{{ c.icon }}</text>
          <view class="ci-info">
            <text class="ci-title">{{ c.title }}</text>
            <text class="ci-desc">{{ c.desc }}</text>
          </view>
          <text class="ci-check">{{ c.completed ? '✓' : '○' }}</text>
        </view>
      </view>

      <button class="leave-btn" @click="leaveTrip">结束旅途</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, getCurrentInstance, nextTick } from 'vue'
import { onLoad, onBackPress } from '@dcloudio/uni-app'
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

// 出发点（首次定位后记录，用于"移动距离"任务）
let startLat: number | null = null
let startLng: number | null = null

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

// ── 挑战任务系统 ─────────────────────────────────────────────────────────────
interface Challenge {
  id: string
  icon: string
  title: string
  desc: string
  completed: boolean
}

const challenges = ref<Challenge[]>([
  { id: 'first_stroke',    icon: '✏️', title: '旅途涂鸦',   desc: '在地图上画出第一笔',       completed: false },
  { id: 'three_strokes',   icon: '🎨', title: '涂鸦达人',   desc: '累计画出 3 笔涂鸦',        completed: false },
  { id: 'near_partner',    icon: '🤝', title: '搭子相聚',   desc: '与搭子距离缩短至 200 米内', completed: false },
  { id: 'traveled_200m',   icon: '🏃', title: '小小旅行家', desc: '从出发点移动超过 200 米',   completed: false },
  { id: 'partner_active',  icon: '👫', title: '同行时光',   desc: '与搭子保持实时联系',        completed: false },
])

const completedChallenge = ref<Challenge | null>(null)
const showChallenges = ref(false)
let popupDismissTimer: ReturnType<typeof setTimeout> | null = null
let partnerLocationCount = 0

const completedCount = computed(() => challenges.value.filter(c => c.completed).length)

function checkAndComplete(id: string) {
  const task = challenges.value.find(c => c.id === id)
  if (!task || task.completed) return
  task.completed = true
  triggerCompletion(task)
}

function triggerCompletion(task: Challenge) {
  // 如果当前有弹窗，先清掉计时器再覆盖
  if (popupDismissTimer) clearTimeout(popupDismissTimer)
  completedChallenge.value = task

  // 震动反馈（真机有效）
  try { uni.vibrateShort({ type: 'heavy' }) } catch (_) {}

  // 播放音效（需在 /static/sounds/complete.mp3 放置音频文件）
  try {
    const audio = uni.createInnerAudioContext()
    audio.src = '/static/sounds/complete.mp3'
    audio.play()
  } catch (_) {}

  // 3 秒后自动关闭弹窗
  popupDismissTimer = setTimeout(() => {
    completedChallenge.value = null
    popupDismissTimer = null
  }, 3000)
}

// ── 笔画数据 ─────────────────────────────────────────────────────────────────
interface Pt { latitude: number; longitude: number }
interface Stroke { id: string; points: Pt[] }

const myStrokes = ref<Stroke[]>([])
const partnerStrokes = ref<Stroke[]>([])
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

// ── Canvas ───────────────────────────────────────────────────────────────────
let ctx: any = null
let canvasW = 0
let canvasH = 0
let canvasLeft = 0
let canvasTop = 0
let currentPoints: Pt[] = []

watch(toolMode, (val) => {
  if (val === 'none') currentPoints = []
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
          redrawAllStrokes()
        })
        .exec()
    })
}

function redrawAllStrokes() {
  if (!ctx || !canvasW || !canvasH) return
  ctx.clearRect(0, 0, canvasW, canvasH)
  for (const s of myStrokes.value) drawPathOnCanvas(s.points, '#2196f3BB', 5)
  for (const s of partnerStrokes.value) drawPathOnCanvas(s.points, '#f44336BB', 5)
}

function drawPathOnCanvas(points: Pt[], color: string, width: number) {
  if (points.length < 2 || !ctx) return
  const first = ptToXY(points[0])
  if (!first) return
  ctx.beginPath()
  ctx.strokeStyle = color
  ctx.lineWidth = width
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.moveTo(first.x, first.y)
  for (let i = 1; i < points.length; i++) {
    const p = ptToXY(points[i])
    if (p) ctx.lineTo(p.x, p.y)
  }
  ctx.stroke()
}

// ── 地图视野 ─────────────────────────────────────────────────────────────────
let mapCtx: any = null
let region: { sw: Pt; ne: Pt } | null = null
let lastRegionKey = ''
let regionPollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  setMessageHandler(onWsMessage)
  updateMyLocation()
  locationTimer = setInterval(updateMyLocation, 3000)
  nextTick(() => initCanvas())
  setTimeout(() => {
    mapCtx = uni.createMapContext('trip-map', _inst)
    fetchRegion()
    regionPollTimer = setInterval(() => {
      mapCtx?.getRegion({
        success: (r: any) => {
          const key = `${r.northeast.latitude.toFixed(6)}|${r.northeast.longitude.toFixed(6)}`
          if (key === lastRegionKey) return
          lastRegionKey = key
          region = {
            sw: { latitude: r.southwest.latitude, longitude: r.southwest.longitude },
            ne: { latitude: r.northeast.latitude, longitude: r.northeast.longitude },
          }
          redrawAllStrokes()
        }
      })
    }, 100)
  }, 500)
})

onUnmounted(() => {
  if (locationTimer) clearInterval(locationTimer)
  if (regionPollTimer) clearInterval(regionPollTimer)
  if (popupDismissTimer) clearTimeout(popupDismissTimer)
  disconnectMatch()
})

onLoad((query) => {
  spotId.value = Number(query?.spotId ?? 0)
  spotName.value = decodeURIComponent(query?.spotName ?? '')
  partnerNickname.value = decodeURIComponent(query?.partnerNickname ?? '搭子')
})

function fetchRegion() {
  mapCtx?.getRegion({
    success: (r: any) => {
      lastRegionKey = `${r.northeast.latitude.toFixed(6)}|${r.northeast.longitude.toFixed(6)}`
      region = {
        sw: { latitude: r.southwest.latitude, longitude: r.southwest.longitude },
        ne: { latitude: r.northeast.latitude, longitude: r.northeast.longitude },
      }
      redrawAllStrokes()
    },
  })
}

function onRegionChange(e: any) {
  if (e.type === 'begin') {
    if (ctx) ctx.clearRect(0, 0, canvasW, canvasH)
  }
  if (e.type === 'end') {
    fetchRegion()
  }
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

function redrawCanvas() {
  if (!ctx || !canvasW || !canvasH || currentPoints.length < 2) return
  redrawAllStrokes()
  drawPathOnCanvas(currentPoints, '#2196f3BB', 5)
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
  if (toolMode.value !== 'pen' || currentPoints.length < 2) {
    currentPoints = []
    redrawAllStrokes()
    return
  }

  const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`
  const stroke: Stroke = { id, points: [...currentPoints] }
  currentPoints = []
  myStrokes.value = [...myStrokes.value, stroke]
  updatePolylines()
  redrawAllStrokes()
  sendMatch('drawStroke', { id: stroke.id, points: stroke.points })

  // 涂鸦相关任务检测
  const n = myStrokes.value.length
  if (n >= 1) checkAndComplete('first_stroke')
  if (n >= 3) checkAndComplete('three_strokes')
}

function eraseNear(pt: Pt) {
  const THRESHOLD = 0.0004
  for (const stroke of myStrokes.value) {
    for (const p of stroke.points) {
      if (Math.abs(p.latitude - pt.latitude) + Math.abs(p.longitude - pt.longitude) < THRESHOLD) {
        myStrokes.value = myStrokes.value.filter(s => s.id !== stroke.id)
        updatePolylines()
        redrawAllStrokes()
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
  redrawAllStrokes()
  ids.forEach(id => sendMatch('eraseStroke', { id }))
}

// ── WebSocket 消息 ────────────────────────────────────────────────────────────
function onWsMessage(msg: { type: string; payload: Record<string, any> }) {
  switch (msg.type) {
    case 'locationUpdate': {
      partnerLat.value = msg.payload.latitude
      partnerLng.value = msg.payload.longitude
      partnerLocationCount++

      // 同行任务：累计收到搭子位置 5 次
      if (partnerLocationCount >= 5) checkAndComplete('partner_active')

      // 搭子相聚任务：距离 < 200m
      if (partnerLat.value !== null && partnerLng.value !== null) {
        const d = haversine(myLat.value, myLng.value, partnerLat.value, partnerLng.value)
        if (d < 200) checkAndComplete('near_partner')
      }
      break
    }
    case 'partnerLeft':
      uni.showModal({ title: '搭子已离开', content: '搭子结束了旅途', showCancel: false })
      partnerLat.value = null
      partnerLng.value = null
      break
    case 'partnerDrawStroke': {
      const { id, points } = msg.payload
      partnerStrokes.value = [...partnerStrokes.value.filter(s => s.id !== id), { id, points }]
      updatePolylines()
      redrawAllStrokes()
      break
    }
    case 'partnerEraseStroke':
      partnerStrokes.value = partnerStrokes.value.filter(s => s.id !== msg.payload.id)
      updatePolylines()
      redrawAllStrokes()
      break
  }
}

// ── 位置更新 ─────────────────────────────────────────────────────────────────
function updateMyLocation() {
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      // 记录出发点（首次定位）
      if (startLat === null) {
        startLat = res.latitude
        startLng = res.longitude
      }

      myLat.value = res.latitude
      myLng.value = res.longitude
      sendMatch('location', { latitude: res.latitude, longitude: res.longitude })

      // 移动距离任务：距出发点 > 200m
      if (startLat !== null && startLng !== null) {
        const moved = haversine(startLat, startLng, res.latitude, res.longitude)
        if (moved > 200) checkAndComplete('traveled_200m')
      }
    },
  })
}

function leaveTrip() {
  uni.showModal({
    title: '结束旅途',
    content: '确定要结束本次旅途吗？',
    confirmText: '结束',
    confirmColor: '#e53935',
    success: (res) => {
      if (res.confirm) {
        sendMatch('leave')
        disconnectMatch()
        uni.redirectTo({ url: '/pages/index/index' })
      }
    },
  })
}

onBackPress(() => {
  leaveTrip()
  return true
})

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

/* ── 任务完成弹窗 ─────────────────────────────────────────────── */
.challenge-popup {
  position: fixed;
  top: 80rpx;
  left: 24rpx;
  right: 24rpx;
  z-index: 9999;
  animation: popSlideDown 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes popSlideDown {
  from { opacity: 0; transform: translateY(-60rpx) scale(0.9); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.popup-inner {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 28rpx;
  padding: 28rpx 32rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.35);
  border: 1rpx solid rgba(255, 107, 53, 0.3);
}

.popup-star {
  font-size: 48rpx;
  flex-shrink: 0;
}

.popup-body {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  flex: 1;
}

.popup-tag {
  font-size: 22rpx;
  color: #ff9d6b;
  letter-spacing: 1px;
}

.popup-icon {
  font-size: 40rpx;
}

.popup-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #fff;
}

.popup-desc {
  font-size: 22rpx;
  color: #aaa;
}

/* ── 地图 + 画布 ──────────────────────────────────────────────── */
.map-container { flex: 1; position: relative; }
.map-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.canvas-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: transparent; }

/* ── 工具栏 ───────────────────────────────────────────────────── */
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

/* ── 底部面板 ──────────────────────────────────────────────────── */
.panel { background: #fff; padding: 20rpx 32rpx; }
.panel-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.panel-label { font-size: 26rpx; color: #888; }
.panel-value { font-size: 28rpx; color: #222; font-weight: 500; }

/* ── 挑战任务区 ───────────────────────────────────────────────── */
.challenge-header {
  display: flex;
  align-items: center;
  padding: 16rpx 0 12rpx;
  border-bottom: 1rpx solid #f0f0f0;
  gap: 12rpx;
}

.challenge-progress-bar {
  flex: 1;
  height: 8rpx;
  background: #f0f0f0;
  border-radius: 4rpx;
  overflow: hidden;
}

.challenge-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff6b35, #f7931e);
  border-radius: 4rpx;
  transition: width 0.5s ease;
}

.challenge-summary {
  font-size: 24rpx;
  color: #555;
  flex-shrink: 0;
}

.challenge-toggle {
  font-size: 22rpx;
  color: #aaa;
  flex-shrink: 0;
}

.challenge-list {
  padding: 8rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.challenge-item {
  display: flex;
  align-items: center;
  padding: 16rpx 8rpx;
  gap: 16rpx;
  border-radius: 12rpx;
  transition: background 0.2s;
}

.challenge-item.done {
  background: #f9fff9;
}

.ci-icon { font-size: 36rpx; flex-shrink: 0; }

.ci-info { flex: 1; }
.ci-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #222;
  display: block;
}
.ci-desc {
  font-size: 22rpx;
  color: #888;
  display: block;
  margin-top: 4rpx;
}

.challenge-item.done .ci-title { color: #2e7d32; }

.ci-check {
  font-size: 32rpx;
  color: #ccc;
  flex-shrink: 0;
}
.challenge-item.done .ci-check { color: #4caf50; }

.leave-btn {
  margin-top: 20rpx; width: 100%; height: 80rpx; line-height: 80rpx;
  background: #1a1a2e; color: #fff; border-radius: 40rpx; font-size: 28rpx; border: none;
}
</style>
