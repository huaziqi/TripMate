<template>
  <view class="page">
    <view class="header">
      <text class="title" :style="{ fontSize: rpx(40) }">个性化路线推荐</text>
      <text class="subtitle" :style="{ fontSize: rpx(26) }">灵山胜境 · 根据你的兴趣推荐游览路线与讲解重点</text>
    </view>

    <!-- ================= 兴趣问卷 ================= -->
    <view v-if="showQuiz" class="quiz-card">
      <view class="quiz-section">
        <text class="quiz-title" :style="{ fontSize: rpx(31) }">你的兴趣是？</text>
        <text class="quiz-sub" :style="{ fontSize: rpx(24) }">可多选，我们会为你匹配路线和讲解重点</text>

        <view class="chip-grid">
          <view
            v-for="opt in options"
            :key="opt.key"
            class="chip"
            :class="{ active: selectedInterests.includes(opt.key) }"
            @tap="toggleInterest(opt.key)"
          >
            <text class="chip-icon">{{ opt.icon }}</text>
            <view class="chip-body">
              <text class="chip-label" :style="{ fontSize: rpx(27) }">{{ opt.label }}</text>
              <text class="chip-desc" :style="{ fontSize: rpx(21) }">{{ opt.description }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="quiz-section">
        <text class="quiz-title" :style="{ fontSize: rpx(31) }">计划游玩多久？</text>
        <view class="radio-row">
          <view
            v-for="opt in durationOptions"
            :key="opt.value"
            class="radio-pill"
            :class="{ active: duration === opt.value }"
            @tap="duration = opt.value"
          >
            <text class="radio-label" :style="{ fontSize: rpx(26) }">{{ opt.label }}</text>
            <text class="radio-desc" :style="{ fontSize: rpx(21) }">{{ opt.desc }}</text>
          </view>
        </view>
      </view>

      <view class="quiz-section">
        <text class="quiz-title" :style="{ fontSize: rpx(31) }">和谁一起来？</text>
        <view class="radio-row">
          <view
            v-for="opt in companionOptions"
            :key="opt.value"
            class="radio-pill"
            :class="{ active: companions === opt.value }"
            @tap="companions = opt.value"
          >
            <text class="radio-label" :style="{ fontSize: rpx(26) }">{{ opt.label }}</text>
          </view>
        </view>
      </view>

      <view class="quiz-section">
        <text class="quiz-title" :style="{ fontSize: rpx(31) }">体力状况？</text>
        <text class="quiz-sub" :style="{ fontSize: rpx(24) }">灵山大佛有216级登云道，我们会据此安排</text>
        <view class="radio-row">
          <view
            v-for="opt in staminaOptions"
            :key="opt.value"
            class="radio-pill"
            :class="{ active: stamina === opt.value }"
            @tap="stamina = opt.value"
          >
            <text class="radio-label" :style="{ fontSize: rpx(26) }">{{ opt.label }}</text>
          </view>
        </view>
      </view>

      <view class="quiz-section">
        <text class="quiz-title" :style="{ fontSize: rpx(31) }">还想补充点什么？（选填）</text>
        <textarea
          v-model="freeText"
          class="free-text"
          placeholder="例如：我喜欢拍照，对佛教历史也很感兴趣"
          :maxlength="100"
        />
      </view>

      <button class="submit-btn" :style="{ fontSize: rpx(30) }" :disabled="loading" @tap="submitQuiz">
        {{ loading ? '正在生成...' : '生成我的专属路线' }}
      </button>

      <text class="quiz-hint" :style="{ fontSize: rpx(22) }">
        登录后推荐更准：我们还会参考你的收藏、浏览足迹和与数字人的对话内容
      </text>
    </view>

    <!-- ================= 推荐结果 ================= -->
    <template v-else>
      <!-- 兴趣画像卡片 -->
      <view v-if="result" class="profile-card">
        <view class="profile-head">
          <text class="profile-title" :style="{ fontSize: rpx(32) }">我的兴趣画像</text>
          <view class="reset-btn" @tap="reopenQuiz">
            <text class="reset-text" :style="{ fontSize: rpx(24) }">重新设置</text>
          </view>
        </view>

        <view class="weight-list">
          <view
            v-for="(percent, label) in result.interestWeights"
            :key="label"
            class="weight-row"
          >
            <text class="weight-label" :style="{ fontSize: rpx(24) }">{{ label }}</text>
            <view class="weight-bar-bg">
              <view class="weight-bar" :style="{ width: percent + '%' }" />
            </view>
            <text class="weight-percent" :style="{ fontSize: rpx(23) }">{{ percent }}%</text>
          </view>
        </view>

        <view class="summary-list">
          <text
            v-for="(line, index) in result.profileSummary"
            :key="index"
            class="summary-line"
            :style="{ fontSize: rpx(23) }"
          >
            · {{ line }}
          </text>
        </view>
      </view>

      <view v-if="currentSpotName" class="tip-box">
        <text class="tip-sub" :style="{ fontSize: rpx(24) }">当前关联景点：{{ currentSpotName }}</text>
      </view>

      <view v-if="loading" class="status" :style="{ fontSize: rpx(28) }">正在为你生成专属路线...</view>

      <view v-else-if="displayRoutes.length === 0" class="status" :style="{ fontSize: rpx(28) }">
        暂无推荐路线
        <view class="retry-btn" @tap="loadRecommendation">
          <text class="retry-text" :style="{ fontSize: rpx(26) }">重新加载</text>
        </view>
      </view>

      <view v-else class="route-list">
        <view
          v-for="(route, routeIndex) in displayRoutes"
          :key="route.id"
          class="route-card"
        >
          <!-- 标题行 + 匹配度 -->
          <view class="route-top">
            <view class="route-title-wrap">
              <text class="route-name" :style="{ fontSize: rpx(34) }">{{ route.name }}</text>
              <text class="route-desc" :style="{ fontSize: rpx(25) }">{{ route.description }}</text>
            </view>

            <view
              v-if="route.matchScore > 0"
              class="match-badge"
              :class="matchClass(route.matchScore)"
            >
              <text class="match-num" :style="{ fontSize: rpx(32) }">{{ route.matchScore }}%</text>
              <text class="match-text" :style="{ fontSize: rpx(20) }">匹配</text>
            </view>
          </view>

          <!-- 标签 -->
          <view class="tag-row">
            <view v-if="routeIndex === 0 && route.matchScore > 0" class="tag best" :style="{ fontSize: rpx(23) }">
              最适合你
            </view>
            <view v-for="tag in route.tags" :key="tag" class="tag" :style="{ fontSize: rpx(23) }">{{ tag }}</view>
          </view>

          <view class="meta-row">
            <text class="meta" :style="{ fontSize: rpx(24) }">预计时间：{{ route.estimatedTime }}</text>
            <text v-if="route.suitableFor" class="meta suitable" :style="{ fontSize: rpx(24) }">{{ route.suitableFor }}</text>
          </view>

          <!-- 推荐理由 -->
          <view v-if="route.matchReasons.length > 0" class="reason-list">
            <text
              v-for="(reason, index) in route.matchReasons"
              :key="index"
              class="reason"
              :style="{ fontSize: rpx(24) }"
            >
              ✓ {{ reason }}
            </text>
          </view>

          <!-- 景点串联 -->
          <view class="spot-chain">
            <template v-for="(spot, index) in route.spots" :key="index">
              <view
                class="spot-pill"
                :class="{ disabled: !spot.matched }"
                :style="{ fontSize: rpx(25) }"
                @tap="goSpotDetail(spot)"
              >
                {{ spot.displayName }}
              </view>
              <text v-if="index < route.spots.length - 1" class="arrow">→</text>
            </template>
          </view>

          <!-- 讲解重点（可展开） -->
          <view
            v-if="hasFocus(route)"
            class="focus-toggle"
            @tap="toggleFocus(route.id)"
          >
            <text class="focus-toggle-text" :style="{ fontSize: rpx(25) }">
              {{ expandedRoutes[route.id] ? '收起讲解重点 ▲' : '查看各景点讲解重点 ▼' }}
            </text>
          </view>

          <view v-if="expandedRoutes[route.id]" class="focus-list">
            <view
              v-for="(spot, index) in route.spots"
              :key="index"
              class="focus-item"
            >
              <view class="focus-head">
                <text class="focus-spot" :style="{ fontSize: rpx(26) }">{{ index + 1 }}. {{ spot.displayName }}</text>
                <view v-if="spot.focusLabel" class="focus-label" :style="{ fontSize: rpx(21) }">
                  {{ spot.focusLabel }}
                </view>
              </view>
              <text v-if="spot.focusText" class="focus-text" :style="{ fontSize: rpx(24) }">{{ spot.focusText }}</text>
            </view>
          </view>

          <!-- 路线讲解 -->
          <view class="guide-box">
            <text class="guide-title" :style="{ fontSize: rpx(27) }">路线讲解</text>
            <text class="guide-text" :style="{ fontSize: rpx(25) }">{{ route.guideText }}</text>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useElder } from '@/composables/useElder'
