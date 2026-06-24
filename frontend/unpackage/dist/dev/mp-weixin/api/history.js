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
async function getHistoryList() {
  const { get } = utils_useApi.useApi();
  const res = await get("/api/history");
  return res.data;
}
exports.addHistory = addHistory;
exports.getHistoryList = getHistoryList;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/history.js.map
