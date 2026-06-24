<template>
  <view class="page">
    <view class="header">
      <text class="title">路线推荐</text>
      <text class="subtitle">根据西南大学校内景点，为你推荐适合步行游览的路线</text>
    </view>
	
	<view class="tip-box">
	  <text class="tip-text">以下为西南大学校园路线推荐</text>
	  <text v-if="currentSpotName" class="tip-sub">
	    当前关联景点：{{ currentSpotName }}
	  </text>
	</view>

    <view v-if="loading" class="status">
      加载中...
    </view>

    <view v-else-if="routes.length === 0" class="status">
      暂无推荐路线
    </view>

    <view v-else class="route-list">
      <view
        v-for="route in filteredRoutes"
        :key="route.id"
        class="route-card"
      >
        <view class="route-top">
          <view>
            <text class="route-name">{{ route.name }}</text>
            <text class="route-desc">{{ route.description }}</text>
          </view>

          <view class="tag">
            {{ route.theme }}
          </view>
        </view>

        <view class="meta-row">
          <text class="meta">预计时间：{{ route.estimatedTime }}</text>
        </view>

        <view class="spot-chain">
          <template
            v-for="(spot, index) in route.spots"
            :key="index"
          >
            <view
              class="spot-pill"
              :class="{ disabled: !spot.matched }"
              @tap="goSpotDetail(spot)"
            >
              {{ spot.displayName }}
            </view>

            <text
              v-if="index < route.spots.length - 1"
              class="arrow"
            >
              →
            </text>
          </template>
        </view>

        <view class="guide-box">
          <text class="guide-title">路线讲解</text>
          <text class="guide-text">{{ route.guideText }}</text>
        </view>

        <view
          v-if="hasMissingSpot(route)"
          class="warning"
        >
          有景点未在数据库中匹配到，可先检查 scenic_spot 表中的名称
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getRecommendRoutes } from '@/api/route'
import type { RecommendRoute, RouteSpot } from '@/api/route'

const loading = ref(false)
const routes = ref<RecommendRoute[]>([])

const currentSpotName = ref('')
const filteredRoutes = computed(() => {
  if (!currentSpotName.value.trim()) {
    return routes.value
  }

  const keyword = currentSpotName.value.trim()

  const matched = routes.value.filter(route =>
    route.spots.some(spot =>
      spot.displayName.includes(keyword) ||
      spot.name.includes(keyword)
    )
  )

  return matched.length > 0 ? matched : routes.value
})

onLoad((options) => {
  if (options?.spotName) {
    currentSpotName.value = decodeURIComponent(options.spotName)
  }
})

onShow(() => {
  loadRoutes()
})

async function loadRoutes() {
  loading.value = true

  try {
    routes.value = await getRecommendRoutes()
    console.log('推荐路线：', routes.value)
  } catch (error) {
    console.error('加载推荐路线失败：', error)

    uni.showToast({
      title: '加载路线失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

function goSpotDetail(spot: RouteSpot) {
  if (!spot.matched || !spot.spotId) {
    uni.showToast({
      title: '该景点暂未配置',
      icon: 'none'
    })
    return
  }

  uni.navigateTo({
    url: `/pages/spot-detail/spot-detail?id=${spot.spotId}`
  })
}

function hasMissingSpot(route: RecommendRoute) {
  return route.spots.some(item => !item.matched)
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f7;
  padding: 28rpx;
  box-sizing: border-box;
}

.header {
  margin-bottom: 28rpx;
}

.title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #222;
  margin-bottom: 12rpx;
}

.subtitle {
  display: block;
  font-size: 26rpx;
  color: #777;
  line-height: 1.5;
}

.status {
  padding-top: 160rpx;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}

.route-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.route-card {
  background: #ffffff;
  border-radius: 28rpx;
  padding: 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.route-top {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 18rpx;
}

.route-name {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #222;
  margin-bottom: 10rpx;
}

.route-desc {
  display: block;
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

.tag {
  height: 44rpx;
  line-height: 44rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #eef4ff;
  color: #1677ff;
  font-size: 24rpx;
  flex-shrink: 0;
}

.meta-row {
  margin-bottom: 20rpx;
}

.meta {
  font-size: 25rpx;
  color: #999;
}

.spot-chain {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.spot-pill {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #eaf8ef;
  color: #19a15f;
  font-size: 25rpx;
}

.spot-pill.disabled {
  background: #f1f1f1;
  color: #aaa;
}

.arrow {
  color: #aaa;
  font-size: 26rpx;
}

.guide-box {
  padding: 22rpx;
  border-radius: 20rpx;
  background: #fafafa;
}

.guide-title {
  display: block;
  font-size: 27rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.guide-text {
  display: block;
  font-size: 25rpx;
  color: #666;
  line-height: 1.6;
}

.warning {
  margin-top: 16rpx;
  color: #f56c6c;
  font-size: 24rpx;
}

.tip-box {
  margin-bottom: 24rpx;
  padding: 20rpx 24rpx;
  border-radius: 20rpx;
  background: #eef6ff;
}

.tip-text {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1677ff;
}

.tip-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #666;
}
</style>