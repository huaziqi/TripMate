"use strict";
const common_vendor = require("./common/vendor.js");
const locales_zhCN = require("./locales/zh-CN.js");
const locales_enUS = require("./locales/en-US.js");
const i18n = common_vendor.createI18n({
  legacy: false,
  locale: "zh-CN",
  fallbackLocale: "en-US",
  messages: {
    "zh-CN": locales_zhCN.zhCN,
    "en-US": locales_enUS.enUS
  }
});
exports.i18n = i18n;
//# sourceMappingURL=../.sourcemap/mp-weixin/i18n.js.map
