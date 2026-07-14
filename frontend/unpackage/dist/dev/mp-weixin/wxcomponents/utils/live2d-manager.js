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


function now() {
  return Date.now();
}

const EASING = {
  linear(t) {
    return t;
  },
  easeOutQuad(t) {
    return 1 - (1 - t) * (1 - t);
  },
  easeInOutQuad(t) {
    return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
  },
  easeOutCubic(t) {
    return 1 - Math.pow(1 - t, 3);
  }
};

function resolveEasing(easing) {
  if (typeof easing === "function") return easing;
  return EASING[easing] || EASING.linear;
}

function randomRange(min, max) {
  return min + Math.random() * (max - min);
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}


function getPIXI() {
  return PIXI;
}

function inspectLive2DModel(model) {
  if (!model) {
    console.log("[Live2D inspect] model is null");
    return;
  }

  try {
    console.log("[Live2D inspect] model keys:", Object.keys(model));
  } catch (e) {}

  try {
    console.log("[Live2D inspect] internalModel:", !!model.internalModel);
    if (model.internalModel) {
      console.log(
        "[Live2D inspect] internalModel keys:",
        Object.keys(model.internalModel)
      );
    }
  } catch (e) {}

  try {
    const coreModel =
      (model && model.internalModel && model.internalModel.coreModel) ||
      model.coreModel ||
      null;

    console.log("[Live2D inspect] coreModel:", !!coreModel);

    if (coreModel) {
      console.log("[Live2D inspect] coreModel keys:", Object.keys(coreModel));
      console.log("[Live2D inspect] coreModel methods:", {
        getParameterIndex: typeof coreModel.getParameterIndex,
        setParameterValueByIndex: typeof coreModel.setParameterValueByIndex,
        addParameterValueByIndex: typeof coreModel.addParameterValueByIndex,
        getParameterValueByIndex: typeof coreModel.getParameterValueByIndex,
        setParameterValueById: typeof coreModel.setParameterValueById,
        addParameterValueById: typeof coreModel.addParameterValueById,
        getParameterValueById: typeof coreModel.getParameterValueById
      });

      if (coreModel.parameters) {
        console.log("[Live2D inspect] coreModel.parameters:", {
          ids: coreModel.parameters.ids,
          values: coreModel.parameters.values,
          minimumValues: coreModel.parameters.minimumValues,
          maximumValues: coreModel.parameters.maximumValues,
          defaultValues: coreModel.parameters.defaultValues
        });
      }
    }
  } catch (e) {
    console.log("[Live2D inspect] failed:", e);
  }
}

function getLive2DCoreModel(model) {
  if (!model) return null;
  if (model.internalModel && model.internalModel.coreModel) {
    return model.internalModel.coreModel;
  }
  if (model.coreModel) {
    return model.coreModel;
  }
  return null;
}

function getParameterIndex(coreModel, paramId) {
  if (!coreModel || !paramId) return -1;

  try {
    if (typeof coreModel.getParameterIndex === "function") {
      return coreModel.getParameterIndex(paramId);
    }
  } catch (e) {}

  try {
    const ids = coreModel.parameters && coreModel.parameters.ids;
    if (ids && typeof ids.indexOf === "function") {
      return ids.indexOf(paramId);
    }
  } catch (e) {}

  return -1;
}

function clampParameterValue(coreModel, index, value) {
  try {
    const parameters = coreModel.parameters || {};
    const min =
      parameters.minimumValues && parameters.minimumValues[index] != null
        ? parameters.minimumValues[index]
        : null;
    const max =
      parameters.maximumValues && parameters.maximumValues[index] != null
        ? parameters.maximumValues[index]
        : null;

    let next = value;

    if (min != null && next < min) next = min;
    if (max != null && next > max) next = max;

    return next;
  } catch (e) {
    return value;
  }
}

