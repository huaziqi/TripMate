"use strict";
const common_vendor = require("../../common/vendor.js");
const api_badge = require("../../api/badge.js");
const composables_useAuth = require("../../composables/useAuth.js");
if (!Math) {
  (BadgeCard + Badge3DViewer + TabBar)();
}
const TabBar = () => "../../components/TabBar/TabBar.js";
const BadgeCard = () => "../../components/BadgeCard/BadgeCard.js";
const Badge3DViewer = () => "../../components/Badge3DViewer/Badge3DViewer.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "badges",
  setup(__props) {
    const { listBadges } = api_badge.useBadgeApi();
    const { authState } = composables_useAuth.useAuth();
    const badges = common_vendor.ref([]);
    const selectedBadge = common_vendor.ref(null);
    const activeTab = common_vendor.ref("SPOT");
    const nickname = common_vendor.computed(() => {
      var _a;
      return ((_a = authState.userInfo) == null ? void 0 : _a.nickname) || "旅行者";
    });
    const avatarUrl = common_vendor.computed(() => {
      var _a;
      return ((_a = authState.userInfo) == null ? void 0 : _a.avatarUrl) || "";
    });
    const spotBadges = common_vendor.computed(() => badges.value.filter((b) => b.type === "SPOT"));
    const achievementBadges = common_vendor.computed(() => badges.value.filter((b) => b.type === "ACHIEVEMENT"));
    const currentBadges = common_vendor.computed(() => activeTab.value === "SPOT" ? spotBadges.value : achievementBadges.value);
    const unlockedCount = common_vendor.computed(() => badges.value.filter((b) => b.unlocked).length);
    const total = common_vendor.computed(() => badges.value.length);
    common_vendor.onMounted(async () => {
      try {
        const res = await listBadges();
        if (res.code === 200) {
          badges.value = res.data.map((b) => ({
            ...b,
            unlocked: true,
            unlockedAt: b.unlockedAt || (/* @__PURE__ */ new Date()).toISOString()
          }));
        }
      } catch {
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      }
    });
    function openDetail(badge) {
      selectedBadge.value = badge;
    }
    function formatDate(dateStr) {
      if (!dateStr)
        return "";
      const d = new Date(dateStr);
      return `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()}`;
    }
    function rarityColor(rarity) {
      const map = {
        COMMON: "#9e9e9e",
        RARE: "#2196f3",
        EPIC: "#9c27b0",
        LEGENDARY: "#ffd700"
      };
      return map[rarity] ?? "#9e9e9e";
    }
    function rarityLabel(rarity) {
      const map = {
        COMMON: "普通",
        RARE: "稀有",
        EPIC: "史诗",
        LEGENDARY: "传说"
      };
      return map[rarity] ?? rarity;
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: avatarUrl.value
      }, avatarUrl.value ? {
        b: avatarUrl.value
      } : {}, {
        c: common_vendor.t(nickname.value),
        d: common_vendor.t(unlockedCount.value),
        e: common_vendor.t(total.value),
        f: common_vendor.t(spotBadges.value.length),
        g: activeTab.value === "SPOT" ? 1 : "",
        h: common_vendor.o(($event) => activeTab.value = "SPOT", "8b"),
        i: common_vendor.t(achievementBadges.value.length),
        j: activeTab.value === "ACHIEVEMENT" ? 1 : "",
        k: common_vendor.o(($event) => activeTab.value = "ACHIEVEMENT", "16"),
        l: common_vendor.f(currentBadges.value, (badge, k0, i0) => {
          return {
            a: "4c6063a8-0-" + i0,
            b: common_vendor.p({
              badge,
              size: "medium"
            }),
            c: common_vendor.t(badge.name),
            d: badge.id,
            e: common_vendor.o(($event) => openDetail(badge), badge.id)
          };
        }),
        m: selectedBadge.value
      }, selectedBadge.value ? common_vendor.e({
        n: common_vendor.p({
          badge: selectedBadge.value
        }),
        o: common_vendor.t(rarityLabel(selectedBadge.value.rarity)),
        p: rarityColor(selectedBadge.value.rarity),
        q: common_vendor.t(selectedBadge.value.name),
        r: common_vendor.t(selectedBadge.value.description),
        s: common_vendor.t(formatDate(selectedBadge.value.unlockedAt)),
        t: selectedBadge.value.note
      }, selectedBadge.value.note ? {
        v: common_vendor.t(selectedBadge.value.note)
      } : {}, {
        w: common_vendor.o(($event) => selectedBadge.value = null, "49"),
        x: common_vendor.o(($event) => selectedBadge.value = null, "ed")
      }) : {}, {
        y: common_vendor.p({
          active: "badges"
        })
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-4c6063a8"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/badges/badges.js.map
