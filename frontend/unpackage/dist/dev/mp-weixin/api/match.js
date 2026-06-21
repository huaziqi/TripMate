"use strict";
const common_vendor = require("../common/vendor.js");
const WS_BASE = "ws://localhost:8080/ws";
let task = null;
let handler = null;
let socketReady = false;
function connectMatch(token, onMessage, onOpen) {
  if (task)
    disconnectMatch();
  socketReady = false;
  handler = onMessage;
  task = common_vendor.index.connectSocket({
    url: `${WS_BASE}?token=${token}`,
    complete: () => {
    }
  });
  task.onOpen(() => {
    socketReady = true;
    onOpen == null ? void 0 : onOpen();
  });
  task.onMessage((res) => {
    try {
      const msg = JSON.parse(res.data);
      handler == null ? void 0 : handler(msg);
    } catch (e) {
      common_vendor.index.__f__("error", "at api/match.ts:39", "[match.ts] 解析消息失败", e);
    }
  });
  task.onError((err) => {
    common_vendor.index.__f__("error", "at api/match.ts:44", "[match.ts] WebSocket 错误", err);
    socketReady = false;
  });
  task.onClose(() => {
    socketReady = false;
  });
}
function sendMatch(type, payload = {}) {
  if (!task || !socketReady)
    return;
  try {
    task.send({ data: JSON.stringify({ type, payload }) });
  } catch (e) {
    common_vendor.index.__f__("error", "at api/match.ts:58", "[match.ts] 发送失败", e);
  }
}
function setMessageHandler(onMessage) {
  handler = onMessage;
}
function disconnectMatch() {
  if (!task)
    return;
  socketReady = false;
  const t = task;
  task = null;
  handler = null;
  try {
    t.close({});
  } catch (_) {
  }
}
exports.connectMatch = connectMatch;
exports.disconnectMatch = disconnectMatch;
exports.sendMatch = sendMatch;
exports.setMessageHandler = setMessageHandler;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/match.js.map
