<template>
  <view class="badge-wrap" :class="[`size-${size}`, { locked: !badge.unlocked }]">
    <view class="badge-circle" :style="circleStyle">
      <text class="badge-icon">{{ badge.unlocked ? badge.icon : '❓' }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BadgeDTO } from '@/api/badge'

const props = defineProps<{
  badge: BadgeDTO
  size?: 'medium' | 'large'
}>()

const rarityColor: Record<string, string> = {
  COMMON: '#9e9e9e',
  RARE: '#2196f3',
  EPIC: '#9c27b0',
  LEGENDARY: '#ffd700'
}

const circleStyle = computed(() => {
  if (!props.badge.unlocked) return { background: '#ddd' }
  const color = rarityColor[props.badge.rarity] ?? '#9e9e9e'
  return { background: `linear-gradient(135deg, ${color}99, ${color})` }
})
</script>

<style scoped>
.badge-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.size-medium .badge-circle { width: 100rpx; height: 100rpx; }
.size-large  .badge-circle { width: 140rpx; height: 140rpx; }

.badge-icon { font-size: 44rpx; line-height: 1; }
.size-large .badge-icon { font-size: 64rpx; }

.locked .badge-circle { opacity: 0.4; }
</style>
