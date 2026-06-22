<template>
  <view class="page">

    <!-- 未登录 -->
    <view v-if="!authState.isLoggedIn" class="login-card">
      <view class="avatar-circle">
        <text class="avatar-icon">👤</text>
      </view>
      <text class="unlogged-tip" :style="{ fontSize: rpx(28) }">未登录</text>
      <button class="login-btn" :style="{ fontSize: rpx(30) }" @click="handleLogin">
        一键微信登录
      </button>
    </view>

    <!-- 已登录 -->
    <view v-else>

      <!-- 个人信息头部 -->
      <view class="profile-header">
        <button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
          <image
            v-if="authState.userInfo?.avatarUrl"
            class="avatar"
            :src="authState.userInfo.avatarUrl"
            mode="aspectFill"
          />
          <view v-else class="avatar-circle">
            <text class="avatar-icon">👤</text>
          </view>
        </button>
        <view class="profile-info">
          <input
            class="nickname-input"
            type="nickname"
            :value="authState.userInfo?.nickname || '微信用户'"
            :style="{ fontSize: rpx(34) }"
            placeholder="点击修改昵称"
            @blur="onNicknameBlur"
          />
          <text class="openid-text" :style="{ fontSize: rpx(22) }">
            ID: {{ authState.userInfo?.openid?.slice(0, 12) }}...
          </text>
        </view>
      </view>

      <!-- 功能菜单 -->
      <view class="menu-group">
        <view class="menu-item" @click="onNotifications">
          <text class="menu-label" :style="{ fontSize: rpx(28) }">消息通知</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="divider" />
        <view class="menu-item" @click="onMyPosts">
          <text class="menu-label" :style="{ fontSize: rpx(28) }">我的攻略</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="divider" />
        <view class="menu-item" @click="onCollect">
          <text class="menu-label" :style="{ fontSize: rpx(28) }">攻略收藏</text>
          <text class="menu-arrow">›</text>
        </view>
		<view class="divider" />
		<view class="menu-item" @click="onSpotFavorites">
		  <text class="menu-label" :style="{ fontSize: rpx(28) }">景点收藏</text>
		  <text class="menu-arrow">›</text>
		</view>
        <view class="divider" />
        <view class="menu-item" @click="onLanguage">
          <text class="menu-label" :style="{ fontSize: rpx(28) }">语言设置</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="divider" />
        <view class="menu-item">
          <text class="menu-label" :style="{ fontSize: rpx(28) }">长辈模式</text>
          <switch :checked="isElderMode" color="#07c160" @change="onElderToggle" />
        </view>
        <view class="divider" />
        <view class="menu-item" @click="onAbout">
          <text class="menu-label" :style="{ fontSize: rpx(28) }">关于 TripMate</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <!-- 退出登录 -->
      <button class="logout-btn" :style="{ fontSize: rpx(28) }" @click="handleLogout">
        退出登录
      </button>

    </view>

    <view class="tabbar-placeholder" />
    <TabBar active="mine" />
  </view>
</template>

<script setup lang="ts">
import { useAuth } from '@/composables/useAuth'
import { useElder } from '@/composables/useElder'
import TabBar from '@/components/TabBar/TabBar.vue'

const { authState, login, logout, saveProfile } = useAuth()
const { rpx, isElderMode, toggleElderMode } = useElder()

async function handleLogin() {
  try {
    await login()
    uni.showToast({ title: '登录成功', icon: 'success' })
  } catch {
    uni.showToast({ title: '登录失败，请重试', icon: 'none' })
  }
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确认退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        logout()
        uni.showToast({ title: '已退出登录', icon: 'none' })
      }
    }
  })
}

function onSpotFavorites() {
  uni.navigateTo({ url: '/pages/spot-favorites/spot-favorites' })
}

async function onChooseAvatar(e: any) {
  const newAvatarUrl: string = e.detail.avatarUrl
  if (!newAvatarUrl) return
  try {
    await saveProfile(authState.userInfo?.nickname || '', newAvatarUrl)
    uni.showToast({ title: '头像已更新', icon: 'success' })
  } catch {
    uni.showToast({ title: '更新失败', icon: 'none' })
  }
}

async function onNicknameBlur(e: any) {
  const newNickname: string = e.detail.value?.trim()
  if (!newNickname || newNickname === authState.userInfo?.nickname) return
  try {
    await saveProfile(newNickname, authState.userInfo?.avatarUrl || '')
    uni.showToast({ title: '昵称已更新', icon: 'success' })
  } catch {
    uni.showToast({ title: '更新失败', icon: 'none' })
  }
}

function onNotifications() {
  uni.navigateTo({ url: '/pages/notifications/notifications' })
}

function onMyPosts() {
  uni.navigateTo({ url: '/pages/mine/my-posts/my-posts' })
}

function onCollect() {
  uni.navigateTo({ url: '/pages/mine/my-favorites/my-favorites' })
}

function onLanguage() {
  uni.navigateTo({ url: '/pages/language/language' })
}

function onElderToggle() {
  toggleElderMode()
}

function onAbout() {
  uni.showModal({
    title: '关于 TripMate',
    content: 'TripMate 智能旅行助手\n版本 1.0.0\n\n让旅行更简单、更美好。',
    showCancel: false
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background-color: #f7f8fa;
}

/* 未登录 */
.login-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 32rpx 0;
}

.unlogged-tip {
  margin-top: 24rpx;
  color: #999;
}

.login-btn {
  margin-top: 48rpx;
  width: 480rpx;
  background-color: #07c160;
  color: #fff;
  border-radius: 48rpx;
  border: none;
}

/* 头像通用 */
.avatar-circle {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background-color: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-icon {
  font-size: 60rpx;
}

/* 已登录 — 个人信息头部 */
.profile-header {
  display: flex;
  align-items: center;
  background-color: #fff;
  padding: 48rpx 32rpx;
  margin-bottom: 24rpx;
}

.avatar-btn {
  padding: 0;
  margin: 0;
  background: none;
  border: none;
  line-height: 1;
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-btn::after {
  border: none;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
}

.profile-info {
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
}

.nickname-input {
  color: #1a1a1a;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.openid-text {
  color: #bbb;
}

/* 功能菜单 */
.menu-group {
  background-color: #fff;
  border-radius: 16rpx;
  margin: 0 24rpx 24rpx;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 36rpx 32rpx;
}

.menu-label {
  color: #1a1a1a;
}

.menu-arrow {
  font-size: 40rpx;
  color: #ccc;
}

.divider {
  height: 1rpx;
  background-color: #f0f0f0;
  margin: 0 32rpx;
}

/* 退出登录 */
.logout-btn {
  margin: 0 24rpx;
  background-color: #fff;
  color: #f56c6c;
  border-radius: 16rpx;
  border: none;
}

.logout-btn::after {
  border: none;
}

/* TabBar 占位 */
.tabbar-placeholder {
  height: 140rpx;
}
</style>
