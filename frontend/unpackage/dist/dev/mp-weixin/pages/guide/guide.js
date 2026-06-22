"use strict";
const common_vendor = require("../../common/vendor.js");
const api_post = require("../../api/post.js");
const composables_useAuth = require("../../composables/useAuth.js");
if (!Math) {
  TabBar();
}
const TabBar = () => "../../components/TabBar/TabBar.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "guide",
  setup(__props) {
    const { authState } = composables_useAuth.useAuth();
    const categories = [
      { value: "ALL", label: "全部" },
      { value: "SCENIC", label: "景点攻略" },
      { value: "FOOD", label: "美食推荐" },
      { value: "TRANSPORT", label: "交通住宿" },
      { value: "FREE_TRAVEL", label: "自由行" },
      { value: "FAMILY", label: "亲子游" }
    ];
    const categoryMap = Object.fromEntries(
      categories.map((c) => [c.value, c.label])
    );
    function categoryLabel(v) {
      return categoryMap[v] || v;
    }
    const activeCategory = common_vendor.ref("ALL");
    const sort = common_vendor.ref("new");
    const posts = common_vendor.ref([]);
    const page = common_vendor.ref(0);
    const loading = common_vendor.ref(false);
    const noMore = common_vendor.ref(false);
    const refreshing = common_vendor.ref(false);
    common_vendor.onMounted(() => load(true));
    function onCategory(v) {
      if (activeCategory.value === v)
        return;
      activeCategory.value = v;
      load(true);
    }
    function onSort(v) {
      if (sort.value === v)
        return;
      sort.value = v;
      load(true);
    }
    async function load(reset = false) {
      if (loading.value)
        return;
      if (reset) {
        page.value = 0;
        noMore.value = false;
      }
      if (noMore.value)
        return;
      loading.value = true;
      try {
        const category = activeCategory.value === "ALL" ? void 0 : activeCategory.value;
        const res = await api_post.fetchPosts({ category, sort: sort.value, page: page.value, size: 10 });
        if (res.code === 200) {
          const items = res.data.items;
          posts.value = reset ? items : [...posts.value, ...items];
          if (posts.value.length >= res.data.total)
            noMore.value = true;
          else
            page.value++;
        }
      } finally {
        loading.value = false;
        refreshing.value = false;
      }
    }
    function loadMore() {
      load();
    }
    function onRefresh() {
      refreshing.value = true;
      load(true);
    }
    function goDetail(id) {
      common_vendor.index.navigateTo({ url: `/pages/guide/detail/detail?id=${id}` });
    }
    function goCreate() {
      if (!authState.isLoggedIn) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        return;
      }
      common_vendor.index.navigateTo({ url: "/pages/guide/create/create" });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(categories, (c, k0, i0) => {
          return {
            a: common_vendor.t(c.label),
            b: c.value,
            c: activeCategory.value === c.value ? 1 : "",
            d: common_vendor.o(($event) => onCategory(c.value), c.value)
          };
        }),
        b: common_vendor.o(($event) => _ctx.uni.navigateTo({
          url: "/pages/guide/search/search"
        })),
        c: sort.value === "new" ? 1 : "",
        d: common_vendor.o(($event) => onSort("new")),
        e: sort.value === "hot" ? 1 : "",
        f: common_vendor.o(($event) => onSort("hot")),
        g: posts.value.length === 0 && !loading.value
      }, posts.value.length === 0 && !loading.value ? {} : {}, {
        h: common_vendor.f(posts.value, (item, k0, i0) => {
          var _a, _b, _c;
          return common_vendor.e({
            a: item.coverUrl
          }, item.coverUrl ? {
            b: item.coverUrl
          } : {}, {
            c: common_vendor.t(categoryLabel(item.category)),
            d: common_vendor.t(item.title),
            e: (_a = item.author) == null ? void 0 : _a.avatarUrl
          }, ((_b = item.author) == null ? void 0 : _b.avatarUrl) ? {
            f: item.author.avatarUrl
          } : {}, {
            g: common_vendor.t(((_c = item.author) == null ? void 0 : _c.nickname) || "旅行者"),
            h: common_vendor.t(item.likeCount),
            i: common_vendor.t(item.commentCount),
            j: item.id,
            k: common_vendor.o(($event) => goDetail(item.id), item.id)
          });
        }),
        i: loading.value
      }, loading.value ? {} : {}, {
        j: noMore.value && posts.value.length > 0
      }, noMore.value && posts.value.length > 0 ? {} : {}, {
        k: common_vendor.o(loadMore),
        l: refreshing.value,
        m: common_vendor.o(onRefresh),
        n: common_vendor.o(goCreate),
        o: common_vendor.p({
          active: "guide"
        })
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-04b95c5c"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/guide/guide.js.map
