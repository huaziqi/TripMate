"use strict";
const utils_useApi = require("../utils/useApi.js");
function wxLogin(code) {
  const { post } = utils_useApi.useApi();
  return post("/api/wx/login", { code }, { withToken: false });
}
function updateProfile(nickname, avatarUrl) {
  const { post } = utils_useApi.useApi();
  return post("/api/wx/profile", { nickname, avatarUrl });
}
exports.updateProfile = updateProfile;
exports.wxLogin = wxLogin;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/auth.js.map