import {
  getInterestOptions,
  getPersonalizedRoutes,
  getRecommendRoutes
} from '@/api/route'
import type {
  InterestOption,
  PersonalizedRecommendation,
  PersonalizedRoute,
  PersonalizedRouteSpot,
  PersonalizeRequest
} from '@/api/route'

const PROFILE_STORAGE_KEY = 'interest_profile'

const { rpx } = useElder()

// 兜底问卷选项：接口失败时使用，与后端保持一致
const FALLBACK_OPTIONS: InterestOption[] = [
  { key: 'history', label: '历史文化', description: '千年古刹、玄奘典故、佛教东传', icon: '📜' },
  { key: 'buddhist_art', label: '佛教艺术', description: '琉璃壁画、木雕、曼荼罗与石刻', icon: '🎨' },
  { key: 'nature', label: '自然风光', description: '太湖碧波、菩提林荫、四季花海', icon: '🏞️' },
  { key: 'architecture', label: '建筑美学', description: '梵宫、藏式坛城、南传白塔', icon: '🏛️' },
  { key: 'blessing', label: '祈福体验', description: '接圣水、撞钟、摸佛掌、抱佛脚', icon: '🙏' },
  { key: 'family', label: '亲子同游', description: '动态表演、互动展馆', icon: '👨‍👩‍👧' },
  { key: 'photography', label: '摄影打卡', description: '佛光夕照、星空穹顶、灯光秀', icon: '📷' },
  { key: 'zen', label: '禅修静心', description: '抄经、禅茶、慢生活', icon: '🧘' }
]

