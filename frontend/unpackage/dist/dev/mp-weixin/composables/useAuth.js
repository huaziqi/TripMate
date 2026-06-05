"use strict";
const common_vendor = require("../common/vendor.js");
const api_auth = require("../api/auth.js");
const USER_INFO_KEY = "userInfo";
const authState = common_vendor.reactive({
  isLoggedIn: false,
  userInfo: null
});
function useAuth() {
  function loadFromStorage() {
    const token = common_vendor.index.getStorageSync("token");
    const raw = common_vendor.index.getStorageSync(USER_INFO_KEY);
    if (token && raw) {
      try {
        authState.userInfo = JSON.parse(raw);
        authState.isLoggedIn = true;
      } catch {
        logout();
      }
    }
  }
  async function login() {
    return new Promise((resolve, reject) => {
      common_vendor.index.login({
        provider: "weixin",
        success: async (loginRes) => {
          try {
            const res = await api_auth.wxLogin(loginRes.code);
            if (res.code === 200) {
              const { token, openid, nickname, avatarUrl } = res.data;
              common_vendor.index.setStorageSync("token", token);
              const info = { openid, nickname, avatarUrl };
              common_vendor.index.setStorageSync(USER_INFO_KEY, JSON.stringify(info));
              authState.userInfo = info;
              authState.isLoggedIn = true;
              resolve();
            } else {
              reject(new Error(res.message));
            }
          } catch (e) {
            reject(e);
          }
        },
        fail: (err) => reject(err)
      });
    });
  }
  function logout() {
    common_vendor.index.removeStorageSync("token");
    common_vendor.index.removeStorageSync(USER_INFO_KEY);
    authState.isLoggedIn = false;
    authState.userInfo = null;
  }
  async function saveProfile(nickname, avatarUrl) {
    await api_auth.updateProfile(nickname, avatarUrl);
    if (authState.userInfo) {
      authState.userInfo.nickname = nickname;
      authState.userInfo.avatarUrl = avatarUrl;
      common_vendor.index.setStorageSync(USER_INFO_KEY, JSON.stringify(authState.userInfo));
    }
  }
  return { authState, login, logout, loadFromStorage, saveProfile };
}
exports.useAuth = useAuth;
//# sourceMappingURL=../../.sourcemap/mp-weixin/composables/useAuth.js.map
