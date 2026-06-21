<template>
  <view class="page">
    <view class="search-bar">
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索攻略标题或内容..."
        confirm-type="search"
        @confirm="doSearch"
        :focus="true"
      />
      <text class="search-btn" @click="doSearch">搜索</text>
    </view>

    <view v-if="results.length === 0 && searched && !loading" class="empty">
      <text class="empty-text">未找到相关攻略</text>
    </view>

    <view
      v-for="item in results"
      :key="item.id"
      class="post-card"
      @click="uni.navigateTo({ url: `/pages/guide/detail/detail?id=${item.id}` })"
    >
      <image v-if="item.coverUrl" class="cover" :src="item.coverUrl" mode="aspectFill" />
      <view v-else class="cover cover-placeholder" />
      <view class="card-body">
        <text class="post-title">{{ item.title }}</text>
        <text class="post-snippet">{{ item.content?.slice(0, 60) }}...</text>
        <view class="card-meta">
          <text class="author-name">{{ item.author?.nickname || '旅行者' }}</text>
          <view class="stats-row">
            <text class="stat">👍 {{ item.likeCount }}</text>
            <text class="stat">💬 {{ item.commentCount }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!noMore && results.length > 0" class="load-more" @click="loadMore">
      <text>加载更多</text>
    </view>
    <view v-if="noMore && results.length > 0" class="loading-tip"><text>已经到底了</text></view>
    <view v-if="loading" class="loading-tip"><text>搜索中...</text></view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchPosts, type PostItem } from '@/api/post'

const keyword = ref('')
const results = ref<PostItem[]>([])
const page = ref(0)
const loading = ref(false)
const noMore = ref(false)
const searched = ref(false)

async function doSearch() {
  if (!keyword.value.trim()) return
  page.value = 0
  noMore.value = false
  results.value = []
  searched.value = true
  await load()
}

async function loadMore() {
  if (!noMore.value) await load()
}

async function load() {
  if (loading.value || noMore.value) return
  loading.value = true
  try {
    const res = await searchPosts(keyword.value.trim(), page.value, 10)
    if (res.code === 200) {
      const items = res.data.items
      results.value = page.value === 0 ? items : [...results.value, ...items]
      if (results.value.length >= res.data.total) noMore.value = true
      else page.value++
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; }
.search-bar {
  display: flex; align-items: center; gap: 16rpx;
  padding: 20rpx 24rpx; background: #fff;
  border-bottom: 1rpx solid #eee;
}
.search-input {
  flex: 1; height: 68rpx; background: #f5f5f5; border-radius: 34rpx;
  padding: 0 28rpx; font-size: 28rpx; color: #333;
}
.search-btn { font-size: 28rpx; color: #1677ff; white-space: nowrap; }
.empty { padding: 120rpx 0; text-align: center; }
.empty-text { font-size: 28rpx; color: #bbb; }
.post-card {
  background: #fff; margin: 20rpx 24rpx 0;
  border-radius: 20rpx; overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
}
.cover { width: 100%; height: 200rpx; display: block; }
.cover-placeholder { width: 100%; height: 120rpx; background: linear-gradient(135deg,#e0f0ff,#b3d9ff); }
.card-body { padding: 20rpx 24rpx; }
.post-title { font-size: 30rpx; font-weight: 600; color: #1a1a1a; display: block; margin-bottom: 8rpx; }
.post-snippet { font-size: 24rpx; color: #888; display: block; margin-bottom: 12rpx; }
.card-meta { display: flex; justify-content: space-between; align-items: center; }
.author-name { font-size: 24rpx; color: #666; }
.stats-row { display: flex; gap: 16rpx; }
.stat { font-size: 24rpx; color: #999; }
.load-more { text-align: center; padding: 24rpx; font-size: 26rpx; color: #1677ff; }
.loading-tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #bbb; }
</style>