const durationOptions = [
  { value: 'half', label: '半天以内', desc: '≤3小时' },
  { value: 'most', label: '大半天', desc: '4-5小时' },
  { value: 'full', label: '一整天', desc: '6小时以上' }
]

const companionOptions = [
  { value: 'solo', label: '独自出行' },
  { value: 'partner', label: '情侣/朋友' },
  { value: 'kids', label: '带小孩' },
  { value: 'elder', label: '带老人' }
]

const staminaOptions = [
  { value: 'low', label: '轻松为主' },
  { value: 'medium', label: '一般' },
  { value: 'high', label: '体力充沛' }
]

const options = ref<InterestOption[]>(FALLBACK_OPTIONS)

const showQuiz = ref(false)
const selectedInterests = ref<string[]>([])
const duration = ref('most')
const companions = ref('solo')
const stamina = ref('medium')
const freeText = ref('')

const loading = ref(false)
const result = ref<PersonalizedRecommendation | null>(null)
const fallbackRoutes = ref<PersonalizedRoute[]>([])
const expandedRoutes = reactive<Record<string, boolean>>({})

const currentSpotName = ref('')

const displayRoutes = computed<PersonalizedRoute[]>(() => {
  const routes = result.value ? result.value.routes : fallbackRoutes.value

  if (!currentSpotName.value.trim()) {
    return routes
  }

  const keyword = currentSpotName.value.trim()
  const matched = routes.filter(route =>
    route.spots.some(
      spot =>
        spot.displayName.includes(keyword) || spot.name.includes(keyword)
    )
  )

  return matched.length > 0 ? matched : routes
})

