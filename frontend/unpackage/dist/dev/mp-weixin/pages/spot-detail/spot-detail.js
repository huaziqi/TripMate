"use strict";
const common_vendor = require("../../common/vendor.js");
const api_spot = require("../../api/spot.js");
const api_favorite = require("../../api/favorite.js");
const api_history = require("../../api/history.js");
const composables_useAuth = require("../../composables/useAuth.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "spot-detail",
  setup(__props) {
    const loading = common_vendor.ref(false);
    const spot = common_vendor.ref(null);
    const isFavorited = common_vendor.ref(false);
    const { authState, login } = composables_useAuth.useAuth();
    common_vendor.onLoad((options) => {
      const id = Number(options == null ? void 0 : options.id);
      if (!id) {
        common_vendor.index.showToast({
          title: "景点ID无效",
          icon: "none"
        });
        return;
      }
      loadSpotDetail(id);
    });
    async function loadSpotDetail(id) {
      loading.value = true;
      try {
        spot.value = await api_spot.getScenicSpotById(id);
        if (authState.isLoggedIn) {
          try {
            isFavorited.value = await api_favorite.checkFavorite(id);
            await api_history.addHistory(
              "VIEW_SPOT",
              id,
              `查看了景点：${spot.value.name}`
            );
          } catch (authError) {
            common_vendor.index.__f__("error", "at pages/spot-detail/spot-detail.vue:140", "收藏状态或历史记录处理失败：", authError);
          }
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/spot-detail/spot-detail.vue:144", "加载景点详情失败：", error);
        common_vendor.index.showToast({
          title: "加载失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    }
    function backToMap() {
      common_vendor.index.navigateBack();
    }
    function playGuide() {
      common_vendor.index.showToast({
        title: "语音导览后续接入",
        icon: "none"
      });
    }
    async function favoriteSpot() {
      if (!spot.value) {
        return;
      }
      if (!authState.isLoggedIn) {
        try {
          await login();
        } catch {
          common_vendor.index.showToast({
            title: "请先登录",
            icon: "none"
          });
          return;
        }
      }
      try {
        if (isFavorited.value) {
          await api_favorite.removeFavorite(spot.value.id);
          isFavorited.value = false;
          common_vendor.index.showToast({
            title: "已取消收藏",
            icon: "none"
          });
        } else {
          await api_favorite.addFavorite(spot.value.id);
          isFavorited.value = true;
          await api_history.addHistory(
            "FAVORITE_SPOT",
            spot.value.id,
            `收藏了景点：${spot.value.name}`
          );
          common_vendor.index.showToast({
            title: "收藏成功",
            icon: "success"
          });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/spot-detail/spot-detail.vue:208", "收藏操作失败：", error);
        common_vendor.index.showToast({
          title: "操作失败",
          icon: "none"
        });
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: loading.value
      }, loading.value ? {} : !spot.value ? {} : common_vendor.e({
        c: spot.value.imageUrl
      }, spot.value.imageUrl ? {
        d: spot.value.imageUrl
      } : {}, {
        e: common_vendor.t(spot.value.name),
        f: common_vendor.t(spot.value.category),
        g: common_vendor.t(spot.value.address || "暂无地址"),
        h: common_vendor.t(spot.value.region || "暂无地区"),
        i: common_vendor.t(spot.value.description || "暂无景点简介"),
        j: common_vendor.t(spot.value.latitude),
        k: common_vendor.t(spot.value.longitude),
        l: common_vendor.o(backToMap),
        m: common_vendor.o(playGuide),
        n: common_vendor.t(isFavorited.value ? "已收藏" : "收藏"),
        o: common_vendor.o(favoriteSpot)
      }), {
        b: !spot.value
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1d237977"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/spot-detail/spot-detail.js.map
