<script setup lang="ts">
import { useI18n } from 'vue-i18n'

interface Props {
  active?: string
}

const props = withDefaults(defineProps<Props>(), { active: 'home' })
const { t } = useI18n()

const leftTabs  = [
  { key: 'home',  icon: '🏠', url: '/pages/index/index' },
  { key: 'guide', icon: '🗺️', url: '/pages/guide/guide' },
]
const rightTabs = [
  { key: 'language', icon: '🌐', url: '/pages/language/language' },
  { key: 'mine',     icon: '👤', url: '/pages/mine/mine' },
]

function switchTab(url: string, key: string) {
  if (key === props.active) return
  uni.redirectTo({ url })
}

function goMatch() {
  uni.navigateTo({ url: '/pages/match/match' })
}
</script>

<template>
  <view class="tabbar">
    <view
      v-for="item in leftTabs"
      :key="item.key"
      class="tabbar-item"
      :class="{ active: item.key === props.active }"
      @click="switchTab(item.url, item.key)"
    >
      <view class="tabbar-icon">{{ item.icon }}</view>
      <view class="tabbar-text">{{ t(`tabbar.${item.key}`) }}</view>
    </view>

    <view class="tabbar-center" @click="goMatch">
      <view class="center-btn">
        <text class="center-icon">🧳</text>
      </view>
      <view class="tabbar-text center-text">{{ t('tabbar.match') }}</view>
    </view>

    <view
      v-for="item in rightTabs"
      :key="item.key"
      class="tabbar-item"
      :class="{ active: item.key === props.active }"
      @click="switchTab(item.url, item.key)"
    >
      <view class="tabbar-icon">{{ item.icon }}</view>
      <view class="tabbar-text">{{ t(`tabbar.${item.key}`) }}</view>
    </view>
  </view>
</template>

<style scoped>
.tabbar {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  height: 120rpx;
  padding-bottom: env(safe-area-inset-bottom);
  background: #fff;
  display: flex;
  align-items: center;
  border-top: 1rpx solid #eee;
  z-index: 999;
}

.tabbar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #777;
}

.tabbar-item.active {
  color: #2f80ed;
  font-weight: 600;
}

.tabbar-icon { font-size: 42rpx; margin-bottom: 6rpx; }
.tabbar-text { font-size: 24rpx; }

.tabbar-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  padding-bottom: 10rpx;
  position: relative;
}

.center-btn {
  width: 108rpx;
  height: 108rpx;
  border-radius: 54rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 -6rpx 24rpx rgba(255, 107, 53, 0.45);
  position: absolute;
  top: -38rpx;
}

.center-icon { font-size: 48rpx; }
.center-text { color: #ff6b35; font-weight: 600; margin-top: 42rpx; }
</style>
