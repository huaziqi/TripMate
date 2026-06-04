<template>
  <div>
    <h2 style="margin-bottom:24px">系统配置</h2>
    <el-table :data="configs" border>
      <el-table-column prop="configKey" label="配置键" width="240" />
      <el-table-column label="配置值">
        <template #default="{ row }">
          <el-input v-if="editing === row.configKey" v-model="editValue" size="small" />
          <span v-else>{{ row.configValue }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <template v-if="editing === row.configKey">
            <el-button size="small" type="primary" @click="saveEdit(row)">保存</el-button>
            <el-button size="small" @click="editing = ''">取消</el-button>
          </template>
          <el-button v-else size="small" @click="startEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listSettings, updateSetting, type SystemConfig } from '@/api/settings'

const configs = ref<SystemConfig[]>([])
const editing = ref('')
const editValue = ref('')

onMounted(async () => {
  const res = await listSettings()
  configs.value = res.data.data
})

function startEdit(row: SystemConfig) {
  editing.value = row.configKey
  editValue.value = row.configValue
}

async function saveEdit(row: SystemConfig) {
  await updateSetting(row.configKey, editValue.value)
  row.configValue = editValue.value
  editing.value = ''
  ElMessage.success('保存成功')
}
</script>
