<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background:#001529">
      <div style="color:#fff;padding:20px;font-size:18px;font-weight:700">TripMate 管理</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="#ccc"
        active-text-color="#fff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item v-if="auth.isSuperAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>管理员账号</span>
        </el-menu-item>
        <el-menu-item v-if="auth.isSuperAdmin" index="/settings">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="display:flex;align-items:center;justify-content:flex-end;border-bottom:1px solid #eee">
        <span style="margin-right:16px">{{ auth.username }}</span>
        <el-button size="small" @click="logout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DataLine, User, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.clearAuth()
  router.push('/login')
}
</script>
