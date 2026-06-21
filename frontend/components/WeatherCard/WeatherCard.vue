<template>
  <view class="weather-card">

    <view class="top">
      <text class="city" :style="{ fontSize: rpx(34) }">
        {{ city }}
      </text>
      <text class="update-time" :style="{ fontSize: rpx(22) }">
        {{ reporttime }}
      </text>
    </view>

    <view class="content">

      <view class="left">
        <text class="temp" :style="{ fontSize: rpx(96) }">
          {{ temperature }}°
        </text>
        <text class="weather-label" :style="{ fontSize: rpx(32) }">
          {{ displayWeather }}
        </text>
      </view>

      <view class="right">
        <view class="info-item">
          <text :style="{ fontSize: rpx(26) }">{{ t('weather.card.windDirection') }}</text>
          <text :style="{ fontSize: rpx(26) }">{{ winddirection }}</text>
        </view>
        <view class="info-item">
          <text :style="{ fontSize: rpx(26) }">{{ t('weather.card.windPower') }}</text>
          <text :style="{ fontSize: rpx(26) }">{{ windpower }}{{ t('weather.card.windPowerUnit') }}</text>
        </view>
        <view class="info-item">
          <text :style="{ fontSize: rpx(26) }">{{ t('weather.card.humidity') }}</text>
          <text :style="{ fontSize: rpx(26) }">{{ humidity }}%</text>
        </view>
      </view>

    </view>

  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchWeather } from '@/api/weather'
import { useElder } from '@/composables/useElder'
import type { WeatherData } from '@/types/weather'

const { t } = useI18n()
const { rpx } = useElder()

// ----------------------------------------------------------------
// 定位状态（用枚举替代硬编码文字，切换语言后自动更新）
// ----------------------------------------------------------------
type LocationStatus = 'locating' | 'failed' | 'done'
const locationStatus = ref<LocationStatus>('locating')
const cityName       = ref('')

const city = computed(() => {
  if (locationStatus.value === 'locating') return t('weather.card.locating')
  if (locationStatus.value === 'failed')   return t('weather.card.locateFailed')
  return cityName.value
})

// ----------------------------------------------------------------
// 天气数据
// ----------------------------------------------------------------
const weather      = ref('--')
const temperature  = ref('--')
const winddirection = ref('--')
const windpower    = ref('--')
const humidity     = ref('--')
const reporttime   = ref('--')

// 天气状况国际化：以后端返回的中文字符串作为 i18n key 查询译文
// 第二个参数为 fallback，key 不存在时原样显示
const displayWeather = computed(() =>
  weather.value === '--'
    ? '--'
    : t(`weather.condition.${weather.value}`, weather.value)
)

// ----------------------------------------------------------------
// 生命周期
// ----------------------------------------------------------------
onMounted(() => {
  getLocation()
})

// ----------------------------------------------------------------
// 方法
// ----------------------------------------------------------------
function getLocation() {
  uni.getLocation({
    type: 'gcj02',
    success: ({ longitude, latitude }) => loadWeather(longitude, latitude),
    fail: () => { locationStatus.value = 'failed' }
  })
}

async function loadWeather(longitude: number, latitude: number) {
  try {
    const res = await fetchWeather(longitude, latitude)
    applyData(res.data)
    locationStatus.value = 'done'
  } catch {
    locationStatus.value = 'failed'
  }
}

function applyData(data: WeatherData) {
  cityName.value      = data.city
  weather.value       = data.weather
  temperature.value   = data.temperature
  winddirection.value = data.winddirection
  windpower.value     = data.windpower
  humidity.value      = data.humidity
  reporttime.value    = data.reporttime
}
</script>

<style scoped>
.weather-card {
  background: linear-gradient(135deg, #4facfe, #00c6fb);
  border-radius: 24rpx;
  padding: 32rpx;
  color: white;
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.08);
}

.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.city {
  font-weight: 600;
}

.update-time {
  opacity: 0.8;
}

.content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left {
  display: flex;
  flex-direction: column;
}

.temp {
  font-weight: bold;
  line-height: 1;
}

.weather-label {
  margin-top: 12rpx;
}

.right {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.info-item {
  display: flex;
  gap: 16rpx;
  justify-content: space-between;
}
</style>
