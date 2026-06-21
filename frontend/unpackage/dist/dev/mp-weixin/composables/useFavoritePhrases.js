"use strict";
const common_vendor = require("../common/vendor.js");
const FAVORITES_KEY = "favorite_phrases";
const favorites = common_vendor.ref([]);
try {
  const stored = common_vendor.index.getStorageSync(FAVORITES_KEY);
  if (Array.isArray(stored)) {
    favorites.value = stored;
  }
} catch {
}
function save() {
  try {
    common_vendor.index.setStorageSync(FAVORITES_KEY, favorites.value);
  } catch {
  }
}
function useFavoritePhrases() {
  function isFavorite(zh) {
    return favorites.value.some((f) => f.zh === zh);
  }
  function toggleFavorite(phrase) {
    const idx = favorites.value.findIndex((f) => f.zh === phrase.zh);
    if (idx >= 0) {
      favorites.value.splice(idx, 1);
    } else {
      favorites.value.push(phrase);
      common_vendor.index.vibrateShort({ type: "medium" });
    }
    save();
  }
  function removeFavorite(zh) {
    favorites.value = favorites.value.filter((f) => f.zh !== zh);
    save();
  }
  return { favorites, isFavorite, toggleFavorite, removeFavorite };
}
exports.useFavoritePhrases = useFavoritePhrases;
//# sourceMappingURL=../../.sourcemap/mp-weixin/composables/useFavoritePhrases.js.map
