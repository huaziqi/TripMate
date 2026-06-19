<template>
  <view class="page">
    <map
      class="map"
      :latitude="myLat"
      :longitude="myLng"
      :scale="14"
      :markers="markers"
      :show-location="false"
    />

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

const spotName = ref('')
const partnerNickname = ref('')
const spotId = ref(0)

const myLat = ref(29.8266)
const myLng = ref(106.422)
const partnerLat = ref<number | null>(null)
const partnerLng = ref<number | null>(null)

let locationTimer: ReturnType<typeof setInterval> | null = null

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

onLoad((query) => {
  spotId.value = Number(query?.spotId ?? 0)
  spotName.value = decodeURIComponent(query?.spotName ?? '')
  partnerNickname.value = decodeURIComponent(query?.partnerNickname ?? '搭子')
})

onMounted(() => {
  setMessageHandler(onWsMessage)
  updateMyLocation()
  locationTimer = setInterval(updateMyLocation, 3000)
})

onUnmounted(() => {
  if (locationTimer) clearInterval(locationTimer)
  disconnectMatch()
})

function onWsMessage(msg: { type: string; payload: Record<string, any> }) {
  if (msg.type === 'locationUpdate') {
    partnerLat.value = msg.payload.latitude
    partnerLng.value = msg.payload.longitude
  } else if (msg.type === 'partnerLeft') {
    uni.showModal({
      title: '搭子已离开',
      content: '搭子结束了旅途',
      showCancel: false,
    })
    partnerLat.value = null
    partnerLng.value = null
  }
}

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

.panel {
  background: #fff;
  padding: 28rpx 32rpx;
  border-top: 1rpx solid #eee;
}
.panel-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.panel-label { font-size: 26rpx; color: #888; }
.panel-value { font-size: 28rpx; color: #222; font-weight: 500; }

.leave-btn {
  margin-top: 24rpx; width: 100%; height: 88rpx; line-height: 88rpx;
  background: #1a1a2e; color: #fff;
  border-radius: 44rpx; font-size: 30rpx; border: none;
}
</style>