onLoad((query) => {
  if (query?.spotName) {
    currentSpotName.value = decodeURIComponent(query.spotName)
  }
})

onShow(() => {
  loadOptions()

  const saved = loadSavedProfile()
  if (saved) {
    applyProfile(saved)
    loadRecommendation()
  } else {
    showQuiz.value = true
  }
})

// ---------- 问卷 ----------

async function loadOptions() {
  try {
    const list = await getInterestOptions()
    if (list && list.length > 0) {
      options.value = list
    }
  } catch (error) {
    console.warn('加载兴趣选项失败，使用内置选项：', error)
  }
}

function toggleInterest(key: string) {
  const index = selectedInterests.value.indexOf(key)
  if (index >= 0) {
    selectedInterests.value.splice(index, 1)
  } else {
    selectedInterests.value.push(key)
  }
}

function buildProfile(): PersonalizeRequest {
  return {
    interests: [...selectedInterests.value],
    duration: duration.value,
    companions: companions.value,
    stamina: stamina.value,
    freeText: freeText.value.trim() || undefined
  }
}

function loadSavedProfile(): PersonalizeRequest | null {
  try {
    const raw = uni.getStorageSync(PROFILE_STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as PersonalizeRequest
  } catch {
    return null
  }
}

function applyProfile(profile: PersonalizeRequest) {
  selectedInterests.value = profile.interests ?? []
  duration.value = profile.duration || 'most'
  companions.value = profile.companions || 'solo'
  stamina.value = profile.stamina || 'medium'
  freeText.value = profile.freeText ?? ''
}

async function submitQuiz() {
  if (selectedInterests.value.length === 0 && !freeText.value.trim()) {
    uni.showToast({ title: '至少选一个兴趣或写一句描述', icon: 'none' })
    return
  }

  const profile = buildProfile()
  uni.setStorageSync(PROFILE_STORAGE_KEY, JSON.stringify(profile))
  showQuiz.value = false
  await loadRecommendation()
}

function reopenQuiz() {
  showQuiz.value = true
}

// ---------- 推荐 ----------

async function loadRecommendation() {
  loading.value = true

  try {
    result.value = await getPersonalizedRoutes(buildProfile())

    // 默认展开匹配度最高的路线的讲解重点
    const top = result.value.routes[0]
    if (top) {
      expandedRoutes[top.id] = true
    }
  } catch (error) {
    console.error('个性化推荐失败，回退到通用推荐：', error)
    result.value = null
    await loadFallbackRoutes()
  } finally {
    loading.value = false
  }
}

/** 个性化接口异常时回退到通用推荐 */
async function loadFallbackRoutes() {
  try {
    const routes = await getRecommendRoutes()
    fallbackRoutes.value = routes.map(route => ({
      ...route,
      matchScore: 0,
      matchReasons: [],
      tags: [route.theme],
      suitableFor: '',
      spots: route.spots.map<PersonalizedRouteSpot>(spot => ({
        ...spot,
        focusText: '',
        focusLabel: null
      }))
    }))
  } catch (error) {
    console.error('加载通用推荐也失败：', error)
    uni.showToast({ title: '加载路线失败', icon: 'none' })
  }
}

function matchClass(score: number) {
  if (score >= 75) return 'hot'
  if (score >= 50) return 'good'
  return 'normal'
}

function hasFocus(route: PersonalizedRoute) {
  return route.spots.some(spot => !!spot.focusText)
}

function toggleFocus(routeId: string) {
  expandedRoutes[routeId] = !expandedRoutes[routeId]
}

function goSpotDetail(spot: PersonalizedRouteSpot) {
  if (!spot.matched || !spot.spotId) {
    uni.showToast({ title: '该景点暂未配置', icon: 'none' })
    return
  }

  uni.navigateTo({
    url: `/pages/spot-detail/spot-detail?id=${spot.spotId}`
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f7;
  padding: 28rpx;
  box-sizing: border-box;
}

.header {
  margin-bottom: 28rpx;
}

.title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #222;
  margin-bottom: 12rpx;
}

.subtitle {
  display: block;
  font-size: 26rpx;
  color: #777;
  line-height: 1.5;
}

/* ---------- 问卷 ---------- */

.quiz-card {
  background: #ffffff;
  border-radius: 28rpx;
  padding: 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.quiz-section {
  margin-bottom: 36rpx;
}

.quiz-title {
  display: block;
  font-size: 31rpx;
  font-weight: 700;
  color: #222;
  margin-bottom: 8rpx;
}

.quiz-sub {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 16rpx;
}

.chip-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 12rpx;
}

.chip {
  display: flex;
  align-items: center;
  gap: 12rpx;
  width: calc(50% - 8rpx);
  box-sizing: border-box;
  padding: 18rpx 20rpx;
  border-radius: 20rpx;
  background: #f5f7fa;
  border: 2rpx solid transparent;
}

.chip.active {
  background: #eef4ff;
  border-color: #1677ff;
}

.chip-icon {
  font-size: 40rpx;
  flex-shrink: 0;
}

.chip-body {
  min-width: 0;
}

.chip-label {
  display: block;
  font-size: 27rpx;
  font-weight: 600;
  color: #333;
}

.chip.active .chip-label {
  color: #1677ff;
}

.chip-desc {
  display: block;
  font-size: 21rpx;
  color: #999;
  margin-top: 4rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.radio-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 12rpx;
}

.radio-pill {
  padding: 14rpx 26rpx;
  border-radius: 999rpx;
  background: #f5f7fa;
  border: 2rpx solid transparent;
  text-align: center;
}

.radio-pill.active {
  background: #eef4ff;
  border-color: #1677ff;
}

.radio-label {
  display: block;
  font-size: 26rpx;
  color: #333;
  font-weight: 600;
}

.radio-pill.active .radio-label {
  color: #1677ff;
}

.radio-desc {
  display: block;
  font-size: 21rpx;
  color: #999;
  margin-top: 2rpx;
}

.free-text {
  width: 100%;
  min-height: 120rpx;
  box-sizing: border-box;
  margin-top: 12rpx;
  padding: 20rpx;
  border-radius: 20rpx;
  background: #f5f7fa;
  font-size: 26rpx;
  color: #333;
}

.submit-btn {
  margin-top: 10rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  background: #1677ff;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
}

.submit-btn[disabled] {
  opacity: 0.6;
}

.quiz-hint {
  display: block;
  margin-top: 20rpx;
  font-size: 22rpx;
  color: #999;
  text-align: center;
  line-height: 1.6;
}

/* ---------- 画像卡片 ---------- */

.profile-card {
  background: #ffffff;
  border-radius: 28rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.profile-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.profile-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #222;
}

.reset-btn {
  padding: 8rpx 22rpx;
  border-radius: 999rpx;
  background: #eef4ff;
}

.reset-text {
  font-size: 24rpx;
  color: #1677ff;
}

.weight-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin-bottom: 20rpx;
}

.weight-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.weight-label {
  width: 130rpx;
  flex-shrink: 0;
  font-size: 24rpx;
  color: #555;
}

.weight-bar-bg {
  flex: 1;
  height: 16rpx;
  border-radius: 999rpx;
  background: #f0f2f5;
  overflow: hidden;
}

.weight-bar {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #4d9cff, #1677ff);
}

.weight-percent {
  width: 64rpx;
  flex-shrink: 0;
  text-align: right;
  font-size: 23rpx;
  color: #1677ff;
  font-weight: 600;
}

.summary-list {
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
}

.summary-line {
  display: block;
  font-size: 23rpx;
  color: #888;
  line-height: 1.7;
}

/* ---------- 结果列表 ---------- */

.tip-box {
  margin-bottom: 24rpx;
  padding: 20rpx 24rpx;
  border-radius: 20rpx;
  background: #eef6ff;
}

.tip-sub {
  font-size: 24rpx;
  color: #1677ff;
}

.status {
  padding-top: 120rpx;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}

.retry-btn {
  margin: 30rpx auto 0;
  width: 200rpx;
  padding: 14rpx 0;
  border-radius: 999rpx;
  background: #1677ff;
}

.retry-text {
  color: #fff;
  font-size: 26rpx;
}

.route-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.route-card {
  background: #ffffff;
  border-radius: 28rpx;
  padding: 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.route-top {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 16rpx;
}

.route-title-wrap {
  flex: 1;
  min-width: 0;
}

.route-name {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #222;
  margin-bottom: 10rpx;
}

.route-desc {
  display: block;
  font-size: 25rpx;
  color: #666;
  line-height: 1.5;
}

.match-badge {
  flex-shrink: 0;
  align-self: flex-start;
  padding: 12rpx 20rpx;
  border-radius: 20rpx;
  text-align: center;
}

.match-badge.hot {
  background: #fff1e8;
}

.match-badge.good {
  background: #eef4ff;
}

.match-badge.normal {
  background: #f5f5f5;
}

.match-num {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
}

.match-badge.hot .match-num {
  color: #ff6b00;
}

.match-badge.good .match-num {
  color: #1677ff;
}

.match-badge.normal .match-num {
  color: #999;
}

.match-text {
  display: block;
  font-size: 20rpx;
  color: #999;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.tag {
  height: 44rpx;
  line-height: 44rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #eef4ff;
  color: #1677ff;
  font-size: 23rpx;
}

.tag.best {
  background: #ff6b00;
  color: #ffffff;
  font-weight: 600;
}

.meta-row {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  margin-bottom: 16rpx;
}

.meta {
  font-size: 24rpx;
  color: #999;
}

.meta.suitable {
  color: #19a15f;
}

.reason-list {
  margin-bottom: 20rpx;
  padding: 18rpx 22rpx;
  border-radius: 18rpx;
  background: #f6fff9;
}

.reason {
  display: block;
  font-size: 24rpx;
  color: #19a15f;
  line-height: 1.7;
}

.spot-chain {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.spot-pill {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #eaf8ef;
  color: #19a15f;
  font-size: 25rpx;
}

.spot-pill.disabled {
  background: #f1f1f1;
  color: #aaa;
}

.arrow {
  color: #aaa;
  font-size: 26rpx;
}

.focus-toggle {
  margin-bottom: 16rpx;
  text-align: center;
}

.focus-toggle-text {
  font-size: 25rpx;
  color: #1677ff;
}

.focus-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-bottom: 20rpx;
  padding: 22rpx;
  border-radius: 20rpx;
  background: #f8faff;
}

.focus-item {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.focus-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.focus-spot {
  font-size: 26rpx;
  font-weight: 600;
  color: #333;
}

.focus-label {
  padding: 2rpx 14rpx;
  border-radius: 999rpx;
  background: #eef4ff;
  color: #1677ff;
  font-size: 21rpx;
}

.focus-text {
  font-size: 24rpx;
  color: #666;
  line-height: 1.6;
}

.guide-box {
  padding: 22rpx;
  border-radius: 20rpx;
  background: #fafafa;
}

.guide-title {
  display: block;
  font-size: 27rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.guide-text {
  display: block;
  font-size: 25rpx;
  color: #666;
  line-height: 1.6;
}
</style>
