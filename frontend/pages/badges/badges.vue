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

    <!-- Tab 选项栏 -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: activeTab === 'SPOT' }"
        @click="activeTab = 'SPOT'"
      >
        <text>景点勋章</text>
        <text class="tab-count">{{ spotBadges.length }}</text>
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'ACHIEVEMENT' }"
        @click="activeTab = 'ACHIEVEMENT'"
      >
        <text>成就勋章</text>
        <text class="tab-count">{{ achievementBadges.length }}</text>
      </view>
    </view>

    <!-- 勋章网格 -->
    <view class="section">
      <view class="badge-grid">
        <view
          v-for="badge in currentBadges"
          :key="badge.id"
          class="grid-item"
          @click="openDetail(badge)"
        >
          <BadgeCard :badge="badge" size="medium" />
          <text class="badge-name">{{ badge.name }}</text>
        </view>
      </view>
    </view>

    <!-- 详情弹窗 -->
    <view v-if="selectedBadge" class="modal-mask" @click.self="selectedBadge = null">
      <view class="modal">
        <Badge3DViewer :badge="selectedBadge" />
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
import Badge3DViewer from '@/components/Badge3DViewer/Badge3DViewer.vue'
import { useBadgeApi, type BadgeDTO } from '@/api/badge'
import { useAuth } from '@/composables/useAuth'

const { listBadges } = useBadgeApi()
const { authState } = useAuth()

const badges = ref<BadgeDTO[]>([])
const selectedBadge = ref<BadgeDTO | null>(null)
const activeTab = ref<'SPOT' | 'ACHIEVEMENT'>('SPOT')

const nickname = computed(() => authState.userInfo?.nickname || '旅行者')
const avatarUrl = computed(() => authState.userInfo?.avatarUrl || '')

const spotBadges = computed(() => badges.value.filter(b => b.type === 'SPOT'))
const achievementBadges = computed(() => badges.value.filter(b => b.type === 'ACHIEVEMENT'))
const currentBadges = computed(() => activeTab.value === 'SPOT' ? spotBadges.value : achievementBadges.value)
const unlockedCount = computed(() => badges.value.filter(b => b.unlocked).length)
const total = computed(() => badges.value.length)

onMounted(async () => {
  if (!authState.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  try {
    const res = await listBadges()
    if (res.code === 200) {
      badges.value = res.data.map(b => ({
        ...b,
        unlocked: true,
        unlockedAt: b.unlockedAt || new Date().toISOString()
      }))
    }
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

/* Tab 选项栏 */
.tab-bar {
  display: flex;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}
.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 28rpx 0;
  font-size: 28rpx;
  color: #888;
  position: relative;
}
.tab-item.active {
  color: #1a1a2e;
  font-weight: 600;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20%;
  right: 20%;
  height: 4rpx;
  background: #1a1a2e;
  border-radius: 2rpx;
}
.tab-count {
  font-size: 22rpx;
  background: #f0f0f0;
  color: #888;
  padding: 2rpx 12rpx;
  border-radius: 20rpx;
}
.tab-item.active .tab-count {
  background: #1a1a2e;
  color: #fff;
}

.section { margin: 24rpx 0 0; }

.badge-grid { display: flex; flex-wrap: wrap; padding: 0 16rpx; }
.grid-item { width: 25%; display: flex; flex-direction: column; align-items: center; padding: 8rpx; margin-bottom: 16rpx; }
.badge-name { font-size: 22rpx; color: #333; margin-top: 8rpx; text-align: center; line-height: 1.3; }

.modal-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.75);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}
.modal {
  background: #1a1a2e;
  border-radius: 24rpx;
  padding: 32rpx 40rpx 40rpx;
  width: 580rpx;
  display: flex; flex-direction: column; align-items: center;
}
.modal-rarity-tag { margin-top: 16rpx; padding: 4rpx 20rpx; border-radius: 20rpx; color: #fff; font-size: 22rpx; }
.modal-name { font-size: 36rpx; font-weight: 700; color: #fff; margin-top: 16rpx; }
.modal-desc { font-size: 26rpx; color: rgba(255,255,255,0.7); margin-top: 12rpx; text-align: center; }
.modal-date { font-size: 24rpx; color: rgba(255,255,255,0.5); margin-top: 16rpx; }
.modal-note { font-size: 22rpx; color: rgba(255,255,255,0.4); margin-top: 8rpx; }
.modal-close { margin-top: 32rpx; padding: 16rpx 80rpx; background: rgba(255,255,255,0.15); color: #fff; border-radius: 40rpx; font-size: 28rpx; }

.tabbar-placeholder { height: 140rpx; }
</style>
