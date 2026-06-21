"use strict";
const common_vendor = require("../../common/vendor.js");
const api_weather = require("../../api/weather.js");
const composables_useElder = require("../../composables/useElder.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "WeatherCard",
  setup(__props) {
    const { t } = common_vendor.useI18n();
    const { rpx } = composables_useElder.useElder();
    const locationStatus = common_vendor.ref("locating");
    const cityName = common_vendor.ref("");
    const city = common_vendor.computed(() => {
      if (locationStatus.value === "locating")
        return t("weather.card.locating");
      if (locationStatus.value === "failed")
        return t("weather.card.locateFailed");
      return cityName.value;
    });
    const weather = common_vendor.ref("--");
    const temperature = common_vendor.ref("--");
    const winddirection = common_vendor.ref("--");
    const windpower = common_vendor.ref("--");
    const humidity = common_vendor.ref("--");
    const reporttime = common_vendor.ref("--");
    const displayWeather = common_vendor.computed(
      () => weather.value === "--" ? "--" : t(`weather.condition.${weather.value}`, weather.value)
    );
    common_vendor.onMounted(() => {
      getLocation();
    });
    function getLocation() {
      common_vendor.index.getLocation({
        type: "gcj02",
        success: ({ longitude, latitude }) => loadWeather(longitude, latitude),
        fail: () => {
          locationStatus.value = "failed";
        }
      });
    }
    async function loadWeather(longitude, latitude) {
      try {
        const res = await api_weather.fetchWeather(longitude, latitude);
        applyData(res.data);
        locationStatus.value = "done";
      } catch {
        locationStatus.value = "failed";
      }
    }
    function applyData(data) {
      cityName.value = data.city;
      weather.value = data.weather;
      temperature.value = data.temperature;
      winddirection.value = data.winddirection;
      windpower.value = data.windpower;
      humidity.value = data.humidity;
      reporttime.value = data.reporttime;
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.t(city.value),
        b: common_vendor.unref(rpx)(34),
        c: common_vendor.t(reporttime.value),
        d: common_vendor.unref(rpx)(22),
        e: common_vendor.t(temperature.value),
        f: common_vendor.unref(rpx)(96),
        g: common_vendor.t(displayWeather.value),
        h: common_vendor.unref(rpx)(32),
        i: common_vendor.t(common_vendor.unref(t)("weather.card.windDirection")),
        j: common_vendor.unref(rpx)(26),
        k: common_vendor.t(winddirection.value),
        l: common_vendor.unref(rpx)(26),
        m: common_vendor.t(common_vendor.unref(t)("weather.card.windPower")),
        n: common_vendor.unref(rpx)(26),
        o: common_vendor.t(windpower.value),
        p: common_vendor.t(common_vendor.unref(t)("weather.card.windPowerUnit")),
        q: common_vendor.unref(rpx)(26),
        r: common_vendor.t(common_vendor.unref(t)("weather.card.humidity")),
        s: common_vendor.unref(rpx)(26),
        t: common_vendor.t(humidity.value),
        v: common_vendor.unref(rpx)(26)
      };
    };
  }
});
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1c9047b6"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/components/WeatherCard/WeatherCard.js.map
