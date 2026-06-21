"use strict";
const utils_useApi = require("../utils/useApi.js");
const { post } = utils_useApi.useApi();
function translateText(text, from, to) {
  return post("/api/translate", { text, from, to }, { withToken: false });
}
exports.translateText = translateText;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/translate.js.map
