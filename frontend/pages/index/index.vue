<template>
  <view class="page">

    <!-- 顶部问候 -->
    <view class="header">
      <text class="title">TripMate</text>
      <text class="subtitle">你的智能旅行助手</text>
    </view>

    <!-- 天气卡片 -->
    <view class="section">
      <WeatherCard />
    </view>
	
	<!--地图测试入口-->
	<view class="section">
		<button class="map-btn" @click="goMapTest">地图测试</button>
	</view>

    <!-- 底部 TabBar 占位，防止内容被遮挡 -->
    <view class="tabbar-placeholder" />

    <TabBar active="home" />
  </view>
</template>

<script setup lang="ts">
import TabBar from '@/components/TabBar/TabBar.vue'
import WeatherCard from '@/components/WeatherCard/WeatherCard.vue'
function goMapTest() {
	uni.navigateTo({
		url:'/pages/map-test/map-test'
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

.section {
  margin: 24rpx 24rpx 0;
}

/* 与 TabBar 高度（120rpx）+ safe area 保持一致，防止内容被遮挡 */
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
