import http from './http'

export interface AdminPost {
  id: number
  title: string
  content: string
  category: string
  coverUrl?: string
  likeCount: number
  commentCount: number
  viewCount: number
  status: string
  createdAt: string
  authorId: number
  authorNickname: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export function listPosts(params: { page?: number; size?: number; status?: string }) {
  return http.get('/admin/posts', { params })
}

export function deletePost(id: number) {
  return http.post(`/admin/posts/${id}/delete`)
}

export function restorePost(id: number) {
  return http.post(`/admin/posts/${id}/restore`)
}
