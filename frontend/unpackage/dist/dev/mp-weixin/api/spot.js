"use strict";
const utils_useApi = require("../utils/useApi.js");
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
exports.useSpotApi = useSpotApi;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/spot.js.map
