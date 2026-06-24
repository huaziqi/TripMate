"use strict";
const common_vendor = require("../../common/vendor.js");
const api_spot = require("../../api/spot.js");
const api_match = require("../../api/match.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "match",
  setup(__props) {
    const { listSpots, searchSpots } = api_spot.useSpotApi();
    const step = common_vendor.ref("select");
    let transitioning = false;
    const spots = common_vendor.ref([]);
    const loading = common_vendor.ref(true);
    const keyword = common_vendor.ref("");
    const selectedSpot = common_vendor.ref(null);
    const myNickname = common_vendor.ref("");
    const myAvatarUrl = common_vendor.ref("");
    const myReady = common_vendor.ref(false);
    const partnerNickname = common_vendor.ref("");
    const partnerAvatarUrl = common_vendor.ref("");
    const partnerReady = common_vendor.ref(false);
    const countdown = common_vendor.ref(15);
    let countdownTimer = null;
    common_vendor.onMounted(async () => {
      try {
        spots.value = await listSpots();
      } finally {
        loading.value = false;
      }
    });
    common_vendor.onUnmounted(() => {
      if (!transitioning)
        api_match.disconnectMatch();
      clearCountdown();
    });
    async function onSearch() {
      loading.value = true;
      try {
        spots.value = await searchSpots(keyword.value);
      } finally {
        loading.value = false;
      }
    }
    function startMatch() {
      if (!selectedSpot.value)
        return;
      const token = common_vendor.index.getStorageSync("token");
      if (!token) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        return;
      }
      step.value = "waiting";
      api_match.connectMatch(token, onWsMessage, () => {
        common_vendor.index.getLocation({
          type: "gcj02",
          success: (loc) => {
            api_match.sendMatch("join", {
              spotId: selectedSpot.value.id,
              spotName: selectedSpot.value.name,
              latitude: loc.latitude,
              longitude: loc.longitude
            });
          },
          fail: () => {
            api_match.sendMatch("join", {
              spotId: selectedSpot.value.id,
              spotName: selectedSpot.value.name,
              latitude: 0,
              longitude: 0
            });
          }
        });
      });
    }
    function goSolo() {
      if (!selectedSpot.value)
        return;
      common_vendor.index.redirectTo({
        url: `/pages/trip/trip?spotId=${selectedSpot.value.id}&spotName=${encodeURIComponent(selectedSpot.value.name)}&solo=true`
      });
    }
    function onWsMessage(msg) {
      if (msg.type === "waiting") {
        step.value = "waiting";
      } else if (msg.type === "matched") {
        myNickname.value = msg.payload.myNickname ?? "我";
        myAvatarUrl.value = msg.payload.myAvatarUrl ?? "";
        myReady.value = false;
        partnerNickname.value = msg.payload.partnerNickname ?? "旅行者";
        partnerAvatarUrl.value = msg.payload.partnerAvatarUrl ?? "";
        partnerReady.value = false;
        step.value = "matched";
        startCountdown();
      } else if (msg.type === "partnerConfirmed") {
        partnerReady.value = true;
      } else if (msg.type === "confirmed") {
        clearCountdown();
        transitioning = true;
        common_vendor.index.redirectTo({
          url: `/pages/trip/trip?spotId=${selectedSpot.value.id}&spotName=${encodeURIComponent(selectedSpot.value.name)}&partnerNickname=${encodeURIComponent(partnerNickname.value)}`
        });
      } else if (msg.type === "partnerCancelled") {
        clearCountdown();
        step.value = "select";
        common_vendor.index.showToast({ title: "搭子取消了，重新匹配", icon: "none" });
      }
    }
    function confirmMatch() {
      if (myReady.value)
        return;
      myReady.value = true;
      api_match.sendMatch("confirm");
    }
    function cancelMatch() {
      clearCountdown();
      api_match.sendMatch("cancel");
      api_match.disconnectMatch();
      step.value = "select";
    }
    function startCountdown() {
      countdown.value = 15;
      countdownTimer = setInterval(() => {
        countdown.value--;
        if (countdown.value <= 0) {
          clearCountdown();
          cancelMatch();
          common_vendor.index.showToast({ title: "确认超时，已自动取消", icon: "none" });
        }
      }, 1e3);
    }
    function clearCountdown() {
      if (countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }
    return (_ctx, _cache) => {
      var _a, _b, _c, _d;
      return common_vendor.e({
        a: step.value === "select"
      }, step.value === "select" ? common_vendor.e({
        b: common_vendor.o([($event) => keyword.value = $event.detail.value, onSearch], "3c"),
        c: keyword.value,
        d: loading.value
      }, loading.value ? {} : spots.value.length === 0 ? {} : {}, {
        e: spots.value.length === 0,
        f: common_vendor.f(spots.value, (spot, k0, i0) => {
          var _a2;
          return {
            a: common_vendor.t(spot.name),
            b: common_vendor.t(spot.region || spot.address),
            c: spot.id,
            d: ((_a2 = selectedSpot.value) == null ? void 0 : _a2.id) === spot.id ? 1 : "",
            e: common_vendor.o(($event) => selectedSpot.value = spot, spot.id)
          };
        }),
        g: !selectedSpot.value,
        h: common_vendor.o(goSolo, "b3"),
        i: common_vendor.t(selectedSpot.value ? "开始匹配" : "请先选择景点"),
        j: !selectedSpot.value,
        k: common_vendor.o(startMatch, "1b")
      }) : step.value === "waiting" ? {
        m: common_vendor.t((_a = selectedSpot.value) == null ? void 0 : _a.name),
        n: common_vendor.o(cancelMatch, "b4")
      } : step.value === "matched" ? common_vendor.e({
        p: myAvatarUrl.value
      }, myAvatarUrl.value ? {
        q: myAvatarUrl.value
      } : {
        r: common_vendor.t(((_b = myNickname.value) == null ? void 0 : _b[0]) ?? "我")
      }, {
        s: common_vendor.t(myNickname.value),
        t: common_vendor.t(myReady.value ? "✓ 已准备" : "待确认"),
        v: myReady.value ? 1 : "",
        w: partnerAvatarUrl.value
      }, partnerAvatarUrl.value ? {
        x: partnerAvatarUrl.value
      } : {
        y: common_vendor.t(((_c = partnerNickname.value) == null ? void 0 : _c[0]) ?? "他")
      }, {
        z: common_vendor.t(partnerNickname.value),
        A: common_vendor.t(partnerReady.value ? "✓ 已准备" : "待确认"),
        B: partnerReady.value ? 1 : "",
        C: common_vendor.t((_d = selectedSpot.value) == null ? void 0 : _d.name),
        D: common_vendor.t(countdown.value),
        E: common_vendor.t(myReady.value ? "已确认出发" : "确认出发"),
        F: myReady.value,
        G: common_vendor.o(confirmMatch, "cb"),
        H: common_vendor.o(cancelMatch, "52")
      }) : {}, {
        l: step.value === "waiting",
        o: step.value === "matched"
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-d5601611"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/match/match.js.map
