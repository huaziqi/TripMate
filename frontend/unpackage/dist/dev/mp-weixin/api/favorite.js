"use strict";
const utils_useApi = require("../utils/useApi.js");
function addFavorite(spotId) {
  const { post } = utils_useApi.useApi();
  return post(`/api/favorites/${spotId}`);
}
function removeFavorite(spotId) {
  const { del } = utils_useApi.useApi();
  return del(`/api/favorites/${spotId}`);
}
async function checkFavorite(spotId) {
  const { get } = utils_useApi.useApi();
  const res = await get(`/api/favorites/check/${spotId}`);
  return res.data;
}
async function getFavoriteSpots() {
  const { get } = utils_useApi.useApi();
  const res = await get("/api/favorites");
  return res.data;
}
exports.addFavorite = addFavorite;
exports.checkFavorite = checkFavorite;
exports.getFavoriteSpots = getFavoriteSpots;
exports.removeFavorite = removeFavorite;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/favorite.js.map
