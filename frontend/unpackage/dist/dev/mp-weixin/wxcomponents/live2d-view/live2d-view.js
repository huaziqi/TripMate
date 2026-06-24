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
        setTimeout(() => {
          this.initLive2D();
        }, 150);
      }
    },

    detached() {
      this.destroyLive2D();
      this._destroyed = true;
    }
  },

  methods: {
    async initLive2D() {
      if (this._initing || this.live2dPlayer || this._destroyed) {
        return this.live2dPlayer;
      }

      this._initing = true;

      try {
        const canvas = await this.getCanvasNodeWithRetry(6, 120);

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
        const query = this.createSelectorQuery().in(this);

        query
          .select("#live2dCanvas")
          .fields({ node: true, size: true })
          .exec((res) => {
            console.log("[live2d-view] canvas query result:", res);

            const item = res && res[0];
            const canvas = item && item.node;

            if (!canvas) {
              reject(new Error("Live2D canvas node not found"));
              return;
            }

            resolve(canvas);
          });
      });
    },

    async getCanvasNodeWithRetry(retryCount = 6, delay = 120) {
      let lastError = null;

      for (let i = 0; i < retryCount; i++) {
        try {
          const canvas = await this.getCanvasNode();
          if (canvas) {
            return canvas;
          }
        } catch (err) {
          lastError = err;
        }

        await new Promise((resolve) => setTimeout(resolve, delay));
      }

      throw lastError || new Error("Live2D canvas node not found");
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
    }
  }
});