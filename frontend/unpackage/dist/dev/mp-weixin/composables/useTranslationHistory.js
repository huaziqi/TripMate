"use strict";
const common_vendor = require("../common/vendor.js");
const HISTORY_KEY = "translation_history";
const MAX_HISTORY = 30;
const history = common_vendor.ref([]);
try {
  const stored = common_vendor.index.getStorageSync(HISTORY_KEY);
  if (Array.isArray(stored)) {
    history.value = stored;
  }
} catch {
}
function saveHistory() {
  try {
    common_vendor.index.setStorageSync(HISTORY_KEY, history.value);
  } catch {
  }
}
function useTranslationHistory() {
  function addHistory(item) {
    const newItem = {
      ...item,
      id: Date.now().toString(),
      timestamp: Date.now()
    };
    history.value = history.value.filter(
      (h) => !(h.sourceText === item.sourceText && h.from === item.from && h.to === item.to)
    );
    history.value.unshift(newItem);
    if (history.value.length > MAX_HISTORY) {
      history.value = history.value.slice(0, MAX_HISTORY);
    }
    saveHistory();
  }
  function removeHistory(id) {
    history.value = history.value.filter((h) => h.id !== id);
    saveHistory();
  }
  function clearHistory() {
    history.value = [];
    saveHistory();
  }
  function formatTime(timestamp) {
    const date = new Date(timestamp);
    const now = /* @__PURE__ */ new Date();
    const diff = now.getTime() - timestamp;
    if (diff < 6e4)
      return "刚刚";
    if (diff < 36e5)
      return `${Math.floor(diff / 6e4)} 分钟前`;
    if (diff < 864e5)
      return `${Math.floor(diff / 36e5)} 小时前`;
    const month = (date.getMonth() + 1).toString().padStart(2, "0");
    const day = date.getDate().toString().padStart(2, "0");
    return `${month}-${day}`;
  }
  return { history, addHistory, removeHistory, clearHistory, formatTime };
}
exports.useTranslationHistory = useTranslationHistory;
//# sourceMappingURL=../../.sourcemap/mp-weixin/composables/useTranslationHistory.js.map
