<template>
  <div v-loading="loading">
    <!-- 页头 -->
    <div style="display:flex;align-items:center;margin-bottom:20px">
      <div>
        <h2 style="margin:0">数据大屏概览</h2>
        <div style="color:#898781;font-size:13px;margin-top:4px">
          数字人服务运营数据 · 游客行为分析
        </div>
      </div>
      <div style="flex:1" />
      <el-button
        v-if="auth.isSuperAdmin"
        type="primary"
        :loading="importing"
        @click="fileInput?.click()"
      >
        导入游客行为数据(xlsx)
      </el-button>
      <input
        ref="fileInput"
        type="file"
        accept=".xlsx"
        style="display:none"
        @change="onImportFile"
      />
    </div>

    <template v-if="ov">
      <!-- ==================== 数字人服务 ==================== -->
      <div class="section-title">数字人服务</div>
      <div class="kpi-grid">
        <div class="stat-tile">
          <div class="stat-label">今日提问数</div>
          <div class="stat-value">{{ ov.todayQuestions }}</div>
          <div v-if="todayDelta !== null" class="stat-delta" :style="{ color: todayDelta >= 0 ? '#006300' : '#52514e' }">
            {{ todayDelta >= 0 ? '+' : '' }}{{ todayDelta }} vs 昨日
          </div>
        </div>
        <div class="stat-tile">
          <div class="stat-label">近7天提问数</div>
          <div class="stat-value">{{ ov.weekQuestions }}</div>
        </div>
        <div class="stat-tile">
          <div class="stat-label">今日活跃会话</div>
          <div class="stat-value">{{ ov.todaySessions }}</div>
        </div>
        <div class="stat-tile">
          <div class="stat-label">累计服务会话</div>
          <div class="stat-value">{{ ov.totalSessions }}</div>
        </div>
        <div class="stat-tile">
          <div class="stat-label">注册用户</div>
          <div class="stat-value">{{ ov.totalUsers }}</div>
        </div>
        <div class="stat-tile">
          <div class="stat-label">知识库条目</div>
          <div class="stat-value">{{ ov.knowledgeCount }}</div>
        </div>
      </div>

      <div class="chart-grid">
        <div class="chart-card" style="grid-column:span 14">
          <div class="chart-title">近14天服务趋势</div>
          <v-chart :option="serviceTrendOpt" autoresize style="height:280px" />
        </div>
        <div class="chart-card" style="grid-column:span 10">
          <div class="chart-title">热门问答 Top10</div>
          <v-chart
            v-if="ov.hotQuestions.length"
            :option="hotQuestionsOpt"
            autoresize
            style="height:280px"
          />
          <el-empty v-else description="暂无问答记录" :image-size="60" style="height:280px" />
        </div>
      </div>

      <!-- ==================== 游客行为分析 ==================== -->
      <div class="section-title" style="margin-top:24px">游客行为分析</div>

      <template v-if="ov.visitorDataReady">
        <div class="kpi-grid">
          <div class="stat-tile">
            <div class="stat-label">游玩总人次</div>
            <div class="stat-value">{{ formatNum(ov.totalVisits) }}</div>
          </div>
          <div class="stat-tile">
            <div class="stat-label">平均满意度</div>
            <div class="stat-value">{{ ov.avgSatisfaction }} <span style="font-size:14px;color:#898781">/ 5</span></div>
          </div>
          <div class="stat-tile">
            <div class="stat-label">人均消费</div>
            <div class="stat-value">¥{{ ov.avgSpend }}</div>
          </div>
          <div class="stat-tile">
            <div class="stat-label">性别比例</div>
            <div class="stat-value" style="font-size:20px">{{ genderText }}</div>
          </div>
        </div>

        <div class="chart-grid">
          <div class="chart-card" style="grid-column:span 12">
            <div class="chart-title">游客满意度趋势</div>
            <v-chart :option="satisfactionTrendOpt" autoresize style="height:260px" />
          </div>
          <div class="chart-card" style="grid-column:span 12">
            <div class="chart-title">每日游玩人次</div>
            <v-chart :option="dailyVisitsOpt" autoresize style="height:260px" />
          </div>
          <div class="chart-card" style="grid-column:span 8">
            <div class="chart-title">满意度分布</div>
            <v-chart :option="satisfactionDistOpt" autoresize style="height:260px" />
          </div>
          <div class="chart-card" style="grid-column:span 8">
            <div class="chart-title">年龄结构</div>
            <v-chart :option="ageDistOpt" autoresize style="height:260px" />
          </div>
          <div class="chart-card" style="grid-column:span 8">
            <div class="chart-title">景区类型偏好</div>
            <v-chart :option="typeDistOpt" autoresize style="height:260px" />
          </div>
        </div>
      </template>

      <el-card v-else shadow="never" style="margin-top:8px">
        <el-empty description="尚未导入游客行为数据">
          <el-button v-if="auth.isSuperAdmin" type="primary" @click="fileInput?.click()">
            导入 xlsx 数据
          </el-button>
          <span v-else style="color:#898781">请联系超级管理员导入</span>
        </el-empty>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import type { EChartsCoreOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { useAuthStore } from '@/stores/auth'
import { getOverview, importVisitorData, type DashboardOverview } from '@/api/dashboard'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const auth = useAuthStore()
const ov = ref<DashboardOverview | null>(null)
const loading = ref(false)
const importing = ref(false)
const fileInput = ref<HTMLInputElement>()

/** 调色板：dataviz 校验通过（蓝/绿类目对 + 蓝色 ordinal 渐变） */
const C = {
  blue: '#2a78d6',
  green: '#008300',
  ink2: '#52514e',
  muted: '#898781',
  grid: '#e1e0d9',
  axis: '#c3c2b7',
  ordinal: ['#86b6ef', '#5598e7', '#2a78d6', '#1c5cab', '#104281'],
}

const AGE_ORDER = ['18岁以下', '18-30岁', '31-45岁', '46-60岁', '60岁以上']

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await getOverview()
    ov.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function onImportFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  importing.value = true
  try {
    const res = await importVisitorData(file)
    if (res.data.code === 200) {
      const { totalRows, dateFrom, dateTo } = res.data.data
      ElMessage.success(`导入成功：${totalRows} 条记录（${dateFrom} ~ ${dateTo}）`)
      load()
    } else {
      ElMessage.error(res.data.message)
    }
  } finally {
    importing.value = false
  }
}

