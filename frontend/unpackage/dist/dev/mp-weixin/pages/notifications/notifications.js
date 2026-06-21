"use strict";
const common_vendor = require("../../common/vendor.js");
const api_notification = require("../../api/notification.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "notifications",
  setup(__props) {
    const list = common_vendor.ref([]);
    const page = common_vendor.ref(0);
    const loading = common_vendor.ref(false);
    const noMore = common_vendor.ref(false);
    common_vendor.onMounted(async () => {
      await load();
      api_notification.markAllRead().catch(() => {
      });
    });
    async function load(reset = false) {
      if (loading.value || noMore.value)
        return;
      if (reset) {
        page.value = 0;
        noMore.value = false;
      }
      loading.value = true;
      try {
        const res = await api_notification.fetchNotifications(page.value, 20);
        if (res.code === 200) {
          const items = res.data.items;
          list.value = reset ? items : [...list.value, ...items];
          if (list.value.length >= res.data.total)
            noMore.value = true;
          else
            page.value++;
        }
      } finally {
        loading.value = false;
      }
    }
    function typeBadge(type) {
      const map = {
        LIKE_POST: "👍",
        COMMENT_POST: "💬",
        NEW_FOLLOWER: "👤",
        MENTION_COMMENT: "@"
      };
      return map[type] || "🔔";
    }
    function actionText(item) {
      switch (item.type) {
        case "LIKE_POST":
          return `赞了你的攻略《${item.postTitle || ""}》`;
        case "COMMENT_POST":
          return `评论了你的攻略《${item.postTitle || ""}》`;
        case "NEW_FOLLOWER":
          return "关注了你";
        case "MENTION_COMMENT":
          return "在评论中提到了你";
        default:
          return "";
      }
    }
    function formatTime(t) {
      const d = new Date(t);
      const now = /* @__PURE__ */ new Date();
      const diff = (now.getTime() - d.getTime()) / 1e3;
      if (diff < 60)
        return "刚刚";
      if (diff < 3600)
        return `${Math.floor(diff / 60)}分钟前`;
      if (diff < 86400)
        return `${Math.floor(diff / 3600)}小时前`;
      return `${Math.floor(diff / 86400)}天前`;
    }
    function onTap(item) {
      if (item.postId) {
        common_vendor.index.navigateTo({ url: `/pages/guide/detail/detail?id=${item.postId}` });
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: list.value.length === 0 && !loading.value
      }, list.value.length === 0 && !loading.value ? {} : {}, {
        b: common_vendor.f(list.value, (item, k0, i0) => {
          var _a, _b, _c;
          return common_vendor.e({
            a: (_a = item.fromUser) == null ? void 0 : _a.avatarUrl
          }, ((_b = item.fromUser) == null ? void 0 : _b.avatarUrl) ? {
            b: item.fromUser.avatarUrl
          } : {}, {
            c: common_vendor.t(typeBadge(item.type)),
            d: common_vendor.t(((_c = item.fromUser) == null ? void 0 : _c.nickname) || "旅行者"),
            e: common_vendor.t(actionText(item)),
            f: item.commentContent
          }, item.commentContent ? {
            g: common_vendor.t(item.commentContent)
          } : {}, {
            h: common_vendor.t(formatTime(item.createdAt)),
            i: !item.read
          }, !item.read ? {} : {}, {
            j: item.id,
            k: !item.read ? 1 : "",
            l: common_vendor.o(($event) => onTap(item), item.id)
          });
        }),
        c: loading.value
      }, loading.value ? {} : {}, {
        d: noMore.value && list.value.length > 0
      }, noMore.value && list.value.length > 0 ? {} : {});
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e30a2353"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/notifications/notifications.js.map
