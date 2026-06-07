<template>
  <view class="page">

    <!-- 顶部暗色 header -->
    <view class="header">
      <view class="header-avatar">
        <image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill" />
        <view v-else class="avatar-placeholder"><text class="avatar-icon">👤</text></view>
      </view>
      <text class="header-name">{{ nickname }}</text>
      <text class="header-count">已获得 {{ unlockedCount }} / {{ total }} 枚勋章</text>
    </view>

    <!-- 最近获得 -->
    <view v-if="recentBadges.length" class="section">
      <text class="section-title">最近获得</text>
      <scroll-view scroll-x class="recent-scroll">
        <view class="recent-list">
          <view
            v-for="badge in recentBadges"
            :key="badge.id"
            class="recent-item"
            @click="openDetail(badge)"
          >
            <BadgeCard :badge="badge" size="large" />
            <text class="badge-name">{{ badge.name }}</text>
            <text class="badge-date">{{ formatDate(badge.unlockedAt) }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 景点勋章 -->
    <view class="section">
      <text class="section-title">景点勋章</text>
      <view class="badge-grid">
        <view
          v-for="badge in spotBadges"
          :key="badge.id"
          class="grid-item"
          @click="badge.unlocked && openDetail(badge)"
        >
          <BadgeCard :badge="badge" size="medium" />
          <text class="badge-name" :class="{ locked: !badge.unlocked }">{{ badge.name }}</text>
          <text v-if="!badge.unlocked" class="badge-condition">{{ badge.unlockCondition }}</text>
        </view>
      </view>
    </view>

    <!-- 成就勋章 -->
    <view class="section">
      <text class="section-title">成就勋章</text>
      <view class="badge-grid">
        <view
          v-for="badge in achievementBadges"
          :key="badge.id"
          class="grid-item"
          @click="badge.unlocked && openDetail(badge)"
        >
          <BadgeCard :badge="badge" size="medium" />
          <text class="badge-name" :class="{ locked: !badge.unlocked }">{{ badge.name }}</text>
          <text v-if="!badge.unlocked" class="badge-condition">{{ badge.unlockCondition }}</text>
        </view>
      </view>
    </view>

    <!-- 详情弹窗 -->
    <view v-if="selectedBadge" class="modal-mask" @click.self="selectedBadge = null">
      <view class="modal">
        <BadgeCard :badge="selectedBadge" size="large" />
        <view class="modal-rarity-tag" :style="{ background: rarityColor(selectedBadge.rarity) }">
          {{ rarityLabel(selectedBadge.rarity) }}
        </view>
        <text class="modal-name">{{ selectedBadge.name }}</text>
        <text class="modal-desc">{{ selectedBadge.description }}</text>
        <text class="modal-date">🗓 获得于 {{ formatDate(selectedBadge.unlockedAt) }}</text>
        <text v-if="selectedBadge.note" class="modal-note">{{ selectedBadge.note }}</text>
        <view class="modal-close" @click="selectedBadge = null">关闭</view>
      </view>
    </view>

    <view class="tabbar-placeholder" />
    <TabBar active="badges" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import TabBar from '@/components/TabBar/TabBar.vue'
import BadgeCard from '@/components/BadgeCard/BadgeCard.vue'
import { useBadgeApi, type BadgeDTO } from '@/api/badge'
import { useAuth } from '@/composables/useAuth'

const { listBadges } = useBadgeApi()
const { authState } = useAuth()

const badges = ref<BadgeDTO[]>([])
const selectedBadge = ref<BadgeDTO | null>(null)

const nickname = computed(() => authState.userInfo?.nickname || '旅行者')
const avatarUrl = computed(() => authState.userInfo?.avatarUrl || '')

const spotBadges = computed(() => badges.value.filter(b => b.type === 'SPOT'))
const achievementBadges = computed(() => badges.value.filter(b => b.type === 'ACHIEVEMENT'))
const recentBadges = computed(() =>
  badges.value
    .filter(b => b.unlocked)
    .sort((a, b) => new Date(b.unlockedAt!).getTime() - new Date(a.unlockedAt!).getTime())
    .slice(0, 6)
)
const unlockedCount = computed(() => badges.value.filter(b => b.unlocked).length)
const total = computed(() => badges.value.length)

onMounted(async () => {
  if (!authState.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  try {
    const res = await listBadges()
    if (res.code === 200) badges.value = res.data
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
})

function openDetail(badge: BadgeDTO) {
  selectedBadge.value = badge
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()}`
}

function rarityColor(rarity: string): string {
  const map: Record<string, string> = {
    COMMON: '#9e9e9e',
    RARE: '#2196f3',
    EPIC: '#9c27b0',
    LEGENDARY: '#ffd700'
  }
  return map[rarity] ?? '#9e9e9e'
}

function rarityLabel(rarity: string): string {
  const map: Record<string, string> = {
    COMMON: '普通', RARE: '稀有', EPIC: '史诗', LEGENDARY: '传说'
  }
  return map[rarity] ?? rarity
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f0f2f5; }

.header {
  background: #1a1a2e;
  padding: 60rpx 32rpx 40rpx;
  display: flex; flex-direction: column; align-items: center;
}
.header-avatar {
  width: 120rpx; height: 120rpx;
  border-radius: 60rpx; overflow: hidden;
  border: 4rpx solid rgba(255,255,255,0.3);
  margin-bottom: 16rpx;
  display: flex; align-items: center; justify-content: center;
  background: #333;
}
.avatar { width: 120rpx; height: 120rpx; }
.avatar-placeholder { width: 120rpx; height: 120rpx; display: flex; align-items: center; justify-content: center; background: #333; }
.avatar-icon { font-size: 60rpx; }
.header-name { color: #fff; font-size: 32rpx; font-weight: 600; margin-bottom: 8rpx; }
.header-count { color: rgba(255,255,255,0.6); font-size: 24rpx; }

.section { margin: 24rpx 0 0; }
.section-title { font-size: 28rpx; font-weight: 600; color: #1a1a1a; padding: 0 32rpx 20rpx; display: block; }

.recent-scroll { width: 100%; }
.recent-list { display: flex; flex-direction: row; padding: 0 24rpx 16rpx; }
.recent-item { display: flex; flex-direction: column; align-items: center; margin-right: 24rpx; width: 160rpx; flex-shrink: 0; }
.badge-date { font-size: 20rpx; color: #999; margin-top: 4rpx; }

.badge-grid { display: flex; flex-wrap: wrap; padding: 0 16rpx; }
.grid-item { width: 25%; display: flex; flex-direction: column; align-items: center; padding: 8rpx; margin-bottom: 16rpx; }
.badge-name { font-size: 22rpx; color: #333; margin-top: 8rpx; text-align: center; line-height: 1.3; }
.badge-name.locked { color: #bbb; }
.badge-condition { font-size: 18rpx; color: #bbb; text-align: center; margin-top: 4rpx; line-height: 1.3; }

.modal-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.6);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}
.modal {
  background: #fff; border-radius: 24rpx;
  padding: 48rpx 40rpx 40rpx;
  width: 560rpx;
  display: flex; flex-direction: column; align-items: center;
}
.modal-rarity-tag { margin-top: 16rpx; padding: 4rpx 20rpx; border-radius: 20rpx; color: #fff; font-size: 22rpx; }
.modal-name { font-size: 36rpx; font-weight: 700; color: #1a1a1a; margin-top: 16rpx; }
.modal-desc { font-size: 26rpx; color: #666; margin-top: 12rpx; text-align: center; }
.modal-date { font-size: 24rpx; color: #999; margin-top: 16rpx; }
.modal-note { font-size: 22rpx; color: #aaa; margin-top: 8rpx; }
.modal-close { margin-top: 32rpx; padding: 16rpx 80rpx; background: #1a1a2e; color: #fff; border-radius: 40rpx; font-size: 28rpx; }

.tabbar-placeholder { height: 140rpx; }
</style>
