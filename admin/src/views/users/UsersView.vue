<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>管理员账号</h2>
      <el-button type="primary" @click="openCreate">新增管理员</el-button>
    </div>

    <el-table :data="users" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="role" label="角色" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" @click="openReset(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createVisible" title="新增管理员" width="400px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role">
            <el-option label="普通管理员" value="ADMIN" />
            <el-option label="超级管理员" value="SUPER_ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="400px">
      <el-input v-model="newPassword" type="password" placeholder="新密码" show-password />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, createUser, updateUser, deleteUser, type AdminUser } from '@/api/users'

const users = ref<AdminUser[]>([])

async function load() {
  const res = await listUsers()
  users.value = res.data.data
}

onMounted(load)

const createVisible = ref(false)
const createFormRef = ref()
const createForm = reactive({ username: '', password: '', role: 'ADMIN' })
const createRules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }],
  role: [{ required: true }],
}

function openCreate() { createVisible.value = true }

async function handleCreate() {
  await createFormRef.value.validate()
  await createUser(createForm)
  ElMessage.success('创建成功')
  createVisible.value = false
  load()
}

async function toggleStatus(row: AdminUser) {
  await updateUser(row.id, { status: row.status === 1 ? 0 : 1 })
  load()
}

const resetVisible = ref(false)
const newPassword = ref('')
let resetTargetId = 0

function openReset(row: AdminUser) {
  resetTargetId = row.id
  newPassword.value = ''
  resetVisible.value = true
}

async function handleReset() {
  if (!newPassword.value) return ElMessage.warning('请输入新密码')
  await updateUser(resetTargetId, { password: newPassword.value })
  ElMessage.success('密码已重置')
  resetVisible.value = false
}

async function handleDelete(row: AdminUser) {
  await ElMessageBox.confirm(`确认删除管理员 ${row.username}？`, '确认', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  load()
}
</script>
