"use strict";
const common_vendor = require("../../common/vendor.js");
const api_route = require("../../api/route.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "route",
  setup(__props) {
    const loading = common_vendor.ref(false);
    const routes = common_vendor.ref([]);
    const currentSpotName = common_vendor.ref("");
    const filteredRoutes = common_vendor.computed(() => {
      if (!currentSpotName.value.trim()) {
        return routes.value;
      }
      const keyword = currentSpotName.value.trim();
      const matched = routes.value.filter(
        (route) => route.spots.some(
          (spot) => spot.displayName.includes(keyword) || spot.name.includes(keyword)
        )
      );
      return matched.length > 0 ? matched : routes.value;
    });
    common_vendor.onLoad((options) => {
      if (options == null ? void 0 : options.spotName) {
        currentSpotName.value = decodeURIComponent(options.spotName);
      }
    });
    common_vendor.onShow(() => {
      loadRoutes();
    });
    async function loadRoutes() {
      loading.value = true;
      try {
        routes.value = await api_route.getRecommendRoutes();
        common_vendor.index.__f__("log", "at pages/route/route.vue:124", "推荐路线：", routes.value);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/route/route.vue:126", "加载推荐路线失败：", error);
        common_vendor.index.showToast({
          title: "加载路线失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    }
    function goSpotDetail(spot) {
      if (!spot.matched || !spot.spotId) {
        common_vendor.index.showToast({
          title: "该景点暂未配置",
          icon: "none"
        });
        return;
      }
      common_vendor.index.navigateTo({
        url: `/pages/spot-detail/spot-detail?id=${spot.spotId}`
      });
    }
    function hasMissingSpot(route) {
      return route.spots.some((item) => !item.matched);
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: currentSpotName.value
      }, currentSpotName.value ? {
        b: common_vendor.t(currentSpotName.value)
      } : {}, {
        c: loading.value
      }, loading.value ? {} : routes.value.length === 0 ? {} : {
        e: common_vendor.f(filteredRoutes.value, (route, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(route.name),
            b: common_vendor.t(route.description),
            c: common_vendor.t(route.theme),
            d: common_vendor.t(route.estimatedTime),
            e: common_vendor.f(route.spots, (spot, index, i1) => {
              return common_vendor.e({
                a: common_vendor.t(spot.displayName),
                b: !spot.matched ? 1 : "",
                c: common_vendor.o(($event) => goSpotDetail(spot), index),
                d: index < route.spots.length - 1
              }, index < route.spots.length - 1 ? {} : {}, {
                e: index
              });
            }),
            f: common_vendor.t(route.guideText),
            g: hasMissingSpot(route)
          }, hasMissingSpot(route) ? {} : {}, {
            h: route.id
          });
        })
      }, {
        d: routes.value.length === 0
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-9b6e348a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/route/route.js.map