function setModelParameterValue(model, paramId, value, clamp = true) {
  const coreModel = getLive2DCoreModel(model);
  if (!coreModel) return false;

  const index = getParameterIndex(coreModel, paramId);
  const nextValue =
    clamp && index >= 0 ? clampParameterValue(coreModel, index, value) : value;

  try {
    if (typeof coreModel.setParameterValueById === "function") {
      coreModel.setParameterValueById(paramId, nextValue);
      return true;
    }
  } catch (e) {}

  if (index < 0) return false;

  try {
    if (typeof coreModel.setParameterValueByIndex === "function") {
      coreModel.setParameterValueByIndex(index, nextValue);
      return true;
    }
  } catch (e) {}

  try {
    if (coreModel.parameters && coreModel.parameters.values) {
      coreModel.parameters.values[index] = nextValue;
      return true;
    }
  } catch (e) {}

  return false;
}

function addModelParameterValue(model, paramId, delta, clamp = true) {
  const coreModel = getLive2DCoreModel(model);
  if (!coreModel) return false;

  const index = getParameterIndex(coreModel, paramId);
  if (index < 0) return false;

  const current = getModelParameterValue(model, paramId);
  if (current == null) return false;

  const nextValue = clamp
    ? clampParameterValue(coreModel, index, current + delta)
    : current + delta;

  return setModelParameterValue(model, paramId, nextValue, false);
}

function getModelParameterValue(model, paramId) {
  const coreModel = getLive2DCoreModel(model);
  if (!coreModel) return null;

  try {
    if (typeof coreModel.getParameterValueById === "function") {
      return coreModel.getParameterValueById(paramId);
    }
  } catch (e) {}

  const index = getParameterIndex(coreModel, paramId);
  if (index < 0) return null;

  try {
    if (typeof coreModel.getParameterValueByIndex === "function") {
      return coreModel.getParameterValueByIndex(index);
    }
  } catch (e) {}

  try {
    if (coreModel.parameters && coreModel.parameters.values) {
      return coreModel.parameters.values[index];
    }
  } catch (e) {}

  return null;
}

function getModelParameterInfo(model, paramId) {
  const coreModel = getLive2DCoreModel(model);
  if (!coreModel) return null;

  const index = getParameterIndex(coreModel, paramId);
  if (index < 0) return null;

  try {
    const parameters = coreModel.parameters || {};
    return {
      index,
      id: parameters.ids ? parameters.ids[index] : paramId,
      value: parameters.values ? parameters.values[index] : null,
      defaultValue: parameters.defaultValues
        ? parameters.defaultValues[index]
        : null,
      minValue: parameters.minimumValues
        ? parameters.minimumValues[index]
        : null,
      maxValue: parameters.maximumValues
        ? parameters.maximumValues[index]
        : null
    };
  } catch (e) {
    return { index, id: paramId };
  }
}

function listModelParameters(model) {
  const coreModel = getLive2DCoreModel(model);
  if (!coreModel) return [];

  try {
    const parameters = coreModel.parameters || {};
    const ids = parameters.ids || [];
    const values = parameters.values || [];
    const defaultValues = parameters.defaultValues || [];
    const minimumValues = parameters.minimumValues || [];
    const maximumValues = parameters.maximumValues || [];

    const result = [];

    for (let i = 0; i < ids.length; i++) {
      result.push({
        index: i,
        id: ids[i],
        value: values[i],
        defaultValue: defaultValues[i],
        minValue: minimumValues[i],
        maxValue: maximumValues[i]
      });
    }

    return result;
  } catch (e) {
    return [];
  }
}

function setModelParameters(model, params = {}, clamp = true) {
  if (!params || typeof params !== "object") return false;

  let success = false;

  Object.keys(params).forEach((paramId) => {
    const ok = setModelParameterValue(model, paramId, params[paramId], clamp);
    if (ok) success = true;
  });

  return success;
}

