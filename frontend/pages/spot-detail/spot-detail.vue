<template>
  <view class="page">
    <view v-if="loading" class="loading">
      加载中...
    </view>

    <view v-else-if="!spot" class="empty">
      未找到景点信息
    </view>

    <view v-else>
      <!-- 顶部图片区域 -->
      <view class="cover">
        <image
          v-if="spot.imageUrl && !coverError"
          class="cover-image"
          :src="spot.imageUrl"
          mode="aspectFill"
          @error="coverError = true"
        />

        <view v-else class="cover-placeholder">
          <text class="cover-icon">🏞️</text>
          <text class="cover-text">暂无景点图片</text>
        </view>
      </view>

      <!-- 景点基础信息 -->
      <view class="card">
        <view class="title-row">
          <text class="spot-name">{{ spot.name }}</text>
          <text class="category">{{ spot.category }}</text>
        </view>

        <view class="address-row">
          <text class="label">地址：</text>
          <text class="value">{{ spot.address || '暂无地址' }}</text>
        </view>

        <view class="address-row">
          <text class="label">地区：</text>
          <text class="value">{{ spot.region || '暂无地区' }}</text>
        </view>
      </view>

      <!-- 景点简介 -->
      <view class="card">
        <view class="section-title">景点简介</view>

        <text class="description">
          {{ spot.description || '暂无景点简介' }}
        </text>
      </view>

      <!-- 坐标信息，开发阶段可以保留，后面正式版可隐藏 -->
      <view class="card">
        <view class="section-title">位置信息</view>

        <view class="address-row">
          <text class="label">纬度：</text>
          <text class="value">{{ spot.latitude }}</text>
        </view>

        <view class="address-row">
          <text class="label">经度：</text>
          <text class="value">{{ spot.longitude }}</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-bar">
        <button class="action-btn secondary" @tap="backToMap">
          在地图中查看
        </button>

        <button class="action-btn" @tap="playGuide">
          播放导览
        </button>

        <button class="action-btn favorite" @tap="favoriteSpot">
          {{ isFavorited ? '已收藏':'收藏' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getScenicSpotById,
  type ScenicSpot
} from '@/api/spot'

import {
  addFavorite,
  removeFavorite,
  checkFavorite
} from '@/api/favorite'

import { addHistory } from '@/api/history'
import { useAuth } from '@/composables/useAuth'


const loading = ref(false)
const spot = ref<ScenicSpot | null>(null)
const coverError = ref(false)
const isFavorited = ref(false)
const { authState, login } = useAuth()

onLoad((options) => {
  const id = Number(options?.id)

  if (!id) {
    uni.showToast({
      title: '景点ID无效',
      icon: 'none'
    })
    return
  }

  loadSpotDetail(id)
})

async function loadSpotDetail(id: number) {
  loading.value = true

  try {
    spot.value = await getScenicSpotById(id)

    if (authState.isLoggedIn) {
      try {
        isFavorited.value = await checkFavorite(id)

        await addHistory(
          'VIEW_SPOT',
          id,
          `查看了景点：${spot.value.name}`
        )
      } catch (authError) {
        console.error('收藏状态或历史记录处理失败：', authError)
      }
    }
  } catch (error) {
    console.error('加载景点详情失败：', error)

    uni.showToast({
      title: '加载失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

function backToMap() {
  uni.navigateBack()
}

function playGuide() {
  uni.showToast({
    title: '语音导览后续接入',
    icon: 'none'
  })
}

async function favoriteSpot() {
  if (!spot.value) {
    return
  }

  if (!authState.isLoggedIn) {
    try {
      await login()
    } catch {
      uni.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }
  }

  try {
    if (isFavorited.value) {
      await removeFavorite(spot.value.id)
      isFavorited.value = false

      uni.showToast({
        title: '已取消收藏',
        icon: 'none'
      })
    } else {
      await addFavorite(spot.value.id)
      isFavorited.value = true

      await addHistory(
        'FAVORITE_SPOT',
        spot.value.id,
        `收藏了景点：${spot.value.name}`
      )

      uni.showToast({
        title: '收藏成功',
        icon: 'success'
      })
    }
  } catch (error) {
    console.error('收藏操作失败：', error)

    uni.showToast({
      title: '操作失败',
      icon: 'none'
    })
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f7;
  padding-bottom: 140rpx;
}

.loading,
.empty {
  padding: 80rpx 0;
  text-align: center;
  color: #888;
  font-size: 28rpx;
}

.cover {
  width: 100%;
  height: 360rpx;
  background: #e9eef5;
}

.cover-image {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.cover-icon {
  font-size: 80rpx;
}

.cover-text {
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #888;
}

.card {
  margin: 24rpx;
  padding: 28rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.06);
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.spot-name {
  flex: 1;
  font-size: 40rpx;
  font-weight: 700;
  color: #222;
}

.category {
  margin-left: 20rpx;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: #eaf2ff;
  color: #1677ff;
  font-size: 24rpx;
}

.address-row {
  display: flex;
  margin-top: 20rpx;
  font-size: 28rpx;
  line-height: 1.5;
}

.label {
  color: #666;
  flex-shrink: 0;
}

.value {
  color: #333;
}

.section-title {
  margin-bottom: 18rpx;
  font-size: 32rpx;
  font-weight: 700;
  color: #222;
}

.description {
  font-size: 28rpx;
  line-height: 1.8;
  color: #444;
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 16rpx;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  box-shadow: 0 -6rpx 20rpx rgba(0, 0, 0, 0.08);
}

.action-btn {
  flex: 1;
  height: 82rpx;
  line-height: 82rpx;
  margin: 0;
  padding: 0;
  border-radius: 18rpx;
  background: #1677ff;
  color: #ffffff;
  font-size: 28rpx;
}

.secondary {
  background: #eef4ff;
  color: #1677ff;
}

.favorite {
  background: #ff7a1a;
}
</style>