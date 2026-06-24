<template>
  <view class="page">
    <!-- 背景氛围 -->
    <view class="bg-layer"></view>
    <view class="glow glow-1"></view>
    <view class="glow glow-2"></view>

    <!-- Live2D 展示区 -->
    <!-- #ifdef MP-WEIXIN -->
    <view class="live2d-stage">
      <view class="live2d-wrap">
        <live2d-view
          className="live2d-box"
          :autoInit="true"
          :stageWidth="750"
          @ready="handleReady"
          @error="handleError"
        />
      </view>
    </view>
    <!-- #endif -->

    <!-- #ifndef MP-WEIXIN -->
    <view class="placeholder">
      <text>请在微信小程序平台查看 Live2D 页面</text>
    </view>
    <!-- #endif -->

    <!-- 底部氛围层 -->
    <view class="bottom-atmosphere"></view>

    <!-- 输入对话框 -->
    <view class="chat-panel">
      <view class="chat-box">
        <input
          class="chat-input"
          v-model="inputText"
          type="text"
          placeholder="和看板娘说点什么吧..."
          confirm-type="send"
          @confirm="handleSend"
        />
        <view class="send-btn" @tap="handleSend">
          <text class="send-text">发送</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
	
import { ref } from 'vue'
const inputText = ref('')

function handleReady(e: any) {
  console.log('[live2d-page] component ready:', e)
}

function handleError(e: any) {
  console.error('[live2d-page] component error:', e)
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text) return

  console.log('[chat] send:', text)

  // 这里后续可以接入你的聊天接口
  inputText.value = ''
}
</script>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(180deg, #fdfcff 0%, #f7f5ff 42%, #f3f6ff 100%);
}

/* 背景层 */
.bg-layer {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 18%, rgba(255, 210, 235, 0.36), transparent 28%),
    radial-gradient(circle at 82% 26%, rgba(196, 220, 255, 0.34), transparent 30%),
    radial-gradient(circle at 50% 78%, rgba(224, 214, 255, 0.26), transparent 34%);
  pointer-events: none;
}

.glow {
  position: absolute;
  border-radius: 9999rpx;
  filter: blur(30rpx);
  opacity: 0.65;
  pointer-events: none;
}

.glow-1 {
  top: 140rpx;
  left: -40rpx;
  width: 260rpx;
  height: 260rpx;
  background: rgba(255, 205, 226, 0.45);
}

.glow-2 {
  top: 260rpx;
  right: -60rpx;
  width: 320rpx;
  height: 320rpx;
  background: rgba(194, 216, 255, 0.42);
}

/* Live2D主区域 */
.live2d-stage {
  position: relative;
  z-index: 2;
  width: 100%;
  padding-top: 80rpx;
}

.live2d-wrap {
  width: 70%;
  height: 1000rpx;
  margin: 0 auto;
}

:deep(.live2d-box) {
  width: 100%;
  height: 100%;
  display: block;
}

/* 底部氛围 */
.bottom-atmosphere {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 180rpx;
  height: 300rpx;
  z-index: 1;
  background: radial-gradient(
    ellipse at center,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(237, 233, 255, 0.72) 42%,
    rgba(237, 233, 255, 0) 75%
  );
  pointer-events: none;
}

/* 输入区 */
.chat-panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 5;
  padding: 20rpx 24rpx 34rpx;
  background: linear-gradient(180deg, rgba(246, 247, 251, 0) 0%, rgba(246, 247, 251, 0.92) 30%, rgba(246, 247, 251, 1) 100%);
  box-sizing: border-box;
}

.chat-box {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(12rpx);
  box-shadow: 0 12rpx 36rpx rgba(179, 184, 255, 0.18);
}

.chat-input {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
  border-radius: 24rpx;
  background: rgba(246, 247, 255, 0.95);
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.send-btn {
  flex-shrink: 0;
  height: 72rpx;
  padding: 0 28rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #8ea7ff 0%, #b59cff 100%);
  box-shadow: 0 8rpx 20rpx rgba(150, 146, 255, 0.28);
}

.send-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: 600;
}

.placeholder {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 30rpx;
}
</style>