<template>
  <view class="page">
    <!-- 景点搜索栏 -->
    <view class="search-section">
      <view class="search-bar">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="请输入景点名称"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <button
          class="search-btn"
          :disabled="searching"
          @tap="handleSearch"
        >
          {{ searching ? '搜索中' : '搜索' }}
        </button>
      </view>

      <!-- 搜索结果 -->
      <view v-if="searchResults.length > 0" class="search-results">
        <view
          v-for="spot in searchResults"
          :key="spot.id"
          class="result-item"
          @tap="selectSpot(spot)"
        >
          <view class="result-content">
            <view class="result-main">
              <text class="result-name">{{ spot.name }}</text>
              <text class="result-category">{{ spot.category }}</text>
            </view>
            <text class="result-address">{{ spot.address }}</text>
          </view>
          <view class="result-right">
            <text
              v-if="spot.distance !== undefined"
              class="result-distance"
            >
              {{ formatDistance(spot.distance) }}
            </text>
            <text
              class="detail-link"
              @tap.stop="goSpotDetail(spot.id)"
            >
              详情
            </text>
          </view>
        </view>
      </view>
    </view>

    <view class="map-wrapper">
      <map
        id="myMap"
        class="map"
        :latitude="latitude"
        :longitude="longitude"
        :scale="16"
        :markers="markers"
        :enable-satellite="isSatellite"
        :show-location="false"
      />
      <cover-view class="map-type-switch">
        <cover-view
          class="map-type-item"
          :class="{ active: mapType === 'normal' }"
          @click="switchMapType('normal')"
        >
          普通
        </cover-view>
        <cover-view
          class="map-type-item"
          :class="{ active: mapType === 'satellite' }"
          @click="switchMapType('satellite')"
        >
          卫星
        </cover-view>
      </cover-view>
    </view>

    <!-- 附近景点推荐 -->
    <view class="nearby-section">
      <view class="nearby-header">
        <text class="nearby-title">附近景点推荐</text>
        <text v-if="loadingNearby" class="nearby-loading">加载中...</text>
      </view>

      <view v-if="!loadingNearby && nearbySpots.length === 0" class="empty-text">
        暂无附近景点
      </view>

      <scroll-view v-else scroll-y class="nearby-scroll">
        <view
          v-for="(spot, index) in nearbySpots"
          :key="spot.id"
          class="nearby-item"
          @tap="selectSpot(spot)"
        >
          <view class="nearby-main">
            <view class="nearby-name-line">
              <text class="nearby-name">{{ spot.name }}</text>
              <text v-if="index === 0" class="nearest-tag">最近</text>
            </view>
            <text class="nearby-address">{{ spot.address }}</text>
          </view>
          <view class="nearby-right">
            <text class="nearby-distance">{{ formatDistance(spot.distance) }}</text>
            <text class="detail-link" @tap.stop="goSpotDetail(spot.id)">详情</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="panel">
      <view class="address">
        <text>当前位置：</text>
        <text>{{ currentAddress }}</text>
      </view>
      <button class="btn" @click="locateCurrentPosition">定位到当前位置</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { reverseGeocoder } from '@/api/location'
import {
  searchScenicSpots,
  getNearbySpots,
  type ScenicSpot,
  type NearbySpot
} from '@/api/spot'

// 默认位置：西南大学附近，防止定位失败时地图空白
const defaultLatitude = 29.8266
const defaultLongitude = 106.4220

const latitude = ref(defaultLatitude)
const longitude = ref(defaultLongitude)
const currentAddress = ref('正在获取地址')
const mapType = ref<'normal' | 'satellite'>('normal')

const isSatellite = computed(() => {
  return mapType.value === 'satellite'
})

const keyword = ref('')
const searching = ref(false)
const searchResults = ref<ScenicSpot[]>([])
const selectedSpot = ref<ScenicSpot | null>(null)

const nearbySpots = ref<NearbySpot[]>([])
const loadingNearby = ref(false)
const userLatitude = ref<number | null>(null)
const userLongitude = ref<number | null>(null)

const USER_ID = 1

function switchMapType(type: 'normal' | 'satellite') {
  mapType.value = type
}

const markers = ref([
  {
    id: 1,
    latitude: defaultLatitude,
    longitude: defaultLongitude,
    title: '默认位置',
    width: 36,
    height: 36
  }
])

onLoad(() => {
  locateCurrentPosition()
})

// 每次进入页面都重新定位一次
onShow(() => {
  locateCurrentPosition()
})

