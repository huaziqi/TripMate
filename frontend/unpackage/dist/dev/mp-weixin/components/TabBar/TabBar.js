"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "TabBar",
  props: {
    active: { default: "home" }
  },
  setup(__props) {
    const props = __props;
    const { t } = common_vendor.useI18n();
    const leftTabs = [
      { key: "home", icon: "🏠", url: "/pages/index/index" },
      { key: "guide", icon: "🗺️", url: "/pages/guide/guide" }
    ];
    const rightTabs = [
      { key: "language", icon: "🌐", url: "/pages/language/language" },
      { key: "mine", icon: "👤", url: "/pages/mine/mine" }
    ];
    function switchTab(url, key) {
      if (key === props.active)
        return;
      common_vendor.index.redirectTo({ url });
    }
    function goMatch() {
      common_vendor.index.navigateTo({ url: "/pages/match/match" });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.f(leftTabs, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.icon),
            b: common_vendor.t(common_vendor.unref(t)(`tabbar.${item.key}`)),
            c: item.key,
            d: item.key === props.active ? 1 : "",
            e: common_vendor.o(($event) => switchTab(item.url, item.key), item.key)
          };
        }),
        b: common_vendor.t(common_vendor.unref(t)("tabbar.match")),
        c: common_vendor.o(goMatch),
        d: common_vendor.f(rightTabs, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.icon),
            b: common_vendor.t(common_vendor.unref(t)(`tabbar.${item.key}`)),
            c: item.key,
            d: item.key === props.active ? 1 : "",
            e: common_vendor.o(($event) => switchTab(item.url, item.key), item.key)
          };
        })
      };
    };
  }
});
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e632d448"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/components/TabBar/TabBar.js.map
