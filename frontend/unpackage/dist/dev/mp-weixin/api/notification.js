"use strict";
const utils_useApi = require("../utils/useApi.js");
const { get, post } = utils_useApi.useApi();
function fetchNotifications(page = 0, size = 20) {
  return get(`/api/notifications?page=${page}&size=${size}`);
}
function markAllRead() {
  return post("/api/notifications/read-all");
}
exports.fetchNotifications = fetchNotifications;
exports.markAllRead = markAllRead;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/notification.js.map
