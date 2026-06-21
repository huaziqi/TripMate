<!-- frontend/pages/guide/guide.vue -->
<template>
  <view class="page">

    <!-- 分类 Tab 横向滚动 + 搜索入口 -->
    <view class="top-bar">
      <scroll-view class="category-bar" scroll-x>
        <view class="category-list">
          <view
            v-for="c in categories"
            :key="c.value"
            class="category-item"
            :class="{ active: activeCategory === c.value }"
            @click="onCategory(c.value)"
          >{{ c.label }}</view>
        </view>
      </scroll-view>
      <view class="search-icon" @click="uni.navigateTo({ url: '/pages/guide/search/search' })">
        <text class="search-icon-text">🔍</text>
      </view>
    </view>

    <!-- 排序切换 -->
    <view class="sort-bar">
      <view class="sort-pill">
        <text
          class="sort-item"
          :class="{ active: sort === 'new' }"
          @click="onSort('new')"
        >最新</text>
        <text
          class="sort-item"
          :class="{ active: sort === 'hot' }"
          @click="onSort('hot')"
        >热门</text>
      </view>
    </view>

    <!-- 帖子列表 -->
    <scroll-view
      class="feed"
      scroll-y
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view v-if="posts.length === 0 && !loading" class="empty">
        <text class="empty-text">暂无攻略，来发布第一篇吧~</text>
      </view>

      <view
        v-for="item in posts"
        :key="item.id"
        class="post-card"
        @click="goDetail(item.id)"
      >
        <!-- 封面图 -->
        <image
          v-if="item.coverUrl"
          class="cover"
          :src="item.coverUrl"
          mode="aspectFill"
        />
        <view v-else class="cover cover-placeholder" />

        <!-- 分类角标 -->
        <view class="category-badge">{{ categoryLabel(item.category) }}</view>

        <!-- 内容 -->
        <view class="card-body">
          <text class="post-title">{{ item.title }}</text>
          <view class="card-meta">
            <view class="author-row">
              <image
                v-if="item.author?.avatarUrl"
                class="avatar"
                :src="item.author.avatarUrl"
                mode="aspectFill"
              />
              <view v-else class="avatar avatar-placeholder" />
              <text class="author-name">{{ item.author?.nickname || '旅行者' }}</text>
            </view>
            <view class="stats-row">
              <text class="stat">👍 {{ item.likeCount }}</text>
              <text class="stat">💬 {{ item.commentCount }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-tip"><text>加载中...</text></view>
      <view v-if="noMore && posts.length > 0" class="loading-tip"><text>已经到底了</text></view>
      <view class="tabbar-placeholder" />
    </scroll-view>

    <!-- 发帖浮动按钮 -->
    <view class="fab" @click="goCreate">
      <text class="fab-icon">✏️</text>
    </view>

    <TabBar active="guide" />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import TabBar from '@/components/TabBar/TabBar.vue'
import { fetchPosts, type PostItem } from '@/api/post'
import { useAuth } from '@/composables/useAuth'

const { authState } = useAuth()

const categories = [
  { value: 'ALL',          label: '全部' },
  { value: 'SCENIC',       label: '景点攻略' },
  { value: 'FOOD',         label: '美食推荐' },
  { value: 'TRANSPORT',    label: '交通住宿' },
  { value: 'FREE_TRAVEL',  label: '自由行' },
  { value: 'FAMILY',       label: '亲子游' },
]

const categoryMap: Record<string, string> = Object.fromEntries(
  categories.map(c => [c.value, c.label])
)
function categoryLabel(v: string) { return categoryMap[v] || v }

const activeCategory = ref('ALL')
const sort = ref<'new' | 'hot'>('new')
const posts = ref<PostItem[]>([])
const page = ref(0)
const loading = ref(false)
const noMore = ref(false)
const refreshing = ref(false)

onMounted(() => load(true))

function onCategory(v: string) {
  if (activeCategory.value === v) return
  activeCategory.value = v
  load(true)
}

function onSort(v: 'new' | 'hot') {
  if (sort.value === v) return
  sort.value = v
  load(true)
}

async function load(reset = false) {
  if (loading.value) return
  if (reset) { page.value = 0; noMore.value = false }
  if (noMore.value) return
  loading.value = true
  try {
    const category = activeCategory.value === 'ALL' ? undefined : activeCategory.value
    const res = await fetchPosts({ category, sort: sort.value, page: page.value, size: 10 })
    if (res.code === 200) {
      const items = res.data.items
      posts.value = reset ? items : [...posts.value, ...items]
      if (posts.value.length >= res.data.total) noMore.value = true
      else page.value++
    }
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function loadMore() { load() }

function onRefresh() {
  refreshing.value = true
  load(true)
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/guide/detail/detail?id=${id}` })
}

function goCreate() {
  if (!authState.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/guide/create/create' })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; display: flex; flex-direction: column; }

/* 顶部栏（分类 + 搜索） */
.top-bar { display: flex; align-items: center; background: #fff; border-bottom: 1rpx solid #eee; flex-shrink: 0; }
.search-icon { padding: 0 24rpx; flex-shrink: 0; display: flex; align-items: center; }
.search-icon-text { font-size: 40rpx; }

/* 分类条 */
.category-bar { flex: 1; }
.category-list { display: flex; flex-direction: row; padding: 0 16rpx; white-space: nowrap; }
.category-item {
  display: inline-block;
  padding: 24rpx 24rpx;
  font-size: 28rpx;
  color: #666;
  white-space: nowrap;
  border-bottom: 4rpx solid transparent;
}
.category-item.active { color: #1677ff; border-bottom-color: #1677ff; font-weight: 600; }

/* 排序 */
.sort-bar { display: flex; justify-content: flex-end; padding: 12rpx 24rpx; background: #f7f8fa; }
.sort-pill { display: flex; background: #ebebeb; border-radius: 32rpx; overflow: hidden; }
.sort-item { padding: 10rpx 28rpx; font-size: 24rpx; color: #666; }
.sort-item.active { background: #1677ff; color: #fff; border-radius: 32rpx; }

/* Feed 滚动区 */
.feed { flex: 1; }

/* 帖子卡片 */
.post-card {
  background: #fff;
  margin: 0 24rpx 20rpx;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
  position: relative;
}
.cover { width: 100%; height: 320rpx; display: block; }
.cover-placeholder { width: 100%; height: 180rpx; background: linear-gradient(135deg,#e0f0ff,#b3d9ff); }
.category-badge {
  position: absolute; top: 16rpx; left: 16rpx;
  background: rgba(22,119,255,0.85); color: #fff;
  font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 16rpx;
}
.card-body { padding: 20rpx 24rpx 24rpx; }
.post-title { font-size: 30rpx; font-weight: 600; color: #1a1a1a; display: block; margin-bottom: 20rpx; }
.card-meta { display: flex; justify-content: space-between; align-items: center; }
.author-row { display: flex; align-items: center; gap: 12rpx; }
.avatar { width: 48rpx; height: 48rpx; border-radius: 50%; }
.avatar-placeholder { width: 48rpx; height: 48rpx; border-radius: 50%; background: #e0e0e0; }
.author-name { font-size: 24rpx; color: #666; }
.stats-row { display: flex; gap: 20rpx; }
.stat { font-size: 24rpx; color: #999; }

/* 空状态 */
.empty { padding: 120rpx 0; text-align: center; }
.empty-text { font-size: 28rpx; color: #bbb; }

/* 加载提示 */
.loading-tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #bbb; }

/* 发帖 FAB */
.fab {
  position: fixed; right: 40rpx; bottom: 180rpx;
  width: 108rpx; height: 108rpx; border-radius: 54rpx;
  background: linear-gradient(135deg,#1677ff,#4facfe);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(22,119,255,0.4);
  z-index: 100;
}
.fab-icon { font-size: 44rpx; }

.tabbar-placeholder { height: 140rpx; }
</style>
