<template>
  <view class="page">
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
	
	<!-- 地图类型切换按钮 -->
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

    <view class="panel">
      <view class="title">当前位置地图</view>

      <view class="row">纬度：{{ latitude }}</view>
      <view class="row">经度：{{ longitude }}</view>
	  <view class="row">地图模式：{{ mapType === 'normal' ? '普通地图' : '卫星地图' }}</view>
	  
	  
	  <view class="address">
		  <text>当前位置：</text>
		  <text>{{ currentAddress }}</text>
	  </view>
          
      <button class="btn" @click="locateCurrentPosition">
        定位到当前位置
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { reverseGeocoder } from '@/api/location'

// 默认位置：西南大学附近，防止定位失败时地图空白
const defaultLatitude = 29.8266
const defaultLongitude = 106.4220

const latitude = ref(defaultLatitude)
const longitude = ref(defaultLongitude)
const currentAddress = ref('正在获取地址')
const mapType = ref<'normal' | 'satellite'>('normal')

const isSatellite = computed(()=>{
	return mapType.value === 'satellite'
})

function switchMapType(type:'normal' | 'satellite'){
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
    success: (res) => {
      console.log('定位结果：', res.latitude, res.longitude)

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
	  
	  updateAddress(res.latitude, res.longitude)
	
      uni.showToast({
        title: '定位成功',
        icon: 'success'
      })
    },
    fail: (err) => {
      console.log('定位失败：', err)

      uni.showModal({
        title: '定位失败',
        content: '请检查是否允许小程序获取位置。当前显示默认测试位置。',
        showCancel: false
      })

      latitude.value = defaultLatitude
      longitude.value = defaultLongitude

      markers.value = [
        {
          id: 1,
          latitude: defaultLatitude,
          longitude: defaultLongitude,
          title: '默认位置',
          width: 36,
          height: 36
        }
      ]
    },
    complete: () => {
      uni.hideLoading()
    }
  })
}

async function updateAddress(latitudeValue: number, longitudeValue: number){
	try{
		currentAddress.value = '...正在解析地址'
		
		const result = await reverseGeocoder(latitudeValue, longitudeValue)
		
		currentAddress.value = result.recommendAddress || result.address
	
		console.log('当前地址',result)
	}catch(err){
		console.log('地址解析失败：',err)
		currentAddress.value = '地址解析失败'
	}
	
}
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
</style>