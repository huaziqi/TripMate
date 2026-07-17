<template>
  <div>
    <!-- 筛选 + 操作栏 -->
    <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap">
      <el-select
        v-model="filters.spotKey"
        placeholder="所属景点（全部）"
        clearable
        filterable
        style="width:200px"
      >
        <el-option
          v-for="opt in spotOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-select v-model="filters.category" placeholder="分类（全部）" clearable style="width:160px">
        <el-option
          v-for="(label, value) in CATEGORY_LABELS"
          :key="value"
          :label="label"
          :value="value"
        />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="按标题搜索"
        clearable
        style="width:220px"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <div style="flex:1" />
      <el-button type="primary" @click="openUpload">上传文档</el-button>
      <el-button @click="openCreate">手动新增</el-button>
    </div>

    <el-table v-loading="loading" :data="docs" border>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="分类" width="110">
        <template #default="{ row }">
          <el-tag :type="categoryTagType(row.category)">{{ CATEGORY_LABELS[row.category as KnowledgeCategory] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="所属景点" width="140">
        <template #default="{ row }">
          <span v-if="row.spotKey">{{ row.spotKey }}</span>
          <el-tag v-else size="small" type="info">通用</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="preview" label="内容预览" min-width="240" show-overflow-tooltip />
      <el-table-column label="字数" width="90" align="right">
        <template #default="{ row }">{{ row.contentLength }}</template>
      </el-table-column>
      <el-table-column prop="sourceFileName" label="来源文件" width="180" show-overflow-tooltip />
      <el-table-column label="更新时间" width="160">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled"
            @change="(v: string | number | boolean) => toggleEnabled(row, Boolean(v))"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 上传文档对话框 -->
    <el-dialog v-model="uploadVisible" title="上传知识文档" width="520px">
      <el-form label-width="90px">
        <el-form-item label="文件" required>
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept=".docx,.txt,.md"
            :on-change="onFileChange"
            :on-remove="() => (uploadForm.file = null)"
          >
            <el-button>选择文件</el-button>
            <template #tip>
              <div style="color:#999;font-size:12px">支持 .docx / .txt / .md，上传后自动提取正文</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="uploadForm.title" placeholder="留空则使用文件名" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="uploadForm.category" style="width:100%">
            <el-option
              v-for="(label, value) in CATEGORY_LABELS"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属景点">
          <el-select
            v-model="uploadForm.spotKey"
            placeholder="留空为通用知识"
            clearable
            filterable
            allow-create
            style="width:100%"
          >
            <el-option
              v-for="opt in spotOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 新增 / 编辑对话框 -->
    <el-dialog
      v-model="editVisible"
      :title="editingId ? '编辑知识文档' : '新增知识文档'"
      width="960px"
      top="4vh"
    >
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" />
        </el-form-item>
        <div style="display:flex;gap:16px">
          <el-form-item label="分类" required style="flex:1">
            <el-select v-model="editForm.category" style="width:100%">
              <el-option
                v-for="(label, value) in CATEGORY_LABELS"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="所属景点" style="flex:1">
            <el-select
              v-model="editForm.spotKey"
              placeholder="留空为通用知识"
              clearable
              filterable
              allow-create
              style="width:100%"
            >
              <el-option
                v-for="opt in spotOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="editForm.enabled" />
          </el-form-item>
        </div>
        <el-form-item label="内容" required>
          <MdEditor
            v-model="editForm.content"
            style="height:460px;width:100%"
            :toolbars-exclude="mdToolbarsExclude"
            :preview="false"
            no-upload-img
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { MdEditor, type ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import http from '@/api/http'
import {
  CATEGORY_LABELS,
  listKnowledge,
  getKnowledge,
  createKnowledge,
  updateKnowledge,
  deleteKnowledge,
  uploadKnowledge,
  type KnowledgeCategory,
  type KnowledgeDocItem,
} from '@/api/knowledge'

const mdToolbarsExclude: ToolbarNames[] = [
  'image',
  'save',
  'github',
  'htmlPreview',
  'catalog',
  'mermaid',
  'katex',
]

const docs = ref<KnowledgeDocItem[]>([])
const loading = ref(false)
const filters = reactive({ spotKey: '', category: '' as KnowledgeCategory | '', keyword: '' })

const spotOptions = ref<{ value: string; label: string }[]>([])

async function load() {
  loading.value = true
  try {
    const res = await listKnowledge({
      spotKey: filters.spotKey || undefined,
      category: filters.category || undefined,
      keyword: filters.keyword || undefined,
    })
    docs.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function loadSpotOptions() {
  try {
    const res = await http.get<{ data: { spotKey: string; personaName: string }[] }>(
      '/admin/guide/configs',
    )
    spotOptions.value = res.data.data.map(c => ({
      value: c.spotKey,
      label: `${c.spotKey}（${c.personaName}）`,
    }))
  } catch {
    // 拉不到景点列表不影响知识库本身
  }
}

onMounted(() => {
  load()
  loadSpotOptions()
})

function categoryTagType(category: KnowledgeCategory) {
  const map: Record<KnowledgeCategory, 'success' | 'warning' | 'primary' | 'info'> = {
    EXPLANATION: 'success',
    HISTORY: 'warning',
    FAQ: 'primary',
    OTHER: 'info',
  }
  return map[category]
}

function formatTime(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

// ---------- 上传 ----------
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadForm = reactive({
  file: null as File | null,
  title: '',
  category: 'EXPLANATION' as KnowledgeCategory,
  spotKey: '',
})

function openUpload() {
  uploadForm.file = null
  uploadForm.title = ''
  uploadForm.category = 'EXPLANATION'
  uploadForm.spotKey = ''
  uploadVisible.value = true
}

function onFileChange(file: UploadFile) {
  uploadForm.file = (file.raw as File) ?? null
}

async function submitUpload() {
  if (!uploadForm.file) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  try {
    const res = await uploadKnowledge({
      file: uploadForm.file,
      title: uploadForm.title || undefined,
      category: uploadForm.category,
      spotKey: uploadForm.spotKey || undefined,
    })
    if (res.data.code === 200) {
      ElMessage.success('上传成功')
      uploadVisible.value = false
      load()
    } else {
      ElMessage.error(res.data.message)
    }
  } finally {
    uploading.value = false
  }
}

// ---------- 新增 / 编辑 ----------
const editVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const editForm = reactive({
  title: '',
  category: 'EXPLANATION' as KnowledgeCategory,
  spotKey: '',
  content: '',
  enabled: true,
})

function openCreate() {
  editingId.value = null
  editForm.title = ''
  editForm.category = 'EXPLANATION'
  editForm.spotKey = ''
  editForm.content = ''
  editForm.enabled = true
  editVisible.value = true
}

async function openEdit(row: KnowledgeDocItem) {
  const res = await getKnowledge(row.id)
  if (res.data.code !== 200) {
    ElMessage.error(res.data.message)
    return
  }
  const doc = res.data.data
  editingId.value = doc.id
  editForm.title = doc.title
  editForm.category = doc.category
  editForm.spotKey = doc.spotKey ?? ''
  editForm.content = doc.content
  editForm.enabled = doc.enabled
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.title.trim() || !editForm.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: editForm.title,
      category: editForm.category,
      spotKey: editForm.spotKey || null,
      content: editForm.content,
      enabled: editForm.enabled,
    }
    const res = editingId.value
      ? await updateKnowledge(editingId.value, payload)
      : await createKnowledge(payload)
    if (res.data.code === 200) {
      ElMessage.success('保存成功')
      editVisible.value = false
      load()
    } else {
      ElMessage.error(res.data.message)
    }
  } finally {
    saving.value = false
  }
}

// ---------- 启停 / 删除 ----------
async function toggleEnabled(row: KnowledgeDocItem, enabled: boolean) {
  const res = await getKnowledge(row.id)
  if (res.data.code !== 200) {
    ElMessage.error(res.data.message)
    return
  }
  const doc = res.data.data
  const updated = await updateKnowledge(row.id, {
    title: doc.title,
    category: doc.category,
    spotKey: doc.spotKey,
    content: doc.content,
    enabled,
  })
  if (updated.data.code === 200) {
    row.enabled = enabled
    ElMessage.success(enabled ? '已启用' : '已停用')
  } else {
    ElMessage.error(updated.data.message)
  }
}

async function remove(row: KnowledgeDocItem) {
  await ElMessageBox.confirm(`确定删除「${row.title}」吗？删除后不可恢复。`, '删除确认', {
    type: 'warning',
  })
  const res = await deleteKnowledge(row.id)
  if (res.data.code === 200) {
    ElMessage.success('已删除')
    load()
  } else {
    ElMessage.error(res.data.message)
  }
}
</script>
