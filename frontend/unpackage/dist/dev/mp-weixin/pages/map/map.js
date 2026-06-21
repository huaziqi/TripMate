"use strict";
const common_vendor = require("../../common/vendor.js");
const api_location = require("../../api/location.js");
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
          common_vendor.index.__f__("log", "at pages/map/map.vue:106", "定位结果：", res.latitude, res.longitude);
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
          common_vendor.index.__f__("log", "at pages/map/map.vue:130", "定位失败：", err);
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
    async function updateAddress(latitudeValue, longitudeValue) {
      try {
        currentAddress.value = "...正在解析地址";
        const result = await api_location.reverseGeocoder(latitudeValue, longitudeValue);
        currentAddress.value = result.recommendAddress || result.address;
        common_vendor.index.__f__("log", "at pages/map/map.vue:166", "当前地址", result);
      } catch (err) {
        common_vendor.index.__f__("log", "at pages/map/map.vue:168", "地址解析失败：", err);
        currentAddress.value = "地址解析失败";
      }
    }
    return (_ctx, _cache) => {
      return {
        a: latitude.value,
        b: longitude.value,
        c: markers.value,
        d: isSatellite.value,
        e: mapType.value === "normal" ? 1 : "",
        f: common_vendor.o(($event) => switchMapType("normal"), "fa"),
        g: mapType.value === "satellite" ? 1 : "",
        h: common_vendor.o(($event) => switchMapType("satellite"), "bc"),
        i: common_vendor.t(latitude.value),
        j: common_vendor.t(longitude.value),
        k: common_vendor.t(mapType.value === "normal" ? "普通地图" : "卫星地图"),
        l: common_vendor.t(currentAddress.value),
        m: common_vendor.o(locateCurrentPosition, "93")
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e06b858f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/map/map.js.map
