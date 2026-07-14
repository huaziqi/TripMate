"use strict";
const common_vendor = require("../../common/vendor.js");
const api_tts = require("../../api/tts.js");
const api_spot = require("../../api/spot.js");
const api_favorite = require("../../api/favorite.js");
const api_history = require("../../api/history.js");
const composables_useAuth = require("../../composables/useAuth.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "spot-detail",
  setup(__props) {
    const loading = common_vendor.ref(true);
    const spot = common_vendor.ref(null);
    const coverError = common_vendor.ref(false);
    const isFavorited = common_vendor.ref(false);
    const imageLoadFailed = common_vendor.ref(false);
    const audioContext = common_vendor.ref(null);
    const isPlaying = common_vendor.ref(false);
    const generatingAudio = common_vendor.ref(false);
    const currentAudioUrl = common_vendor.ref("");
    const { authState, login } = composables_useAuth.useAuth();
    const guideButtonText = common_vendor.computed(() => {
      if (generatingAudio.value) {
        return "生成中";
      }
      if (isPlaying.value) {
        return "暂停导览";
      }
      return "播放导览";
    });
    common_vendor.onLoad((options) => {
      const id = Number(options == null ? void 0 : options.id);
      if (!id || Number.isNaN(id)) {
        loading.value = false;
        common_vendor.index.showToast({
          title: "景点ID无效",
          icon: "none"
        });
        return;
      }
      loadSpotDetail(id);
    });
    common_vendor.onUnload(() => {
      stopGuideAudio();
    });
    async function loadSpotDetail(id) {
      var _a;
      loading.value = true;
      imageLoadFailed.value = false;
      try {
        const res = await api_spot.getScenicSpotById(id);
        const data = (res == null ? void 0 : res.data) || res;
        if (!data) {
          spot.value = null;
          return;
        }
        spot.value = {
          ...data,
          imageUrl: data.imageUrl || data.image_url || ""
        };
        common_vendor.index.__f__("log", "at pages/spot-detail/spot-detail.vue:176", "景点详情数据：", spot.value);
        common_vendor.index.__f__("log", "at pages/spot-detail/spot-detail.vue:177", "图片路径：", (_a = spot.value) == null ? void 0 : _a.imageUrl);
        if (authState.isLoggedIn) {
          try {
            const favoriteRes = await api_favorite.checkFavorite(id);
            isFavorited.value = Boolean((favoriteRes == null ? void 0 : favoriteRes.data) ?? favoriteRes);
          } catch (authError) {
            common_vendor.index.__f__("error", "at pages/spot-detail/spot-detail.vue:184", "收藏状态处理失败：", authError);
          }
          try {
            if (spot.value) {
              await api_history.addHistory(
                "VIEW_SPOT",
                spot.value.id,
                `查看了景点：${spot.value.name}`
              );
            }
          } catch (authError) {
            common_vendor.index.__f__("error", "at pages/spot-detail/spot-detail.vue:196", "记录浏览历史失败：", authError);
          }
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/spot-detail/spot-detail.vue:200", "加载景点详情失败：", error);
        spot.value = null;
        common_vendor.index.showToast({
          title: "加载失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    }
    function getImageSrc(url) {
      if (!url)
        return "";
      if (url.startsWith("http://") || url.startsWith("https://")) {
        return url;
      }
      if (url.startsWith("/static/")) {
        return url;
      }
      if (url.startsWith("static/")) {
        return "/" + url;
      }
      return url;
    }
    function handleImageError(error) {
      var _a;
      imageLoadFailed.value = true;
      coverError.value = true;
      common_vendor.index.__f__("error", "at pages/spot-detail/spot-detail.vue:234", "图片加载失败：", (_a = spot.value) == null ? void 0 : _a.imageUrl, error);
    }
    function buildGuideText() {
      if (!spot.value) {
        return "";
      }
      const name = spot.value.name || "该景点";
      const category = spot.value.category || "校园景点";
      const description = spot.value.description || "";
      const address = spot.value.address || "";
      let text = `欢迎来到${name}。`;
      if (category) {
        text += `这里属于${category}类景点。`;
      }
      if (address) {
        text += `它位于${address}。`;
      }
      if (description) {
        text += description;
      } else {
        text += "这里是西南大学北碚校区的重要景点之一，适合游览、拍照和打卡。";
      }
      if (text.length > 140) {
        text = text.slice(0, 140);
      }
      return text;
    }
    function goRouteRecommend() {
      if (!spot.value) {
        return;
      }
      common_vendor.index.navigateTo({
        url: `/pages/route/route?spotName=${encodeURIComponent(spot.value.name)}`
      });
    }
    function backToMap() {
      common_vendor.index.navigateBack();
    }
    async function playGuide() {
      var _a;
      if (!spot.value) {
        return;
      }
      if (generatingAudio.value) {
        return;
      }
      if (audioContext.value && isPlaying.value) {
        audioContext.value.pause();
        isPlaying.value = false;
        return;
      }
      if (audioContext.value && currentAudioUrl.value) {
        audioContext.value.play();
        isPlaying.value = true;
        return;
      }
      try {
        generatingAudio.value = true;
        common_vendor.index.showLoading({
          title: "生成导览中"
        });
        const guideText = buildGuideText();
        if (!guideText) {
          common_vendor.index.showToast({
            title: "暂无导览内容",
            icon: "none"
          });
          return;
        }
        common_vendor.index.__f__("log", "at pages/spot-detail/spot-detail.vue:322", "TTS导览文本：", guideText);
        const result = await api_tts.synthesizeSpeech({ text: guideText, lang: "zh" });
        const audioUrl = (result == null ? void 0 : result.audioUrl) || ((_a = result == null ? void 0 : result.data) == null ? void 0 : _a.audioUrl);
        common_vendor.index.__f__("log", "at pages/spot-detail/spot-detail.vue:327", "TTS返回结果：", result);
        common_vendor.index.__f__("log", "at pages/spot-detail/spot-detail.vue:328", "TTS音频地址：", audioUrl);
        if (!audioUrl) {
          common_vendor.index.showToast({
            title: "未获取到音频",
            icon: "none"
          });
          return;
        }
        currentAudioUrl.value = audioUrl;
        const ctx = common_vendor.index.createInnerAudioContext();
        ctx.src = audioUrl;
        ctx.autoplay = false;
        ctx.onPlay(() => {
          isPlaying.value = true;
          common_vendor.index.__f__("log", "at pages/spot-detail/spot-detail.vue:346", "开始播放景点导览");
        });
        ctx.onPause(() => {
          isPlaying.value = false;
          common_vendor.index.__f__("log", "at pages/spot-detail/spot-detail.vue:351", "暂停景点导览");
        });
        ctx.onEnded(() => {
          isPlaying.value = false;
          common_vendor.index.__f__("log", "at pages/spot-detail/spot-detail.vue:356", "景点导览播放结束");
        });
        ctx.onError((error) => {
          common_vendor.index.__f__("error", "at pages/spot-detail/spot-detail.vue:360", "景点导览播放失败：", error);
          isPlaying.value = false;
          common_vendor.index.showToast({
            title: "音频播放失败",
            icon: "none"
          });
        });
        audioContext.value = ctx;
        ctx.play();
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/spot-detail/spot-detail.vue:373", "生成或播放导览失败：", error);
        common_vendor.index.showToast({
          title: "导览生成失败",
          icon: "none"
        });
      } finally {
        generatingAudio.value = false;
        common_vendor.index.hideLoading();
      }
    }
    function stopGuideAudio() {
      if (audioContext.value) {
        audioContext.value.stop();
        audioContext.value.destroy();
        audioContext.value = null;
      }
      isPlaying.value = false;
      currentAudioUrl.value = "";
    }
    async function favoriteSpot() {
      if (!spot.value) {
        return;
      }
      let token = common_vendor.index.getStorageSync("token");
      if (!token) {
        try {
          await login();
          token = common_vendor.index.getStorageSync("token");
        } catch {
          common_vendor.index.showToast({
            title: "请先登录",
            icon: "none"
          });
          return;
        }
      }
      if (!token) {
        common_vendor.index.showToast({
          title: "登录失败",
          icon: "none"
        });
        return;
      }
      try {
        if (isFavorited.value) {
          await api_favorite.removeFavorite(spot.value.id);
          isFavorited.value = false;
          common_vendor.index.showToast({
            title: "已取消收藏",
            icon: "none"
          });
        } else {
          await api_favorite.addFavorite(spot.value.id);
          isFavorited.value = true;
          try {
            await api_history.addHistory(
              "FAVORITE_SPOT",
              spot.value.id,
              `收藏了景点：${spot.value.name}`
            );
          } catch (historyError) {
            common_vendor.index.__f__("warn", "at pages/spot-detail/spot-detail.vue:444", "收藏成功，但记录历史失败：", historyError);
          }
          common_vendor.index.showToast({
            title: "收藏成功",
            icon: "success"
          });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/spot-detail/spot-detail.vue:453", "收藏操作失败：", error);
        common_vendor.index.showToast({
          title: "收藏失败，请重新登录",
          icon: "none"
        });
        common_vendor.index.removeStorageSync("token");
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: loading.value
      }, loading.value ? {} : !spot.value ? {} : common_vendor.e({
        c: spot.value.imageUrl && !imageLoadFailed.value && !coverError.value
      }, spot.value.imageUrl && !imageLoadFailed.value && !coverError.value ? {
        d: getImageSrc(spot.value.imageUrl),
        e: common_vendor.o(handleImageError, "5b")
      } : {}, {
        f: common_vendor.t(spot.value.name),
        g: common_vendor.t(spot.value.category || "景点"),
        h: common_vendor.t(spot.value.address || "暂无地址"),
        i: common_vendor.t(spot.value.region || "暂无地区"),
        j: common_vendor.t(spot.value.description || "暂无景点简介"),
        k: common_vendor.o(goRouteRecommend, "37"),
        l: common_vendor.o(backToMap, "1e"),
        m: common_vendor.t(guideButtonText.value),
        n: generatingAudio.value ? 1 : "",
        o: common_vendor.o(playGuide, "27"),
        p: common_vendor.t(isFavorited.value ? "已收藏" : "收藏"),
        q: common_vendor.o(favoriteSpot, "6f")
      }), {
        b: !spot.value
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1d237977"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/spot-detail/spot-detail.js.map
