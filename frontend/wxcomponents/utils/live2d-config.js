const isDev = true;

module.exports = {
  env: {
    isDev
  },

  model: {
    url: isDev
      ? "http://localhost:8080/live2d_model/person4_42_vts/person4_42.model3.json"
      : "https://your-domain.com/person4_42_vts/person4_42.model3.json",

    scaleBase: 0.40,
    xRatio: 0.50,
    yRatio: 1,
    anchorX: 0.5,
    anchorY: 1,
    interactive: true
  },

  stage: {
    width: 750
  }
};