"use strict";
const common_vendor = require("../../common/vendor.js");
const api_companion = require("../../api/companion.js");
const api_tts = require("../../api/tts.js");
if (!Array) {
  const _component_live2d_view = common_vendor.resolveComponent("live2d-view");
  _component_live2d_view();
}
const MAX_HISTORY_MESSAGES = 36;
const MAX_HISTORY_CHARS = 3600;
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "live2d",
  setup(__props) {
    const live2dRef = common_vendor.ref(null);
    const isLive2DReady = common_vendor.ref(false);
    function limitHistory(history) {
      var _a;
      if (!history.length)
        return [];
      let trimmed = history.slice(-MAX_HISTORY_MESSAGES);
      let total = 0;
      const result = [];
      for (let i = trimmed.length - 1; i >= 0; i--) {
        const item = trimmed[i];
        const len = ((_a = item.content) == null ? void 0 : _a.length) || 0;
        if (result.length > 0 && total + len > MAX_HISTORY_CHARS) {
          break;
        }
        total += len;
        result.unshift(item);
      }
      return result;
    }
    const localSessionMessages = common_vendor.ref([
      { role: "ASSISTANT", content: "你好呀，有什么想和我说的吗？" }
    ]);
    let currentChatTask = null;
    const isStreaming = common_vendor.ref(false);
    const inputText = common_vendor.ref("");
    const audioContext = common_vendor.index.createInnerAudioContext();
    const currentPlayingMsgId = common_vendor.ref(null);
    const localAudioCache = common_vendor.ref({});
    common_vendor.ref({});
    const chatHistory = common_vendor.ref([
      {
        id: createMsgId(),
        content: "你好呀，有什么想和我说的吗？",
        isSelf: false,
        audioStatus: "idle"
      }
    ]);
    function createMsgId() {
      return `msg_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    }
    audioContext.autoplay = false;
    audioContext.obeyMuteSwitch = false;
    function downloadAudioToLocal(url) {
      return new Promise((resolve, reject) => {
        common_vendor.index.downloadFile({
          url,
          success: (res) => {
            if (res.statusCode === 200 && res.tempFilePath) {
              resolve(res.tempFilePath);
            } else {
              reject(new Error("语音下载失败"));
            }
          },
          fail: reject
        });
      });
    }
    function clearAudioCache() {
      stopCurrentAudio();
      Object.keys(localAudioCache.value).forEach((key) => {
        var _a, _b;
        const filePath = localAudioCache.value[key];
        if (filePath) {
          (_b = (_a = common_vendor.index).getFileSystemManager) == null ? void 0 : _b.call(_a).unlink({
            filePath,
            success: () => {
            },
            fail: () => {
            }
          });
        }
      });
      localAudioCache.value = {};
    }
    audioContext.onPlay(() => {
      common_vendor.index.__f__("log", "at pages/live2d/live2d.vue:305", "[tts] playing...");
    });
    audioContext.onEnded(() => {
      const msgId = currentPlayingMsgId.value;
      if (msgId) {
        const target = chatHistory.value.find((item) => item.id === msgId);
        if (target) {
          target.audioStatus = "played";
        }
      }
      if (live2dRef.value && isLive2DReady.value) {
        live2dRef.value.stopTalking();
      }
      currentPlayingMsgId.value = null;
    });
    audioContext.onStop(() => {
      const msgId = currentPlayingMsgId.value;
      if (msgId) {
        const target = chatHistory.value.find((item) => item.id === msgId);
        if (target && target.audioStatus === "playing") {
          target.audioStatus = "played";
        }
      }
      if (live2dRef.value && isLive2DReady.value) {
        live2dRef.value.stopTalking();
      }
      currentPlayingMsgId.value = null;
    });
    audioContext.onError((err) => {
      common_vendor.index.__f__("error", "at pages/live2d/live2d.vue:337", "[tts] audio error:", err);
      const msgId = currentPlayingMsgId.value;
      if (msgId) {
        const target = chatHistory.value.find((item) => item.id === msgId);
        if (target) {
          target.audioStatus = "error";
        }
      }
      if (live2dRef.value && isLive2DReady.value) {
        live2dRef.value.stopTalking();
      }
      currentPlayingMsgId.value = null;
    });
    function stopCurrentAudio(resetStatus = false) {
      if (currentPlayingMsgId.value) {
        const target = chatHistory.value.find((item) => item.id === currentPlayingMsgId.value);
        if (target && resetStatus) {
          target.audioStatus = "idle";
        }
      }
      audioContext.stop();
      if (live2dRef.value && isLive2DReady.value) {
        live2dRef.value.stopTalking();
      }
      currentPlayingMsgId.value = null;
    }
    common_vendor.ref(120);
    const currentTheme = common_vendor.ref("theme-purple");
    const isSkinPopupShow = common_vendor.ref(false);
    function handleReady(e) {
      common_vendor.index.__f__("log", "at pages/live2d/live2d.vue:379", "[live2d-page] component ready:", e);
      isLive2DReady.value = true;
    }
    function handleError(e) {
      common_vendor.index.__f__("error", "at pages/live2d/live2d.vue:385", "[live2d-page] component error:", e);
      isLive2DReady.value = false;
    }
    function addLeftBubble(content) {
      chatHistory.value.push({
        id: createMsgId(),
        content,
        isSelf: false,
        audioStatus: content.trim() ? "idle" : void 0
      });
      scrollToBottom();
    }
    function addRightBubble(content) {
      chatHistory.value.push({
        id: createMsgId(),
        content,
        isSelf: true
      });
      scrollToBottom();
    }
    function updateLastLeftBubble(content) {
      for (let i = chatHistory.value.length - 1; i >= 0; i--) {
        if (!chatHistory.value[i].isSelf) {
          chatHistory.value[i].content = content;
          if (content.trim()) {
            chatHistory.value[i].audioStatus = "idle";
          }
          break;
        }
      }
      scrollToBottom(20);
    }
    const scrollTop = common_vendor.ref(0);
    const lastMsgId = common_vendor.ref("");
    function scrollToBottom(delay = 60) {
      common_vendor.nextTick$1(() => {
        setTimeout(() => {
          const query = common_vendor.index.createSelectorQuery();
          query.select("#chatBubbleContainer").boundingClientRect();
          query.select(".chat-bubble-area").boundingClientRect();
          query.exec((res) => {
            if (!res || res.length < 2)
              return;
            const containerRect = res[0];
            const contentRect = res[1];
            if (!containerRect || !contentRect)
              return;
            scrollTop.value = Math.max(contentRect.height - containerRect.height, 0);
            lastMsgId.value = "lastMsg";
          });
        }, delay);
      });
    }
    function handleSend() {
      stopCurrentAudio();
      const text = inputText.value.trim();
      if (!text || isStreaming.value)
        return;
      common_vendor.index.__f__("log", "at pages/live2d/live2d.vue:453", "[chat] send:", text);
      if (currentChatTask) {
        currentChatTask.abort();
        currentChatTask = null;
      }
      addRightBubble(text);
      localSessionMessages.value.push({
        role: "USER",
        content: text
      });
      localSessionMessages.value = limitHistory(localSessionMessages.value);
      inputText.value = "";
      addLeftBubble("");
      let assistantReply = "";
      isStreaming.value = true;
      const requestHistory = limitHistory(localSessionMessages.value);
      currentChatTask = api_companion.chatWithCompanionStream({
        message: text,
        history: requestHistory,
        onDelta(delta) {
          assistantReply += delta;
          updateLastLeftBubble(assistantReply);
        },
        onDone() {
          currentChatTask = null;
          isStreaming.value = false;
          const finalReply = assistantReply.trim() || " ";
          updateLastLeftBubble(finalReply);
          localSessionMessages.value.push({
            role: "ASSISTANT",
            content: finalReply
          });
          localSessionMessages.value = limitHistory(localSessionMessages.value);
          scrollToBottom();
        },
        onError(error) {
          common_vendor.index.__f__("error", "at pages/live2d/live2d.vue:499", "[chat] stream error:", error);
          currentChatTask = null;
          isStreaming.value = false;
          const errorText = error || "网络开小差了，请稍后再试～";
          updateLastLeftBubble(errorText);
          localSessionMessages.value.push({
            role: "ASSISTANT",
            content: errorText
          });
          localSessionMessages.value = limitHistory(localSessionMessages.value);
          scrollToBottom();
        }
      });
    }
    function handleStop() {
      stopCurrentAudio();
      if (currentChatTask) {
        currentChatTask.abort();
        currentChatTask = null;
      }
      isStreaming.value = false;
      const lastMessage = chatHistory.value[chatHistory.value.length - 1];
      if (lastMessage && !lastMessage.isSelf && !lastMessage.content.trim()) {
        lastMessage.content = "已停止回复";
      }
      scrollToBottom();
    }
    async function handleVoiceTap(msg) {
      if (!msg.content.trim())
        return;
      if (currentPlayingMsgId.value === msg.id && msg.audioStatus === "playing") {
        stopCurrentAudio();
        if (live2dRef.value && isLive2DReady.value) {
          live2dRef.value.stopTalking();
        }
        msg.audioStatus = "played";
        return;
      }
      stopCurrentAudio();
      if (live2dRef.value && isLive2DReady.value) {
        live2dRef.value.stopTalking();
      }
      try {
        msg.audioStatus = "loading";
        let localPath = localAudioCache.value[msg.id];
        if (!localPath) {
          let audioUrl = msg.audioUrl;
          if (!audioUrl) {
            const res = await api_tts.synthesizeSpeech({
              text: msg.content,
              lang: "zh"
            });
            audioUrl = res.data.audioUrl;
            msg.audioUrl = audioUrl;
          }
          localPath = await downloadAudioToLocal(audioUrl);
          localAudioCache.value[msg.id] = localPath;
        }
        audioContext.src = localPath;
        currentPlayingMsgId.value = msg.id;
        msg.audioStatus = "playing";
        startLive2DTalkingByAudio(localPath, msg.content);
        audioContext.play();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/live2d/live2d.vue:585", "[tts] synthesize/play error:", error);
        msg.audioStatus = "error";
        if (live2dRef.value && isLive2DReady.value) {
          live2dRef.value.stopTalking();
        }
        common_vendor.index.showToast({
          title: "语音播放失败",
          icon: "none"
        });
      }
    }
    function getAudioDuration(src) {
      return new Promise((resolve) => {
        const ctx = common_vendor.index.createInnerAudioContext();
        let settled = false;
        const done = (ms) => {
          if (settled)
            return;
          settled = true;
          try {
            ctx.destroy();
          } catch (e) {
          }
          resolve(ms);
        };
        ctx.autoplay = false;
        ctx.src = src;
        ctx.onCanplay(() => {
          setTimeout(() => {
            const durationSec = Number(ctx.duration || 0);
            if (durationSec > 0) {
              done(durationSec * 1e3);
            }
          }, 200);
        });
        ctx.onError(() => {
          done(0);
        });
        setTimeout(() => {
          const durationSec = Number(ctx.duration || 0);
          done(durationSec > 0 ? durationSec * 1e3 : 0);
        }, 1200);
      });
    }
    async function startLive2DTalkingByAudio(localPath, text = "") {
      if (!live2dRef.value || !isLive2DReady.value)
        return;
      let duration = await getAudioDuration(localPath);
      if (!duration || duration <= 0) {
        duration = Math.min(8e3, Math.max(1200, text.length * 220));
      }
      try {
        live2dRef.value.startTalking(duration, {
          enableBlink: true,
          enableHeadMotion: true,
          mouthOpenBase: 0.08,
          mouthOpenRange: 0.42,
          mouthFormBase: 0.18,
          // 默认偏开心
          mouthFormRange: 0.06
        });
      } catch (err) {
        common_vendor.index.__f__("error", "at pages/live2d/live2d.vue:652", "[live2d] startTalking failed:", err);
      }
    }
    function toggleSkinPopup() {
      isSkinPopupShow.value = !isSkinPopupShow.value;
    }
    function closeSkinPopup() {
      isSkinPopupShow.value = false;
    }
    function setTheme(theme) {
      currentTheme.value = theme;
    }
    function resetAll() {
      if (currentChatTask) {
        currentChatTask.abort();
        currentChatTask = null;
      }
      isStreaming.value = false;
      clearAudioCache();
      chatHistory.value = [
        {
          id: createMsgId(),
          content: "你好呀，有什么想和我说的吗？",
          isSelf: false,
          audioStatus: "idle"
        }
      ];
      localSessionMessages.value = [
        { role: "ASSISTANT", content: "你好呀，有什么想和我说的吗？" }
      ];
      inputText.value = "";
      currentTheme.value = "theme-purple";
      closeSkinPopup();
      scrollToBottom();
      common_vendor.index.showToast({
        title: "已重置",
        icon: "success",
        duration: 1500
      });
    }
    common_vendor.onBeforeUnmount(() => {
      if (currentChatTask) {
        currentChatTask.abort();
        currentChatTask = null;
      }
      clearAudioCache();
      audioContext.destroy();
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(toggleSkinPopup, "2f"),
        b: common_vendor.o(resetAll, "0c"),
        c: isSkinPopupShow.value
      }, isSkinPopupShow.value ? {
        d: common_vendor.o(closeSkinPopup, "1c")
      } : {}, {
        e: isSkinPopupShow.value
      }, isSkinPopupShow.value ? {
        f: currentTheme.value === "theme-purple" ? 1 : "",
        g: common_vendor.o(($event) => setTheme("theme-purple"), "b1"),
        h: currentTheme.value === "theme-blue" ? 1 : "",
        i: common_vendor.o(($event) => setTheme("theme-blue"), "40"),
        j: currentTheme.value === "theme-green" ? 1 : "",
        k: common_vendor.o(($event) => setTheme("theme-green"), "b2"),
        l: common_vendor.o(closeSkinPopup, "38")
      } : {}, {
        m: common_vendor.f(chatHistory.value, (msg, idx, i0) => {
          return common_vendor.e({
            a: common_vendor.t(msg.content),
            b: !msg.isSelf && msg.content.trim()
          }, !msg.isSelf && msg.content.trim() ? {
            c: common_vendor.t(msg.audioStatus === "loading" ? "⏳" : msg.audioStatus === "playing" ? "🔊" : msg.audioStatus === "played" ? "✅" : msg.audioStatus === "error" ? "⚠️" : "🎧"),
            d: msg.audioStatus === "loading" ? 1 : "",
            e: msg.audioStatus === "playing" ? 1 : "",
            f: msg.audioStatus === "played" ? 1 : "",
            g: msg.audioStatus === "error" ? 1 : "",
            h: common_vendor.o(($event) => handleVoiceTap(msg), msg.content + idx)
          } : {}, {
            i: msg.content + idx,
            j: !msg.isSelf ? 1 : "",
            k: msg.isSelf ? 1 : "",
            l: idx === chatHistory.value.length - 1 ? "lastMsg" : ""
          });
        }),
        n: scrollTop.value,
        o: lastMsgId.value,
        p: common_vendor.sr(live2dRef, "25246cd7-0", {
          "k": "live2dRef"
        }),
        q: common_vendor.o(handleReady, "1b"),
        r: common_vendor.o(handleError, "ee"),
        s: common_vendor.p({
          className: "live2d-box",
          autoInit: true,
          stageWidth: 750
        }),
        t: common_vendor.o(handleSend, "72"),
        v: inputText.value,
        w: common_vendor.o(($event) => inputText.value = $event.detail.value, "46"),
        x: common_vendor.t(isStreaming.value ? "停止" : "发送"),
        y: isStreaming.value ? 1 : "",
        z: common_vendor.o(($event) => isStreaming.value ? handleStop() : handleSend(), "27"),
        A: common_vendor.n(currentTheme.value)
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-25246cd7"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/live2d/live2d.js.map
