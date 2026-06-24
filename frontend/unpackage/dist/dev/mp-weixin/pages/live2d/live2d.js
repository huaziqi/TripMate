"use strict";
const common_vendor = require("../../common/vendor.js");
if (!Array) {
  const _component_live2d_view = common_vendor.resolveComponent("live2d-view");
  _component_live2d_view();
}
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "live2d",
  setup(__props) {
    const inputText = common_vendor.ref("");
    function handleReady(e) {
      common_vendor.index.__f__("log", "at pages/live2d/live2d.vue:57", "[live2d-page] component ready:", e);
    }
    function handleError(e) {
      common_vendor.index.__f__("error", "at pages/live2d/live2d.vue:61", "[live2d-page] component error:", e);
    }
    function handleSend() {
      const text = inputText.value.trim();
      if (!text)
        return;
      common_vendor.index.__f__("log", "at pages/live2d/live2d.vue:68", "[chat] send:", text);
      inputText.value = "";
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(handleReady, "c9"),
        b: common_vendor.o(handleError, "13"),
        c: common_vendor.p({
          className: "live2d-box",
          autoInit: true,
          stageWidth: 750
        }),
        d: common_vendor.o(handleSend, "9b"),
        e: inputText.value,
        f: common_vendor.o(($event) => inputText.value = $event.detail.value, "a1"),
        g: common_vendor.o(handleSend, "c0")
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-25246cd7"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/live2d/live2d.js.map
