<script setup lang="ts">
import { useI18n } from 'vue-i18n'

interface Props {
  active?: string
}

interface TabItem {
  key: string
  icon: string
  url: string
}

const props = withDefaults(
  defineProps<Props>(),
  {
    active: 'home'
  }
)

const { t } = useI18n()

const tabs: TabItem[] = [
  {
    key: 'home',
    icon: '🏠',
    url: '/pages/index/index'
  },
  {
    key: 'guide',
    icon: '🗺️',
    url: '/pages/guide/guide'
  },
  {
    key: 'language',
    icon: '🌐',
    url: '/pages/language/language'
  },
  {
    key: 'badges',
    icon: '🏅',
    url: '/pages/badges/badges'
  },
  {
    key: 'mine',
    icon: '👤',
    url: '/pages/mine/mine'
  }
]

const switchTab = (item: TabItem): void => {
  if (item.key === props.active) return

  uni.redirectTo({
    url: item.url
  })
}
</script>

<template>
  <view class="tabbar">
    <view
      v-for="item in tabs"
      :key="item.key"
      class="tabbar-item"
      :class="{ active: item.key === props.active }"
      @click="switchTab(item)"
    >
      <view class="tabbar-icon">
        {{ item.icon }}
      </view>

      <view class="tabbar-text">
        {{ t(`tabbar.${item.key}`) }}
      </view>

    </view>
  </view>
</template>

<style scoped>
.tabbar{
  position:fixed;
  left:0;
  right:0;
  bottom:0;

  height:120rpx;
  padding-bottom:env(safe-area-inset-bottom);

  background:#fff;

  display:flex;

  border-top:1rpx solid #eee;

  z-index:999;
}

.tabbar-item{
  flex:1;

  display:flex;
  flex-direction:column;

  justify-content:center;
  align-items:center;

  color:#777;
}

.tabbar-item.active{
  color:#2f80ed;
  font-weight:600;
}

.tabbar-icon{
  font-size:42rpx;
  margin-bottom:6rpx;
}

.tabbar-text{
  font-size:24rpx;
}
</style>