function locateCurrentPosition() {
  uni.showLoading({
    title: '定位中...'
  })

  uni.getLocation({
    type: 'gcj02',
    isHighAccuracy: true,
    success: async (res) => {
      console.log('定位结果：', res.latitude, res.longitude)

      userLatitude.value = res.latitude
      userLongitude.value = res.longitude

      latitude.value = res.latitude
      longitude.value = res.longitude

      markers.value = [
        {
          id: 1,
          latitude: res.latitude,
          longitude: res.longitude,
          title: '当前位置',
          width: 36,
          height: 36,
        }
      ]

      // 原有的逆地址解析
      await updateAddress(res.latitude, res.longitude)

      await loadNearbySpots(res.latitude, res.longitude)

      uni.showToast({
        title: '定位成功',
        icon: 'success'
      })
    },
    fail: (err) => {
      console.log('定位失败：', err)
    },
    complete: () => {
      uni.hideLoading()  // 确保定位 loading 关闭
    }
  })
}

type SelectableSpot = {
  id: number
  name: string
  address: string
  category: string
  latitude: number
  longitude: number
}

function selectSpot(spot: SelectableSpot) {
  latitude.value = Number(spot.latitude)
  longitude.value = Number(spot.longitude)

  markers.value = [
    {
      id: spot.id,
      latitude: Number(spot.latitude),
      longitude: Number(spot.longitude),
      title: spot.name,
      width: 36,
      height: 36,
    }
  ]
}

function formatDistance(distance: number) {
  if (distance < 1000) {
    return `${Math.round(distance)} 米`
  }

  return `${(distance / 1000).toFixed(1)} 公里`
}

function calculateDistance(
  latitude1: number,
  longitude1: number,
  latitude2: number,
  longitude2: number
): number {
  const earthRadius = 6371000

  const lat1 = latitude1 * Math.PI / 180
  const lat2 = latitude2 * Math.PI / 180
  const latDifference = (latitude2 - latitude1) * Math.PI / 180
  const lngDifference = (longitude2 - longitude1) * Math.PI / 180

  const a =
    Math.sin(latDifference / 2) ** 2 +
    Math.cos(lat1) *
    Math.cos(lat2) *
    Math.sin(lngDifference / 2) ** 2

  const c = 2 * Math.atan2(
    Math.sqrt(a),
    Math.sqrt(1 - a)
  )

  return earthRadius * c
}

function goSpotDetail(id:number){
	uni.navigateTo({
		url:`/pages/spot-detail/spot-detail?id=${id}`
	})
}

async function updateAddress(latitudeValue: number, longitudeValue: number) {
  try {
    currentAddress.value = '...正在解析地址'

    const result = await reverseGeocoder(latitudeValue, longitudeValue)

    currentAddress.value = result.recommendAddress || result.address

    console.log('当前地址', result)
  } catch (err) {
    console.log('地址解析失败：', err)
    currentAddress.value = '地址解析失败'
  }
}

async function handleSearch() {
  const value = keyword.value.trim()

  if (!value) {
    uni.showToast({
      title: '请输入景点名称',
      icon: 'none'
    })
    return
  }

  searching.value = true

  try {
    const spots = await searchScenicSpots(value)

    searchResults.value = spots.map((spot) => {
      let distance: number | undefined

      if (
        userLatitude.value !== null &&
        userLongitude.value !== null
      ) {
        distance = calculateDistance(
          userLatitude.value,
          userLongitude.value,
          Number(spot.latitude),
          Number(spot.longitude)
        )
      }

      return {
        ...spot,
        distance
      }
    })

    searchResults.value.sort((a, b) => {
      return (a.distance ?? Infinity) - (b.distance ?? Infinity)
    })

    if (searchResults.value.length === 0) {
      uni.showToast({
        title: '未找到相关景点',
        icon: 'none'
      })
    }
  } catch (error) {
    console.error('搜索景点失败：', error)

    uni.showToast({
      title: '搜索失败',
      icon: 'none'
    })
  } finally {
    searching.value = false
  }
}

async function loadNearbySpots(
  currentLatitude: number,
  currentLongitude: number
) {
  loadingNearby.value = true

  try {
    nearbySpots.value = await getNearbySpots(
      currentLatitude,
      currentLongitude,
      10
    )

    console.log('最近景点：', nearbySpots.value)
  } catch (error) {
    console.error('加载附近景点失败：', error)

    uni.showToast({
      title: '附近景点加载失败',
      icon: 'none'
    })
  } finally {
    loadingNearby.value = false
  }
}

