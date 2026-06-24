<template>
  <view class="page">
    <view v-if="loading" class="loading">
      加载中...
    </view>

    <view v-else-if="!spot" class="empty">
      未找到景点信息
    </view>

    <view v-else class="detail-content">
      <view class="image-box">
        <image
          v-if="spot.imageUrl && !imageLoadFailed && !coverError"
          class="spot-image"
          :src="getImageSrc(spot.imageUrl)"
          mode="aspectFill"
          @error="handleImageError"
          @error="coverError = true"
        />

        <view v-else class="image-placeholder">
          <text class="placeholder-icon">🏞️</text>
          <text class="placeholder-text">暂无景点图片</text>
        </view>
      </view>

      <!-- 景点基础信息 -->
      <view class="card">
        <view class="title-row">
          <text class="spot-name">{{ spot.name }}</text>
          <text class="category">{{ spot.category || '景点' }}</text>
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

      <!-- 语音导览说明 -->
      <view class="card">
        <view class="section-title">语音导览</view>
        <text class="guide-tip">
          点击下方“播放导览”，系统将根据当前景点信息生成语音讲解。
        </text>
      </view>

      <!-- 路线推荐 -->
      <view class="card route-entry-card" @tap="goRouteRecommend">
        <view class="route-entry-left">
          <text class="section-title">路线推荐</text>
          <text class="route-entry-desc">查看西南大学校园推荐路线</text>
        </view>
        <text class="route-entry-arrow">›</text>
      </view>

      <!-- 操作按钮 -->
      <view class="action-bar">
        <view class="action-btn secondary" @tap="backToMap">
          <text class="action-btn-text">在地图中查看</text>
        </view>

        <view
          class="action-btn"
          :class="{ disabled: generatingAudio }"
          @tap="playGuide"
        >
          <text class="action-btn-text">{{ guideButtonText }}</text>
        </view>

        <view class="action-btn favorite" @tap="favoriteSpot">
          <text class="action-btn-text">{{ isFavorited ? '已收藏' : '收藏' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { synthesizeSpeech } from '@/api/tts'

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

const loading = ref(true)
const spot = ref<ScenicSpot | null>(null)
const coverError = ref(false)
const isFavorited = ref(false)
const imageLoadFailed = ref(false)

const audioContext = ref<UniApp.InnerAudioContext | null>(null)
const isPlaying = ref(false)
const generatingAudio = ref(false)
const currentAudioUrl = ref('')

const { authState, login } = useAuth()

const guideButtonText = computed(() => {
  if (generatingAudio.value) {
    return '生成中'
  }

  if (isPlaying.value) {
    return '暂停导览'
  }

  return '播放导览'
})

onLoad((options) => {
  const id = Number(options?.id)

  if (!id || Number.isNaN(id)) {
    loading.value = false

    uni.showToast({
      title: '景点ID无效',
      icon: 'none'
    })

    return
  }

  loadSpotDetail(id)
})

onUnload(() => {
  stopGuideAudio()
})

async function loadSpotDetail(id: number) {
  loading.value = true
  imageLoadFailed.value = false

  try {
    const res: any = await getScenicSpotById(id)
    const data = res?.data || res

    if (!data) {
      spot.value = null
      return
    }

    spot.value = {
      ...data,
      imageUrl: data.imageUrl || data.image_url || ''
    }

    console.log('景点详情数据：', spot.value)
    console.log('图片路径：', spot.value?.imageUrl)

    if (authState.isLoggedIn) {
      try {
        const favoriteRes: any = await checkFavorite(id)
        isFavorited.value = Boolean(favoriteRes?.data ?? favoriteRes)
      } catch (authError) {
        console.error('收藏状态处理失败：', authError)
      }

      try {
        if (spot.value) {
          await addHistory(
            'VIEW_SPOT',
            spot.value.id,
            `查看了景点：${spot.value.name}`
          )
        }
      } catch (authError) {
        console.error('记录浏览历史失败：', authError)
      }
    }
  } catch (error) {
    console.error('加载景点详情失败：', error)

    spot.value = null

    uni.showToast({
      title: '加载失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

function getImageSrc(url?: string) {
  if (!url) return ''

  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  if (url.startsWith('/static/')) {
    return url
  }

  if (url.startsWith('static/')) {
    return '/' + url
  }

  return url
}

function handleImageError(error: any) {
  imageLoadFailed.value = true
  console.error('图片加载失败：', spot.value?.imageUrl, error)
}

function buildGuideText() {
  if (!spot.value) {
    return ''
  }

  const name = spot.value.name || '该景点'
  const category = spot.value.category || '校园景点'
  const description = spot.value.description || ''
  const address = spot.value.address || ''

  let text = `欢迎来到${name}。`

  if (category) {
    text += `这里属于${category}类景点。`
  }

  if (address) {
    text += `它位于${address}。`
  }

  if (description) {
    text += description
  } else {
    text += '这里是西南大学北碚校区的重要景点之一，适合游览、拍照和打卡。'
  }

  if (text.length > 140) {
    text = text.slice(0, 140)
  }

  return text
}

function goRouteRecommend() {
  if (!spot.value) {
    return
  }

  uni.navigateTo({
    url: `/pages/route/route?spotName=${encodeURIComponent(spot.value.name)}`
  })
}

function backToMap() {
  uni.navigateBack()
}

async function playGuide() {
  if (!spot.value) {
    return
  }

  if (generatingAudio.value) {
    return
  }

  if (audioContext.value && isPlaying.value) {
    audioContext.value.pause()
    isPlaying.value = false
    return
  }

  if (audioContext.value && currentAudioUrl.value) {
    audioContext.value.play()
    isPlaying.value = true
    return
  }

  try {
    generatingAudio.value = true

    uni.showLoading({
      title: '生成导览中'
    })

    const guideText = buildGuideText()

    if (!guideText) {
      uni.showToast({
        title: '暂无导览内容',
        icon: 'none'
      })
      return
    }

    console.log('TTS导览文本：', guideText)

    const result: any = await synthesizeSpeech(guideText)
    const audioUrl = result?.audioUrl || result?.data?.audioUrl

    console.log('TTS返回结果：', result)
    console.log('TTS音频地址：', audioUrl)

    if (!audioUrl) {
      uni.showToast({
        title: '未获取到音频',
        icon: 'none'
      })
      return
    }

    currentAudioUrl.value = audioUrl

    const ctx = uni.createInnerAudioContext()
    ctx.src = audioUrl
    ctx.autoplay = false

    ctx.onPlay(() => {
      isPlaying.value = true
      console.log('开始播放景点导览')
    })

    ctx.onPause(() => {
      isPlaying.value = false
      console.log('暂停景点导览')
    })

    ctx.onEnded(() => {
      isPlaying.value = false
      console.log('景点导览播放结束')
    })

    ctx.onError((error) => {
      console.error('景点导览播放失败：', error)

      isPlaying.value = false

      uni.showToast({
        title: '音频播放失败',
        icon: 'none'
      })
    })

    audioContext.value = ctx
    ctx.play()
  } catch (error) {
    console.error('生成或播放导览失败：', error)

    uni.showToast({
      title: '导览生成失败',
      icon: 'none'
    })
  } finally {
    generatingAudio.value = false
    uni.hideLoading()
  }
}

function stopGuideAudio() {
  if (audioContext.value) {
    audioContext.value.stop()
    audioContext.value.destroy()
    audioContext.value = null
  }

  isPlaying.value = false
  currentAudioUrl.value = ''
}

async function favoriteSpot() {
  if (!spot.value) {
    return
  }

  let token = uni.getStorageSync('token')

  if (!token) {
    try {
      await login()
      token = uni.getStorageSync('token')
    } catch {
      uni.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }
  }

  if (!token) {
    uni.showToast({
      title: '登录失败',
      icon: 'none'
    })
    return
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

      try {
        await addHistory(
          'FAVORITE_SPOT',
          spot.value.id,
          `收藏了景点：${spot.value.name}`
        )
      } catch (historyError) {
        console.warn('收藏成功，但记录历史失败：', historyError)
      }

      uni.showToast({
        title: '收藏成功',
        icon: 'success'
      })
    }
  } catch (error) {
    console.error('收藏操作失败：', error)

    uni.showToast({
      title: '收藏失败，请重新登录',
      icon: 'none'
    })

    uni.removeStorageSync('token')
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
  height: 88rpx;
  border-radius: 18rpx;
  background: #1677ff;
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 600;

  display: flex;
  align-items: center;
  justify-content: center;

  text-align: center;
  box-sizing: border-box;
  overflow: hidden;
}

.action-btn-text {
  width: 100%;
  text-align: center;
  line-height: 88rpx;
}


.action-btn.secondary {
  background: #eef5ff;
  color: #1677ff;
}

.action-btn.favorite {
  background: #ff7a1a;
  color: #ffffff;
}

.route-entry-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.route-entry-left {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.route-entry-desc {
  font-size: 24rpx;
  color: #888;
}

.route-entry-arrow {
  font-size: 40rpx;
  color: #bbb;
}

.image-box {
  width: 100%;
  height: 420rpx;
  background: #e9eef5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spot-image {
  width: 100%;
  height: 100%;
}

.image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #999;
}

.placeholder-icon {
  font-size: 72rpx;
  margin-bottom: 16rpx;
}

.placeholder-text {
  font-size: 28rpx;
  color: #999;
}

.guide-tip {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.action-btn.disabled {
  opacity: 0.6;
}
</style>