function addModelParameters(model, params = {}, clamp = true) {
  if (!params || typeof params !== "object") return false;

  let success = false;

  Object.keys(params).forEach((paramId) => {
    const ok = addModelParameterValue(model, paramId, params[paramId], clamp);
    if (ok) success = true;
  });

  return success;
}

function resetModelParameter(model, paramId) {
  const info = getModelParameterInfo(model, paramId);
  if (!info) return false;
  if (info.defaultValue == null) return false;
  return setModelParameterValue(model, paramId, info.defaultValue, false);
}

function resetModelParameters(model, paramIds) {
  const allParams = listModelParameters(model);
  if (!allParams.length) return false;

  let targetIds = paramIds;

  if (!Array.isArray(targetIds) || !targetIds.length) {
    targetIds = allParams.map((item) => item.id);
  }

  let success = false;

  targetIds.forEach((paramId) => {
    const ok = resetModelParameter(model, paramId);
    if (ok) success = true;
  });

  return success;
}

async function createLive2DPlayer(canvas, options = {}) {
  const systemInfo = wx.getSystemInfoSync();
  const screenWidth = systemInfo.screenWidth;
  const screenHeight = systemInfo.screenHeight;

  const stageWidth = options.stageWidth || config.stage.width || 750;
  const stageHeight = parseInt((stageWidth * screenHeight) / screenWidth);

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

  inspectLive2DModel(model);

  const scale =
    Math.min(stageWidth / 750, stageHeight / 1334) *
    (modelConfig.scaleBase || 0.3);

  model.scale.set(scale);
  model.anchor.set(modelConfig.anchorX ?? 0.5, modelConfig.anchorY ?? 1);
  model.x = stageWidth * (modelConfig.xRatio ?? 0.5);
  model.y = stageHeight * (modelConfig.yRatio ?? 0.9);
  model.eventMode = modelConfig.interactive ? "static" : "none";

  stage.addChild(model);

  let destroyed = false;
  let rafId = null;
  let speechTimer = null;
  let talkingLoopTimer = null;
  let blinkTimer = null;
  let stopTalkingHandler = null;
  const paramAnimations = new Map();

	function clearSpeechTimer() {
	  if (speechTimer) {
		clearTimeout(speechTimer);
		speechTimer = null;
	  }
	}

	function clearTalkingLoopTimer() {
	  if (talkingLoopTimer) {
		clearTimeout(talkingLoopTimer);
		talkingLoopTimer = null;
	  }
	}
	
  function animate() {
    if (destroyed) return;
    rafId = canvas.requestAnimationFrame(animate);
    renderer.render(stage);
  }
  
  function stopModelParamAnimation(paramId) {
    const task = paramAnimations.get(paramId);
    if (task) {
      task.cancelled = true;
      paramAnimations.delete(paramId);
      return true;
    }
    return false;
  }
  
  function stopAllModelAnimations() {
    paramAnimations.forEach((task) => {
      task.cancelled = true;
    });
    paramAnimations.clear();
  }
  
  function animateModelParam(paramId, toValue, options = {}) {
    const fromValueRaw = getModelParameterValue(model, paramId);
    const fromValue = fromValueRaw == null ? 0 : fromValueRaw;
    const duration = Math.max(0, options.duration || 300);
    const delay = Math.max(0, options.delay || 0);
    const clamp = options.clamp !== false;
    const easing = resolveEasing(options.easing || "easeOutQuad");
  
    stopModelParamAnimation(paramId);
  
    return new Promise((resolve) => {
      const task = {
        cancelled: false
      };
  
      paramAnimations.set(paramId, task);
  
      const startAt = now() + delay;
  
      function step() {
        if (destroyed || task.cancelled) {
          if (paramAnimations.get(paramId) === task) {
            paramAnimations.delete(paramId);
          }
          resolve(false);
          return;
        }
  
        const currentTime = now();
  
        if (currentTime < startAt) {
          canvas.requestAnimationFrame(step);
          return;
        }
  
        if (duration === 0) {
          setModelParameterValue(model, paramId, toValue, clamp);
          if (paramAnimations.get(paramId) === task) {
            paramAnimations.delete(paramId);
          }
          resolve(true);
          return;
        }
  
        const progress = Math.min(1, (currentTime - startAt) / duration);
        const eased = easing(progress);
        const value = fromValue + (toValue - fromValue) * eased;
  
        setModelParameterValue(model, paramId, value, clamp);
  
        if (progress >= 1) {
          if (paramAnimations.get(paramId) === task) {
            paramAnimations.delete(paramId);
          }
          resolve(true);
          return;
        }
  
        canvas.requestAnimationFrame(step);
      }
  
      canvas.requestAnimationFrame(step);
    });
  }
  
  function animateModelParams(params = {}, options = {}) {
    const tasks = Object.keys(params).map((paramId) => {
      return animateModelParam(paramId, params[paramId], options);
    });
  
    return Promise.all(tasks);
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

    inspectModel() {
      inspectLive2DModel(model);
    },

    listModelParameters() {
      return listModelParameters(model);
    },

    updateModelParam(paramId, value, clamp = true) {
      return setModelParameterValue(model, paramId, value, clamp);
    },

    updateModelParams(params, clamp = true) {
      return setModelParameters(model, params, clamp);
    },

    addModelParam(paramId, delta, clamp = true) {
      return addModelParameterValue(model, paramId, delta, clamp);
    },

    addModelParams(params, clamp = true) {
      return addModelParameters(model, params, clamp);
    },

    getModelParam(paramId) {
      return getModelParameterValue(model, paramId);
    },

    getModelParamInfo(paramId) {
      return getModelParameterInfo(model, paramId);
    },

    resetModelParam(paramId) {
      return resetModelParameter(model, paramId);
    },

    resetModelParams(paramIds) {
      return resetModelParameters(model, paramIds);
    },

    lookTo(x = 0, y = 0) {
      const params = {
        ParamAngleX: x * 30,
        ParamAngleY: y * 30,
        ParamEyeBallX: x,
        ParamEyeBallY: y
      };
      return setModelParameters(model, params, true);
    },

    speakOnce(level = 1, duration = 300) {
      const mouthValue = Math.max(0, Math.min(1, level));

      this.updateModelParam("ParamMouthOpenY", mouthValue, true);

      if (speechTimer) {
        clearTimeout(speechTimer);
        speechTimer = null;
      }

      speechTimer = setTimeout(() => {
        this.resetModelParam("ParamMouthOpenY");
        speechTimer = null;
      }, duration);
    },

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
	
	
	animateModelParam(paramId, toValue, options = {}) {
	  return animateModelParam(paramId, toValue, options);
	},

	animateModelParams(params, options = {}) {
	  return animateModelParams(params, options);
	},

	stopModelParamAnimation(paramId) {
	  return stopModelParamAnimation(paramId);
	},

	stopAllModelAnimations() {
	  return stopAllModelAnimations();
	},
	
	
	async blinkOnce(duration = 160) {
	  const leftInfo = this.getModelParamInfo("ParamEyeLOpen");
	  const rightInfo = this.getModelParamInfo("ParamEyeROpen");

	  if (!leftInfo && !rightInfo) return false;

	  const closeParams = {};
	  const openParams = {};

	  if (leftInfo) {
		closeParams.ParamEyeLOpen = leftInfo.minValue != null ? leftInfo.minValue : 0;
		openParams.ParamEyeLOpen = leftInfo.defaultValue != null ? leftInfo.defaultValue : 1;
	  }

	  if (rightInfo) {
		closeParams.ParamEyeROpen = rightInfo.minValue != null ? rightInfo.minValue : 0;
		openParams.ParamEyeROpen = rightInfo.defaultValue != null ? rightInfo.defaultValue : 1;
	  }

	  await this.animateModelParams(closeParams, {
		duration: Math.max(40, duration * 0.35),
		easing: "easeOutQuad"
	  });

	  await this.animateModelParams(openParams, {
		duration: Math.max(60, duration * 0.65),
		easing: "easeOutQuad"
	  });

	  return true;
	},
		
	async playSendAction() {
	  const headX = randomRange(-12, 12);
	  const headY = randomRange(-4, 4);
	  const eyeX = Math.max(-1, Math.min(1, headX / 12));
	  const eyeY = Math.max(-1, Math.min(1, headY / 10));
	  const mouthOpen = randomRange(0.6, 1);
	  const mouthFormInfo = this.getModelParamInfo("ParamMouthForm");

	  const enterParams = {
		ParamAngleX: headX,
		ParamAngleY: headY,
		ParamEyeBallX: eyeX,
		ParamEyeBallY: eyeY,
		ParamMouthOpenY: mouthOpen
	  };

	  if (mouthFormInfo) {
	    const base =
	      mouthFormInfo.defaultValue != null ? mouthFormInfo.defaultValue : 0;
	    enterParams.ParamMouthForm = base + randomRange(0.02, 0.08);
	  }

	  await this.animateModelParams(enterParams, {
		duration: 180,
		easing: "easeOutQuad"
	  });

	  await wait(220);

	  const resetParams = {
		ParamAngleX: 0,
		ParamAngleY: 0,
		ParamEyeBallX: 0,
		ParamEyeBallY: 0,
		ParamMouthOpenY: 0
	  };

	  if (mouthFormInfo) {
		const defaultMouthForm =
		  mouthFormInfo.defaultValue != null ? mouthFormInfo.defaultValue : 0;
		resetParams.ParamMouthForm = defaultMouthForm;
	  }

	  await this.animateModelParams(resetParams, {
		duration: 260,
		easing: "easeInOutQuad"
	  });

	  return true;
	},
		
	
	
	async startTalking(duration = 2000, options = {}) {
	  await this.stopTalking();
	
	  if (destroyed) return false;
	
	  const startedAt = now();
	  const totalDuration = Math.max(100, duration);
	
	  const enableBlink = options.enableBlink !== false;
	  const enableHeadMotion = options.enableHeadMotion !== false;
	
	  const mouthParamId = options.mouthParamId || "ParamMouthOpenY";
	  const mouthFormParamId = options.mouthFormParamId || "ParamMouthForm";
	
	  // 推荐：
	  // fixed  = 固定嘴型
	  // subtle = 小范围轻微波动
	  // random = 兼容旧逻辑，不建议默认使用
	  const mouthFormMode = options.mouthFormMode || "fixed";
	  const mouthFormRange =
	    typeof options.mouthFormRange === "number" ? options.mouthFormRange : 0.05;
	
	  const mouthFormInfo = this.getModelParamInfo(mouthFormParamId);
	
	  let stopped = false;
	
	  const stopTalkingInner = async () => {
	    if (stopped) return false;
	    stopped = true;
	
	    clearSpeechTimer();
	    clearTalkingLoopTimer();
	
	    const resetParams = {
	      [mouthParamId]: 0,
	      ParamAngleX: 0,
	      ParamAngleY: 0,
	      ParamEyeBallX: 0,
	      ParamEyeBallY: 0
	    };
	
	    if (mouthFormInfo) {
	      resetParams[mouthFormParamId] =
	        mouthFormInfo.defaultValue != null ? mouthFormInfo.defaultValue : 0;
	    }
	
	    await this.animateModelParams(resetParams, {
	      duration: 180,
	      easing: "easeOutQuad"
	    });
	
	    if (stopTalkingHandler === stopTalkingInner) {
	      stopTalkingHandler = null;
	    }
	
	    return true;
	  };
	
	  stopTalkingHandler = stopTalkingInner;
	
	  const loop = async () => {
	    if (destroyed || stopped) return;
	
	    const elapsed = now() - startedAt;
	    if (elapsed >= totalDuration) {
	      await stopTalkingInner();
	      return;
	    }
	
	    const mouthOpen = randomRange(0.25, 0.85);
	    const nextParams = {
	      [mouthParamId]: mouthOpen
	    };
	
	    if (mouthFormInfo) {
	      const base =
	        mouthFormInfo.defaultValue != null ? mouthFormInfo.defaultValue : 0;
	
	      if (mouthFormMode === "fixed") {
	        nextParams[mouthFormParamId] = base;
	      } else if (mouthFormMode === "subtle") {
	        nextParams[mouthFormParamId] =
	          base + randomRange(-mouthFormRange, mouthFormRange);
	      } else if (mouthFormMode === "random") {
	        nextParams[mouthFormParamId] = randomRange(-0.2, 0.8);
	      } else {
	        nextParams[mouthFormParamId] = base;
	      }
	    }
	
	    if (enableHeadMotion && Math.random() < 0.45) {
	      const headX = randomRange(-8, 8);
	      const headY = randomRange(-3, 3);
	
	      nextParams.ParamAngleX = headX;
	      nextParams.ParamAngleY = headY;
	      nextParams.ParamEyeBallX = Math.max(-1, Math.min(1, headX / 10));
	      nextParams.ParamEyeBallY = Math.max(-1, Math.min(1, headY / 8));
	    }
	
	    await this.animateModelParams(nextParams, {
	      duration: randomRange(70, 140),
	      easing: "easeOutQuad"
	    });
	
	    if (destroyed || stopped) return;
	
	    await this.animateModelParam(mouthParamId, randomRange(0.05, 0.2), {
	      duration: randomRange(60, 120),
	      easing: "easeInOutQuad"
	    });
	
	    if (destroyed || stopped) return;
	
	    if (enableBlink && Math.random() < 0.12) {
	      this.blinkOnce(150);
	    }
	
	    clearTalkingLoopTimer();
	    talkingLoopTimer = setTimeout(loop, randomRange(50, 110));
	  };
	
	  clearSpeechTimer();
	  speechTimer = setTimeout(() => {
	    if (!stopped) {
	      stopTalkingInner();
	    }
	  }, totalDuration);
	
	  loop();
	
	  return true;
	},

	async stopTalking() {
	  clearSpeechTimer();
	  clearTalkingLoopTimer();
	
	  if (typeof stopTalkingHandler === "function") {
	    const fn = stopTalkingHandler;
	    stopTalkingHandler = null;
	    await fn();
	    return true;
	  }
	
	  return false;
	},
	
	
	
	async destroy() {
	  if (destroyed) return;
	
	  destroyed = true;
	
	  clearSpeechTimer();
	  clearTalkingLoopTimer();
	
	  if (blinkTimer) {
	    clearTimeout(blinkTimer);
	    blinkTimer = null;
	  }
	
	  if (typeof stopTalkingHandler === "function") {
	    const fn = stopTalkingHandler;
	    stopTalkingHandler = null;
	    try {
	      await fn();
	    } catch (e) {}
	  }
	
	  stopAllModelAnimations();
	
	  try {
	    if (rafId != null) {
	      canvas.cancelAnimationFrame(rafId);
	      rafId = null;
	    }
	  } catch (e) {}
	
	  try {
	    if (stage && model && typeof stage.removeChild === "function") {
	      stage.removeChild(model);
	    }
	  } catch (e) {}
	
	  try {
	    if (model && typeof model.destroy === "function") {
	      model.destroy({
	        children: true,
	        texture: true,
	        baseTexture: true
	      });
	    }
	  } catch (e) {}
	
	  try {
	    if (renderer && typeof renderer.destroy === "function") {
	      renderer.destroy(true);
	    }
	  } catch (e) {}
	},
 
 
  };
}

module.exports = {
  getPIXI,
  createLive2DPlayer
};