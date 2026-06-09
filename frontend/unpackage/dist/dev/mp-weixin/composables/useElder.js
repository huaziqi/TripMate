"use strict";
const common_vendor = require("../common/vendor.js");
const ELDER_MODE_KEY = "elder_mode";
const ELDER_SCALE_KEY = "elder_font_scale";
const ELDER_SCALE = 1.4;
const NORMAL_SCALE = 1;
const isElderMode = common_vendor.ref(false);
const fontScale = common_vendor.ref(NORMAL_SCALE);
try {
  const stored = common_vendor.index.getStorageSync(ELDER_MODE_KEY);
  if (stored === true) {
    isElderMode.value = true;
    fontScale.value = ELDER_SCALE;
  }
} catch {
}
function persist() {
  common_vendor.index.setStorageSync(ELDER_MODE_KEY, isElderMode.value);
  common_vendor.index.setStorageSync(ELDER_SCALE_KEY, fontScale.value);
}
function useElder() {
  function enableElderMode() {
    isElderMode.value = true;
    fontScale.value = ELDER_SCALE;
    persist();
  }
  function disableElderMode() {
    isElderMode.value = false;
    fontScale.value = NORMAL_SCALE;
    persist();
  }
  function toggleElderMode() {
    isElderMode.value ? disableElderMode() : enableElderMode();
  }
  function rpx(base) {
    return `${Math.round(base * fontScale.value)}rpx`;
  }
  return {
    isElderMode,
    fontScale,
    enableElderMode,
    disableElderMode,
    toggleElderMode,
    rpx
  };
}
exports.useElder = useElder;
//# sourceMappingURL=../../.sourcemap/mp-weixin/composables/useElder.js.map
