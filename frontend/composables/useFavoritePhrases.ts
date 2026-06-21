import { ref } from 'vue'

const FAVORITES_KEY = 'favorite_phrases'

export interface FavoritePhrase {
  zh: string
  en: string
  category: string
}

const favorites = ref<FavoritePhrase[]>([])

try {
  const stored = uni.getStorageSync(FAVORITES_KEY)
  if (Array.isArray(stored)) {
    favorites.value = stored
  }
} catch {
  // ignore
}

function save() {
  try {
    uni.setStorageSync(FAVORITES_KEY, favorites.value)
  } catch {
    // ignore
  }
}

export function useFavoritePhrases() {
  function isFavorite(zh: string): boolean {
    return favorites.value.some(f => f.zh === zh)
  }

  function toggleFavorite(phrase: FavoritePhrase) {
    const idx = favorites.value.findIndex(f => f.zh === phrase.zh)
    if (idx >= 0) {
      favorites.value.splice(idx, 1)
    } else {
      favorites.value.push(phrase)
      uni.vibrateShort({ type: 'medium' })
    }
    save()
  }

  function removeFavorite(zh: string) {
    favorites.value = favorites.value.filter(f => f.zh !== zh)
    save()
  }

  return { favorites, isFavorite, toggleFavorite, removeFavorite }
}
