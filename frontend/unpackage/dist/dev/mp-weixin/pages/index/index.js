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
    function goMapTest() {
      common_vendor.index.navigateTo({
        url: "/pages/map-test/map-test"
      });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(goMapTest),
        b: common_vendor.p({
          active: "home"
        })
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1cf27b2a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map
