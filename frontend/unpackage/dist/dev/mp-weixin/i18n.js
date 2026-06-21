"use strict";
const common_vendor = require("./common/vendor.js");
const translate$1 = {
  title: "翻译",
  tabs: {
    text: "文本翻译",
    phrases: "常用短语",
    history: "翻译历史"
  },
  placeholder: "请输入要翻译的文字（最多 500 字）",
  charCount: "{count}/500",
  btn: "翻译",
  translating: "翻译中...",
  copy: "复制",
  copied: "已复制",
  swap: "互换",
  clear: "清空",
  result: "翻译结果",
  noResult: "翻译结果将显示在这里",
  history: {
    empty: "暂无翻译历史",
    clearAll: "清空历史",
    confirmClear: "确认清空所有翻译历史？"
  },
  phrases: {
    greeting: "问候",
    dining: "餐饮",
    transport: "交通",
    shopping: "购物",
    hotel: "住宿",
    emergency: "紧急",
    scenic: "景点",
    time: "时间",
    tapToTranslate: "点击短语快速翻译"
  },
  lang: {
    zh: "中文",
    en: "英语",
    ja: "日语",
    ko: "韩语",
    fr: "法语",
    es: "西班牙语",
    de: "德语",
    ru: "俄语",
    ar: "阿拉伯语",
    th: "泰语"
  },
  error: {
    empty: "请输入要翻译的文字",
    failed: "翻译失败，请检查网络后重试",
    sameLang: "源语言和目标语言不能相同"
  }
};
const tabbar$1 = {
  home: "首页",
  guide: "攻略",
  elder: "老年版",
  language: "翻译",
  badges: "勋章",
  mine: "我的",
  match: "出发"
};
const weather$1 = {
  card: {
    locating: "定位中...",
    locateFailed: "定位失败",
    windDirection: "风向",
    windPower: "风力",
    windPowerUnit: "级",
    humidity: "湿度"
  },
  condition: {
    "晴": "晴",
    "少云": "少云",
    "晴间多云": "晴间多云",
    "多云": "多云",
    "阴": "阴",
    "有风": "有风",
    "平静": "平静",
    "微风": "微风",
    "和风": "和风",
    "清风": "清风",
    "强风/劲风": "强风/劲风",
    "疾风": "疾风",
    "大风": "大风",
    "烈风": "烈风",
    "风暴": "风暴",
    "狂爆风": "狂爆风",
    "飓风": "飓风",
    "热带风暴": "热带风暴",
    "霾": "霾",
    "中度霾": "中度霾",
    "重度霾": "重度霾",
    "严重霾": "严重霾",
    "阵雨": "阵雨",
    "雷阵雨": "雷阵雨",
    "雷阵雨并伴有冰雹": "雷阵雨并伴有冰雹",
    "小雨": "小雨",
    "中雨": "中雨",
    "大雨": "大雨",
    "暴雨": "暴雨",
    "大暴雨": "大暴雨",
    "特大暴雨": "特大暴雨",
    "冻雨": "冻雨",
    "小雨-中雨": "小雨-中雨",
    "中雨-大雨": "中雨-大雨",
    "大雨-暴雨": "大雨-暴雨",
    "暴雨-大暴雨": "暴雨-大暴雨",
    "大暴雨-特大暴雨": "大暴雨-特大暴雨",
    "雨": "雨",
    "小雪": "小雪",
    "中雪": "中雪",
    "大雪": "大雪",
    "暴雪": "暴雪",
    "小雪-中雪": "小雪-中雪",
    "中雪-大雪": "中雪-大雪",
    "大雪-暴雪": "大雪-暴雪",
    "雪": "雪",
    "雨夹雪": "雨夹雪",
    "雨雪天气": "雨雪天气",
    "阵雪": "阵雪",
    "浮尘": "浮尘",
    "扬沙": "扬沙",
    "沙尘暴": "沙尘暴",
    "强沙尘暴": "强沙尘暴",
    "龙卷风": "龙卷风",
    "雾": "雾",
    "浓雾": "浓雾",
    "强浓雾": "强浓雾",
    "轻雾": "轻雾",
    "大雾": "大雾",
    "特强浓雾": "特强浓雾",
    "热": "热",
    "冷": "冷",
    "未知": "未知"
  }
};
const zh = {
  translate: translate$1,
  tabbar: tabbar$1,
  weather: weather$1
};
const translate = {
  title: "Translate",
  tabs: {
    text: "Text",
    phrases: "Phrases",
    history: "History"
  },
  placeholder: "Enter text to translate (max 500 chars)",
  charCount: "{count}/500",
  btn: "Translate",
  translating: "Translating...",
  copy: "Copy",
  copied: "Copied",
  swap: "Swap",
  clear: "Clear",
  result: "Translation",
  noResult: "Translation will appear here",
  history: {
    empty: "No translation history",
    clearAll: "Clear All",
    confirmClear: "Clear all translation history?"
  },
  phrases: {
    greeting: "Greetings",
    dining: "Dining",
    transport: "Transport",
    shopping: "Shopping",
    hotel: "Hotel",
    emergency: "Emergency",
    scenic: "Attractions",
    time: "Time",
    tapToTranslate: "Tap a phrase to translate"
  },
  lang: {
    zh: "Chinese",
    en: "English",
    ja: "Japanese",
    ko: "Korean",
    fr: "French",
    es: "Spanish",
    de: "German",
    ru: "Russian",
    ar: "Arabic",
    th: "Thai"
  },
  error: {
    empty: "Please enter text to translate",
    failed: "Translation failed, check your network",
    sameLang: "Source and target language cannot be the same"
  }
};
const tabbar = {
  home: "Home",
  guide: "Guide",
  elder: "Elder",
  language: "Translate",
  badges: "Badges",
  mine: "Mine",
  match: "Go"
};
const weather = {
  card: {
    locating: "Locating...",
    locateFailed: "Location failed",
    windDirection: "Wind Dir",
    windPower: "Wind",
    windPowerUnit: "Level",
    humidity: "Humidity"
  },
  condition: {
    "晴": "Sunny",
    "少云": "Few Clouds",
    "晴间多云": "Partly Cloudy",
    "多云": "Cloudy",
    "阴": "Overcast",
    "有风": "Windy",
    "平静": "Calm",
    "微风": "Light Breeze",
    "和风": "Gentle Breeze",
    "清风": "Fresh Breeze",
    "强风/劲风": "Strong Breeze",
    "疾风": "Near Gale",
    "大风": "Gale",
    "烈风": "Strong Gale",
    "风暴": "Storm",
    "狂爆风": "Violent Storm",
    "飓风": "Hurricane",
    "热带风暴": "Tropical Storm",
    "霾": "Haze",
    "中度霾": "Moderate Haze",
    "重度霾": "Heavy Haze",
    "严重霾": "Severe Haze",
    "阵雨": "Shower",
    "雷阵雨": "Thundershower",
    "雷阵雨并伴有冰雹": "Thunderstorm with Hail",
    "小雨": "Light Rain",
    "中雨": "Moderate Rain",
    "大雨": "Heavy Rain",
    "暴雨": "Rainstorm",
    "大暴雨": "Heavy Rainstorm",
    "特大暴雨": "Severe Rainstorm",
    "冻雨": "Freezing Rain",
    "小雨-中雨": "Light to Moderate Rain",
    "中雨-大雨": "Moderate to Heavy Rain",
    "大雨-暴雨": "Heavy Rain to Rainstorm",
    "暴雨-大暴雨": "Rainstorm to Heavy Rainstorm",
    "大暴雨-特大暴雨": "Heavy to Severe Rainstorm",
    "雨": "Rain",
    "小雪": "Light Snow",
    "中雪": "Moderate Snow",
    "大雪": "Heavy Snow",
    "暴雪": "Blizzard",
    "小雪-中雪": "Light to Moderate Snow",
    "中雪-大雪": "Moderate to Heavy Snow",
    "大雪-暴雪": "Heavy Snow to Blizzard",
    "雪": "Snow",
    "雨夹雪": "Sleet",
    "雨雪天气": "Rain and Snow",
    "阵雪": "Snow Shower",
    "浮尘": "Floating Dust",
    "扬沙": "Blowing Sand",
    "沙尘暴": "Sandstorm",
    "强沙尘暴": "Severe Sandstorm",
    "龙卷风": "Tornado",
    "雾": "Fog",
    "浓雾": "Dense Fog",
    "强浓雾": "Heavy Dense Fog",
    "轻雾": "Mist",
    "大雾": "Thick Fog",
    "特强浓雾": "Extreme Dense Fog",
    "热": "Hot",
    "冷": "Cold",
    "未知": "Unknown"
  }
};
const en = {
  translate,
  tabbar,
  weather
};
const LOCALE_KEY = "app_locale";
let savedLocale = "zh";
try {
  const stored = common_vendor.index.getStorageSync(LOCALE_KEY);
  if (stored === "en" || stored === "zh")
    savedLocale = stored;
} catch {
}
const i18n = common_vendor.createI18n({
  legacy: false,
  locale: savedLocale,
  fallbackLocale: "en",
  messages: { zh, en }
});
function setLocale(lang) {
  i18n.global.locale.value = lang;
  try {
    common_vendor.index.setStorageSync(LOCALE_KEY, lang);
  } catch {
  }
}
exports.i18n = i18n;
exports.setLocale = setLocale;
//# sourceMappingURL=../.sourcemap/mp-weixin/i18n.js.map
