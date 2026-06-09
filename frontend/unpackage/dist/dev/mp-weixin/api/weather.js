"use strict";
const utils_useApi = require("../utils/useApi.js");
const { post } = utils_useApi.useApi();
function fetchWeather(longitude, latitude) {
  return post("/api/weather", { longitude, latitude });
}
exports.fetchWeather = fetchWeather;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/weather.js.map
