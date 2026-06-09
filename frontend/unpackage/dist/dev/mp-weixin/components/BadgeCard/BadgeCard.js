"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "BadgeCard",
  props: {
    badge: {},
    size: {}
  },
  setup(__props) {
    const props = __props;
    const rarityColor = {
      COMMON: "#9e9e9e",
      RARE: "#2196f3",
      EPIC: "#9c27b0",
      LEGENDARY: "#ffd700"
    };
    const circleStyle = common_vendor.computed(() => {
      if (!props.badge.unlocked)
        return { background: "#ddd" };
      const color = rarityColor[props.badge.rarity] ?? "#9e9e9e";
      return { background: `linear-gradient(135deg, ${color}99, ${color})` };
    });
    return (_ctx, _cache) => {
      return {
        a: common_vendor.t(_ctx.badge.unlocked ? _ctx.badge.icon : "❓"),
        b: common_vendor.s(circleStyle.value),
        c: common_vendor.n(`size-${_ctx.size}`),
        d: common_vendor.n({
          locked: !_ctx.badge.unlocked
        })
      };
    };
  }
});
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-650a979c"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/components/BadgeCard/BadgeCard.js.map
