<template>
  <view class="page">

    <!-- 阶段 1：选景点 -->
    <view v-if="step === 'select'" class="step-select">
      <view class="search-bar">
        <input
          class="search-input"
          placeholder="搜索景点…"
          v-model="keyword"
          @input="onSearch"
        />
      </view>

      <scroll-view class="spot-list" scroll-y>
        <view v-if="loading" class="hint">加载中…</view>
        <view v-else-if="spots.length === 0" class="hint">暂无景点数据</view>
        <view
          v-for="spot in spots"
          :key="spot.id"
          class="spot-item"
          :class="{ selected: selectedSpot?.id === spot.id }"
          @click="selectedSpot = spot"
        >
          <text class="spot-name">{{ spot.name }}</text>
          <text class="spot-region">{{ spot.region || spot.address }}</text>
        </view>
      </scroll-view>

      <view class="bottom-bar">
        <button
          class="start-btn"
          :disabled="!selectedSpot"
          @click="startMatch"
        >
          {{ selectedSpot ? `前往 ${selectedSpot.name}，开始匹配` : '请先选择景点' }}
        </button>
      </view>
    </view>

    <!-- 阶段 2：等待中 -->
    <view v-else-if="step === 'waiting'" class="step-waiting">
      <view class="spin-wrap">
        <view class="spin-ring" />
        <text class="spin-icon">🧳</text>
      </view>
      <text class="waiting-spot">{{ selectedSpot?.name }}</text>
      <text class="waiting-tip">正在寻找搭子…</text>
      <button class="cancel-btn" @click="cancelMatch">取消匹配</button>
    </view>

    <!-- 阶段 3：匹配成功确认 -->
    <view v-else-if="step === 'matched'" class="step-matched">
      <view class="matched-card">
        <text class="matched-title">🎉 发现搭子！</text>

        <!-- 双人头像昵称 -->
        <view class="duo-row">
          <view class="user-col">
            <image v-if="myAvatarUrl" class="avatar" :src="myAvatarUrl" mode="aspectFill" />
            <view v-else class="avatar avatar-placeholder">
              <text class="avatar-letter">{{ myNickname?.[0] ?? '我' }}</text>
            </view>
            <text class="user-name">{{ myNickname }}</text>
            <view class="ready-tag" :class="{ ready: myReady }">
              {{ myReady ? '✓ 已准备' : '待确认' }}
            </view>
          </view>

          <text class="vs-text">VS</text>

          <view class="user-col">
            <image v-if="partnerAvatarUrl" class="avatar" :src="partnerAvatarUrl" mode="aspectFill" />
            <view v-else class="avatar avatar-placeholder partner">
              <text class="avatar-letter">{{ partnerNickname?.[0] ?? '他' }}</text>
            </view>
            <text class="user-name">{{ partnerNickname }}</text>
            <view class="ready-tag" :class="{ ready: partnerReady }">
              {{ partnerReady ? '✓ 已准备' : '待确认' }}
            </view>
          </view>
        </view>

        <text class="matched-spot">目的地：{{ selectedSpot?.name }}</text>
        <text class="countdown-tip">{{ countdown }} 秒后自动取消</text>

        <view class="matched-actions">
          <button class="confirm-btn" :disabled="myReady" @click="confirmMatch">
            {{ myReady ? '已确认出发' : '确认出发' }}
          </button>
          <button class="cancel-btn-sm" @click="cancelMatch">取消</button>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useSpotApi, type Spot } from '@/api/spot'
import { connectMatch, sendMatch, disconnectMatch } from '@/api/match'

const { listSpots, searchSpots } = useSpotApi()

type Step = 'select' | 'waiting' | 'matched'
const step = ref<Step>('select')
let transitioning = false

const spots = ref<Spot[]>([])
const loading = ref(true)
const keyword = ref('')
const selectedSpot = ref<Spot | null>(null)
const myNickname = ref('')
const myAvatarUrl = ref('')
const myReady = ref(false)
const partnerNickname = ref('')
const partnerAvatarUrl = ref('')
const partnerReady = ref(false)
const countdown = ref(15)

let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  try {
    spots.value = await listSpots()
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  if (!transitioning) disconnectMatch()
  clearCountdown()
})

async function onSearch() {
  loading.value = true
  try {
    spots.value = await searchSpots(keyword.value)
  } finally {
    loading.value = false
  }
}

function startMatch() {
  if (!selectedSpot.value) return
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  step.value = 'waiting'

  connectMatch(token, onWsMessage, () => {
    // onOpen: 连接建立后再发 join，避免 readyState not OPEN 错误
    uni.getLocation({
      type: 'gcj02',
      success: (loc) => {
        sendMatch('join', {
          spotId: selectedSpot.value!.id,
          spotName: selectedSpot.value!.name,
          latitude: loc.latitude,
          longitude: loc.longitude,
        })
      },
      fail: () => {
        sendMatch('join', {
          spotId: selectedSpot.value!.id,
          spotName: selectedSpot.value!.name,
          latitude: 0,
          longitude: 0,
        })
      },
    })
  })
}

