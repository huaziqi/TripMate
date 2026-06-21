<!-- frontend/pages/guide/create/create.vue -->
<template>
  <view class="page">

    <!-- 标题输入 -->
    <view class="field">
      <input
        v-model="form.title"
        class="title-input"
        placeholder="标题（最多 100 字）"
        :maxlength="100"
      />
      <text class="char-count-small">{{ form.title.length }}/100</text>
    </view>
    <view class="divider" />

    <!-- 分类选择 -->
    <view class="field">
      <text class="field-label">分类</text>
      <scroll-view class="cat-scroll" scroll-x>
        <view class="cat-list">
          <view
            v-for="c in categories"
            :key="c.value"
            class="cat-chip"
            :class="{ active: form.category === c.value }"
            @click="form.category = c.value"
          >{{ c.label }}</view>
        </view>
      </scroll-view>
    </view>
    <view class="divider" />

    <!-- 正文输入 -->
    <view class="field">
      <textarea
        v-model="form.content"
        class="content-textarea"
        placeholder="分享你的旅行心得..."
        :maxlength="1000"
        auto-height
      />
      <text class="char-count">{{ form.content.length }}/1000</text>
    </view>
    <view class="divider" />

    <!-- 图片上传 -->
    <view class="field">
      <text class="field-label">图片（最多 3 张）</text>
      <view class="img-list">
        <view
          v-for="(url, i) in form.imageUrls"
          :key="i"
          class="img-item"
        >
          <image class="img-preview" :src="url" mode="aspectFill" />
          <view class="img-del" @click="removeImage(i)">✕</view>
        </view>
        <view
          v-if="form.imageUrls.length < 3"
          class="img-add"
          @click="chooseImage"
        >
          <text class="img-add-icon">📷</text>
          <text class="img-add-label">添加图片</text>
        </view>
      </view>
    </view>

    <!-- 发布按钮 -->
    <view class="submit-wrap">
      <button class="submit-btn" :disabled="submitting" @click="submit">
        {{ submitting ? '发布中...' : '发布攻略' }}
      </button>
    </view>

  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { createPost } from '@/api/post'
import { uploadImage } from '@/api/upload'

const categories = [
  { value: 'SCENIC',      label: '景点攻略' },
  { value: 'FOOD',        label: '美食推荐' },
  { value: 'TRANSPORT',   label: '交通住宿' },
  { value: 'FREE_TRAVEL', label: '自由行' },
  { value: 'FAMILY',      label: '亲子游' },
]

const form = ref({
  title: '',
  content: '',
  category: 'SCENIC',
  imageUrls: [] as string[]
})

const submitting = ref(false)

function chooseImage() {
  uni.chooseImage({
    count: 3 - form.value.imageUrls.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      uni.showLoading({ title: '上传中...' })
      try {
        for (const path of res.tempFilePaths) {
          const url = await uploadImage(path)
          form.value.imageUrls.push(url)
          if (form.value.imageUrls.length >= 3) break
        }
      } catch {
        // uploadImage already shows toast on error
      } finally {
        uni.hideLoading()
      }
    }
  })
}

function removeImage(index: number) {
  form.value.imageUrls.splice(index, 1)
}

async function submit() {
  if (!form.value.title.trim()) {
    uni.showToast({ title: '请输入标题', icon: 'none' }); return
  }
  if (!form.value.content.trim()) {
    uni.showToast({ title: '请输入内容', icon: 'none' }); return
  }
  submitting.value = true
  try {
    const res = await createPost({
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      category: form.value.category,
      imageUrls: form.value.imageUrls
    })
    if (res.code === 200) {
      uni.showToast({ title: '发布成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1200)
      // submitting 保持 true，防止 1.2s 内重复点击（页面即将销毁）
    } else {
      submitting.value = false
    }
  } catch {
    submitting.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #fff; padding-bottom: 40rpx; }

.field { padding: 24rpx 32rpx; }
.field-label { display: block; font-size: 26rpx; color: #999; margin-bottom: 16rpx; }
.divider { height: 1rpx; background: #f0f0f0; }

.title-input {
  width: 100%; font-size: 34rpx; font-weight: 600;
  color: #1a1a1a; border: none; outline: none; height: 80rpx;
}
.char-count-small { display: block; text-align: right; font-size: 22rpx; color: #bbb; margin-top: 4rpx; }

/* 分类 */
.cat-scroll { margin-top: 4rpx; }
.cat-list { display: flex; gap: 16rpx; flex-direction: row; padding: 4rpx 0; }
.cat-chip {
  display: inline-flex; align-items: center;
  padding: 12rpx 28rpx; border-radius: 32rpx;
  border: 1rpx solid #ddd; font-size: 26rpx; color: #666;
  white-space: nowrap;
}
.cat-chip.active { border-color: #1677ff; color: #1677ff; background: #e6f0ff; }

/* 正文 */
.content-textarea {
  width: 100%; min-height: 300rpx; font-size: 28rpx; color: #1a1a1a;
  border: none; outline: none; line-height: 1.8; box-sizing: border-box;
}
.char-count { display: block; text-align: right; font-size: 22rpx; color: #bbb; margin-top: 8rpx; }

/* 图片 */
.img-list { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 4rpx; }
.img-item { position: relative; width: 200rpx; height: 200rpx; border-radius: 12rpx; overflow: hidden; }
.img-preview { width: 100%; height: 100%; }
.img-del {
  position: absolute; top: 4rpx; right: 4rpx;
  width: 40rpx; height: 40rpx; border-radius: 50%;
  background: rgba(0,0,0,0.5); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 22rpx;
}
.img-add {
  width: 200rpx; height: 200rpx; border-radius: 12rpx;
  border: 2rpx dashed #ccc; display: flex;
  flex-direction: column; align-items: center; justify-content: center; gap: 8rpx;
}
.img-add-icon { font-size: 48rpx; }
.img-add-label { font-size: 24rpx; color: #999; }

/* 发布 */
.submit-wrap { padding: 40rpx 32rpx 0; }
.submit-btn {
  width: 100%; height: 88rpx; background: #1677ff; color: #fff;
  border: none; border-radius: 44rpx; font-size: 32rpx;
}
</style>
