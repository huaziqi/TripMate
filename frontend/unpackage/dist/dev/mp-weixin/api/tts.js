"use strict";
const utils_useApi = require("../utils/useApi.js");
const { post } = utils_useApi.useApi();
function synthesizeSpeech(payload) {
  return post("/api/tts/synthesize", payload);
}
exports.synthesizeSpeech = synthesizeSpeech;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/tts.js.map