function onWsMessage(msg: { type: string; payload: Record<string, any> }) {
  if (msg.type === 'waiting') {
    step.value = 'waiting'
  } else if (msg.type === 'matched') {
    myNickname.value = msg.payload.myNickname ?? '我'
    myAvatarUrl.value = msg.payload.myAvatarUrl ?? ''
    myReady.value = false
    partnerNickname.value = msg.payload.partnerNickname ?? '旅行者'
    partnerAvatarUrl.value = msg.payload.partnerAvatarUrl ?? ''
    partnerReady.value = false
    step.value = 'matched'
    startCountdown()
  } else if (msg.type === 'partnerConfirmed') {
    partnerReady.value = true
  } else if (msg.type === 'confirmed') {
    clearCountdown()
    transitioning = true
    uni.redirectTo({
      url: `/pages/trip/trip?spotId=${selectedSpot.value!.id}&spotName=${encodeURIComponent(selectedSpot.value!.name)}&partnerNickname=${encodeURIComponent(partnerNickname.value)}`
    })
  } else if (msg.type === 'partnerCancelled') {
    clearCountdown()
    step.value = 'select'
    uni.showToast({ title: '搭子取消了，重新匹配', icon: 'none' })
  }
}

function confirmMatch() {
  if (myReady.value) return
  myReady.value = true
  sendMatch('confirm')
}

function cancelMatch() {
  clearCountdown()
  sendMatch('cancel')
  disconnectMatch()
  step.value = 'select'
}

function startCountdown() {
  countdown.value = 15
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearCountdown()
      cancelMatch()
      uni.showToast({ title: '确认超时，已自动取消', icon: 'none' })
    }
  }, 1000)
}

function clearCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}
</script>

<style scoped>
.page { height: 100vh; background: #f5f6f7; display: flex; flex-direction: column; overflow: hidden; }

.step-select { flex: 1; display: flex; flex-direction: column; }
.search-bar { padding: 20rpx 24rpx; background: #fff; border-bottom: 1rpx solid #eee; }
.search-input {
  width: 100%; height: 72rpx; background: #f0f2f5;
  border-radius: 36rpx; padding: 0 28rpx;
  font-size: 28rpx; box-sizing: border-box;
}
.spot-list { flex: 1; overflow: hidden; }
.hint { text-align: center; color: #aaa; font-size: 28rpx; padding: 60rpx 0; }
.spot-item {
  background: #fff; margin: 16rpx 24rpx; padding: 28rpx 32rpx;
  border-radius: 16rpx; border: 2rpx solid transparent;
}
.spot-item.selected { border-color: #ff6b35; background: #fff8f5; }
.spot-name { font-size: 30rpx; font-weight: 600; color: #222; display: block; }
.spot-region { font-size: 24rpx; color: #888; margin-top: 6rpx; display: block; }
.bottom-bar {
  padding: 24rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
  flex-shrink: 0;
}
.start-btn {
  width: 100%; height: 88rpx; line-height: 88rpx;
  background: linear-gradient(135deg, #ff6b35, #f7931e);
  color: #fff; border-radius: 44rpx; font-size: 32rpx; font-weight: 600; border: none;
}
.start-btn[disabled] { background: #ccc; }

.step-waiting {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 32rpx;
}
.spin-wrap {
  width: 200rpx; height: 200rpx; position: relative;
  display: flex; align-items: center; justify-content: center;
}
.spin-ring {
  position: absolute; inset: 0;
  border: 8rpx solid #ff6b35; border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
.spin-icon { font-size: 80rpx; }
@keyframes spin { to { transform: rotate(360deg); } }
.waiting-spot { font-size: 36rpx; font-weight: 700; color: #222; }
.waiting-tip { font-size: 28rpx; color: #888; }
.cancel-btn {
  margin-top: 40rpx; padding: 20rpx 80rpx;
  background: #f5f5f5; color: #666; border-radius: 44rpx;
  font-size: 28rpx; border: none;
}

.step-matched {
  flex: 1; display: flex; align-items: center; justify-content: center; padding: 40rpx;
}
.matched-card {
  background: #fff; border-radius: 32rpx; padding: 60rpx 48rpx;
  width: 100%; display: flex; flex-direction: column; align-items: center;
  gap: 24rpx; box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.1);
}
.matched-title { font-size: 40rpx; font-weight: 700; }

.duo-row {
  display: flex; align-items: center; justify-content: space-between;
  width: 100%; gap: 16rpx; margin: 8rpx 0;
}
.user-col {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 12rpx;
}
.avatar {
  width: 120rpx; height: 120rpx; border-radius: 60rpx;
  background: #f0f2f5; border: 4rpx solid #eee;
}
.avatar-placeholder {
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #ff6b35, #f7931e);
}
.avatar-placeholder.partner {
  background: linear-gradient(135deg, #2196f3, #00bcd4);
}
.avatar-letter { font-size: 48rpx; color: #fff; font-weight: 700; }
.user-name { font-size: 28rpx; font-weight: 600; color: #222; text-align: center; }
.ready-tag {
  font-size: 22rpx; padding: 6rpx 18rpx; border-radius: 20rpx;
  background: #f0f2f5; color: #aaa;
}
.ready-tag.ready { background: #e8f5e9; color: #2e7d32; }
.vs-text { font-size: 36rpx; font-weight: 700; color: #ccc; flex-shrink: 0; }

.matched-spot { font-size: 28rpx; color: #666; }
.countdown-tip { font-size: 24rpx; color: #aaa; }
.matched-actions { display: flex; gap: 24rpx; margin-top: 8rpx; width: 100%; }
.confirm-btn {
  flex: 2; height: 88rpx; line-height: 88rpx;
  background: linear-gradient(135deg, #ff6b35, #f7931e);
  color: #fff; border-radius: 44rpx; font-size: 30rpx; font-weight: 600; border: none;
}
.cancel-btn-sm {
  flex: 1; height: 88rpx; line-height: 88rpx;
  background: #f5f5f5; color: #666; border-radius: 44rpx; font-size: 28rpx; border: none;
}
</style>
