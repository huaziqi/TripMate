<template>
  <view class="page">
    <view v-if="list.length === 0 && !loading" class="empty">
      <text class="empty-text">暂无消息</text>
    </view>

    <view
      v-for="item in list"
      :key="item.id"
      class="notif-item"
      :class="{ unread: !item.read }"
      @click="onTap(item)"
    >
      <view class="avatar-wrap">
        <image
          v-if="item.fromUser?.avatarUrl"
          class="avatar"
          :src="item.fromUser.avatarUrl"
          mode="aspectFill"
        />
        <view v-else class="avatar avatar-placeholder" />
        <view class="type-badge">{{ typeBadge(item.type) }}</view>
      </view>
      <view class="content">
        <text class="actor">{{ item.fromUser?.nickname || '旅行者' }}</text>
        <text class="action-text">{{ actionText(item) }}</text>
        <text v-if="item.commentContent" class="comment-preview">{{ item.commentContent }}</text>
        <text class="time">{{ formatTime(item.createdAt) }}</text>
      </view>
      <view v-if="!item.read" class="unread-dot" />
    </view>

    <view v-if="loading" class="loading-tip"><text>加载中...</text></view>
    <view v-if="noMore && list.length > 0" class="loading-tip"><text>没有更多了</text></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchNotifications, markAllRead, type NotificationItem } from '@/api/notification'

const list = ref<NotificationItem[]>([])
const page = ref(0)
const loading = ref(false)
const noMore = ref(false)

onMounted(async () => {
  await load()
  markAllRead().catch(() => {})
})

async function load(reset = false) {
  if (loading.value || noMore.value) return
  if (reset) { page.value = 0; noMore.value = false }
  loading.value = true
  try {
    const res = await fetchNotifications(page.value, 20)
    if (res.code === 200) {
      const items = res.data.items
      list.value = reset ? items : [...list.value, ...items]
      if (list.value.length >= res.data.total) noMore.value = true
      else page.value++
    }
  } finally {
    loading.value = false
  }
}

function typeBadge(type: string) {
  const map: Record<string, string> = {
    LIKE_POST: '👍', COMMENT_POST: '💬', NEW_FOLLOWER: '👤', MENTION_COMMENT: '@'
  }
  return map[type] || '🔔'
}

function actionText(item: NotificationItem) {
  switch (item.type) {
    case 'LIKE_POST': return `赞了你的攻略《${item.postTitle || ''}》`
    case 'COMMENT_POST': return `评论了你的攻略《${item.postTitle || ''}》`
    case 'NEW_FOLLOWER': return '关注了你'
    case 'MENTION_COMMENT': return '在评论中提到了你'
    default: return ''
  }
}

function formatTime(t: string) {
  const d = new Date(t)
  const now = new Date()
  const diff = (now.getTime() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  return `${Math.floor(diff / 86400)}天前`
}

function onTap(item: NotificationItem) {
  if (item.postId) {
    uni.navigateTo({ url: `/pages/guide/detail/detail?id=${item.postId}` })
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; }
.empty { padding: 120rpx 0; text-align: center; }
.empty-text { font-size: 28rpx; color: #bbb; }
.notif-item {
  display: flex; align-items: flex-start; gap: 24rpx;
  padding: 28rpx 32rpx; background: #fff;
  border-bottom: 1rpx solid #f0f0f0; position: relative;
}
.notif-item.unread { background: #f0f6ff; }
.avatar-wrap { position: relative; flex-shrink: 0; }
.avatar { width: 80rpx; height: 80rpx; border-radius: 50%; }
.avatar-placeholder { width: 80rpx; height: 80rpx; border-radius: 50%; background: #e0e0e0; }
.type-badge {
  position: absolute; bottom: -4rpx; right: -4rpx;
  width: 32rpx; height: 32rpx; border-radius: 50%;
  background: #1677ff; display: flex; align-items: center; justify-content: center;
  font-size: 18rpx;
}
.content { flex: 1; min-width: 0; }
.actor { font-size: 28rpx; font-weight: 600; color: #1a1a1a; display: block; margin-bottom: 4rpx; }
.action-text { font-size: 26rpx; color: #555; display: block; }
.comment-preview {
  display: block; font-size: 24rpx; color: #999; margin-top: 8rpx;
  overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}
.time { display: block; font-size: 22rpx; color: #bbb; margin-top: 8rpx; }
.unread-dot {
  position: absolute; top: 28rpx; right: 24rpx;
  width: 16rpx; height: 16rpx; border-radius: 50%; background: #ff4d4f;
}
.loading-tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #bbb; }
</style>
