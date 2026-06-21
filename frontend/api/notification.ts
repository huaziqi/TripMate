import { useApi } from '@/utils/useApi'

const { get, post } = useApi()

export interface NotificationItem {
  id: number
  type: 'LIKE_POST' | 'COMMENT_POST' | 'NEW_FOLLOWER' | 'MENTION_COMMENT'
  fromUser?: { id: number; nickname: string; avatarUrl?: string }
  postId?: number
  postTitle?: string
  commentContent?: string
  read: boolean
  createdAt: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export function fetchNotifications(page = 0, size = 20) {
  return get<PageResult<NotificationItem>>(`/api/notifications?page=${page}&size=${size}`)
}

export function fetchUnreadCount() {
  return get<{ count: number }>('/api/notifications/unread-count')
}

export function markAllRead() {
  return post<void>('/api/notifications/read-all')
}
