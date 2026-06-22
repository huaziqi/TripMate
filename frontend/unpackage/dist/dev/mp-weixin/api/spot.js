"use strict";
const common_vendor = require("../common/vendor.js");
const utils_useApi = require("../utils/useApi.js");
const BASE_URL = "http://127.0.0.1:8080";
function searchScenicSpots(keyword) {
  return new Promise((resolve, reject) => {
    common_vendor.index.request({
      url: `${BASE_URL}/api/spots/search`,
      method: "GET",
      data: {
        keyword
      },
      timeout: 500,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300 && Array.isArray(res.data)) {
          resolve(res.data);
          return;
        }
        reject(new Error(`搜索接口异常：${res.statusCode}`));
      },
      fail: (err) => {
        common_vendor.index.__f__("error", "at api/spot.ts:51", "搜索请求失败：", err);
        reject(err);
      }
    });
  });
}
function getNearbySpots(latitude, longitude, limit = 3) {
  return new Promise((resolve, reject) => {
    common_vendor.index.request({
      url: `${BASE_URL}/api/spots/nearby`,
      method: "GET",
      data: {
        latitude,
        longitude,
        limit
      },
      success: (res) => {
        common_vendor.index.__f__("log", "at api/spot.ts:74", "附近景点响应：", res.data);
        if (res.statusCode >= 200 && res.statusCode < 300 && Array.isArray(res.data)) {
          resolve(res.data);
          return;
        }
        reject(new Error(`附近景点接口异常：${res.statusCode}`));
      },
      fail: (error) => {
        common_vendor.index.__f__("error", "at api/spot.ts:88", "附近景点请求失败：", error);
        reject(error);
      }
    });
  });
}
function getScenicSpotById(id) {
  return new Promise((resolve, reject) => {
    common_vendor.index.request({
      url: `${BASE_URL}/api/spots/${id}`,
      method: "GET",
      success: (res) => {
        common_vendor.index.__f__("log", "at api/spot.ts:103", "景点详情响应：", res.data);
        if (res.statusCode >= 200 && res.statusCode < 300 && res.data) {
          resolve(res.data);
          return;
        }
        reject(new Error(`景点详情接口异常：${res.statusCode}`));
      },
      fail: (error) => {
        common_vendor.index.__f__("error", "at api/spot.ts:118", "景点详情请求失败：", error);
        reject(error);
      }
    });
  });
}
function useSpotApi() {
  const { get } = utils_useApi.useApi();
  function listSpots() {
    return get("/api/spots").then((r) => r.data ?? []);
  }
  function searchSpots(keyword) {
    return get("/api/spots/search", { keyword }).then((r) => r.data ?? []);
  }
  return { listSpots, searchSpots };
}
exports.getNearbySpots = getNearbySpots;
exports.getScenicSpotById = getScenicSpotById;
exports.searchScenicSpots = searchScenicSpots;
exports.useSpotApi = useSpotApi;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/spot.js.map
