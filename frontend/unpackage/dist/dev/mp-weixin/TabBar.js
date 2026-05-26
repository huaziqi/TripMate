"use strict";
const common_vendor = require("./common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "TabBar",
  props: {
    active: { default: "home" }
  },
  setup(__props) {
    const props = __props;
    const { t } = common_vendor.useI18n();
    const tabs = [
      {
        key: "home",
        icon: "🏠",
        url: "/pages/index/index"
      },
      {
        key: "guide",
        icon: "🗺️",
        url: "/pages/guide/guide"
      },
      {
        key: "elder",
        icon: "👴",
        url: "/pages/elder/elder"
      },
      {
        key: "language",
        icon: "🌐",
        url: "/pages/language/language"
      },
      {
        key: "mine",
        icon: "👤",
        url: "/pages/mine/mine"
      }
    ];
    const switchTab = (item) => {
      if (item.key === props.active)
        return;
      common_vendor.index.switchTab({
        url: item.url
      });
    };
    return (_ctx, _cache) => {
      return {
        a: common_vendor.f(tabs, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.icon),
            b: common_vendor.t(common_vendor.unref(t)(`tabbar.${item.key}`)),
            c: item.key,
            d: common_vendor.o(($event) => switchTab(item), item.key)
          };
        }),
        b: _ctx.active === props.active ? 1 : ""
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e632d448"]]);
exports.MiniProgramPage = MiniProgramPage;
//# sourceMappingURL=../.sourcemap/mp-weixin/TabBar.js.map