function formatNum(n: number) {
  return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n)
}

const todayDelta = computed(() => {
  const t = ov.value?.serviceTrend
  if (!t || t.length < 2) return null
  return t[t.length - 1].questions - t[t.length - 2].questions
})

const genderText = computed(() => {
  const dist = ov.value?.genderDist ?? []
  const total = dist.reduce((s, d) => s + d.value, 0)
  if (!total) return '—'
  return dist.map(d => `${d.label} ${Math.round((d.value / total) * 100)}%`).join(' · ')
})

// ---------- 公共坐标轴样式（发丝网格、隐性轴线） ----------
const catAxis = (data: string[]) => ({
  type: 'category' as const,
  data,
  axisLine: { lineStyle: { color: C.axis } },
  axisTick: { show: false },
  axisLabel: { color: C.muted },
})
const valAxis = () => ({
  type: 'value' as const,
  axisLabel: { color: C.muted },
  splitLine: { lineStyle: { color: C.grid, type: 'solid' as const } },
})

// ---------- 数字人服务趋势（双系列折线 + 图例） ----------
const serviceTrendOpt = computed<EChartsCoreOption>(() => {
  const t = ov.value?.serviceTrend ?? []
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'line' } },
    legend: { top: 0, textStyle: { color: C.ink2 } },
    grid: { left: 8, right: 16, top: 36, bottom: 8, containLabel: true },
    xAxis: catAxis(t.map(p => p.date.slice(5))),
    yAxis: { ...valAxis(), minInterval: 1 },
    series: [
      {
        name: '游客提问数',
        type: 'line',
        data: t.map(p => p.questions),
        lineStyle: { width: 2 },
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: C.blue, borderColor: '#fff', borderWidth: 2 },
        color: C.blue,
      },
      {
        name: '活跃会话数',
        type: 'line',
        data: t.map(p => p.sessions),
        lineStyle: { width: 2 },
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: C.green, borderColor: '#fff', borderWidth: 2 },
        color: C.green,
      },
    ],
  }
})

// ---------- 热门问答（单色横向条形，值标在条端） ----------
const hotQuestionsOpt = computed<EChartsCoreOption>(() => {
  const hq = (ov.value?.hotQuestions ?? []).slice().reverse()
  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: { name: string; value: number }) => `${p.name}<br/>提问 ${p.value} 次`,
    },
    grid: { left: 8, right: 40, top: 8, bottom: 8, containLabel: true },
    xAxis: { ...valAxis(), minInterval: 1 },
    yAxis: {
      ...catAxis(hq.map(q => q.question)),
      axisLabel: { color: C.muted, width: 120, overflow: 'truncate' as const },
    },
    series: [
      {
        type: 'bar',
        data: hq.map(q => q.count),
        barWidth: 14,
        itemStyle: { color: C.blue, borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', color: C.ink2 },
      },
    ],
  }
})

