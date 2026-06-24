const defaultConfig = require("../utils/live2d-config");
const { createLive2DPlayer } = require("../utils/live2d-manager");

Component({
  properties: {
    className: {
      type: String,
      value: ""
    },

    autoInit: {
      type: Boolean,
      value: true
    },

    stageWidth: {
      type: Number,
      value: 0
    },

    modelConfig: {
      type: Object,
      value: null
    }
  },

  data: {},

  lifetimes: {
    attached() {
      this.live2dPlayer = null;
      this._initing = false;
      this._destroyed = false;
    },

    ready() {
      if (this.properties.autoInit) {
        this.initLive2D();
      }
    },

    detached() {
      this.destroyLive2D();
      this._destroyed = true;
    }
  },

  methods: {
    async initLive2D() {
      if (this._initing || this.live2dPlayer) {
        return this.live2dPlayer;
      }

      this._initing = true;

      try {
        const canvas = await this.getCanvasNode();
        if (!canvas || this._destroyed) {
          this._initing = false;
          return null;
        }

        const mergedModelConfig = {
          ...defaultConfig.model,
          ...(this.properties.modelConfig || {})
        };

        this.live2dPlayer = await createLive2DPlayer(canvas, {
          stageWidth: this.properties.stageWidth || defaultConfig.stage.width,
          model: mergedModelConfig
        });

        this.triggerEvent("ready", {
          stageWidth: this.live2dPlayer.stageWidth,
          stageHeight: this.live2dPlayer.stageHeight
        });

        return this.live2dPlayer;
      } catch (err) {
        console.error("Live2D component init failed:", err);
        this.triggerEvent("error", { error: err });
        return null;
      } finally {
        this._initing = false;
      }
    },

    getCanvasNode() {
      return new Promise((resolve, reject) => {
        const query = this.createSelectorQuery();
        query.select("#live2dCanvas").node().exec((res) => {
          if (!res || !res[0] || !res[0].node) {
            reject(new Error("Live2D canvas node not found"));
            return;
          }
          resolve(res[0].node);
        });
      });
    },

    handleTouch(e) {
      if (this.live2dPlayer) {
        this.live2dPlayer.dispatchTouchEvent(e);
      }
    },

    destroyLive2D() {
      if (this.live2dPlayer) {
        this.live2dPlayer.destroy();
        this.live2dPlayer = null;
      }
    },

    setModelPosition(position) {
      if (this.live2dPlayer) {
        this.live2dPlayer.setModelPosition(position);
      }
    },

    setModelScale(scaleBase) {
      if (this.live2dPlayer) {
        this.live2dPlayer.setModelScale(scaleBase);
      }
    },

    updateModelParam(paramId, value, clamp = true) {
      if (!this.live2dPlayer) return false;
      return this.live2dPlayer.updateModelParam(paramId, value, clamp);
    },

    updateModelParams(params, clamp = true) {
      if (!this.live2dPlayer) return false;
      return this.live2dPlayer.updateModelParams(params, clamp);
    },

    addModelParam(paramId, delta, clamp = true) {
      if (!this.live2dPlayer) return false;
      return this.live2dPlayer.addModelParam(paramId, delta, clamp);
    },

    addModelParams(params, clamp = true) {
      if (!this.live2dPlayer) return false;
      return this.live2dPlayer.addModelParams(params, clamp);
    },

    getModelParam(paramId) {
      if (!this.live2dPlayer) return null;
      return this.live2dPlayer.getModelParam(paramId);
    },

    getModelParamInfo(paramId) {
      if (!this.live2dPlayer) return null;
      return this.live2dPlayer.getModelParamInfo(paramId);
    },

    listModelParameters() {
      if (!this.live2dPlayer) return [];
      return this.live2dPlayer.listModelParameters();
    },

    resetModelParam(paramId) {
      if (!this.live2dPlayer) return false;
      return this.live2dPlayer.resetModelParam(paramId);
    },

    resetModelParams(paramIds) {
      if (!this.live2dPlayer) return false;
      return this.live2dPlayer.resetModelParams(paramIds);
    },

    lookTo(x, y) {
      if (!this.live2dPlayer) return false;
      return this.live2dPlayer.lookTo(x, y);
    },

    speakOnce(level = 1, duration = 300) {
      if (!this.live2dPlayer) return;
      this.live2dPlayer.speakOnce(level, duration);
    },
	animateModelParam(paramId, toValue, options = {}) {
	  if (!this.live2dPlayer) return Promise.resolve(false);
	  return this.live2dPlayer.animateModelParam(paramId, toValue, options);
	},

	animateModelParams(params, options = {}) {
	  if (!this.live2dPlayer) return Promise.resolve([]);
	  return this.live2dPlayer.animateModelParams(params, options);
	},

	stopModelParamAnimation(paramId) {
	  if (!this.live2dPlayer) return false;
	  return this.live2dPlayer.stopModelParamAnimation(paramId);
	},

	stopAllModelAnimations() {
	  if (!this.live2dPlayer) return false;
	  return this.live2dPlayer.stopAllModelAnimations();
	},

	blinkOnce(duration = 160) {
	  if (!this.live2dPlayer) return Promise.resolve(false);
	  return this.live2dPlayer.blinkOnce(duration);
	},

	playSendAction() {
	  if (!this.live2dPlayer) return Promise.resolve(false);
	  return this.live2dPlayer.playSendAction();
	},

	startTalking(duration = 2000, options = {}) {
	  if (!this.live2dPlayer) return Promise.resolve(false);
	  return this.live2dPlayer.startTalking(duration, options);
	},

	stopTalking() {
	  if (!this.live2dPlayer) return false;
	  return this.live2dPlayer.stopTalking();
	},
    inspectModel() {
      if (!this.live2dPlayer) return;
      this.live2dPlayer.inspectModel();
    }
  }
});