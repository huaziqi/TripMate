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
      common_vendor.index.navigateTo({
        url
      });
    }
    function goMapTest() {
      common_vendor.index.navigateTo({
        url: "/pages/map-test/map-test"
      });
    }
    function showComingSoon() {
      common_vendor.index.showToast({
        title: "敬请期待",
        icon: "none"
      });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(($event) => goTo("/pages/badges/badges")),
        b: common_vendor.o(($event) => goTo("/pages/guide/guide")),
        c: common_vendor.o(($event) => goTo("/pages/language/language")),
        d: common_vendor.o(showComingSoon),
        e: common_vendor.o(goMapTest),
        f: common_vendor.p({
          active: "home"
        })
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1cf27b2a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map
