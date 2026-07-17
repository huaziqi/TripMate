<template>
  <div>
    <!-- 筛选 + 操作栏 -->
    <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap">
      <el-select
        v-model="filters.spotKey"
        placeholder="所属景区（全部）"
        clearable
        filterable
        style="width:180px"
      >
        <el-option
          v-for="opt in spotKeyOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-select v-model="filters.zoneName" placeholder="子景区（全部）" clearable style="width:200px">
        <el-option v-for="z in zones" :key="z" :label="z" :value="z" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="按景点ID / 名称搜索"
        clearable
        style="width:220px"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <div style="flex:1" />
      <el-button type="primary" @click="importVisible = true">导入数据集(docx)</el-button>
      <el-button @click="openCreate">新增景点</el-button>
    </div>

    <el-table v-loading="loading" :data="entries" border row-key="id">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div style="padding:8px 48px;display:grid;grid-template-columns:1fr 1fr;gap:10px 32px">
            <div v-for="[key, label] in SPOT_FIELD_LABELS" :key="key">
              <template v-if="row[key]">
                <div style="font-weight:600;color:#409eff;margin-bottom:2px">{{ label }}</div>
                <div style="white-space:pre-wrap;color:#555;line-height:1.6">{{ row[key] }}</div>
              </template>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="spotCode" label="景点ID" width="110" sortable />
      <el-table-column prop="name" label="景点名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="zoneName" label="子景区" width="150" show-overflow-tooltip />
      <el-table-column prop="spotKey" label="所属景区" width="110" />
      <el-table-column prop="coreFunction" label="核心功能" min-width="200" show-overflow-tooltip />
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

    <!-- 每个景点一个编辑表单：基础信息 + 逐字段子方块 -->
    <el-dialog
      v-model="editVisible"
      :title="editingId ? `编辑景点 ${editForm.spotCode} · ${editForm.name}` : '新增景点'"
      width="860px"
      top="4vh"
    >
      <el-form label-width="0">
        <div style="display:grid;grid-template-columns:repeat(4, 1fr);gap:12px;margin-bottom:8px">
          <div>
            <div class="field-label">景点ID <span style="color:#f56c6c">*</span></div>
            <el-input v-model="editForm.spotCode" placeholder="如 LS-001" :disabled="!!editingId" />
          </div>
          <div>
            <div class="field-label">景点名称 <span style="color:#f56c6c">*</span></div>
            <el-input v-model="editForm.name" />
          </div>
          <div>
            <div class="field-label">子景区</div>
            <el-select
              v-model="editForm.zoneName"
              clearable
              filterable
              allow-create
              placeholder="如 灵山胜境"
              style="width:100%"
            >
              <el-option v-for="z in zones" :key="z" :label="z" :value="z" />
            </el-select>
          </div>
          <div>
            <div class="field-label">所属景区 <span style="color:#f56c6c">*</span></div>
            <el-select
              v-model="editForm.spotKey"
              filterable
              allow-create
              placeholder="如 lingshan"
              style="width:100%"
            >
              <el-option
                v-for="opt in spotKeyOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </div>
        </div>

        <!-- 知识字段子方块，逐项独立编辑 -->
        <div
          v-for="[key, label] in SPOT_FIELD_LABELS"
          :key="key"
          style="border:1px solid #e4e7ed;border-radius:6px;padding:10px 14px;margin-bottom:10px;background:#fafafa"
        >
          <div class="field-label" style="margin-bottom:6px">{{ label }}</div>
          <el-input
            v-model="editForm[key]"
            type="textarea"
            :autosize="{ minRows: key === 'description' ? 4 : 2, maxRows: 12 }"
            :placeholder="`填写${label}，留空则不注入数字人知识`"
          />
        </div>

        <div style="display:flex;align-items:center;gap:8px">
          <span class="field-label">启用（注入数字人知识库）</span>
          <el-switch v-model="editForm.enabled" />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入数据集对话框 -->
    <el-dialog v-model="importVisible" title="导入景点结构化数据集" width="520px">
      <el-form label-width="90px">
        <el-form-item label="文件" required>
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept=".docx"
            :on-change="onImportFileChange"
            :on-remove="() => (importFile = null)"
          >
            <el-button>选择 .docx 文件</el-button>
            <template #tip>
              <div style="color:#999;font-size:12px">
                解析文档中的景点表格（需含"景点ID""景点名称"列），按景点ID自动新增或更新
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="所属景区" required>
          <el-select
            v-model="importSpotKey"
            filterable
            allow-create
            placeholder="如 lingshan"
            style="width:100%"
          >
            <el-option
              v-for="opt in spotKeyOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import http from '@/api/http'
import {
  SPOT_FIELD_LABELS,
  listSpotEntries,
  listSpotZones,
  createSpotEntry,
  updateSpotEntry,
  deleteSpotEntry,
  importSpotDocx,
  type SpotEntry,
  type SpotFieldKey,
} from '@/api/knowledge'

const entries = ref<SpotEntry[]>([])
const zones = ref<string[]>([])
const loading = ref(false)
const filters = reactive({ spotKey: '', zoneName: '', keyword: '' })

const spotKeyOptions = ref<{ value: string; label: string }[]>([])

