"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_post = require("../../../api/post.js");
const composables_useAuth = require("../../../composables/useAuth.js");
const api_follow = require("../../../api/follow.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "detail",
  setup(__props) {
    const { authState } = composables_useAuth.useAuth();
    const following = common_vendor.ref(false);
    const categoryMap = {
      SCENIC: "景点攻略",
      FOOD: "美食推荐",
      TRANSPORT: "交通住宿",
      FREE_TRAVEL: "自由行",
      FAMILY: "亲子游"
    };
    function categoryLabel(v) {
      return categoryMap[v] || v;
    }
    function formatTime(s) {
      if (!s)
        return "";
      return s.slice(0, 16).replace("T", " ");
    }
    const postId = common_vendor.ref(0);
    const post = common_vendor.ref(null);
    const comments = common_vendor.ref([]);
    const commentPage = common_vendor.ref(0);
    const commentNoMore = common_vendor.ref(false);
    const showInput = common_vendor.ref(false);
    const commentText = common_vendor.ref("");
    const submitting = common_vendor.ref(false);
    const replyingTo = common_vendor.ref(null);
    common_vendor.onMounted(() => {
      var _a;
      const pages = getCurrentPages();
      const current = pages[pages.length - 1];
      postId.value = Number(((_a = current.options) == null ? void 0 : _a.id) || 0);
      if (postId.value) {
        loadDetail();
        loadComments(true);
      }
    });
    async function loadDetail() {
      var _a;
      const res = await api_post.fetchPostDetail(postId.value);
      if (res.code === 200) {
        post.value = res.data;
        following.value = ((_a = res.data.author) == null ? void 0 : _a.following) ?? false;
      }
    }
    async function onFollow() {
      var _a, _b;
      if (!((_b = (_a = post.value) == null ? void 0 : _a.author) == null ? void 0 : _b.id))
        return;
      const res = await api_follow.toggleFollow(post.value.author.id);
      if (res.code === 200) {
        following.value = res.data.following;
      }
    }
    async function loadComments(reset = false) {
      if (reset) {
        commentPage.value = 0;
        commentNoMore.value = false;
      }
      const res = await api_post.fetchComments(postId.value, { page: commentPage.value, size: 20 });
      if (res.code === 200) {
        const items = res.data.items;
        comments.value = reset ? items : [...comments.value, ...items];
        if (comments.value.length >= res.data.total)
          commentNoMore.value = true;
        else
          commentPage.value++;
      }
    }
    function loadMoreComments() {
      loadComments();
    }
    function focusComment() {
      if (!authState.isLoggedIn) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        return;
      }
      replyingTo.value = null;
      showInput.value = true;
    }
    function onReply(comment) {
      if (!authState.isLoggedIn) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        return;
      }
      replyingTo.value = comment;
      showInput.value = true;
    }
    async function submitComment() {
      var _a;
      if (!commentText.value.trim())
        return;
      submitting.value = true;
      try {
        const res = await api_post.createComment(postId.value, commentText.value, ((_a = replyingTo.value) == null ? void 0 : _a.id) ?? void 0);
        if (res.code === 200) {
          if (replyingTo.value) {
            const parent = comments.value.find((c) => c.id === replyingTo.value.id);
            if (parent) {
              if (!parent.replies)
                parent.replies = [];
              parent.replies.push(res.data);
            }
          } else {
            comments.value.unshift(res.data);
          }
          if (post.value)
            post.value.commentCount++;
          commentText.value = "";
          showInput.value = false;
          replyingTo.value = null;
          common_vendor.index.showToast({ title: "评论成功", icon: "success" });
        }
      } finally {
        submitting.value = false;
      }
    }
    async function onLike() {
      if (!authState.isLoggedIn) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        return;
      }
      if (!post.value)
        return;
      const res = await api_post.toggleLike(postId.value);
      if (res.code === 200 && post.value) {
        post.value.liked = res.data.liked;
        post.value.likeCount = res.data.likeCount;
      }
    }
    async function onFavorite() {
      if (!authState.isLoggedIn) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        return;
      }
      if (!post.value)
        return;
      const res = await api_post.toggleFavorite(postId.value);
      if (res.code === 200 && post.value) {
        post.value.favorited = res.data.favorited;
        common_vendor.index.showToast({ title: post.value.favorited ? "已收藏" : "已取消收藏", icon: "none" });
      }
    }
    return (_ctx, _cache) => {
      var _a, _b, _c, _d, _e, _f, _g, _h, _i, _j, _k;
      return common_vendor.e({
        a: post.value && post.value.imageUrls && post.value.imageUrls.length > 0
      }, post.value && post.value.imageUrls && post.value.imageUrls.length > 0 ? {
        b: common_vendor.f(post.value.imageUrls, (url, i, i0) => {
          return {
            a: url,
            b: common_vendor.o(() => {
            }, i),
            c: i
          };
        })
      } : {}, {
        c: !post.value
      }, !post.value ? {} : common_vendor.e({
        d: (_a = post.value.author) == null ? void 0 : _a.avatarUrl
      }, ((_b = post.value.author) == null ? void 0 : _b.avatarUrl) ? {
        e: post.value.author.avatarUrl
      } : {}, {
        f: common_vendor.t(((_c = post.value.author) == null ? void 0 : _c.nickname) || "旅行者"),
        g: common_vendor.t(formatTime(post.value.createdAt)),
        h: common_vendor.t(categoryLabel(post.value.category)),
        i: common_vendor.unref(authState).isLoggedIn && ((_e = (_d = post.value) == null ? void 0 : _d.author) == null ? void 0 : _e.id)
      }, common_vendor.unref(authState).isLoggedIn && ((_g = (_f = post.value) == null ? void 0 : _f.author) == null ? void 0 : _g.id) ? {
        j: common_vendor.t(following.value ? "已关注" : "+ 关注"),
        k: following.value ? 1 : "",
        l: common_vendor.o(onFollow)
      } : {}, {
        m: common_vendor.t(post.value.title),
        n: common_vendor.t(post.value.content),
        o: common_vendor.t(post.value.viewCount),
        p: common_vendor.t(post.value.likeCount),
        q: common_vendor.t(post.value.commentCount),
        r: comments.value.length === 0
      }, comments.value.length === 0 ? {} : {}, {
        s: common_vendor.f(comments.value, (c, k0, i0) => {
          var _a2, _b2, _c2;
          return common_vendor.e({
            a: (_a2 = c.author) == null ? void 0 : _a2.avatarUrl
          }, ((_b2 = c.author) == null ? void 0 : _b2.avatarUrl) ? {
            b: c.author.avatarUrl
          } : {}, {
            c: common_vendor.t(((_c2 = c.author) == null ? void 0 : _c2.nickname) || "旅行者"),
            d: common_vendor.t(c.content),
            e: common_vendor.t(formatTime(c.createdAt)),
            f: common_vendor.f(c.replies, (reply, k1, i1) => {
              var _a3;
              return {
                a: common_vendor.t(((_a3 = reply.author) == null ? void 0 : _a3.nickname) || "旅行者"),
                b: common_vendor.t(reply.content),
                c: reply.id
              };
            }),
            g: common_vendor.o(($event) => onReply(c), c.id),
            h: c.id
          });
        }),
        t: commentNoMore.value && comments.value.length > 0
      }, commentNoMore.value && comments.value.length > 0 ? {} : {}, {
        v: !commentNoMore.value && comments.value.length > 0
      }, !commentNoMore.value && comments.value.length > 0 ? {
        w: common_vendor.o(loadMoreComments)
      } : {}), {
        x: common_vendor.o(focusComment),
        y: common_vendor.t(((_h = post.value) == null ? void 0 : _h.liked) ? "❤️" : "🤍"),
        z: common_vendor.t(((_i = post.value) == null ? void 0 : _i.likeCount) || 0),
        A: common_vendor.o(onLike),
        B: common_vendor.t(((_j = post.value) == null ? void 0 : _j.favorited) ? "⭐" : "☆"),
        C: common_vendor.o(onFavorite),
        D: showInput.value
      }, showInput.value ? {
        E: common_vendor.o(($event) => {
          showInput.value = false;
          replyingTo.value = null;
        }),
        F: replyingTo.value ? `回复 ${((_k = replyingTo.value.author) == null ? void 0 : _k.nickname) || "旅行者"}...` : "说说你的想法（500字内）",
        G: commentText.value,
        H: common_vendor.o(($event) => commentText.value = $event.detail.value),
        I: common_vendor.t(commentText.value.length),
        J: submitting.value,
        K: common_vendor.o(submitComment),
        L: common_vendor.o(() => {
        })
      } : {});
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-69bc6572"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/guide/detail/detail.js.map
