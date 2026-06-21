// frontend/api/post.ts
import { useApi } from '@/utils/useApi'

const { get, post, del } = useApi()

export interface PostAuthor {
  id: number
  nickname: string
  avatarUrl: string
}

export interface PostItem {
  id: number
  title: string
  content: string
  category: string
  coverUrl: string
  imageUrls: string[]
  viewCount: number
  likeCount: number
  commentCount: number
  createdAt: string
  author: PostAuthor
  liked: boolean
  favorited: boolean
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface CommentItem {
  id: number
  content: string
  createdAt: string
  author: PostAuthor
  replies?: CommentItem[]
}

export function fetchPosts(params: {
  category?: string
  sort?: 'new' | 'hot'
  page?: number
  size?: number
}) {
  return get<PageResult<PostItem>>('/api/posts', params as any)
}

export function fetchPostDetail(id: number) {
  return get<PostItem>(`/api/posts/${id}`)
}

export function createPost(data: {
  title: string
  content: string
  category: string
  imageUrls: string[]
}) {
  return post<PostItem>('/api/posts', data)
}

export function deletePost(id: number) {
  return post<void>(`/api/posts/${id}/delete`)
}

export function toggleLike(id: number) {
  return post<{ liked: boolean; likeCount: number }>(`/api/posts/${id}/like`)
}

export function toggleFavorite(id: number) {
  return post<{ favorited: boolean }>(`/api/posts/${id}/favorite`)
}

export function fetchComments(id: number, params: { page?: number; size?: number }) {
  return get<PageResult<CommentItem>>(`/api/posts/${id}/comments`, params as any)
}

export function createComment(id: number, content: string, parentId?: number) {
  return post<CommentItem>(`/api/posts/${id}/comments`, { content, parentId })
}

export function fetchMyPosts(params: { page?: number; size?: number }) {
  return get<PageResult<PostItem>>('/api/posts/my', params as any)
}

export function fetchMyFavorites(params: { page?: number; size?: number }) {
  return get<PageResult<PostItem>>('/api/posts/my/favorites', params as any)
}
