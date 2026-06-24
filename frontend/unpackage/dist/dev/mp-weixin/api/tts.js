"use strict";
const utils_useApi = require("../utils/useApi.js");
const { post } = utils_useApi.useApi();
function synthesizeSpeech(text, lang) {
  return post("/api/tts/synthesize", { text, lang });
}
exports.synthesizeSpeech = synthesizeSpeech;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/tts.js.map
