<!-- frontend/pages/guide/detail/detail.vue -->
<template>
  <view class="page">

    <!-- 图片轮播 -->
    <swiper v-if="post && post.imageUrls && post.imageUrls.length > 0"
      class="swiper" indicator-dots autoplay circular>
      <swiper-item v-for="(url, i) in post.imageUrls" :key="i">
        <image class="swiper-img" :src="url" mode="aspectFill" />
      </swiper-item>
    </swiper>

    <scroll-view class="content-scroll" scroll-y>

      <view v-if="!post" class="loading-wrap"><text>加载中...</text></view>

      <view v-else class="content">

        <!-- 作者信息 -->
        <view class="author-row">
          <image v-if="post.author?.avatarUrl" class="avatar" :src="post.author.avatarUrl" mode="aspectFill" />
          <view v-else class="avatar avatar-placeholder" />
          <view class="author-info">
            <text class="author-name">{{ post.author?.nickname || '旅行者' }}</text>
            <text class="post-time">{{ formatTime(post.createdAt) }} · {{ categoryLabel(post.category) }}</text>
          </view>
        </view>

        <!-- 标题 + 正文 -->
        <text class="post-title">{{ post.title }}</text>
        <text class="post-content">{{ post.content }}</text>

        <!-- 统计 -->
        <view class="stat-row">
          <text class="stat-item">👁 {{ post.viewCount }} 阅读</text>
          <text class="stat-item">👍 {{ post.likeCount }} 点赞</text>
          <text class="stat-item">💬 {{ post.commentCount }} 评论</text>
        </view>

        <!-- 评论列表 -->
        <view class="section-title">全部评论</view>
        <view v-if="comments.length === 0" class="no-comment"><text>暂无评论，快来说两句~</text></view>
        <view v-for="c in comments" :key="c.id" class="comment-item">
          <image v-if="c.author?.avatarUrl" class="c-avatar" :src="c.author.avatarUrl" mode="aspectFill" />
          <view v-else class="c-avatar c-avatar-placeholder" />
          <view class="c-body">
            <text class="c-name">{{ c.author?.nickname || '旅行者' }}</text>
            <text class="c-content">{{ c.content }}</text>
            <text class="c-time">{{ formatTime(c.createdAt) }}</text>
          </view>
        </view>
        <view v-if="commentNoMore && comments.length > 0" class="load-more-tip"><text>已显示全部评论</text></view>
        <button v-if="!commentNoMore && comments.length > 0" class="load-more-btn" @click="loadMoreComments">加载更多评论</button>

        <view class="bottom-placeholder" />
      </view>
    </scroll-view>

    <!-- 底部固定操作栏 -->
    <view class="action-bar">
      <view class="comment-input-wrap" @click="focusComment">
        <text class="comment-placeholder">说点什么...</text>
      </view>
      <view class="action-btn" @click="onLike">
        <text>{{ post?.liked ? '❤️' : '🤍' }}</text>
        <text class="action-label">{{ post?.likeCount || 0 }}</text>
      </view>
      <view class="action-btn" @click="onFavorite">
        <text>{{ post?.favorited ? '⭐' : '☆' }}</text>
        <text class="action-label">收藏</text>
      </view>
    </view>

    <!-- 评论输入 popup -->
    <view v-if="showInput" class="comment-popup" @click.stop>
      <view class="comment-popup-mask" @click="showInput = false" />
      <view class="comment-popup-box">
        <textarea
          v-model="commentText"
          class="comment-textarea"
          placeholder="说点什么..."
          :maxlength="500"
          auto-focus
        />
        <view class="comment-popup-footer">
          <text class="char-count">{{ commentText.length }}/500</text>
          <button class="submit-btn" :disabled="submitting" @click="submitComment">发送</button>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchPostDetail, fetchComments, createComment, toggleLike, toggleFavorite, type PostItem, type CommentItem } from '@/api/post'
import { useAuth } from '@/composables/useAuth'

const { authState } = useAuth()

const categoryMap: Record<string, string> = {
  SCENIC: '景点攻略', FOOD: '美食推荐', TRANSPORT: '交通住宿',
  FREE_TRAVEL: '自由行', FAMILY: '亲子游'
}
function categoryLabel(v: string) { return categoryMap[v] || v }

function formatTime(s: string) {
  if (!s) return ''
  return s.slice(0, 16).replace('T', ' ')
}

const postId = ref(0)
const post = ref<PostItem | null>(null)
const comments = ref<CommentItem[]>([])
const commentPage = ref(0)
const commentNoMore = ref(false)
const showInput = ref(false)
const commentText = ref('')
const submitting = ref(false)

onMounted(() => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as any
  postId.value = Number(current.options?.id || 0)
  if (postId.value) {
    loadDetail()
    loadComments(true)
  }
})

async function loadDetail() {
  const res = await fetchPostDetail(postId.value)
  if (res.code === 200) post.value = res.data
}

async function loadComments(reset = false) {
  if (reset) { commentPage.value = 0; commentNoMore.value = false }
  const res = await fetchComments(postId.value, { page: commentPage.value, size: 20 })
  if (res.code === 200) {
    const items = res.data.items
    comments.value = reset ? items : [...comments.value, ...items]
    if (comments.value.length >= res.data.total) commentNoMore.value = true
    else commentPage.value++
  }
}

