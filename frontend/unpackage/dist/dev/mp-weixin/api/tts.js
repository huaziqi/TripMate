"use strict";
const utils_useApi = require("../utils/useApi.js");
async function synthesizeSpeech(text) {
  var _a;
  const { post } = utils_useApi.useApi();
  const res = await post(
    "/api/tts/synthesize",
    { text },
    { withToken: false }
  );
  if (res == null ? void 0 : res.audioUrl) {
    return res;
  }
  if ((_a = res == null ? void 0 : res.data) == null ? void 0 : _a.audioUrl) {
    return res.data;
  }
  throw new Error("TTS接口未返回 audioUrl");
}
exports.synthesizeSpeech = synthesizeSpeech;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/tts.js.map
