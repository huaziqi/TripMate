import http from './http'

export type KnowledgeCategory = 'EXPLANATION' | 'HISTORY' | 'FAQ' | 'OTHER'

export const CATEGORY_LABELS: Record<KnowledgeCategory, string> = {
  EXPLANATION: '讲解词',
  HISTORY: '文史资料',
  FAQ: '常见问题',
  OTHER: '其他',
}

export interface KnowledgeDocItem {
  id: number
  spotKey: string | null
  title: string
  category: KnowledgeCategory
  enabled: boolean
  sourceFileName: string | null
  contentLength: number
  preview: string
  createdAt: string
  updatedAt: string
}

export interface KnowledgeDoc extends Omit<KnowledgeDocItem, 'contentLength' | 'preview'> {
  content: string
}

export interface SaveKnowledgeDoc {
  spotKey: string | null
  title: string
  category: KnowledgeCategory
  content: string
  enabled: boolean
}

export function listKnowledge(params: {
  spotKey?: string
  category?: KnowledgeCategory | ''
  keyword?: string
}) {
  return http.get<{ code: number; data: KnowledgeDocItem[] }>('/admin/knowledge', { params })
}

export function getKnowledge(id: number) {
  return http.get<{ code: number; message: string; data: KnowledgeDoc }>(`/admin/knowledge/${id}`)
}

export function createKnowledge(data: SaveKnowledgeDoc) {
  return http.post<{ code: number; message: string; data: KnowledgeDoc }>('/admin/knowledge', data)
}

export function updateKnowledge(id: number, data: SaveKnowledgeDoc) {
  return http.put<{ code: number; message: string; data: KnowledgeDoc }>(`/admin/knowledge/${id}`, data)
}

export function deleteKnowledge(id: number) {
  return http.delete<{ code: number; message: string }>(`/admin/knowledge/${id}`)
}

export function uploadKnowledge(form: {
  file: File
  spotKey?: string
  category?: KnowledgeCategory
  title?: string
}) {
  const fd = new FormData()
  fd.append('file', form.file)
  if (form.spotKey) fd.append('spotKey', form.spotKey)
  if (form.category) fd.append('category', form.category)
  if (form.title) fd.append('title', form.title)
  return http.post<{ code: number; message: string; data: KnowledgeDoc }>(
    '/admin/knowledge/upload',
    fd,
  )
}

// ==================== 景点结构化知识 ====================

export interface SpotEntry {
  id: number
  spotKey: string
  spotCode: string
  zoneName: string | null
  name: string
  location: string | null
  scaleInfo: string | null
  coreFunction: string | null
  culture: string | null
  description: string | null
  tourTips: string | null
  ticketInfo: string | null
  remark: string | null
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export type SaveSpotEntry = Omit<SpotEntry, 'id' | 'createdAt' | 'updatedAt'>

/** 景点知识字段中文标签（与结构化数据集表格列对应），也是编辑表单子方块的顺序 */
export const SPOT_FIELD_LABELS = [
  ['location', '具体位置'],
  ['scaleInfo', '建筑/景观参数'],
  ['coreFunction', '核心功能'],
  ['culture', '文化内涵'],
  ['description', '详细介绍'],
  ['tourTips', '游玩亮点'],
  ['ticketInfo', '演艺/开放信息'],
  ['remark', '备注'],
] as const

export type SpotFieldKey = (typeof SPOT_FIELD_LABELS)[number][0]

export function listSpotEntries(params: {
  spotKey?: string
  zoneName?: string
  keyword?: string
}) {
  return http.get<{ code: number; data: SpotEntry[] }>('/admin/knowledge/spots', { params })
}

export function listSpotZones() {
  return http.get<{ code: number; data: string[] }>('/admin/knowledge/spots/zones')
}

export function createSpotEntry(data: SaveSpotEntry) {
  return http.post<{ code: number; message: string; data: SpotEntry }>(
    '/admin/knowledge/spots',
    data,
  )
}

export function updateSpotEntry(id: number, data: SaveSpotEntry) {
  return http.put<{ code: number; message: string; data: SpotEntry }>(
    `/admin/knowledge/spots/${id}`,
    data,
  )
}

export function deleteSpotEntry(id: number) {
  return http.delete<{ code: number; message: string }>(`/admin/knowledge/spots/${id}`)
}

export function importSpotDocx(file: File, spotKey: string) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('spotKey', spotKey)
  return http.post<{
    code: number
    message: string
    data: { created: number; updated: number; skipped: number }
  }>('/admin/knowledge/spots/import', fd)
}