async function load() {
  loading.value = true
  try {
    const res = await listSpotEntries({
      spotKey: filters.spotKey || undefined,
      zoneName: filters.zoneName || undefined,
      keyword: filters.keyword || undefined,
    })
    entries.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function loadZones() {
  const res = await listSpotZones()
  zones.value = res.data.data
}

async function loadSpotKeyOptions() {
  try {
    const res = await http.get<{ data: { spotKey: string; personaName: string }[] }>(
      '/admin/guide/configs',
    )
    spotKeyOptions.value = res.data.data.map(c => ({
      value: c.spotKey,
      label: `${c.spotKey}（${c.personaName}）`,
    }))
  } catch {
    // 拉不到数字人配置不影响本页
  }
}

onMounted(() => {
  load()
  loadZones()
  loadSpotKeyOptions()
})

function formatTime(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

// ---------- 新增 / 编辑 ----------
const editVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyFields = Object.fromEntries(SPOT_FIELD_LABELS.map(([k]) => [k, ''])) as Record<
  SpotFieldKey,
  string
>
const editForm = reactive({
  spotKey: '',
  spotCode: '',
  zoneName: '',
  name: '',
  enabled: true,
  ...emptyFields,
})

function openCreate() {
  editingId.value = null
  editForm.spotKey = filters.spotKey || 'lingshan'
  editForm.spotCode = ''
  editForm.zoneName = ''
  editForm.name = ''
  editForm.enabled = true
  for (const [key] of SPOT_FIELD_LABELS) editForm[key] = ''
  editVisible.value = true
}

function openEdit(row: SpotEntry) {
  editingId.value = row.id
  editForm.spotKey = row.spotKey
  editForm.spotCode = row.spotCode
  editForm.zoneName = row.zoneName ?? ''
  editForm.name = row.name
  editForm.enabled = row.enabled
  for (const [key] of SPOT_FIELD_LABELS) editForm[key] = row[key] ?? ''
  editVisible.value = true
}

function buildPayload() {
  return {
    spotKey: editForm.spotKey.trim(),
    spotCode: editForm.spotCode.trim(),
    zoneName: editForm.zoneName.trim() || null,
    name: editForm.name.trim(),
    enabled: editForm.enabled,
    location: editForm.location.trim() || null,
    scaleInfo: editForm.scaleInfo.trim() || null,
    coreFunction: editForm.coreFunction.trim() || null,
    culture: editForm.culture.trim() || null,
    description: editForm.description.trim() || null,
    tourTips: editForm.tourTips.trim() || null,
    ticketInfo: editForm.ticketInfo.trim() || null,
    remark: editForm.remark.trim() || null,
  }
}

async function submitEdit() {
  if (!editForm.spotKey.trim() || !editForm.spotCode.trim() || !editForm.name.trim()) {
    ElMessage.warning('所属景区、景点ID、景点名称为必填')
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    const res = editingId.value
      ? await updateSpotEntry(editingId.value, payload)
      : await createSpotEntry(payload)
    if (res.data.code === 200) {
      ElMessage.success('保存成功')
      editVisible.value = false
      load()
      loadZones()
    } else {
      ElMessage.error(res.data.message)
    }
  } finally {
    saving.value = false
  }
}

// ---------- 启停 / 删除 ----------
async function toggleEnabled(row: SpotEntry, enabled: boolean) {
  const res = await updateSpotEntry(row.id, {
    spotKey: row.spotKey,
    spotCode: row.spotCode,
    zoneName: row.zoneName,
    name: row.name,
    location: row.location,
    scaleInfo: row.scaleInfo,
    coreFunction: row.coreFunction,
    culture: row.culture,
    description: row.description,
    tourTips: row.tourTips,
    ticketInfo: row.ticketInfo,
    remark: row.remark,
    enabled,
  })
  if (res.data.code === 200) {
    row.enabled = enabled
    ElMessage.success(enabled ? '已启用' : '已停用')
  } else {
    ElMessage.error(res.data.message)
  }
}

async function remove(row: SpotEntry) {
  await ElMessageBox.confirm(
    `确定删除景点「${row.spotCode} ${row.name}」吗？删除后不可恢复。`,
    '删除确认',
    { type: 'warning' },
  )
  const res = await deleteSpotEntry(row.id)
  if (res.data.code === 200) {
    ElMessage.success('已删除')
    load()
  } else {
    ElMessage.error(res.data.message)
  }
}

// ---------- 导入 ----------
const importVisible = ref(false)
const importing = ref(false)
const importFile = ref<File | null>(null)
const importSpotKey = ref('lingshan')

function onImportFileChange(file: UploadFile) {
  importFile.value = (file.raw as File) ?? null
}

async function submitImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!importSpotKey.value.trim()) {
    ElMessage.warning('请填写所属景区')
    return
  }
  importing.value = true
  try {
    const res = await importSpotDocx(importFile.value, importSpotKey.value.trim())
    if (res.data.code === 200) {
      const { created, updated, skipped } = res.data.data
      ElMessage.success(`导入完成：新增 ${created} 条，更新 ${updated} 条，跳过 ${skipped} 条`)
      importVisible.value = false
      importFile.value = null
      load()
      loadZones()
    } else {
      ElMessage.error(res.data.message)
    }
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 4px;
}
</style>
