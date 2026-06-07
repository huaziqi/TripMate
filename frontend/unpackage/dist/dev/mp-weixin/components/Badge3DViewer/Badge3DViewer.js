"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "Badge3DViewer",
  props: {
    badge: {}
  },
  setup(__props) {
    const props = __props;
    const canvasId = `badge3d-${props.badge.id}`;
    const RARITY_COLOR = {
      COMMON: 10395294,
      RARE: 2201331,
      EPIC: 10233776,
      LEGENDARY: 16766720
    };
    let THREE;
    let renderer;
    let scene;
    let camera;
    let coin;
    let rafId;
    let autoSpin = true;
    let touchPrev = { x: 0, y: 0 };
    common_vendor.onMounted(() => {
      const query = common_vendor.index.createSelectorQuery();
      query.select(`#${canvasId}`).node().exec((res) => {
        var _a;
        const canvas = (_a = res[0]) == null ? void 0 : _a.node;
        if (!canvas)
          return;
        initScene(canvas);
      });
    });
    common_vendor.onUnmounted(() => {
      if (rafId)
        cancelAnimationFrame(rafId);
      renderer == null ? void 0 : renderer.dispose();
    });
    function initScene(canvas) {
      THREE = common_vendor.dist.createScopedThreejs(canvas);
      const W = 600;
      const H = 600;
      canvas.width = W;
      canvas.height = H;
      scene = new THREE.Scene();
      scene.background = new THREE.Color(1710638);
      camera = new THREE.PerspectiveCamera(40, W / H, 0.1, 100);
      camera.position.set(0, 0, 5);
      renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
      renderer.setSize(W, H);
      renderer.setPixelRatio(1);
      scene.add(new THREE.AmbientLight(16777215, 0.6));
      const keyLight = new THREE.DirectionalLight(16777215, 1.2);
      keyLight.position.set(3, 4, 5);
      scene.add(keyLight);
      const rimLight = new THREE.DirectionalLight(16777215, 0.5);
      rimLight.position.set(-4, -2, -3);
      scene.add(rimLight);
      const geo = new THREE.CylinderGeometry(1.2, 1.2, 0.18, 64);
      const color = RARITY_COLOR[props.badge.rarity] ?? 10395294;
      const offscreen = common_vendor.wx$1.createOffscreenCanvas({ type: "2d", width: 256, height: 256 });
      const ctx = offscreen.getContext("2d");
      const hex = "#" + color.toString(16).padStart(6, "0");
      ctx.fillStyle = hex;
      ctx.beginPath();
      ctx.arc(128, 128, 128, 0, Math.PI * 2);
      ctx.fill();
      ctx.font = "108px serif";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(props.badge.icon, 128, 128);
      const faceTexture = new THREE.CanvasTexture(offscreen);
      const sideMat = new THREE.MeshStandardMaterial({ color, metalness: 0.9, roughness: 0.15 });
      const frontMat = new THREE.MeshStandardMaterial({ map: faceTexture, metalness: 0.3, roughness: 0.4 });
      const backMat = new THREE.MeshStandardMaterial({ color: 2236962, metalness: 0.8, roughness: 0.2 });
      coin = new THREE.Mesh(geo, [sideMat, frontMat, backMat]);
      coin.rotation.x = Math.PI / 2;
      scene.add(coin);
      const ringGeo = new THREE.TorusGeometry(1.5, 0.04, 8, 64);
      const ringMat = new THREE.MeshStandardMaterial({ color, metalness: 1, roughness: 0 });
      const ring = new THREE.Mesh(ringGeo, ringMat);
      ring.rotation.x = Math.PI / 2;
      scene.add(ring);
      animate();
    }
    function animate() {
      rafId = requestAnimationFrame(animate);
      if (autoSpin && coin)
        coin.rotation.z += 8e-3;
      renderer == null ? void 0 : renderer.render(scene, camera);
    }
    function onTouchStart(e) {
      autoSpin = false;
      const t = e.touches[0];
      touchPrev = { x: t.clientX, y: t.clientY };
    }
    function onTouchMove(e) {
      if (!coin)
        return;
      const t = e.touches[0];
      const dx = t.clientX - touchPrev.x;
      const dy = t.clientY - touchPrev.y;
      coin.rotation.z += dx * 0.012;
      coin.rotation.x += dy * 0.012;
      touchPrev = { x: t.clientX, y: t.clientY };
    }
    function onTouchEnd() {
      setTimeout(() => {
        autoSpin = true;
      }, 2e3);
    }
    return (_ctx, _cache) => {
      return {
        a: canvasId,
        b: common_vendor.o(onTouchStart, "bd"),
        c: common_vendor.o(onTouchMove, "36"),
        d: common_vendor.o(onTouchEnd, "5f")
      };
    };
  }
});
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e649efab"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/components/Badge3DViewer/Badge3DViewer.js.map
