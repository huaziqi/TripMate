"use strict";
const common_vendor = require("../common/vendor.js");
const BASE_URL = "http://localhost:8080";
function request(method, url, data, options = {}) {
  const { headers = {}, withToken = true } = options;
  if (withToken) {
    const token = common_vendor.index.getStorageSync("token");
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }
  return new Promise((resolve, reject) => {
    common_vendor.index.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        "Content-Type": "application/json",
        ...headers
      },
      success: (res) => {
        const result = res.data;
        if (res.statusCode === 200) {
          resolve(result);
        } else if (res.statusCode === 401) {
          common_vendor.index.removeStorageSync("token");
          common_vendor.index.showToast({ title: "请重新登录", icon: "none" });
          reject(new Error("未授权"));
        } else {
          common_vendor.index.showToast({
            title: (result == null ? void 0 : result.message) || "请求失败",
            icon: "none"
          });
          reject(new Error((result == null ? void 0 : result.message) || "请求失败"));
        }
      },
      fail: (err) => {
        common_vendor.index.showToast({ title: "网络异常，请稍后重试", icon: "none" });
        reject(err);
      }
    });
  });
}
function useApi() {
  function get(url, params, options) {
    return request("GET", url, params, options);
  }
  function post(url, body, options) {
    return request("POST", url, body, options);
  }
  return { get, post };
}
exports.useApi = useApi;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/useApi.js.map
