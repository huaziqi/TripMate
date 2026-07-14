"use strict";
const common_vendor = require("../common/vendor.js");
const utils_useApi = require("../utils/useApi.js");
function chatWithCompanionStream(options) {
  const { message, history = [], onDelta, onDone, onError } = options;
  const token = common_vendor.index.getStorageSync("token");
  let sseBuffer = "";
  let finished = false;
  const requestTask = common_vendor.wx$1.request({
    url: `${utils_useApi.BASE_URL}/api/companion/chat`,
    method: "POST",
    enableChunked: true,
    responseType: "arraybuffer",
    header: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
      ...token ? { Authorization: `Bearer ${token}` } : {}
    },
    data: {
      message,
      history
    },
    success() {
    },
    fail(err) {
      common_vendor.index.__f__("error", "at api/companion.ts:81", "[companion] stream request fail:", err);
      if (!finished) {
        finished = true;
        onError == null ? void 0 : onError("网络异常，请稍后再试");
      }
    },
    complete() {
    }
  });
  requestTask.onChunkReceived((res) => {
    try {
      const chunkText = decodeArrayBuffer(res.data);
      sseBuffer += chunkText;
      const events = sseBuffer.split(/\r?\n\r?\n/);
      sseBuffer = events.pop() || "";
      for (const eventText of events) {
        handleSseEvent(eventText, {
          onDelta,
          onDone: () => {
            if (!finished) {
              finished = true;
              onDone == null ? void 0 : onDone();
            }
          },
          onError: (error) => {
            if (!finished) {
              finished = true;
              onError == null ? void 0 : onError(error);
            }
          }
        });
      }
    } catch (err) {
      common_vendor.index.__f__("error", "at api/companion.ts:119", "[companion] handle chunk error:", err);
      if (!finished) {
        finished = true;
        onError == null ? void 0 : onError("解析回复失败");
      }
    }
  });
  return {
    abort() {
      try {
        requestTask.abort();
      } catch (e) {
      }
    }
  };
}
function handleSseEvent(eventText, callbacks) {
  var _a, _b, _c, _d;
  const lines = eventText.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  for (const line of lines) {
    if (!line.startsWith("data:"))
      continue;
    const dataText = line.replace(/^data:\s*/, "");
    if (!dataText || dataText === "[DONE]") {
      (_a = callbacks.onDone) == null ? void 0 : _a.call(callbacks);
      return;
    }
    try {
      const data = JSON.parse(dataText);
      if (data.delta) {
        (_b = callbacks.onDelta) == null ? void 0 : _b.call(callbacks, data.delta);
      }
      if (data.done) {
        (_c = callbacks.onDone) == null ? void 0 : _c.call(callbacks);
      }
      if (data.error) {
        (_d = callbacks.onError) == null ? void 0 : _d.call(callbacks, data.error);
      }
    } catch (err) {
      common_vendor.index.__f__("warn", "at api/companion.ts:202", "[companion] parse sse data failed:", dataText, err);
    }
  }
}
function decodeArrayBuffer(buffer) {
  try {
    if (typeof TextDecoder !== "undefined") {
      return new TextDecoder("utf-8").decode(buffer);
    }
  } catch (e) {
  }
  const uint8Array = new Uint8Array(buffer);
  let result = "";
  for (let i = 0; i < uint8Array.length; i++) {
    result += String.fromCharCode(uint8Array[i]);
  }
  try {
    return decodeURIComponent(escape(result));
  } catch (e) {
    return result;
  }
}
exports.chatWithCompanionStream = chatWithCompanionStream;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/companion.js.map
