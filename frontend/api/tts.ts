import { useApi } from '@/utils/useApi'

const { post } = useApi()

export interface TtsResult {
  audioUrl: string
  sessionId: string
}

export interface TtsRequest {
  text: string
  lang?: string
  voiceType?: number
  speed?: number
  volume?: number
  pitch?: number
}

export function synthesizeSpeech(payload: TtsRequest) {
  return post<TtsResult>('/api/tts/synthesize', payload)
}