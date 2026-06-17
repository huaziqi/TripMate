"use strict";
const common_vendor = require("../common/vendor.js");
const BASE_URL = "http://127.0.0.1:8080";
function searchScenicSpots(keyword) {
  return new Promise((resolve, reject) => {
    common_vendor.index.request({
      url: `${BASE_URL}/api/spots/search`,
      method: "GET",
      data: {
        keyword
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300 && Array.isArray(res.data)) {
          resolve(res.data);
          return;
        }
        reject(new Error(`搜索接口异常：${res.statusCode}`));
      },
      fail: reject
    });
  });
}
exports.searchScenicSpots = searchScenicSpots;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/spot.js.map
