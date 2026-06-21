<template>
  <div class="posts-view">
    <div class="header">
      <h2>帖子管理</h2>
      <el-select
        v-model="statusFilter"
        placeholder="全部状态"
        clearable
        style="width: 140px"
        @change="handleFilterChange"
      >
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已删除" value="DELETED" />
      </el-select>
    </div>

    <el-table :data="posts" v-loading="loading" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="authorNickname" label="作者" width="120" />
      <el-table-column label="分类" width="110">
        <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
      </el-table-column>
      <el-table-column label="数据" width="180">
        <template #default="{ row }">
          <span>👍{{ row.likeCount }} 💬{{ row.commentCount }} 👁{{ row.viewCount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'danger'">
            {{ row.status === 'PUBLISHED' ? '已发布' : '已删除' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PUBLISHED'"
            type="danger"
            size="small"
            @click="handleDelete(row)"
          >删除</el-button>
          <el-button
            v-else
            type="success"
            size="small"
            @click="handleRestore(row)"
          >恢复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 20px; display: flex; justify-content: flex-end"
      @size-change="handlePageChange"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPosts, deletePost, restorePost } from '@/api/posts'
import type { AdminPost } from '@/api/posts'

const posts = ref<AdminPost[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const statusFilter = ref('')

const categoryMap: Record<string, string> = {
  SCENIC: '景点攻略',
  FOOD: '美食推荐',
  TRANSPORT: '交通住宿',
  FREE_TRAVEL: '自由行',
  FAMILY: '亲子游'
}

function categoryLabel(v: string) {
  return categoryMap[v] || v
}

function formatTime(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await listPosts({
      page: currentPage.value - 1,
      size: pageSize.value,
      status: statusFilter.value || undefined
    })
    // http.ts does not unwrap — res.data is Result<PageResult<AdminPostDTO>>
    // Backend wraps in { code, message, data: { items, total, page, size } }
    const result = res.data
    posts.value = result.data.items
    total.value = result.data.total
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  currentPage.value = 1
  loadData()
}

function handlePageChange() {
  loadData()
}

async function handleDelete(row: AdminPost) {
  try {
    await ElMessageBox.confirm(`确定要删除《${row.title}》吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await deletePost(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    // 用户取消，忽略
  }
}

async function handleRestore(row: AdminPost) {
  try {
    await restorePost(row.id)
    ElMessage.success('已恢复')
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}
</script>

<style scoped>
.posts-view { padding: 0; }
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header h2 { margin: 0; font-size: 20px; }
</style>
