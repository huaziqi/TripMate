"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
const composables_useAuth = require("./composables/useAuth.js");
const i18n = require("./i18n.js");
if (!Math) {
  "./pages/index/index.js";
  "./pages/guide/guide.js";
  "./pages/elder/elder.js";
  "./pages/language/language.js";
  "./pages/mine/mine.js";
}
const _sfc_main = {
  onLaunch: function() {
    const { loadFromStorage } = composables_useAuth.useAuth();
    loadFromStorage();
  },
  onShow: function() {
  },
  onHide: function() {
  }
};
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  app.use(i18n.i18n);
  return {
    app
  };
}
createApp().app.mount("#app");
exports.createApp = createApp;
//# sourceMappingURL=../.sourcemap/mp-weixin/app.js.map
