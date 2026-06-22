"use strict";
const utils_useApi = require("../utils/useApi.js");
function addHistory(type, targetId, content) {
  const { post } = utils_useApi.useApi();
  return post("/api/history", {
    type,
    targetId,
    content
  });
}
exports.addHistory = addHistory;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/history.js.map
