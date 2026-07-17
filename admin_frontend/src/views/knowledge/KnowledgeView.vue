<template>
  <div>
    <h2 style="margin-bottom:16px">知识库管理</h2>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="景点知识" name="spots">
        <SpotsPanel />
      </el-tab-pane>
      <el-tab-pane label="知识文档" name="docs">
        <DocsPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SpotsPanel from './SpotsPanel.vue'
import DocsPanel from './DocsPanel.vue'

const activeTab = ref('spots')
</script>
