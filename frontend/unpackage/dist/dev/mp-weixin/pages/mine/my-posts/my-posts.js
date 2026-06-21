"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_post = require("../../../api/post.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "my-posts",
  setup(__props) {
    const categoryMap = {
      SCENIC: "景点攻略",
      FOOD: "美食推荐",
      TRANSPORT: "交通住宿",
      FREE_TRAVEL: "自由行",
      FAMILY: "亲子游"
    };
    const posts = common_vendor.ref([]);
    const page = common_vendor.ref(0);
    const loading = common_vendor.ref(false);
    const noMore = common_vendor.ref(false);
    common_vendor.onMounted(() => load(true));
    async function load(reset = false) {
      if (loading.value || noMore.value)
        return;
      loading.value = true;
      try {
        const res = await api_post.fetchMyPosts({ page: page.value, size: 10 });
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
      }
    }
    function loadMore() {
      load();
    }
    function goDetail(id) {
      common_vendor.index.navigateTo({ url: `/pages/guide/detail/detail?id=${id}` });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: posts.value.length === 0 && !loading.value
      }, posts.value.length === 0 && !loading.value ? {} : {}, {
        b: common_vendor.f(posts.value, (item, k0, i0) => {
          return {
            a: common_vendor.t(item.title),
            b: common_vendor.t(categoryMap[item.category] || item.category),
            c: common_vendor.t(item.likeCount),
            d: common_vendor.t(item.commentCount),
            e: item.id,
            f: common_vendor.o(($event) => goDetail(item.id), item.id)
          };
        }),
        c: loading.value
      }, loading.value ? {} : {}, {
        d: noMore.value && posts.value.length > 0
      }, noMore.value && posts.value.length > 0 ? {} : {}, {
        e: common_vendor.o(loadMore, "72")
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-836382ee"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/mine/my-posts/my-posts.js.map
