const { createPIXI } = require("../libs/pixi.miniprogram");

const unsafeEval = require("../libs/unsafeEval");
const live2d = require("../libs/live2d.min");
const Live2DCubismCore = require("../libs/live2dcubismcore.min");
const installCubism4 = require("../libs/cubism4");
const installPixiLive2d = require("../libs/pixi-live2d-display");

const config = require("./live2d-config");

let PIXI = {
  dispatchEvent: function () {}
};

function getPIXI() {
  return PIXI;
}

async function createLive2DPlayer(canvas, options = {}) {
  const systemInfo = wx.getSystemInfoSync();
  const screenWidth = systemInfo.screenWidth;
  const screenHeight = systemInfo.screenHeight;

  const stageWidth = options.stageWidth || config.stage.width || 750;
  const stageHeight = parseInt(stageWidth * screenHeight / screenWidth);

  canvas.width = screenWidth;
  canvas.height = screenHeight;

  PIXI = createPIXI(canvas, stageWidth);

  unsafeEval(PIXI);
  installCubism4(PIXI, Live2DCubismCore);
  installPixiLive2d(PIXI, live2d, Live2DCubismCore);

  const renderer = PIXI.autoDetectRenderer({
    width: stageWidth,
    height: stageHeight,
    backgroundAlpha: 0,
    premultipliedAlpha: true,
    preserveDrawingBuffer: true,
    view: canvas
  });

  const stage = new PIXI.Container();

  const modelConfig = {
    ...config.model,
    ...options.model
  };

  const model = await PIXI.live2d.Live2DModel.from(modelConfig.url);

  const scale =
    Math.min(stageWidth / 750, stageHeight / 1334) * (modelConfig.scaleBase || 0.3);

  model.scale.set(scale);
  model.anchor.set(
    modelConfig.anchorX ?? 0.5,
    modelConfig.anchorY ?? 1
  );
  model.x = stageWidth * (modelConfig.xRatio ?? 0.5);
  model.y = stageHeight * (modelConfig.yRatio ?? 0.9);
  model.eventMode = modelConfig.interactive ? "static" : "none";

  stage.addChild(model);

  let destroyed = false;
  let rafId = null;

  function animate() {
    if (destroyed) return;
    rafId = canvas.requestAnimationFrame(animate);
    renderer.render(stage);
  }

  animate();

  return {
    PIXI,
    canvas,
    renderer,
    stage,
    model,
    stageWidth,
    stageHeight,

    setModelPosition({ xRatio, yRatio }) {
      if (typeof xRatio === "number") {
        model.x = stageWidth * xRatio;
      }
      if (typeof yRatio === "number") {
        model.y = stageHeight * yRatio;
      }
    },

    setModelScale(scaleBase) {
      const nextScale =
        Math.min(stageWidth / 750, stageHeight / 1334) * scaleBase;
      model.scale.set(nextScale);
    },

    dispatchTouchEvent(e) {
      if (PIXI && typeof PIXI.dispatchEvent === "function") {
        PIXI.dispatchEvent(e);
      }
    },

    destroy() {
      destroyed = true;

      if (rafId != null && canvas.cancelAnimationFrame) {
        canvas.cancelAnimationFrame(rafId);
      }

      try {
        if (stage && model) {
          stage.removeChild(model);
        }
      } catch (e) {}

      try {
        if (model && model.destroy) {
          model.destroy();
        }
      } catch (e) {}

      try {
        if (renderer && renderer.destroy) {
          renderer.destroy(true);
        }
      } catch (e) {}
    }
  };
}

module.exports = {
  getPIXI,
  createLive2DPlayer
};