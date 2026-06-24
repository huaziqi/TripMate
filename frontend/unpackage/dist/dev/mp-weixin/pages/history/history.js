"use strict";
const common_vendor = require("../../common/vendor.js");
const api_history = require("../../api/history.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "history",
  setup(__props) {
    const loading = common_vendor.ref(false);
    const historyList = common_vendor.ref([]);
    common_vendor.onShow(() => {
      loadHistory();
    });
    async function loadHistory() {
      loading.value = true;
      try {
        historyList.value = await api_history.getHistoryList();
        common_vendor.index.__f__("log", "at pages/history/history.vue:64", "历史记录：", historyList.value);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/history/history.vue:66", "加载历史记录失败：", error);
        common_vendor.index.showToast({
          title: "加载历史记录失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    }
    function getTypeText(type) {
      const map = {
        VIEW_SPOT: "查看景点",
        FAVORITE_SPOT: "收藏景点",
        SEARCH_SPOT: "搜索景点",
        PLAY_AUDIO: "播放导览",
        AI_CHAT: "AI 问答"
      };
      return map[type] || "操作记录";
    }
    function getTypeIcon(type) {
      const map = {
        VIEW_SPOT: "👀",
        FAVORITE_SPOT: "⭐",
        SEARCH_SPOT: "🔍",
        PLAY_AUDIO: "🔊",
        AI_CHAT: "🤖"
      };
      return map[type] || "📌";
    }
    function isSpotRecord(item) {
      return ["VIEW_SPOT", "FAVORITE_SPOT", "PLAY_AUDIO"].includes(item.type) && !!item.targetId;
    }
    function handleHistoryClick(item) {
      if (isSpotRecord(item)) {
        common_vendor.index.navigateTo({
          url: `/pages/spot-detail/spot-detail?id=${item.targetId}`
        });
      }
    }
    function formatTime(time) {
      if (!time) {
        return "";
      }
      const date = new Date(time);
      if (Number.isNaN(date.getTime())) {
        return time;
      }
      const now = /* @__PURE__ */ new Date();
      const diff = now.getTime() - date.getTime();
      const minute = 60 * 1e3;
      const hour = 60 * minute;
      const day = 24 * hour;
      if (diff < minute) {
        return "刚刚";
      }
      if (diff < hour) {
        return `${Math.floor(diff / minute)}分钟前`;
      }
      if (diff < day) {
        return `${Math.floor(diff / hour)}小时前`;
      }
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const dayText = String(date.getDate()).padStart(2, "0");
      const hourText = String(date.getHours()).padStart(2, "0");
      const minuteText = String(date.getMinutes()).padStart(2, "0");
      return `${month}-${dayText} ${hourText}:${minuteText}`;
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: loading.value
      }, loading.value ? {} : historyList.value.length === 0 ? {} : {
        c: common_vendor.f(historyList.value, (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(getTypeIcon(item.type)),
            b: common_vendor.t(getTypeText(item.type)),
            c: common_vendor.t(formatTime(item.createTime)),
            d: common_vendor.t(item.content || "暂无内容"),
            e: isSpotRecord(item)
          }, isSpotRecord(item) ? {} : {}, {
            f: item.id,
            g: common_vendor.o(($event) => handleHistoryClick(item), item.id)
          });
        })
      }, {
        b: historyList.value.length === 0
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-b2d018fa"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/history/history.js.map
