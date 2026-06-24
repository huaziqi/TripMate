import { useApi } from '@/utils/useApi'

const { post } = useApi()

export interface TtsResult {
  audioUrl: string
  sessionId: string
}

export function synthesizeSpeech(text: string, lang: string) {
  return post<TtsResult>('/api/tts/synthesize', { text, lang } as any)
}