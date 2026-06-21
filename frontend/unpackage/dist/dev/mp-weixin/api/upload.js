"use strict";
const common_vendor = require("../common/vendor.js");
const BASE_URL = "http://localhost:8080";
function uploadImage(filePath) {
  return new Promise((resolve, reject) => {
    const token = common_vendor.index.getStorageSync("token");
    common_vendor.index.uploadFile({
      url: BASE_URL + "/api/upload",
      filePath,
      name: "file",
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        var _a;
        try {
          const data = JSON.parse(res.data);
          if (data.code === 200 && ((_a = data.data) == null ? void 0 : _a.url)) {
            resolve(data.data.url);
          } else {
            common_vendor.index.showToast({ title: data.message || "上传失败", icon: "none" });
            reject(new Error(data.message));
          }
        } catch {
          reject(new Error("解析响应失败"));
        }
      },
      fail: () => {
        common_vendor.index.showToast({ title: "上传失败，请重试", icon: "none" });
        reject(new Error("上传失败"));
      }
    });
  });
}
exports.uploadImage = uploadImage;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/upload.js.map
