<template>
  <view class="page">
    <TabBar active="elder" />
  </view>
</template>

<script setup lang="ts">
import TabBar from '@/components/TabBar/TabBar.vue'
</script>

<style scoped>
.page {
  min-height: 100vh;
  background-color: #f7f8fa;
}
</style>