defineExpose({
  keyword,
  searching,
  searchResults,
  latitude,
  longitude,
  markers,
  isSatellite,
  mapType,
  nearbySpots,
  loadingNearby,
  currentAddress,
  handleSearch,
  selectSpot,
  switchMapType,
  formatDistance,
  locateCurrentPosition
})
</script>

<style scoped>
.page {
  width: 100%;
  min-height: 100vh;
  background: #f5f6f7;
}

.map-wrapper {
  position: relative;
  width: 100%;
  height: 75vh;
}

.map {
  width: 100%;
  height: 75vh;
}

.map-type-switch {
  position: absolute;
  top: 24rpx;
  right: 24rpx;
  display: flex;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 999rpx;
  padding: 6rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.18);
  z-index: 99;
}

.map-type-item {
  padding: 12rpx 22rpx;
  font-size: 24rpx;
  color: #333;
  border-radius: 999rpx;
}

.map-type-item.active {
  background: #1677ff;
  color: #ffffff;
}

.panel {
  padding: 24rpx;
  background: #ffffff;
}

.title {
  font-size: 34rpx;
  font-weight: bold;
  margin-bottom: 16rpx;
}

.row {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 10rpx;
}

.btn {
  margin-top: 20rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: #1677ff;
  color: #ffffff;
  border-radius: 16rpx;
  font-size: 30rpx;
}
.address{
	margin-top: 16rpx;
	padding:20rpx;
	border-radius: 16rpx;
	background: #f1f5f9;
	color: #333;
	font-size: 28rpx;
	line-height: 1.5;
}

.search-section {
  position: relative;
  padding: 20rpx 24rpx;
  background: #ffffff;
  z-index: 20;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.search-input {
  flex: 1;
  height: 76rpx;
  padding: 0 24rpx;
  border-radius: 16rpx;
  background: #f2f4f7;
  font-size: 28rpx;
  box-sizing: border-box;
}

.search-btn {
  width: 140rpx;
  height: 76rpx;
  line-height: 76rpx;
  margin: 0;
  padding: 0;
  border-radius: 16rpx;
  background: #1677ff;
  color: #ffffff;
  font-size: 28rpx;
}

.search-results {
  margin-top: 12rpx;
  overflow: hidden;
  border-radius: 16rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.12);
}

.result-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 24rpx;
  border-bottom: 1rpx solid #eeeeee;
}

.result-content {
  flex: 1;
  min-width: 0;
}

.result-distance {
  margin-left: 20rpx;
  flex-shrink: 0;
  font-size: 24rpx;
  color: #1677ff;
}

.result-item:last-child {
  border-bottom: none;
}

.result-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #222222;
}

.result-category {
  padding: 4rpx 12rpx;
  border-radius: 10rpx;
  background: #eaf2ff;
  color: #1677ff;
  font-size: 22rpx;
}

.result-address {
  display: block;
  margin-top: 8rpx;
  color: #888888;
  font-size: 24rpx;
}

.nearby-section {
  margin: 20rpx 24rpx;
  overflow: hidden;
  border-radius: 20rpx;
  background: #ffffff;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.08);
}

.nearby-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-bottom: 1rpx solid #eeeeee;
}

.nearby-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #222222;
}

.nearby-loading {
  font-size: 24rpx;
  color: #999999;
}

.nearby-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 24rpx;
  border-bottom: 1rpx solid #eeeeee;
}

.nearby-item:last-child {
  border-bottom: none;
}

.nearby-main {
  flex: 1;
  min-width: 0;
}

.nearby-name-line {
  display: flex;
  align-items: center;
}

.nearby-name {
  font-size: 29rpx;
  font-weight: 600;
  color: #222222;
}

.nearest-tag {
  margin-left: 12rpx;
  padding: 3rpx 10rpx;
  border-radius: 10rpx;
  background: #fff1e8;
  color: #ff6b00;
  font-size: 21rpx;
}

.nearby-address {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #888888;
}

.nearby-distance {
  margin-left: 20rpx;
  font-size: 25rpx;
  color: #1677ff;
}

.empty-text {
  padding: 30rpx;
  text-align: center;
  color: #999999;
  font-size: 26rpx;
}

.nearby-scroll{
	max-height: 420rpx;
}

.result-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10rpx;
  margin-left: 20rpx;
}

.detail-link {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: #eef4ff;
  color: #1677ff;
  font-size: 24rpx;
}

.nearby-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10rpx;
  margin-left: 20rpx;
  flex-shrink: 0;
}
</style>