// ---------- 满意度趋势（单系列折线，无图例） ----------
const satisfactionTrendOpt = computed<EChartsCoreOption>(() => {
  const t = ov.value?.satisfactionTrend ?? []
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (ps: { name: string; value: number }[]) =>
        `${ps[0].name}<br/>平均满意度 ${ps[0].value}`,
    },
    grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
    xAxis: catAxis(t.map(p => p.date)),
    yAxis: {
      ...valAxis(),
      min: (v: { min: number }) => Math.max(1, Math.floor((v.min - 0.05) * 10) / 10),
      max: (v: { max: number }) => Math.min(5, Math.ceil((v.max + 0.05) * 10) / 10),
    },
    series: [
      {
        name: '平均满意度',
        type: 'line',
        data: t.map(p => p.avgSatisfaction),
        showSymbol: false,
        lineStyle: { width: 2 },
        color: C.blue,
      },
    ],
  }
})

// ---------- 每日游玩人次（第二个时序图取绿色，10% 面积雾化） ----------
const dailyVisitsOpt = computed<EChartsCoreOption>(() => {
  const t = ov.value?.satisfactionTrend ?? []
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (ps: { name: string; value: number }[]) =>
        `${ps[0].name}<br/>游玩 ${ps[0].value} 人次`,
    },
    grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
    xAxis: catAxis(t.map(p => p.date)),
    yAxis: valAxis(),
    series: [
      {
        name: '游玩人次',
        type: 'line',
        data: t.map(p => p.visits),
        showSymbol: false,
        lineStyle: { width: 2 },
        areaStyle: { opacity: 0.1 },
        color: C.green,
      },
    ],
  }
})

// ---------- 满意度分布（有序类目 → ordinal 渐变柱） ----------
const satisfactionDistOpt = computed<EChartsCoreOption>(() => {
  const dist = ov.value?.satisfactionDist ?? []
  return {
    tooltip: { trigger: 'item' },
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    xAxis: catAxis(dist.map(d => d.label)),
    yAxis: valAxis(),
    series: [
      {
        type: 'bar',
        data: dist.map((d, i) => ({
          value: d.value,
          itemStyle: {
            color: C.ordinal[Math.min(i, C.ordinal.length - 1)],
            borderRadius: [4, 4, 0, 0],
          },
        })),
        barWidth: 24,
        label: {
          show: true,
          position: 'top',
          color: C.ink2,
          formatter: (p: { value: number }) => formatNum(p.value),
        },
      },
    ],
  }
})

// ---------- 年龄结构（有序类目单色柱） ----------
const ageDistOpt = computed<EChartsCoreOption>(() => {
  const raw = ov.value?.ageDist ?? []
  const dist = AGE_ORDER.map(l => raw.find(d => d.label === l)).filter(d => d != null)
  return {
    tooltip: { trigger: 'item' },
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    xAxis: catAxis(dist.map(d => d!.label)),
    yAxis: valAxis(),
    series: [
      {
        type: 'bar',
        data: dist.map(d => d!.value),
        barWidth: 24,
        itemStyle: { color: C.green, borderRadius: [4, 4, 0, 0] },
        label: {
          show: true,
          position: 'top',
          color: C.ink2,
          formatter: (p: { value: number }) => formatNum(p.value),
        },
      },
    ],
  }
})

// ---------- 景区类型偏好（单色横向条形，超过8类折叠尾部为其他） ----------
const typeDistOpt = computed<EChartsCoreOption>(() => {
  const raw = ov.value?.attractionTypeDist ?? []
  const dist =
    raw.length > 8
      ? [
          ...raw.slice(0, 7),
          { label: '其他', value: raw.slice(7).reduce((s, d) => s + d.value, 0) },
        ]
      : raw
  const shown = dist.slice().reverse()
  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: { name: string; value: number }) => `${p.name}<br/>${formatNum(p.value)} 人次`,
    },
    grid: { left: 8, right: 48, top: 8, bottom: 8, containLabel: true },
    xAxis: valAxis(),
    yAxis: {
      ...catAxis(shown.map(d => d.label)),
      axisLabel: { color: C.muted, width: 90, overflow: 'truncate' as const },
    },
    series: [
      {
        type: 'bar',
        data: shown.map(d => d.value),
        barWidth: 14,
        itemStyle: { color: C.blue, borderRadius: [0, 4, 4, 0] },
        label: {
          show: true,
          position: 'right',
          color: C.ink2,
          formatter: (p: { value: number }) => formatNum(p.value),
        },
      },
    ],
  }
})
</script>

<style scoped>
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #0b0b0b;
  margin-bottom: 12px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.stat-tile {
  background: #fff;
  border: 1px solid rgba(11, 11, 11, 0.1);
  border-radius: 8px;
  padding: 14px 16px;
}

.stat-label {
  font-size: 13px;
  color: #52514e;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: #0b0b0b;
  margin-top: 4px;
}

.stat-delta {
  font-size: 12px;
  margin-top: 2px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(24, 1fr);
  gap: 12px;
}

.chart-card {
  background: #fff;
  border: 1px solid rgba(11, 11, 11, 0.1);
  border-radius: 8px;
  padding: 14px 16px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #0b0b0b;
  margin-bottom: 8px;
}
</style>
