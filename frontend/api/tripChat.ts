import { useApi } from '@/utils/useApi'

const { post } = useApi()

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface TripChatResponse {
  text: string
  audioUrl: string
}

export function sendTripChat(params: {
  message: string
  spotName: string
  history?: ChatMessage[]
}) {
  return post<TripChatResponse>('/api/trip/chat', params as any)
}
