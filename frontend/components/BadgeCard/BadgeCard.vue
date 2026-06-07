<template>
  <view class="badge-wrap" :class="[`size-${size}`, { locked: !badge.unlocked }]">
    <view class="badge-hex" :style="hexStyle">
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

const hexStyle = computed(() => {
  if (!props.badge.unlocked) return { background: '#ddd' }
  const color = rarityColor[props.badge.rarity] ?? '#9e9e9e'
  return { background: `linear-gradient(135deg, ${color}cc, ${color})` }
})
</script>

<style scoped>
.badge-wrap { display: flex; align-items: center; justify-content: center; }

.badge-hex {
  display: flex; align-items: center; justify-content: center;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
}

.size-medium .badge-hex { width: 100rpx; height: 116rpx; }
.size-large  .badge-hex { width: 140rpx; height: 162rpx; }

.badge-icon { font-size: 40rpx; }
.size-large .badge-icon { font-size: 56rpx; }

.locked .badge-hex { filter: grayscale(1); opacity: 0.5; }
</style>
