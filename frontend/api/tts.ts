import { useApi } from '@/utils/useApi'

export interface TtsResponse {
  audioUrl: string
  sessionId: string
}

export async function synthesizeSpeech(text: string): Promise<TtsResponse> {
  const { post } = useApi()

  const res: any = await post(
    '/api/tts/synthesize',
    { text },
    { withToken: false }
  )

  if (res?.audioUrl) {
    return res
  }

  if (res?.data?.audioUrl) {
    return res.data
  }

  throw new Error('TTS接口未返回 audioUrl')
}