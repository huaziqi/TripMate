<template>
  <div>
    <h2 style="margin-bottom:24px">数据统计</h2>
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <el-statistic title="管理员账号数" :value="stats.adminUserCount" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="系统配置项数" :value="stats.configCount" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { getDashboard } from '@/api/dashboard'

const stats = reactive({ adminUserCount: 0, configCount: 0 })

onMounted(async () => {
  const res = await getDashboard()
  Object.assign(stats, res.data.data)
})
</script>
