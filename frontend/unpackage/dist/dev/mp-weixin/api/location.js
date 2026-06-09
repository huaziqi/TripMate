"use strict";
const common_vendor = require("../common/vendor.js");
const TENCENT_MAP_KEY = "EOEBZ-PRZEN-3Z7FR-SPYGR-2PYM7-L6F5K";
function reverseGeocoder(latitude, longitude) {
  return new Promise((resolve, reject) => {
    common_vendor.index.request({
      url: "https://apis.map.qq.com/ws/geocoder/v1/",
      method: "GET",
      data: {
        location: `${latitude},${longitude}`,
        key: TENCENT_MAP_KEY,
        get_poi: 1,
        output: "json"
      },
      success: (res) => {
        var _a;
        const data = res.data;
        common_vendor.index.__f__("log", "at api/location.ts:27", "腾讯逆地址解析结果：", data);
        if (data.status !== 0) {
          reject(data);
          return;
        }
        const result = data.result;
        const component = result.address_component || {};
        resolve({
          address: result.address || "",
          recommendAddress: ((_a = result.formatted_addresses) == null ? void 0 : _a.recommend) || result.address || "",
          province: component.province || "",
          city: component.city || "",
          district: component.district || "",
          street: component.street || "",
          streetNumber: component.street_number || ""
        });
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
}
exports.reverseGeocoder = reverseGeocoder;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/location.js.map
