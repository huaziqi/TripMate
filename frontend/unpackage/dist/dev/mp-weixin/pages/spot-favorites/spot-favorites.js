"use strict";
const common_vendor = require("../../common/vendor.js");
const api_favorite = require("../../api/favorite.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "spot-favorites",
  setup(__props) {
    const loading = common_vendor.ref(false);
    const favoriteSpots = common_vendor.ref([]);
    common_vendor.onShow(() => {
      loadFavorites();
    });
    async function loadFavorites() {
      loading.value = true;
      try {
        favoriteSpots.value = await api_favorite.getFavoriteSpots();
        common_vendor.index.__f__("log", "at pages/spot-favorites/spot-favorites.vue:63", "我的景点收藏：", favoriteSpots.value);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/spot-favorites/spot-favorites.vue:65", "加载景点收藏失败：", error);
        common_vendor.index.showToast({
          title: "加载收藏失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    }
    function goSpotDetail(id) {
      common_vendor.index.navigateTo({
        url: `/pages/spot-detail/spot-detail?id=${id}`
      });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: loading.value
      }, loading.value ? {} : favoriteSpots.value.length === 0 ? {} : {
        c: common_vendor.f(favoriteSpots.value, (spot, k0, i0) => {
          return {
            a: common_vendor.t(spot.name),
            b: common_vendor.t(spot.category),
            c: common_vendor.t(spot.address || "暂无地址"),
            d: common_vendor.t(spot.description || "暂无简介"),
            e: common_vendor.o(($event) => goSpotDetail(spot.id), spot.id),
            f: spot.id,
            g: common_vendor.o(($event) => goSpotDetail(spot.id), spot.id)
          };
        })
      }, {
        b: favoriteSpots.value.length === 0
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-7360ba23"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/spot-favorites/spot-favorites.js.map
