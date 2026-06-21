"use strict";
const utils_useApi = require("../utils/useApi.js");
const { post: apiPost, get } = utils_useApi.useApi();
function toggleFollow(userId) {
  return apiPost(`/api/users/${userId}/follow`);
}
exports.toggleFollow = toggleFollow;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/follow.js.map
