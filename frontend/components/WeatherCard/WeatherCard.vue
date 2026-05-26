<template>
  <view class="weather-card">

    <view class="top">
      <text class="city">{{ city }}</text>
      <text class="update-time">
        {{ reportTime }}
      </text>
    </view>

    <view class="content">

      <view class="left">
        <text class="temp">
          {{ temperature }}°
        </text>

        <text class="weather">
          {{ weather }}
        </text>
      </view>

      <view class="right">

        <view class="info-item">
          <text>风向</text>
          <text>
            {{ winddirection }}
          </text>
        </view>

        <view class="info-item">
          <text>风力</text>
          <text>
            {{ windpower }}级
          </text>
        </view>

        <view class="info-item">
          <text>湿度</text>
          <text>
            {{ humidity }}%
          </text>
        </view>

      </view>

    </view>

  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const city = ref('定位中...')
const weather = ref('--')
const temperature = ref('--')

const winddirection = ref('--')
const windpower = ref('--')

const humidity = ref('--')
const reportTime = ref('--')

const AMAP_KEY = '你的高德KEY'

onMounted(() => {
  getLocation()
})

function getLocation() {

  uni.getLocation({
    type: 'gcj02',

    success: async (res) => {

      const { longitude, latitude } = res

      try {

        const geoRes: any = await uni.request({
          url: 'https://restapi.amap.com/v3/geocode/regeo',

          data: {
            key: AMAP_KEY,
            location: `${longitude},${latitude}`
          }
        })

        const cityName =
          geoRes.data.regeocode
            .addressComponent.city

        city.value = cityName

        getWeather(cityName)

      } catch (e) {
        console.log(e)
      }

    },

    fail() {
      city.value = '定位失败'
    }

  })

}

async function getWeather(
  cityName: string
) {

  try {

    const res: any = await uni.request({
      url:
        'https://restapi.amap.com/v3/weather/weatherInfo',

      data: {
        key: AMAP_KEY,
        city: cityName,
        extensions: 'base'
      }
    })

    const data = res.data.lives[0]

    weather.value = data.weather
    temperature.value = data.temperature

    winddirection.value =
      data.winddirection

    windpower.value =
      data.windpower

    humidity.value =
      data.humidity

    reportTime.value =
      data.reporttime

  } catch (e) {

    console.log(e)

  }

}
</script>

<style scoped>
.weather-card {
  background: linear-gradient(
    135deg,
    #4facfe,
    #00c6fb
  );

  border-radius: 24rpx;

  padding: 32rpx;

  color: white;

  box-shadow:
    0 8rpx 20rpx
    rgba(0,0,0,0.08);
}

.top {
  display: flex;

  justify-content:
    space-between;

  align-items: center;

  margin-bottom: 24rpx;
}

.city {
  font-size: 34rpx;

  font-weight: 600;
}

.update-time {
  font-size: 24rpx;

  opacity: 0.8;
}

.content {

  display: flex;

  justify-content:
    space-between;

  align-items: center;

}

.left {

  display: flex;

  flex-direction: column;

}

.temp {

  font-size: 96rpx;

  font-weight: bold;

  line-height: 1;
}

.weather {

  margin-top: 12rpx;

  font-size: 32rpx;
}

.right {

  display: flex;

  flex-direction: column;

  gap: 14rpx;
}

.info-item {

  display: flex;

  gap: 16rpx;

  justify-content:
    space-between;

  font-size: 26rpx;
}
</style>