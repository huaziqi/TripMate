"use strict";
const common_vendor = require("../../common/vendor.js");
const api_location = require("../../api/location.js");
const api_spot = require("../../api/spot.js");
const defaultLatitude = 29.8266;
const defaultLongitude = 106.422;
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "map",
  setup(__props) {
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
    const selectedSpot = common_vendor.ref(null);
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
        success: (res) => {
          common_vendor.index.__f__("log", "at pages/map/map.vue:157", "定位结果：", res.latitude, res.longitude);
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
          updateAddress(res.latitude, res.longitude);
          common_vendor.index.showToast({
            title: "定位成功",
            icon: "success"
          });
        },
        fail: (err) => {
          common_vendor.index.__f__("log", "at pages/map/map.vue:181", "定位失败：", err);
          common_vendor.index.showModal({
            title: "定位失败",
            content: "请检查是否允许小程序获取位置。当前显示默认测试位置。",
            showCancel: false
          });
          latitude.value = defaultLatitude;
          longitude.value = defaultLongitude;
          markers.value = [
            {
              id: 1,
              latitude: defaultLatitude,
              longitude: defaultLongitude,
              title: "默认位置",
              width: 36,
              height: 36
            }
          ];
        },
        complete: () => {
          common_vendor.index.hideLoading();
        }
      });
    }
    function selectSpot(spot) {
      selectedSpot.value = spot;
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
      searchResults.value = [];
      common_vendor.index.showToast({
        title: spot.name,
        icon: "none"
      });
    }
    async function updateAddress(latitudeValue, longitudeValue) {
      try {
        currentAddress.value = "...正在解析地址";
        const result = await api_location.reverseGeocoder(latitudeValue, longitudeValue);
        currentAddress.value = result.recommendAddress || result.address;
        common_vendor.index.__f__("log", "at pages/map/map.vue:246", "当前地址", result);
      } catch (err) {
        common_vendor.index.__f__("log", "at pages/map/map.vue:248", "地址解析失败：", err);
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
        searchResults.value = spots;
        if (spots.length === 0) {
          common_vendor.index.showToast({
            title: "没有找到相关景点",
            icon: "none"
          });
          return;
        }
        common_vendor.index.showToast({
          title: `找到 ${spots.length} 个景点`,
          icon: "none"
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/map/map.vue:284", "搜索景点失败：", error);
        common_vendor.index.showToast({
          title: "搜索失败，请检查后端",
          icon: "none"
        });
      } finally {
        searching.value = false;
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(handleSearch),
        b: keyword.value,
        c: common_vendor.o(($event) => keyword.value = $event.detail.value),
        d: searching.value,
        e: common_vendor.o(handleSearch),
        f: searchResults.value.length > 0
      }, searchResults.value.length > 0 ? {
        g: common_vendor.f(searchResults.value, (spot, k0, i0) => {
          return {
            a: common_vendor.t(spot.name),
            b: common_vendor.t(spot.category),
            c: common_vendor.t(spot.address),
            d: spot.id,
            e: common_vendor.o(($event) => selectSpot(spot), spot.id)
          };
        })
      } : {}, {
        h: latitude.value,
        i: longitude.value,
        j: markers.value,
        k: isSatellite.value,
        l: mapType.value === "normal" ? 1 : "",
        m: common_vendor.o(($event) => switchMapType("normal")),
        n: mapType.value === "satellite" ? 1 : "",
        o: common_vendor.o(($event) => switchMapType("satellite")),
        p: common_vendor.t(latitude.value),
        q: common_vendor.t(longitude.value),
        r: common_vendor.t(mapType.value === "normal" ? "普通地图" : "卫星地图"),
        s: common_vendor.t(currentAddress.value),
        t: common_vendor.o(locateCurrentPosition)
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e06b858f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/map/map.js.map
