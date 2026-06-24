<template>
  <view class="page">

    <!-- 顶部问候 -->
    <view class="header">
      <text class="title">TripMate</text>
      <text class="subtitle">你的智能旅行助手</text>
    </view>

    <!-- 功能入口区 -->
    <view class="feature-grid">
      <view class="feature-item" @click="goTo('/pages/badges/badges')">
        <text class="feature-icon">🏅</text>
        <text class="feature-label">勋章馆</text>
      </view>
      <view class="feature-item" @click="goTo('/pages/guide/guide')">
        <text class="feature-icon">🗺️</text>
        <text class="feature-label">旅行攻略</text>
      </view>
      <view class="feature-item" @click="goTo('/pages/language/language')">
        <text class="feature-icon">🌐</text>
        <text class="feature-label">翻译</text>
      </view>
      <view class="feature-item" @click="goTo('/pages/map/map')">
        <text class="feature-icon">📍</text>
        <text class="feature-label">景区地图</text>
      </view>
	  view class="feature-item" @click="goTo('/pages/live2d/live2d')">
		<text class="feature-icon">👧</text>
		<text class="feature-label">数字人助手</text>
	  </view>
    </view>

    <!-- 天气卡片 -->
    <view class="section">
      <WeatherCard />
    </view>
	
	<!--地图入口-->
	<view class="section">
		<button class="map-btn" @click="goMap">景区地图</button>
	</view>

    <view class="tabbar-placeholder" />
    <TabBar active="home" />
  </view>
</template>

<script setup lang="ts">
import TabBar from '@/components/TabBar/TabBar.vue'
import WeatherCard from '@/components/WeatherCard/WeatherCard.vue'

function goTo(url: string) {
  uni.navigateTo({
    url
  })
}

function goMap() {
  uni.navigateTo({
    url: '/pages/map/map'
  })
}

function showComingSoon() {
  uni.showToast({
    title: '敬请期待',
    icon: 'none'
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background-color: #f7f8fa;
}

.header {
  padding: 60rpx 32rpx 24rpx;
  background-color: #ffffff;
}

.title {
  display: block;
  font-size: 48rpx;
  font-weight: 700;
  color: #1a1a1a;
}

.subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #999;
}

/* 功能入口区 */
.feature-grid {
  display: flex;
  flex-direction: row;
  justify-content: space-around;
  background: #ffffff;
  padding: 32rpx 16rpx 28rpx;
  margin-top: 2rpx;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.feature-icon {
  width: 96rpx;
  height: 96rpx;
  background: #f0f4ff;
  border-radius: 24rpx;
  font-size: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feature-label {
  font-size: 24rpx;
  color: #555;
}

.section {
  margin: 24rpx 24rpx 0;
}

.tabbar-placeholder {
  height: 140rpx;
}

.map-btn{
	margin-top: 20rpx;
	width: 90%;
	height:88rpx;
	line-height: 88rpx;
	border-radius: 20rpx;
	background:#1677ff;
	color:#ffffff;
	font-size:30rpx;
}
</style>
