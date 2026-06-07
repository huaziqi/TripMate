"use strict";
const utils_useApi = require("../utils/useApi.js");
function useBadgeApi() {
  const { get, post } = utils_useApi.useApi();
  function listBadges() {
    return get("/api/badges");
  }
  function unlockBadge(id) {
    return post(`/api/badges/${id}/unlock`);
  }
  return { listBadges, unlockBadge };
}
exports.useBadgeApi = useBadgeApi;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/badge.js.map
