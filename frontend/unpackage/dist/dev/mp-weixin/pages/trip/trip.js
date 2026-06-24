"use strict";
const common_vendor = require("../../common/vendor.js");
const api_match = require("../../api/match.js");
const api_tripChat = require("../../api/tripChat.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "trip",
  setup(__props) {
    const _inst = common_vendor.getCurrentInstance();
    const spotName = common_vendor.ref("");
    const partnerNickname = common_vendor.ref("");
    const spotId = common_vendor.ref(0);
    const isSolo = common_vendor.ref(false);
    const myLat = common_vendor.ref(29.8266);
    const myLng = common_vendor.ref(106.422);
    const partnerLat = common_vendor.ref(null);
    const partnerLng = common_vendor.ref(null);
    let locationTimer = null;
    let startLat = null;
    let startLng = null;
    const markers = common_vendor.computed(() => {
      const list = [{
        id: 1,
        latitude: myLat.value,
        longitude: myLng.value,
        title: "我",
        width: 40,
        height: 40,
        iconPath: "",
        callout: { content: "我", color: "#fff", bgColor: "#2196f3", padding: 8, borderRadius: 8, display: "ALWAYS" }
      }];
      if (partnerLat.value !== null && partnerLng.value !== null) {
        list.push({
          id: 2,
          latitude: partnerLat.value,
          longitude: partnerLng.value,
          title: partnerNickname.value,
          width: 40,
          height: 40,
          iconPath: "",
          callout: { content: partnerNickname.value, color: "#fff", bgColor: "#f44336", padding: 8, borderRadius: 8, display: "ALWAYS" }
        });
      }
      return list;
    });
    const distanceText = common_vendor.computed(() => {
      if (partnerLat.value === null || partnerLng.value === null)
        return "等待搭子位置…";
      const d = haversine(myLat.value, myLng.value, partnerLat.value, partnerLng.value);
      return d < 1e3 ? `${Math.round(d)} 米` : `${(d / 1e3).toFixed(1)} 千米`;
    });
    const challenges = common_vendor.ref([
      { id: "first_stroke", icon: "✏️", title: "旅途涂鸦", desc: "在地图上画出第一笔", completed: false },
      { id: "three_strokes", icon: "🎨", title: "涂鸦达人", desc: "累计画出 3 笔涂鸦", completed: false },
      { id: "near_partner", icon: "🤝", title: "搭子相聚", desc: "与搭子距离缩短至 200 米内", completed: false },
      { id: "traveled_200m", icon: "🏃", title: "小小旅行家", desc: "从出发点移动超过 200 米", completed: false },
      { id: "partner_active", icon: "👫", title: "同行时光", desc: "与搭子保持实时联系", completed: false }
    ]);
    const completedChallenge = common_vendor.ref(null);
    const showChallenges = common_vendor.ref(false);
    let popupDismissTimer = null;
    let partnerLocationCount = 0;
    const visibleChallenges = common_vendor.computed(
      () => isSolo.value ? challenges.value.filter((c) => !["near_partner", "partner_active"].includes(c.id)) : challenges.value
    );
    const completedCount = common_vendor.computed(() => visibleChallenges.value.filter((c) => c.completed).length);
    function checkAndComplete(id) {
      const task = challenges.value.find((c) => c.id === id);
      if (!task || task.completed)
        return;
      task.completed = true;
      triggerCompletion(task);
    }
    function triggerCompletion(task) {
      if (popupDismissTimer)
        clearTimeout(popupDismissTimer);
      completedChallenge.value = task;
      try {
        common_vendor.index.vibrateShort({ type: "heavy" });
      } catch (_) {
      }
      try {
        const audio = common_vendor.index.createInnerAudioContext();
        audio.src = "/static/sounds/complete.mp3";
        audio.play();
      } catch (_) {
      }
      popupDismissTimer = setTimeout(() => {
        completedChallenge.value = null;
        popupDismissTimer = null;
      }, 3e3);
    }
    const myStrokes = common_vendor.ref([]);
    const partnerStrokes = common_vendor.ref([]);
    const mapPolylines = common_vendor.ref([]);
    function updatePolylines() {
      mapPolylines.value = [
        ...myStrokes.value.filter((s) => s.points.length >= 2).map((s) => ({ points: s.points, color: "#2196f3", width: 5 })),
        ...partnerStrokes.value.filter((s) => s.points.length >= 2).map((s) => ({ points: s.points, color: "#f44336", width: 5 }))
      ];
    }
    const toolMode = common_vendor.ref("none");
    function toggleTool(mode) {
      toolMode.value = toolMode.value === mode ? "none" : mode;
    }
    let ctx = null;
    let canvasW = 0;
    let canvasH = 0;
    let canvasLeft = 0;
    let canvasTop = 0;
    let currentPoints = [];
    common_vendor.watch(toolMode, (val) => {
      if (val === "none")
        currentPoints = [];
    });
    function initCanvas() {
      common_vendor.index.createSelectorQuery().in(_inst).select("#draw-canvas").node().exec((res) => {
        var _a;
        const node = (_a = res[0]) == null ? void 0 : _a.node;
        if (!node)
          return;
        common_vendor.index.createSelectorQuery().in(_inst).select(".map-container").boundingClientRect((rect) => {
          if (!rect)
            return;
          canvasW = rect.width;
          canvasH = rect.height;
          canvasLeft = rect.left;
          canvasTop = rect.top;
          node.width = rect.width;
          node.height = rect.height;
          ctx = node.getContext("2d");
          redrawAllStrokes();
        }).exec();
      });
    }
    function redrawAllStrokes() {
      if (!ctx || !canvasW || !canvasH)
        return;
      ctx.clearRect(0, 0, canvasW, canvasH);
      for (const s of myStrokes.value)
        drawPathOnCanvas(s.points, "#2196f3BB", 5);
      for (const s of partnerStrokes.value)
        drawPathOnCanvas(s.points, "#f44336BB", 5);
    }
    function drawPathOnCanvas(points, color, width) {
      if (points.length < 2 || !ctx)
        return;
      const first = ptToXY(points[0]);
      if (!first)
        return;
      ctx.beginPath();
      ctx.strokeStyle = color;
      ctx.lineWidth = width;
      ctx.lineCap = "round";
      ctx.lineJoin = "round";
      ctx.moveTo(first.x, first.y);
      for (let i = 1; i < points.length; i++) {
        const p = ptToXY(points[i]);
        if (p)
          ctx.lineTo(p.x, p.y);
      }
      ctx.stroke();
    }
    let mapCtx = null;
    let region = null;
    let lastRegionKey = "";
    let regionPollTimer = null;
    common_vendor.onMounted(() => {
      if (!isSolo.value)
        api_match.setMessageHandler(onWsMessage);
      updateMyLocation();
      locationTimer = setInterval(updateMyLocation, 3e3);
      common_vendor.nextTick$1(() => initCanvas());
      setTimeout(() => {
        mapCtx = common_vendor.index.createMapContext("trip-map", _inst);
        fetchRegion();
        regionPollTimer = setInterval(() => {
          mapCtx == null ? void 0 : mapCtx.getRegion({
            success: (r) => {
              const key = `${r.northeast.latitude.toFixed(6)}|${r.northeast.longitude.toFixed(6)}`;
              if (key === lastRegionKey)
                return;
              lastRegionKey = key;
              region = {
                sw: { latitude: r.southwest.latitude, longitude: r.southwest.longitude },
                ne: { latitude: r.northeast.latitude, longitude: r.northeast.longitude }
              };
              redrawAllStrokes();
            }
          });
        }, 100);
      }, 500);
    });
    common_vendor.onUnmounted(() => {
      if (locationTimer)
        clearInterval(locationTimer);
      if (regionPollTimer)
        clearInterval(regionPollTimer);
      if (popupDismissTimer)
        clearTimeout(popupDismissTimer);
      if (!isSolo.value)
        api_match.disconnectMatch();
    });
    common_vendor.onLoad((query) => {
      spotId.value = Number((query == null ? void 0 : query.spotId) ?? 0);
      spotName.value = decodeURIComponent((query == null ? void 0 : query.spotName) ?? "");
      partnerNickname.value = decodeURIComponent((query == null ? void 0 : query.partnerNickname) ?? "搭子");
      isSolo.value = (query == null ? void 0 : query.solo) === "true";
    });
    function fetchRegion() {
      mapCtx == null ? void 0 : mapCtx.getRegion({
        success: (r) => {
          lastRegionKey = `${r.northeast.latitude.toFixed(6)}|${r.northeast.longitude.toFixed(6)}`;
          region = {
            sw: { latitude: r.southwest.latitude, longitude: r.southwest.longitude },
            ne: { latitude: r.northeast.latitude, longitude: r.northeast.longitude }
          };
          redrawAllStrokes();
        }
      });
    }
    function onRegionChange(e) {
      if (e.type === "begin") {
        if (ctx)
          ctx.clearRect(0, 0, canvasW, canvasH);
      }
      if (e.type === "end") {
        fetchRegion();
      }
    }
    function xyToPt(x, y) {
      if (!region || !canvasW || !canvasH)
        return null;
      return {
        latitude: region.ne.latitude - y / canvasH * (region.ne.latitude - region.sw.latitude),
        longitude: region.sw.longitude + x / canvasW * (region.ne.longitude - region.sw.longitude)
      };
    }
    function ptToXY(p) {
      if (!region || !canvasW || !canvasH)
        return null;
      return {
        x: (p.longitude - region.sw.longitude) / (region.ne.longitude - region.sw.longitude) * canvasW,
        y: (region.ne.latitude - p.latitude) / (region.ne.latitude - region.sw.latitude) * canvasH
      };
    }
    function redrawCanvas() {
      if (!ctx || !canvasW || !canvasH || currentPoints.length < 2)
        return;
      redrawAllStrokes();
      drawPathOnCanvas(currentPoints, "#2196f3BB", 5);
    }
    function onDrawStart(e) {
      var _a;
      const t = (_a = e.touches) == null ? void 0 : _a[0];
      if (!t)
        return;
      if (!region) {
        fetchRegion();
        return;
      }
      const x = t.clientX - canvasLeft;
      const y = t.clientY - canvasTop;
      if (toolMode.value === "pen") {
        const pt = xyToPt(x, y);
        currentPoints = pt ? [pt] : [];
      } else if (toolMode.value === "eraser") {
        const pt = xyToPt(x, y);
        if (pt)
          eraseNear(pt);
      }
    }
    function onDrawMove(e) {
      var _a;
      const t = (_a = e.touches) == null ? void 0 : _a[0];
      if (!t || toolMode.value !== "pen")
        return;
      const pt = xyToPt(t.clientX - canvasLeft, t.clientY - canvasTop);
      if (!pt)
        return;
      if (currentPoints.length > 0) {
        const last = currentPoints[currentPoints.length - 1];
        if (Math.abs(pt.latitude - last.latitude) + Math.abs(pt.longitude - last.longitude) < 1e-5)
          return;
      }
      currentPoints.push(pt);
      redrawCanvas();
    }
    function onDrawEnd() {
      if (toolMode.value !== "pen" || currentPoints.length < 2) {
        currentPoints = [];
        redrawAllStrokes();
        return;
      }
      const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
      const stroke = { id, points: [...currentPoints] };
      currentPoints = [];
      myStrokes.value = [...myStrokes.value, stroke];
      updatePolylines();
      redrawAllStrokes();
      if (!isSolo.value)
        api_match.sendMatch("drawStroke", { id: stroke.id, points: stroke.points });
      const n = myStrokes.value.length;
      if (n >= 1)
        checkAndComplete("first_stroke");
      if (n >= 3)
        checkAndComplete("three_strokes");
    }
    function eraseNear(pt) {
      const THRESHOLD = 4e-4;
      for (const stroke of myStrokes.value) {
        for (const p of stroke.points) {
          if (Math.abs(p.latitude - pt.latitude) + Math.abs(p.longitude - pt.longitude) < THRESHOLD) {
            myStrokes.value = myStrokes.value.filter((s) => s.id !== stroke.id);
            updatePolylines();
            redrawAllStrokes();
            if (!isSolo.value)
              api_match.sendMatch("eraseStroke", { id: stroke.id });
            return;
          }
        }
      }
    }
    function clearMyStrokes() {
      const ids = myStrokes.value.map((s) => s.id);
      myStrokes.value = [];
      updatePolylines();
      redrawAllStrokes();
      if (!isSolo.value)
        ids.forEach((id) => api_match.sendMatch("eraseStroke", { id }));
    }
    function onWsMessage(msg) {
      switch (msg.type) {
        case "locationUpdate": {
          partnerLat.value = msg.payload.latitude;
          partnerLng.value = msg.payload.longitude;
          partnerLocationCount++;
          if (partnerLocationCount >= 5)
            checkAndComplete("partner_active");
          if (partnerLat.value !== null && partnerLng.value !== null) {
            const d = haversine(myLat.value, myLng.value, partnerLat.value, partnerLng.value);
            if (d < 200)
              checkAndComplete("near_partner");
          }
          break;
        }
        case "partnerLeft":
          common_vendor.index.showModal({ title: "搭子已离开", content: "搭子结束了旅途", showCancel: false });
          partnerLat.value = null;
          partnerLng.value = null;
          break;
        case "partnerDrawStroke": {
          const { id, points } = msg.payload;
          partnerStrokes.value = [...partnerStrokes.value.filter((s) => s.id !== id), { id, points }];
          updatePolylines();
          redrawAllStrokes();
          break;
        }
        case "partnerEraseStroke":
          partnerStrokes.value = partnerStrokes.value.filter((s) => s.id !== msg.payload.id);
          updatePolylines();
          redrawAllStrokes();
          break;
      }
    }
    function updateMyLocation() {
      common_vendor.index.getLocation({
        type: "gcj02",
        success: (res) => {
          if (startLat === null) {
            startLat = res.latitude;
            startLng = res.longitude;
          }
          myLat.value = res.latitude;
          myLng.value = res.longitude;
          if (!isSolo.value)
            api_match.sendMatch("location", { latitude: res.latitude, longitude: res.longitude });
          if (startLat !== null && startLng !== null) {
            const moved = haversine(startLat, startLng, res.latitude, res.longitude);
            if (moved > 200)
              checkAndComplete("traveled_200m");
          }
        }
      });
    }
    const chatOpen = common_vendor.ref(false);
    const chatInput = common_vendor.ref("");
    const chatMessages = common_vendor.ref([]);
    const aiLoading = common_vendor.ref(false);
    const scrollTarget = common_vendor.ref("chat-bottom");
    function toggleChat() {
      chatOpen.value = !chatOpen.value;
    }
    async function sendChat() {
      const text = chatInput.value.trim();
      if (!text || aiLoading.value)
        return;
      chatMessages.value.push({ role: "user", content: text });
      chatInput.value = "";
      aiLoading.value = true;
      scrollTarget.value = "chat-bottom";
      try {
        const history = chatMessages.value.slice(-6).map((m) => ({ role: m.role, content: m.content }));
        const res = await api_tripChat.sendTripChat({
          message: text,
          spotName: spotName.value || "景区",
          history: history.slice(0, -1)
          // 最后一条是刚加的 user，不重复传
        });
        if (res.code === 200) {
          const aiMsg = {
            role: "assistant",
            content: res.data.text,
            audioUrl: res.data.audioUrl
          };
          chatMessages.value.push(aiMsg);
          if (res.data.audioUrl) {
            playAudio(res.data.audioUrl);
          }
        } else {
          chatMessages.value.push({ role: "assistant", content: res.message || "AI 暂时无法回答，请稍后再试" });
        }
      } catch {
        chatMessages.value.push({ role: "assistant", content: "网络异常，请稍后重试" });
      } finally {
        aiLoading.value = false;
        scrollTarget.value = "chat-bottom";
      }
    }
    function playAudio(url) {
      try {
        const audio = common_vendor.index.createInnerAudioContext();
        audio.src = url;
        audio.play();
      } catch (_) {
      }
    }
    function leaveTrip() {
      common_vendor.index.showModal({
        title: "结束旅途",
        content: "确定要结束本次旅途吗？",
        confirmText: "结束",
        confirmColor: "#e53935",
        success: (res) => {
          if (res.confirm) {
            if (!isSolo.value) {
              api_match.sendMatch("leave");
              api_match.disconnectMatch();
            }
            common_vendor.index.redirectTo({ url: "/pages/index/index" });
          }
        }
      });
    }
    common_vendor.onBackPress(() => {
      leaveTrip();
      return true;
    });
    function haversine(lat1, lng1, lat2, lng2) {
      const R = 6371e3;
      const dLat = (lat2 - lat1) * Math.PI / 180;
      const dLng = (lng2 - lng1) * Math.PI / 180;
      const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLng / 2) ** 2;
      return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: completedChallenge.value
      }, completedChallenge.value ? {
        b: common_vendor.t(completedChallenge.value.icon),
        c: common_vendor.t(completedChallenge.value.title),
        d: common_vendor.t(completedChallenge.value.desc)
      } : {}, {
        e: myLat.value,
        f: myLng.value,
        g: markers.value,
        h: mapPolylines.value,
        i: common_vendor.o(onRegionChange, "99"),
        j: common_vendor.s(toolMode.value !== "none" ? "pointer-events:auto;" : "pointer-events:none;"),
        k: common_vendor.o(onDrawStart, "32"),
        l: common_vendor.o(onDrawMove, "8a"),
        m: common_vendor.o(onDrawEnd, "79"),
        n: toolMode.value === "pen" ? 1 : "",
        o: common_vendor.o(($event) => toggleTool("pen"), "d4"),
        p: toolMode.value === "eraser" ? 1 : "",
        q: common_vendor.o(($event) => toggleTool("eraser"), "c2"),
        r: common_vendor.o(clearMyStrokes, "2a"),
        s: toolMode.value !== "none"
      }, toolMode.value !== "none" ? {} : {}, {
        t: common_vendor.t(spotName.value),
        v: common_vendor.t(isSolo.value ? "独自出发" : partnerNickname.value),
        w: !isSolo.value
      }, !isSolo.value ? {
        x: common_vendor.t(distanceText.value)
      } : {}, {
        y: completedCount.value / visibleChallenges.value.length * 100 + "%",
        z: common_vendor.t(completedCount.value),
        A: common_vendor.t(visibleChallenges.value.length),
        B: common_vendor.t(showChallenges.value ? "▲" : "▼"),
        C: common_vendor.o(($event) => showChallenges.value = !showChallenges.value, "4c"),
        D: showChallenges.value
      }, showChallenges.value ? {
        E: common_vendor.f(visibleChallenges.value, (c, k0, i0) => {
          return {
            a: common_vendor.t(c.icon),
            b: common_vendor.t(c.title),
            c: common_vendor.t(c.desc),
            d: common_vendor.t(c.completed ? "✓" : "○"),
            e: c.id,
            f: c.completed ? 1 : ""
          };
        })
      } : {}, {
        F: common_vendor.o(leaveTrip, "ef"),
        G: common_vendor.o(toggleChat, "24"),
        H: chatOpen.value
      }, chatOpen.value ? common_vendor.e({
        I: common_vendor.o(toggleChat, "9c"),
        J: chatMessages.value.length === 0
      }, chatMessages.value.length === 0 ? {
        K: common_vendor.t(spotName.value || "景区")
      } : {}, {
        L: common_vendor.f(chatMessages.value, (msg, idx, i0) => {
          return common_vendor.e({
            a: common_vendor.t(msg.content),
            b: msg.role === "assistant" && msg.audioUrl
          }, msg.role === "assistant" && msg.audioUrl ? {
            c: common_vendor.o(($event) => playAudio(msg.audioUrl), idx)
          } : {}, {
            d: idx,
            e: "msg-" + idx,
            f: common_vendor.n(msg.role === "user" ? "chat-msg-user" : "chat-msg-ai")
          });
        }),
        M: aiLoading.value
      }, aiLoading.value ? {} : {}, {
        N: scrollTarget.value,
        O: aiLoading.value,
        P: common_vendor.o(sendChat, "22"),
        Q: chatInput.value,
        R: common_vendor.o(($event) => chatInput.value = $event.detail.value, "1f"),
        S: aiLoading.value || !chatInput.value.trim() ? 1 : "",
        T: common_vendor.o(sendChat, "de")
      }) : {});
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-b49b36a1"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/trip/trip.js.map
