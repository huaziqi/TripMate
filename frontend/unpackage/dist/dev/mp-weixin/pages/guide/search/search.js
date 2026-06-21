"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_post = require("../../../api/post.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "search",
  setup(__props) {
    const keyword = common_vendor.ref("");
    const results = common_vendor.ref([]);
    const page = common_vendor.ref(0);
    const loading = common_vendor.ref(false);
    const noMore = common_vendor.ref(false);
    const searched = common_vendor.ref(false);
    async function doSearch() {
      if (!keyword.value.trim())
        return;
      page.value = 0;
      noMore.value = false;
      results.value = [];
      searched.value = true;
      await load();
    }
    async function loadMore() {
      if (!noMore.value)
        await load();
    }
    async function load() {
      if (loading.value || noMore.value)
        return;
      loading.value = true;
      try {
        const res = await api_post.searchPosts(keyword.value.trim(), page.value, 10);
        if (res.code === 200) {
          const items = res.data.items;
          results.value = page.value === 0 ? items : [...results.value, ...items];
          if (results.value.length >= res.data.total)
            noMore.value = true;
          else
            page.value++;
        }
      } finally {
        loading.value = false;
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(doSearch, "06"),
        b: keyword.value,
        c: common_vendor.o(($event) => keyword.value = $event.detail.value, "43"),
        d: common_vendor.o(doSearch, "f3"),
        e: results.value.length === 0 && searched.value && !loading.value
      }, results.value.length === 0 && searched.value && !loading.value ? {} : {}, {
        f: common_vendor.f(results.value, (item, k0, i0) => {
          var _a, _b;
          return common_vendor.e({
            a: item.coverUrl
          }, item.coverUrl ? {
            b: item.coverUrl
          } : {}, {
            c: common_vendor.t(item.title),
            d: common_vendor.t((_a = item.content) == null ? void 0 : _a.slice(0, 60)),
            e: common_vendor.t(((_b = item.author) == null ? void 0 : _b.nickname) || "旅行者"),
            f: common_vendor.t(item.likeCount),
            g: common_vendor.t(item.commentCount),
            h: item.id,
            i: common_vendor.o(($event) => common_vendor.index.navigateTo({
              url: `/pages/guide/detail/detail?id=${item.id}`
            }), item.id)
          });
        }),
        g: !noMore.value && results.value.length > 0
      }, !noMore.value && results.value.length > 0 ? {
        h: common_vendor.o(loadMore, "8e")
      } : {}, {
        i: noMore.value && results.value.length > 0
      }, noMore.value && results.value.length > 0 ? {} : {}, {
        j: loading.value
      }, loading.value ? {} : {});
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-77341291"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/guide/search/search.js.map
