"use strict";
const common_vendor = require("../../common/vendor.js");
const api_location = require("../../api/location.js");
const api_spot = require("../../api/spot.js");
const defaultLatitude = 29.8266;
const defaultLongitude = 106.422;
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "map",
  setup(__props, { expose: __expose }) {
    const latitude = common_vendor.ref(defaultLatitude);
    const longitude = common_vendor.ref(defaultLongitude);
    const currentAddress = common_vendor.ref("正在获取地址");
    const mapType = common_vendor.ref("normal");
    const isSatellite = common_vendor.computed(() => {
      return mapType.value === "satellite";
    });
    const keyword = common_vendor.ref("");
    const searching = common_vendor.ref(false);
    const searchResults = common_vendor.ref([]);
    common_vendor.ref(null);
    const nearbySpots = common_vendor.ref([]);
    const loadingNearby = common_vendor.ref(false);
    const userLatitude = common_vendor.ref(null);
    const userLongitude = common_vendor.ref(null);
    function switchMapType(type) {
      mapType.value = type;
    }
    const markers = common_vendor.ref([
      {
        id: 1,
        latitude: defaultLatitude,
        longitude: defaultLongitude,
        title: "默认位置",
        width: 36,
        height: 36
      }
    ]);
    common_vendor.onLoad(() => {
      locateCurrentPosition();
    });
    common_vendor.onShow(() => {
      locateCurrentPosition();
    });
    function locateCurrentPosition() {
      common_vendor.index.showLoading({
        title: "定位中..."
      });
      common_vendor.index.getLocation({
        type: "gcj02",
        isHighAccuracy: true,
        success: async (res) => {
          common_vendor.index.__f__("log", "at pages/map/map.vue:196", "定位结果：", res.latitude, res.longitude);
          userLatitude.value = res.latitude;
          userLongitude.value = res.longitude;
          latitude.value = res.latitude;
          longitude.value = res.longitude;
          markers.value = [
            {
              id: 1,
              latitude: res.latitude,
              longitude: res.longitude,
              title: "当前位置",
              width: 36,
              height: 36
            }
          ];
          await updateAddress(res.latitude, res.longitude);
          await loadNearbySpots(res.latitude, res.longitude);
          common_vendor.index.showToast({
            title: "定位成功",
            icon: "success"
          });
        },
        fail: (err) => {
          common_vendor.index.__f__("log", "at pages/map/map.vue:226", "定位失败：", err);
        },
        complete: () => {
          common_vendor.index.hideLoading();
        }
      });
    }
    function selectSpot(spot) {
      latitude.value = Number(spot.latitude);
      longitude.value = Number(spot.longitude);
      markers.value = [
        {
          id: spot.id,
          latitude: Number(spot.latitude),
          longitude: Number(spot.longitude),
          title: spot.name,
          width: 36,
          height: 36
        }
      ];
    }
    function formatDistance(distance) {
      if (distance < 1e3) {
        return `${Math.round(distance)} 米`;
      }
      return `${(distance / 1e3).toFixed(1)} 公里`;
    }
    function calculateDistance(latitude1, longitude1, latitude2, longitude2) {
      const earthRadius = 6371e3;
      const lat1 = latitude1 * Math.PI / 180;
      const lat2 = latitude2 * Math.PI / 180;
      const latDifference = (latitude2 - latitude1) * Math.PI / 180;
      const lngDifference = (longitude2 - longitude1) * Math.PI / 180;
      const a = Math.sin(latDifference / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(lngDifference / 2) ** 2;
      const c = 2 * Math.atan2(
        Math.sqrt(a),
        Math.sqrt(1 - a)
      );
      return earthRadius * c;
    }
    function goSpotDetail(id) {
      common_vendor.index.navigateTo({
        url: `/pages/spot-detail/spot-detail?id=${id}`
      });
    }
    async function updateAddress(latitudeValue, longitudeValue) {
      try {
        currentAddress.value = "...正在解析地址";
        const result = await api_location.reverseGeocoder(latitudeValue, longitudeValue);
        currentAddress.value = result.recommendAddress || result.address;
        common_vendor.index.__f__("log", "at pages/map/map.vue:308", "当前地址", result);
      } catch (err) {
        common_vendor.index.__f__("log", "at pages/map/map.vue:310", "地址解析失败：", err);
        currentAddress.value = "地址解析失败";
      }
    }
    async function handleSearch() {
      const value = keyword.value.trim();
      if (!value) {
        common_vendor.index.showToast({
          title: "请输入景点名称",
          icon: "none"
        });
        return;
      }
      searching.value = true;
      try {
        const spots = await api_spot.searchScenicSpots(value);
        searchResults.value = spots.map((spot) => {
          let distance;
          if (userLatitude.value !== null && userLongitude.value !== null) {
            distance = calculateDistance(
              userLatitude.value,
              userLongitude.value,
              Number(spot.latitude),
              Number(spot.longitude)
            );
          }
          return {
            ...spot,
            distance
          };
        });
        searchResults.value.sort((a, b) => {
          return (a.distance ?? Infinity) - (b.distance ?? Infinity);
        });
        if (searchResults.value.length === 0) {
          common_vendor.index.showToast({
            title: "未找到相关景点",
            icon: "none"
          });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/map/map.vue:363", "搜索景点失败：", error);
        common_vendor.index.showToast({
          title: "搜索失败",
          icon: "none"
        });
      } finally {
        searching.value = false;
      }
    }
    async function loadNearbySpots(currentLatitude, currentLongitude) {
      loadingNearby.value = true;
      try {
        nearbySpots.value = await api_spot.getNearbySpots(
          currentLatitude,
          currentLongitude,
          10
        );
        common_vendor.index.__f__("log", "at pages/map/map.vue:387", "最近景点：", nearbySpots.value);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/map/map.vue:389", "加载附近景点失败：", error);
        common_vendor.index.showToast({
          title: "附近景点加载失败",
          icon: "none"
        });
      } finally {
        loadingNearby.value = false;
      }
    }
    __expose({
      keyword,
      searching,
      searchResults,
      latitude,
      longitude,
      markers,
      isSatellite,
      mapType,
      nearbySpots,
      loadingNearby,
      currentAddress,
      handleSearch,
      selectSpot,
      switchMapType,
      formatDistance,
      locateCurrentPosition
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(handleSearch, "6f"),
        b: keyword.value,
        c: common_vendor.o(($event) => keyword.value = $event.detail.value, "89"),
        d: common_vendor.t(searching.value ? "搜索中" : "搜索"),
        e: searching.value,
        f: common_vendor.o(handleSearch, "9c"),
        g: searchResults.value.length > 0
      }, searchResults.value.length > 0 ? {
        h: common_vendor.f(searchResults.value, (spot, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(spot.name),
            b: common_vendor.t(spot.category),
            c: common_vendor.t(spot.address),
            d: spot.distance !== void 0
          }, spot.distance !== void 0 ? {
            e: common_vendor.t(formatDistance(spot.distance))
          } : {}, {
            f: common_vendor.o(($event) => goSpotDetail(spot.id), spot.id),
            g: spot.id,
            h: common_vendor.o(($event) => selectSpot(spot), spot.id)
          });
        })
      } : {}, {
        i: latitude.value,
        j: longitude.value,
        k: markers.value,
        l: isSatellite.value,
        m: mapType.value === "normal" ? 1 : "",
        n: common_vendor.o(($event) => switchMapType("normal"), "fb"),
        o: mapType.value === "satellite" ? 1 : "",
        p: common_vendor.o(($event) => switchMapType("satellite"), "76"),
        q: loadingNearby.value
      }, loadingNearby.value ? {} : {}, {
        r: !loadingNearby.value && nearbySpots.value.length === 0
      }, !loadingNearby.value && nearbySpots.value.length === 0 ? {} : {
        s: common_vendor.f(nearbySpots.value, (spot, index, i0) => {
          return common_vendor.e({
            a: common_vendor.t(spot.name),
            b: index === 0
          }, index === 0 ? {} : {}, {
            c: common_vendor.t(spot.address),
            d: common_vendor.t(formatDistance(spot.distance)),
            e: common_vendor.o(($event) => goSpotDetail(spot.id), spot.id),
            f: spot.id,
            g: common_vendor.o(($event) => selectSpot(spot), spot.id)
          });
        })
      }, {
        t: common_vendor.t(currentAddress.value),
        v: common_vendor.o(locateCurrentPosition, "6b")
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e06b858f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/map/map.js.map
