<template>
  <view class="page">
    <view v-if="loading" class="status">
      加载中...
    </view>

    <view v-else-if="historyList.length === 0" class="empty">
      <text class="empty-icon">🕘</text>
      <text class="empty-title">暂无历史记录</text>
      <text class="empty-desc">浏览景点、收藏景点后会显示在这里</text>
    </view>

    <view v-else class="list">
      <view
        v-for="item in historyList"
        :key="item.id"
        class="history-card"
        @tap="handleHistoryClick(item)"
      >
        <view class="history-icon">
          {{ getTypeIcon(item.type) }}
        </view>

        <view class="history-main">
          <view class="history-row">
            <text class="history-type">{{ getTypeText(item.type) }}</text>
            <text class="history-time">{{ formatTime(item.createTime) }}</text>
          </view>

          <text class="history-content">
            {{ item.content || '暂无内容' }}
          </text>
        </view>

        <text
          v-if="isSpotRecord(item)"
          class="arrow"
        >
          ›
        </text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getHistoryList } from '@/api/history'
import type { HistoryRecord } from '@/api/history'

const loading = ref(false)
const historyList = ref<HistoryRecord[]>([])

onShow(() => {
  loadHistory()
})

async function loadHistory() {
  loading.value = true

  try {
    historyList.value = await getHistoryList()
    console.log('历史记录：', historyList.value)
  } catch (error) {
    console.error('加载历史记录失败：', error)

    uni.showToast({
      title: '加载历史记录失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

function getTypeText(type: string) {
  const map: Record<string, string> = {
    VIEW_SPOT: '查看景点',
    FAVORITE_SPOT: '收藏景点',
    SEARCH_SPOT: '搜索景点',
    PLAY_AUDIO: '播放导览',
    AI_CHAT: 'AI 问答'
  }

  return map[type] || '操作记录'
}

function getTypeIcon(type: string) {
  const map: Record<string, string> = {
    VIEW_SPOT: '👀',
    FAVORITE_SPOT: '⭐',
    SEARCH_SPOT: '🔍',
    PLAY_AUDIO: '🔊',
    AI_CHAT: '🤖'
  }

  return map[type] || '📌'
}

function isSpotRecord(item: HistoryRecord) {
  return ['VIEW_SPOT', 'FAVORITE_SPOT', 'PLAY_AUDIO'].includes(item.type)
    && !!item.targetId
}

function handleHistoryClick(item: HistoryRecord) {
  if (isSpotRecord(item)) {
    uni.navigateTo({
      url: `/pages/spot-detail/spot-detail?id=${item.targetId}`
    })
  }
}

function formatTime(time: string) {
  if (!time) {
    return ''
  }

  const date = new Date(time)

  if (Number.isNaN(date.getTime())) {
    return time
  }

  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) {
    return '刚刚'
  }

  if (diff < hour) {
    return `${Math.floor(diff / minute)}分钟前`
  }

  if (diff < day) {
    return `${Math.floor(diff / hour)}小时前`
  }

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const dayText = String(date.getDate()).padStart(2, '0')
  const hourText = String(date.getHours()).padStart(2, '0')
  const minuteText = String(date.getMinutes()).padStart(2, '0')

  return `${month}-${dayText} ${hourText}:${minuteText}`
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

.history-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 26rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.05);
}

.history-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #eef4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  flex-shrink: 0;
}

.history-main {
  flex: 1;
  min-width: 0;
}

.history-row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 8rpx;
}

.history-type {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
}

.history-time {
  font-size: 24rpx;
  color: #aaa;
  flex-shrink: 0;
}

.history-content {
  display: block;
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

.arrow {
  font-size: 44rpx;
  color: #bbb;
  flex-shrink: 0;
}
</style>