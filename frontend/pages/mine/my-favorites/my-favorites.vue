<template>
  <view class="page">
    <scroll-view scroll-y class="list" @scrolltolower="loadMore">
      <view v-if="posts.length === 0 && !loading" class="empty">
        <text>还没有收藏过攻略</text>
      </view>
      <view
        v-for="item in posts"
        :key="item.id"
        class="post-card"
        @click="goDetail(item.id)"
      >
        <image v-if="item.coverUrl" class="cover" :src="item.coverUrl" mode="aspectFill" />
        <text class="post-title">{{ item.title }}</text>
        <view class="post-meta">
          <text class="meta-text">{{ categoryMap[item.category] || item.category }}</text>
          <text class="meta-text">👍{{ item.likeCount }} 💬{{ item.commentCount }}</text>
        </view>
      </view>
      <view v-if="loading" class="tip"><text>加载中...</text></view>
      <view v-if="noMore && posts.length > 0" class="tip"><text>已经到底了</text></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchMyFavorites, type PostItem } from '@/api/post'

const categoryMap: Record<string, string> = {
  SCENIC: '景点攻略', FOOD: '美食推荐', TRANSPORT: '交通住宿',
  FREE_TRAVEL: '自由行', FAMILY: '亲子游'
}

const posts = ref<PostItem[]>([])
const page = ref(0)
const loading = ref(false)
const noMore = ref(false)

onMounted(() => load(true))

async function load(reset = false) {
  if (loading.value || noMore.value) return
  loading.value = true
  try {
    const res = await fetchMyFavorites({ page: page.value, size: 10 })
    if (res.code === 200) {
      const items = res.data.items
      posts.value = reset ? items : [...posts.value, ...items]
      if (posts.value.length >= res.data.total) noMore.value = true
      else page.value++
    }
  } finally { loading.value = false }
}

function loadMore() { load() }
function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/guide/detail/detail?id=${id}` })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; }
.list { height: 100vh; }
.empty { padding: 120rpx 0; text-align: center; font-size: 28rpx; color: #bbb; }
.post-card {
  background: #fff; margin: 16rpx 24rpx; padding: 0;
  border-radius: 16rpx; overflow: hidden;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05);
}
.cover { width: 100%; height: 240rpx; display: block; }
.post-title { display: block; font-size: 30rpx; font-weight: 600; color: #1a1a1a; padding: 20rpx 24rpx 12rpx; }
.post-meta { display: flex; justify-content: space-between; padding: 0 24rpx 20rpx; }
.meta-text { font-size: 24rpx; color: #999; }
.tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #bbb; }
</style>
