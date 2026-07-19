import { useApi } from '@/utils/useApi'

const { post } = useApi()

export interface PhonemeItem {
  phone: string
  start: number
  end: number
}

export interface VisemeItem {
  viseme: string
  start: number
  end: number
}

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

export interface TtsResponse {
  audioUrl: string
  sessionId: string
  duration?: number
  phonemes?: PhonemeItem[]
  visemes?: VisemeItem[]
}

export function synthesizeSpeech(payload: TtsRequest) {
  return post<TtsResult>('/api/tts/synthesize', payload)
}


export function synthesizeWithTimeline(payload: TtsRequest) {
    return post<TtsResponse>('/api/tts/synthesize-with-timeline', payload)
  }