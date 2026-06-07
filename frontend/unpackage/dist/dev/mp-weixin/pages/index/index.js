"use strict";
const common_vendor = require("../../common/vendor.js");
if (!Math) {
  (WeatherCard + TabBar)();
}
const TabBar = () => "../../components/TabBar/TabBar.js";
const WeatherCard = () => "../../components/WeatherCard/WeatherCard.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "index",
  setup(__props) {
    function goTo(url) {
      common_vendor.index.navigateTo({ url });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(($event) => goTo("/pages/badges/badges"), "1c"),
        b: common_vendor.o(($event) => goTo("/pages/guide/guide"), "6b"),
        c: common_vendor.o(($event) => goTo("/pages/language/language"), "c9"),
        d: common_vendor.o(($event) => common_vendor.index.showToast({
          title: "敬请期待",
          icon: "none"
        }), "e8"),
        e: common_vendor.p({
          active: "home"
        })
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1cf27b2a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map
