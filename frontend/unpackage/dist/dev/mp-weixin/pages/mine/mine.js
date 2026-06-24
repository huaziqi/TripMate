"use strict";
const common_vendor = require("../../common/vendor.js");
const composables_useAuth = require("../../composables/useAuth.js");
const composables_useElder = require("../../composables/useElder.js");
if (!Math) {
  TabBar();
}
const TabBar = () => "../../components/TabBar/TabBar.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "mine",
  setup(__props) {
    const { authState, login, logout, saveProfile } = composables_useAuth.useAuth();
    const { rpx, isElderMode, toggleElderMode } = composables_useElder.useElder();
    async function handleLogin() {
      try {
        await login();
        common_vendor.index.showToast({ title: "登录成功", icon: "success" });
      } catch {
        common_vendor.index.showToast({ title: "登录失败，请重试", icon: "none" });
      }
    }
    function handleLogout() {
      common_vendor.index.showModal({
        title: "退出登录",
        content: "确认退出登录吗？",
        success: (res) => {
          if (res.confirm) {
            logout();
            common_vendor.index.showToast({ title: "已退出登录", icon: "none" });
          }
        }
      });
    }
    function onSpotFavorites() {
      common_vendor.index.navigateTo({ url: "/pages/spot-favorites/spot-favorites" });
    }
    function onHistory() {
      common_vendor.index.navigateTo({
        url: "/pages/history/history"
      });
    }
    async function onChooseAvatar(e) {
      var _a;
      const newAvatarUrl = e.detail.avatarUrl;
      if (!newAvatarUrl)
        return;
      try {
        await saveProfile(((_a = authState.userInfo) == null ? void 0 : _a.nickname) || "", newAvatarUrl);
        common_vendor.index.showToast({ title: "头像已更新", icon: "success" });
      } catch {
        common_vendor.index.showToast({ title: "更新失败", icon: "none" });
      }
    }
    async function onNicknameBlur(e) {
      var _a, _b, _c;
      const newNickname = (_a = e.detail.value) == null ? void 0 : _a.trim();
      if (!newNickname || newNickname === ((_b = authState.userInfo) == null ? void 0 : _b.nickname))
        return;
      try {
        await saveProfile(newNickname, ((_c = authState.userInfo) == null ? void 0 : _c.avatarUrl) || "");
        common_vendor.index.showToast({ title: "昵称已更新", icon: "success" });
      } catch {
        common_vendor.index.showToast({ title: "更新失败", icon: "none" });
      }
    }
    function onNotifications() {
      common_vendor.index.navigateTo({ url: "/pages/notifications/notifications" });
    }
    function onMyPosts() {
      common_vendor.index.navigateTo({ url: "/pages/mine/my-posts/my-posts" });
    }
    function onCollect() {
      common_vendor.index.navigateTo({ url: "/pages/mine/my-favorites/my-favorites" });
    }
    function onLanguage() {
      common_vendor.index.navigateTo({ url: "/pages/language/language" });
    }
    function onElderToggle() {
      toggleElderMode();
    }
    function onAbout() {
      common_vendor.index.showModal({
        title: "关于 TripMate",
        content: "TripMate 智能旅行助手\n版本 1.0.0\n\n让旅行更简单、更美好。",
        showCancel: false
      });
    }
    return (_ctx, _cache) => {
      var _a, _b, _c, _d, _e;
      return common_vendor.e({
        a: !common_vendor.unref(authState).isLoggedIn
      }, !common_vendor.unref(authState).isLoggedIn ? {
        b: common_vendor.unref(rpx)(28),
        c: common_vendor.unref(rpx)(30),
        d: common_vendor.o(handleLogin, "9a")
      } : common_vendor.e({
        e: (_a = common_vendor.unref(authState).userInfo) == null ? void 0 : _a.avatarUrl
      }, ((_b = common_vendor.unref(authState).userInfo) == null ? void 0 : _b.avatarUrl) ? {
        f: common_vendor.unref(authState).userInfo.avatarUrl
      } : {}, {
        g: common_vendor.o(onChooseAvatar, "a3"),
        h: ((_c = common_vendor.unref(authState).userInfo) == null ? void 0 : _c.nickname) || "微信用户",
        i: common_vendor.unref(rpx)(34),
        j: common_vendor.o(onNicknameBlur, "f1"),
        k: common_vendor.t((_e = (_d = common_vendor.unref(authState).userInfo) == null ? void 0 : _d.openid) == null ? void 0 : _e.slice(0, 12)),
        l: common_vendor.unref(rpx)(22),
        m: common_vendor.unref(rpx)(28),
        n: common_vendor.o(onNotifications, "3e"),
        o: common_vendor.unref(rpx)(28),
        p: common_vendor.o(onMyPosts, "59"),
        q: common_vendor.unref(rpx)(28),
        r: common_vendor.o(onHistory, "3e"),
        s: common_vendor.unref(rpx)(28),
        t: common_vendor.o(onCollect, "86"),
        v: common_vendor.unref(rpx)(28),
        w: common_vendor.o(onSpotFavorites, "ec"),
        x: common_vendor.unref(rpx)(28),
        y: common_vendor.o(onLanguage, "8f"),
        z: common_vendor.unref(rpx)(28),
        A: common_vendor.unref(isElderMode),
        B: common_vendor.o(onElderToggle, "76"),
        C: common_vendor.unref(rpx)(28),
        D: common_vendor.o(onAbout, "3c"),
        E: common_vendor.unref(rpx)(28),
        F: common_vendor.o(handleLogout, "fd")
      }), {
        G: common_vendor.p({
          active: "mine"
        })
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-7c2ebfa5"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/mine/mine.js.map