function loadMoreComments() { loadComments() }

function focusComment() {
  if (!authState.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  showInput.value = true
}

async function submitComment() {
  if (!commentText.value.trim()) return
  submitting.value = true
  try {
    const res = await createComment(postId.value, commentText.value)
    if (res.code === 200) {
      comments.value.unshift(res.data)
      if (post.value) post.value.commentCount++
      commentText.value = ''
      showInput.value = false
      uni.showToast({ title: '评论成功', icon: 'success' })
    }
  } finally {
    submitting.value = false
  }
}

async function onLike() {
  if (!authState.isLoggedIn) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  if (!post.value) return
  const res = await toggleLike(postId.value)
  if (res.code === 200 && post.value) {
    post.value.liked = res.data.liked
    post.value.likeCount = res.data.likeCount
  }
}

async function onFavorite() {
  if (!authState.isLoggedIn) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  if (!post.value) return
  const res = await toggleFavorite(postId.value)
  if (res.code === 200 && post.value) {
    post.value.favorited = res.data.favorited
    uni.showToast({ title: post.value.favorited ? '已收藏' : '已取消收藏', icon: 'none' })
  }
}
</script>

<style scoped>
.page { height: 100vh; display: flex; flex-direction: column; background: #fff; }
.swiper { height: 440rpx; flex-shrink: 0; }
.swiper-img { width: 100%; height: 100%; }
.content-scroll { flex: 1; overflow: hidden; }
.loading-wrap { padding: 80rpx; text-align: center; color: #bbb; }

.content { padding: 32rpx 32rpx 0; }
.author-row { display: flex; align-items: center; gap: 16rpx; margin-bottom: 24rpx; }
.avatar { width: 64rpx; height: 64rpx; border-radius: 50%; }
.avatar-placeholder { width: 64rpx; height: 64rpx; border-radius: 50%; background: #e0e0e0; }
.author-info { display: flex; flex-direction: column; }
.author-name { font-size: 28rpx; font-weight: 600; color: #1a1a1a; }
.post-time { font-size: 22rpx; color: #999; margin-top: 4rpx; }

.post-title { display: block; font-size: 36rpx; font-weight: 700; color: #1a1a1a; margin-bottom: 20rpx; }
.post-content { display: block; font-size: 28rpx; color: #333; line-height: 1.8; white-space: pre-wrap; }

.stat-row { display: flex; gap: 24rpx; padding: 24rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.stat-item { font-size: 24rpx; color: #999; }

.section-title { font-size: 30rpx; font-weight: 600; color: #1a1a1a; padding: 24rpx 0 16rpx; }
.no-comment { padding: 40rpx 0; text-align: center; font-size: 26rpx; color: #bbb; }

.comment-item { display: flex; gap: 16rpx; padding: 20rpx 0; border-bottom: 1rpx solid #f9f9f9; }
.c-avatar { width: 52rpx; height: 52rpx; border-radius: 50%; flex-shrink: 0; }
.c-avatar-placeholder { width: 52rpx; height: 52rpx; border-radius: 50%; background: #e0e0e0; flex-shrink: 0; }
.c-body { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.c-name { font-size: 24rpx; font-weight: 600; color: #555; }
.c-content { font-size: 28rpx; color: #1a1a1a; }
.c-time { font-size: 22rpx; color: #bbb; }

.load-more-tip { text-align: center; padding: 20rpx; font-size: 24rpx; color: #bbb; }
.load-more-btn { margin: 16rpx 0; background: #f5f5f5; color: #666; font-size: 26rpx; border: none; border-radius: 8rpx; }
.bottom-placeholder { height: 160rpx; }

/* 底部操作栏 */
.action-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  background: #fff; border-top: 1rpx solid #eee;
  display: flex; align-items: center; padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  gap: 16rpx;
}
.comment-input-wrap {
  flex: 1; background: #f5f5f5; border-radius: 32rpx;
  padding: 16rpx 24rpx; height: 64rpx;
  display: flex; align-items: center;
}
.comment-placeholder { font-size: 26rpx; color: #bbb; }
.action-btn { display: flex; flex-direction: column; align-items: center; padding: 0 12rpx; }
.action-label { font-size: 20rpx; color: #666; margin-top: 4rpx; }

/* 评论弹窗 */
.comment-popup { position: fixed; inset: 0; z-index: 999; display: flex; flex-direction: column; justify-content: flex-end; }
.comment-popup-mask { flex: 1; background: rgba(0,0,0,0.4); }
.comment-popup-box { background: #fff; padding: 24rpx; padding-bottom: env(safe-area-inset-bottom); }
.comment-textarea { width: 100%; height: 180rpx; background: #f5f5f5; border-radius: 12rpx; padding: 16rpx; font-size: 28rpx; box-sizing: border-box; }
.comment-popup-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 16rpx; }
.char-count { font-size: 22rpx; color: #bbb; }
.submit-btn { background: #1677ff; color: #fff; border: none; border-radius: 32rpx; padding: 0 40rpx; height: 64rpx; line-height: 64rpx; font-size: 28rpx; }
</style>
