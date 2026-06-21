"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_post = require("../../../api/post.js");
const api_upload = require("../../../api/upload.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "create",
  setup(__props) {
    const categories = [
      { value: "SCENIC", label: "景点攻略" },
      { value: "FOOD", label: "美食推荐" },
      { value: "TRANSPORT", label: "交通住宿" },
      { value: "FREE_TRAVEL", label: "自由行" },
      { value: "FAMILY", label: "亲子游" }
    ];
    const form = common_vendor.ref({
      title: "",
      content: "",
      category: "SCENIC",
      imageUrls: []
    });
    const submitting = common_vendor.ref(false);
    function chooseImage() {
      common_vendor.index.chooseImage({
        count: 3 - form.value.imageUrls.length,
        sizeType: ["compressed"],
        sourceType: ["album", "camera"],
        success: async (res) => {
          common_vendor.index.showLoading({ title: "上传中..." });
          try {
            for (const path of res.tempFilePaths) {
              const url = await api_upload.uploadImage(path);
              form.value.imageUrls.push(url);
              if (form.value.imageUrls.length >= 3)
                break;
            }
          } catch {
          } finally {
            common_vendor.index.hideLoading();
          }
        }
      });
    }
    function removeImage(index) {
      form.value.imageUrls.splice(index, 1);
    }
    async function submit() {
      if (!form.value.title.trim()) {
        common_vendor.index.showToast({ title: "请输入标题", icon: "none" });
        return;
      }
      if (!form.value.content.trim()) {
        common_vendor.index.showToast({ title: "请输入内容", icon: "none" });
        return;
      }
      submitting.value = true;
      try {
        const res = await api_post.createPost({
          title: form.value.title.trim(),
          content: form.value.content.trim(),
          category: form.value.category,
          imageUrls: form.value.imageUrls
        });
        if (res.code === 200) {
          common_vendor.index.showToast({ title: "发布成功", icon: "success" });
          setTimeout(() => common_vendor.index.navigateBack(), 1200);
        } else {
          submitting.value = false;
        }
      } catch {
        submitting.value = false;
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: form.value.title,
        b: common_vendor.o(($event) => form.value.title = $event.detail.value, "a8"),
        c: common_vendor.t(form.value.title.length),
        d: common_vendor.f(categories, (c, k0, i0) => {
          return {
            a: common_vendor.t(c.label),
            b: c.value,
            c: form.value.category === c.value ? 1 : "",
            d: common_vendor.o(($event) => form.value.category = c.value, c.value)
          };
        }),
        e: form.value.content,
        f: common_vendor.o(($event) => form.value.content = $event.detail.value, "57"),
        g: common_vendor.t(form.value.content.length),
        h: common_vendor.f(form.value.imageUrls, (url, i, i0) => {
          return {
            a: url,
            b: common_vendor.o(($event) => removeImage(i), i),
            c: i
          };
        }),
        i: form.value.imageUrls.length < 3
      }, form.value.imageUrls.length < 3 ? {
        j: common_vendor.o(chooseImage, "84")
      } : {}, {
        k: common_vendor.t(submitting.value ? "发布中..." : "发布攻略"),
        l: submitting.value,
        m: common_vendor.o(submit, "55")
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-adf21bfd"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/guide/create/create.js.map
