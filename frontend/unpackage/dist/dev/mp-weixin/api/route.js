"use strict";
const utils_useApi = require("../utils/useApi.js");
async function getRecommendRoutes() {
  const { get } = utils_useApi.useApi();
  const res = await get("/api/routes/recommend", void 0, {
    withToken: false
  });
  return res.data;
}
exports.getRecommendRoutes = getRecommendRoutes;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/route.js.map
