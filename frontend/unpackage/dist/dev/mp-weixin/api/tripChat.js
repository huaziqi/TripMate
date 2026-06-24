"use strict";
const utils_useApi = require("../utils/useApi.js");
const { post } = utils_useApi.useApi();
function sendTripChat(params) {
  return post("/api/trip/chat", params);
}
exports.sendTripChat = sendTripChat;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/tripChat.js.map
