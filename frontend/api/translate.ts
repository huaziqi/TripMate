import { useApi } from '@/utils/useApi'

const { post } = useApi()

export interface TranslationResult {
  translatedText: string
  detectedLang: string
  from: string
  to: string
}

export function translateText(text: string, from: string, to: string) {
  return post<TranslationResult>('/api/translate', { text, from, to }, { withToken: false })
}
