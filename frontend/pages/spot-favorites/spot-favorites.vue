<template>
  <view class="page">
    <view v-if="loading" class="status">
      加载中...
    </view>

    <view v-else-if="favoriteSpots.length === 0" class="empty">
      <text class="empty-icon">⭐</text>
      <text class="empty-title">还没有收藏景点</text>
      <text class="empty-desc">去景点详情页点击“收藏”后，会显示在这里</text>
    </view>

    <view v-else class="list">
      <view
        v-for="spot in favoriteSpots"
        :key="spot.id"
        class="spot-card"
        @tap="goSpotDetail(spot.id)"
      >
        <view class="spot-main">
          <view class="spot-title-row">
            <text class="spot-name">{{ spot.name }}</text>
            <text class="spot-category">{{ spot.category }}</text>
          </view>

          <text class="spot-address">
            {{ spot.address || '暂无地址' }}
          </text>

          <text class="spot-desc">
            {{ spot.description || '暂无简介' }}
          </text>
        </view>

        <view class="spot-actions">
          <text class="detail-btn" @tap.stop="goSpotDetail(spot.id)">
            详情
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getFavoriteSpots } from '@/api/favorite'
import type { ScenicSpot } from '@/api/spot'

const loading = ref(false)
const favoriteSpots = ref<ScenicSpot[]>([])

onShow(() => {
  loadFavorites()
})

async function loadFavorites() {
  loading.value = true

  try {
    favoriteSpots.value = await getFavoriteSpots()
    console.log('我的景点收藏：', favoriteSpots.value)
  } catch (error) {
    console.error('加载景点收藏失败：', error)

    uni.showToast({
      title: '加载收藏失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

function goSpotDetail(id: number) {
  uni.navigateTo({
    url: `/pages/spot-detail/spot-detail?id=${id}`
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f7;
  padding: 24rpx;
  box-sizing: border-box;
}

.status {
  padding-top: 120rpx;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}

.empty {
  padding-top: 180rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #999;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 24rpx;
}

.empty-title {
  font-size: 32rpx;
  color: #666;
  margin-bottom: 12rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: #aaa;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.spot-card {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  padding: 28rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.06);
}

.spot-main {
  flex: 1;
  min-width: 0;
}

.spot-title-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 12rpx;
}

.spot-name {
  font-size: 32rpx;
  font-weight: 700;
  color: #222;
}

.spot-category {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  background: #eaf2ff;
  color: #1677ff;
  font-size: 22rpx;
  flex-shrink: 0;
}

.spot-address {
  display: block;
  font-size: 26rpx;
  color: #777;
  margin-bottom: 10rpx;
}

.spot-desc {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  font-size: 25rpx;
  color: #999;
  line-height: 1.5;
}

.spot-actions {
  display: flex;
  align-items: center;
}

.detail-btn {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: #eef4ff;
  color: #1677ff;
  font-size: 24rpx;
  white-space: nowrap;
}
</style>