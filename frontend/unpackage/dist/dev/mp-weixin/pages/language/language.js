"use strict";
const common_vendor = require("../../common/vendor.js");
const composables_useElder = require("../../composables/useElder.js");
const composables_useTranslationHistory = require("../../composables/useTranslationHistory.js");
const composables_useFavoritePhrases = require("../../composables/useFavoritePhrases.js");
const api_translate = require("../../api/translate.js");
const i18n = require("../../i18n.js");
if (!Math) {
  TabBar();
}
const TabBar = () => "../../components/TabBar/TabBar.js";
const LANG_KEY = "translate_langs";
const PHRASE_LANG_KEY = "phrase_target_lang";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "language",
  setup(__props) {
    const { t, locale } = common_vendor.useI18n();
    const currentLocale = common_vendor.computed(() => locale.value);
    function switchUiLang(lang) {
      if (lang === locale.value)
        return;
      i18n.setLocale(lang);
      common_vendor.index.showToast({ title: lang === "zh" ? "已切换为中文" : "Switched to English", icon: "none" });
    }
    const { rpx } = composables_useElder.useElder();
    const { history, addHistory, removeHistory, clearHistory, formatTime } = composables_useTranslationHistory.useTranslationHistory();
    const { favorites, isFavorite, toggleFavorite } = composables_useFavoritePhrases.useFavoritePhrases();
    const activeTab = common_vendor.ref("text");
    const tabs = common_vendor.computed(() => [
      { key: "text", label: t("translate.tabs.text") },
      { key: "phrases", label: t("translate.tabs.phrases") },
      { key: "history", label: t("translate.tabs.history") }
    ]);
    const allLanguages = [
      { code: "auto", name: "自动检测", flag: "🔍" },
      { code: "zh", name: "中文", flag: "🇨🇳" },
      { code: "en", name: "English", flag: "🇺🇸" },
      { code: "ja", name: "日本語", flag: "🇯🇵" },
      { code: "ko", name: "한국어", flag: "🇰🇷" },
      { code: "fr", name: "Français", flag: "🇫🇷" },
      { code: "es", name: "Español", flag: "🇪🇸" },
      { code: "de", name: "Deutsch", flag: "🇩🇪" },
      { code: "ru", name: "Русский", flag: "🇷🇺" },
      { code: "th", name: "ภาษาไทย", flag: "🇹🇭" },
      { code: "ar", name: "العربية", flag: "🇸🇦" }
    ];
    const targetLangs = allLanguages.filter((l) => l.code !== "auto");
    const phraseTargetLangs = allLanguages.filter((l) => l.code !== "auto" && l.code !== "zh");
    const fromLangs = allLanguages;
    const pickerLangs = common_vendor.computed(
      () => pickerTarget.value === "from" ? fromLangs : targetLangs
    );
    function langNameByCode(code) {
      var _a;
      return ((_a = allLanguages.find((l) => l.code === code)) == null ? void 0 : _a.name) ?? code;
    }
    function normalizeDetectedLang(raw) {
      if (!raw)
        return "";
      const lower = raw.toLowerCase().split("-")[0];
      if (lower === "zh" || lower === "cn")
        return "zh";
      const found = allLanguages.find((l) => l.code === lower);
      return found ? found.code : "";
    }
    let initFrom = "auto";
    let initTo = "en";
    try {
      const stored = common_vendor.index.getStorageSync(LANG_KEY);
      if (stored == null ? void 0 : stored.from)
        initFrom = stored.from;
      if (stored == null ? void 0 : stored.to)
        initTo = stored.to;
    } catch {
    }
    const fromLang = common_vendor.ref(initFrom);
    const toLang = common_vendor.ref(initTo);
    const recentLangPairs = common_vendor.computed(() => {
      const seen = /* @__PURE__ */ new Set();
      const result = [];
      for (const h of history.value) {
        const key = `${h.from}|${h.to}`;
        if (!seen.has(key)) {
          seen.add(key);
          const fromInfo = allLanguages.find((l) => l.code === h.from);
          const toInfo = allLanguages.find((l) => l.code === h.to);
          if (fromInfo && toInfo) {
            result.push({ from: h.from, to: h.to, fromFlag: fromInfo.flag, toFlag: toInfo.flag, key });
          }
        }
        if (result.length >= 3)
          break;
      }
      return result;
    });
    function applyLangPair(pair) {
      fromLang.value = pair.from;
      toLang.value = pair.to;
      resultText.value = "";
      detectedLang.value = "";
    }
    const fromLangInfo = common_vendor.computed(() => allLanguages.find((l) => l.code === fromLang.value) ?? allLanguages[0]);
    const toLangInfo = common_vendor.computed(() => allLanguages.find((l) => l.code === toLang.value) ?? allLanguages[2]);
    function swapLang() {
      if (fromLang.value === "auto") {
        common_vendor.index.showToast({ title: "自动检测模式下无法互换", icon: "none" });
        return;
      }
      [fromLang.value, toLang.value] = [toLang.value, fromLang.value];
      if (resultText.value) {
        inputText.value = resultText.value;
        resultText.value = "";
        detectedLang.value = "";
      }
    }
    const langPickerVisible = common_vendor.ref(false);
    const pickerTarget = common_vendor.ref("from");
    const currentPickerLang = common_vendor.computed(
      () => pickerTarget.value === "from" ? fromLang.value : toLang.value
    );
    function showLangPicker(target) {
      pickerTarget.value = target;
      langPickerVisible.value = true;
    }
    function selectLang(code) {
      if (pickerTarget.value === "from") {
        fromLang.value = code;
        if (code !== "auto" && code === toLang.value) {
          toLang.value = code === "zh" ? "en" : "zh";
        }
      } else {
        toLang.value = code;
        if (fromLang.value !== "auto" && fromLang.value === code) {
          fromLang.value = code === "zh" ? "en" : "zh";
        }
      }
      langPickerVisible.value = false;
      resultText.value = "";
      detectedLang.value = "";
      try {
        common_vendor.index.setStorageSync(LANG_KEY, { from: fromLang.value, to: toLang.value });
      } catch {
      }
    }
    const inputText = common_vendor.ref("");
    const resultText = common_vendor.ref("");
    const detectedLang = common_vendor.ref("");
    const translating = common_vendor.ref(false);
    const copied = common_vendor.ref(false);
    function onInput(e) {
      inputText.value = e.detail.value ?? "";
      resultText.value = "";
      detectedLang.value = "";
    }
    function clearInput() {
      inputText.value = "";
      resultText.value = "";
      detectedLang.value = "";
    }
    function pasteFromClipboard() {
      common_vendor.index.getClipboardData({
        success: (res) => {
          if (res.data) {
            inputText.value = res.data;
            resultText.value = "";
          }
        }
      });
    }
    async function doTranslate() {
      const text = inputText.value.trim();
      if (!text) {
        common_vendor.index.showToast({ title: t("translate.error.empty"), icon: "none" });
        return;
      }
      common_vendor.index.hideKeyboard();
      translating.value = true;
      try {
        const res = await api_translate.translateText(text, fromLang.value, toLang.value);
        if (res.code === 200 && res.data) {
          resultText.value = res.data.translatedText;
          detectedLang.value = normalizeDetectedLang(res.data.detectedLang ?? "");
          common_vendor.index.vibrateShort({ type: "light" });
          const actualFrom = detectedLang.value || (fromLang.value === "auto" ? "zh" : fromLang.value);
          addHistory({
            sourceText: text,
            translatedText: res.data.translatedText,
            from: actualFrom,
            to: toLang.value
          });
        } else {
          common_vendor.index.showToast({ title: res.message || t("translate.error.failed"), icon: "none" });
        }
      } catch {
        common_vendor.index.showToast({ title: t("translate.error.failed"), icon: "none" });
      } finally {
        translating.value = false;
      }
    }
    function copyResult() {
      common_vendor.index.setClipboardData({
        data: resultText.value,
        success: () => {
          common_vendor.index.vibrateShort({ type: "light" });
          copied.value = true;
          setTimeout(() => {
            copied.value = false;
          }, 2e3);
        }
      });
    }
    function speakResult() {
      common_vendor.index.showToast({ title: "朗读功能即将上线", icon: "none" });
    }
    const historyFilter = common_vendor.ref("");
    const historyLangPairs = common_vendor.computed(() => {
      const pairs = /* @__PURE__ */ new Set();
      history.value.forEach((h) => {
        pairs.add(`${langNameByCode(h.from)}→${langNameByCode(h.to)}`);
      });
      return Array.from(pairs);
    });
    const filteredHistory = common_vendor.computed(() => {
      if (!historyFilter.value)
        return history.value;
      return history.value.filter((h) => {
        const pair = `${langNameByCode(h.from)}→${langNameByCode(h.to)}`;
        return pair === historyFilter.value;
      });
    });
    function reuseHistory(item) {
      inputText.value = item.sourceText;
      resultText.value = item.translatedText;
      fromLang.value = item.from;
      toLang.value = item.to;
      detectedLang.value = "";
      activeTab.value = "text";
    }
    function onHistoryLongPress(item) {
      common_vendor.index.showActionSheet({
        itemList: ["复制原文", "复制译文", "复用到翻译", "删除"],
        success: (res) => {
          switch (res.tapIndex) {
            case 0:
              common_vendor.index.setClipboardData({ data: item.sourceText, success: () => common_vendor.index.showToast({ title: "已复制原文", icon: "none" }) });
              break;
            case 1:
              common_vendor.index.setClipboardData({ data: item.translatedText, success: () => common_vendor.index.showToast({ title: "已复制译文", icon: "none" }) });
              break;
            case 2:
              reuseHistory(item);
              break;
            case 3:
              removeHistory(item.id);
              break;
          }
        }
      });
    }
    function onClearHistory() {
      common_vendor.index.showModal({
        title: "清空历史",
        content: t("translate.history.confirmClear"),
        success: (res) => {
          if (res.confirm)
            clearHistory();
        }
      });
    }
    let initPhraseLang = "en";
    try {
      const stored = common_vendor.index.getStorageSync(PHRASE_LANG_KEY);
      if (stored && phraseTargetLangs.find((l) => l.code === stored))
        initPhraseLang = stored;
    } catch {
    }
    const phraseTargetLang = common_vendor.ref(initPhraseLang);
    const phraseResult = common_vendor.ref(null);
    const phraseCopied = common_vendor.ref(false);
    const phraseLoading = common_vendor.ref(false);
    const phraseSearch = common_vendor.ref("");
    const allPhraseCategories = [
      {
        key: "greeting",
        icon: "👋",
        phrases: [
          { zh: "你好", en: "Hello" },
          { zh: "谢谢", en: "Thank you" },
          { zh: "对不起", en: "I'm sorry" },
          { zh: "再见", en: "Goodbye" },
          { zh: "请问", en: "Excuse me" },
          { zh: "不客气", en: "You're welcome" }
        ]
      },
      {
        key: "dining",
        icon: "🍽️",
        phrases: [
          { zh: "我想要这个", en: "I'd like this one" },
          { zh: "菜单在哪里？", en: "Where is the menu?" },
          { zh: "买单", en: "Check, please" },
          { zh: "不辣", en: "Not spicy please" },
          { zh: "素食", en: "Vegetarian" },
          { zh: "好吃！", en: "Delicious!" }
        ]
      },
      {
        key: "transport",
        icon: "🚌",
        phrases: [
          { zh: "去机场怎么走？", en: "How do I get to the airport?" },
          { zh: "最近的地铁站在哪里？", en: "Where is the nearest subway station?" },
          { zh: "请载我去这里", en: "Please take me here" },
          { zh: "这趟车去哪里？", en: "Where does this bus go?" },
          { zh: "多少钱？", en: "How much?" },
          { zh: "停在这里", en: "Stop here please" }
        ]
      },
      {
        key: "shopping",
        icon: "🛍️",
        phrases: [
          { zh: "这个多少钱？", en: "How much is this?" },
          { zh: "可以便宜一点吗？", en: "Can you give me a discount?" },
          { zh: "我只是看看", en: "Just looking, thanks" },
          { zh: "有没有其他颜色？", en: "Do you have other colors?" },
          { zh: "可以用信用卡吗？", en: "Do you accept credit card?" },
          { zh: "可以退换吗？", en: "Can I return this?" }
        ]
      },
      {
        key: "hotel",
        icon: "🏨",
        phrases: [
          { zh: "我有预订", en: "I have a reservation" },
          { zh: "退房时间是几点？", en: "What time is checkout?" },
          { zh: "能帮我叫醒吗？", en: "Could I have a wake-up call?" },
          { zh: "有无线网络吗？", en: "Is there Wi-Fi?" },
          { zh: "空调坏了", en: "The air conditioner is broken" },
          { zh: "请打扫房间", en: "Please clean my room" }
        ]
      },
      {
        key: "emergency",
        icon: "🆘",
        phrases: [
          { zh: "救命！", en: "Help!" },
          { zh: "请叫救护车", en: "Please call an ambulance" },
          { zh: "请叫警察", en: "Please call the police" },
          { zh: "我迷路了", en: "I'm lost" },
          { zh: "我钱包被偷了", en: "My wallet was stolen" },
          { zh: "我需要医生", en: "I need a doctor" },
          { zh: "这里有药店吗？", en: "Is there a pharmacy nearby?" },
          { zh: "我对这个过敏", en: "I'm allergic to this" }
        ]
      },
      {
        key: "scenic",
        icon: "🏛️",
        phrases: [
          { zh: "入口在哪里？", en: "Where is the entrance?" },
          { zh: "几点开放？", en: "What time does it open?" },
          { zh: "门票多少钱？", en: "How much is the ticket?" },
          { zh: "可以拍照吗？", en: "Can I take photos here?" },
          { zh: "有导游服务吗？", en: "Is there a guided tour?" },
          { zh: "洗手间在哪里？", en: "Where is the restroom?" },
          { zh: "这里是什么地方？", en: "What is this place?" },
          { zh: "可以帮我拍照吗？", en: "Could you take a photo for me?" }
        ]
      },
      {
        key: "time",
        icon: "🕐",
        phrases: [
          { zh: "现在几点？", en: "What time is it now?" },
          { zh: "今天是几号？", en: "What is today's date?" },
          { zh: "等一下", en: "Just a moment" },
          { zh: "快点", en: "Hurry up" },
          { zh: "明天", en: "Tomorrow" },
          { zh: "后天", en: "The day after tomorrow" },
          { zh: "上午", en: "Morning" },
          { zh: "下午", en: "Afternoon" }
        ]
      }
    ];
    const filteredFavorites = common_vendor.computed(() => {
      const q = phraseSearch.value.trim().toLowerCase();
      if (!q)
        return favorites.value;
      return favorites.value.filter(
        (p) => p.zh.includes(q) || p.en.toLowerCase().includes(q)
      );
    });
    const phraseCategories = common_vendor.computed(() => {
      const q = phraseSearch.value.trim().toLowerCase();
      if (!q)
        return allPhraseCategories;
      return allPhraseCategories.map((cat) => ({
        ...cat,
        phrases: cat.phrases.filter(
          (p) => p.zh.includes(q) || p.en.toLowerCase().includes(q)
        )
      })).filter((cat) => cat.phrases.length > 0);
    });
    function onPhraseTargetChange(code) {
      phraseTargetLang.value = code;
      try {
        common_vendor.index.setStorageSync(PHRASE_LANG_KEY, code);
      } catch {
      }
    }
    async function translatePhrase(phrase) {
      if (phraseLoading.value)
        return;
      const sourceText = phrase.zh;
      if (phraseTargetLang.value === "en") {
        phraseResult.value = { source: phrase.zh, translated: phrase.en };
        return;
      }
      phraseLoading.value = true;
      common_vendor.index.showLoading({ title: "翻译中...", mask: true });
      try {
        const res = await api_translate.translateText(sourceText, "zh", phraseTargetLang.value);
        if (res.code === 200 && res.data) {
          phraseResult.value = { source: sourceText, translated: res.data.translatedText };
          addHistory({
            sourceText,
            translatedText: res.data.translatedText,
            from: "zh",
            to: phraseTargetLang.value
          });
        }
      } catch {
        common_vendor.index.showToast({ title: "翻译失败", icon: "none" });
      } finally {
        phraseLoading.value = false;
        common_vendor.index.hideLoading();
      }
    }
    function closePhraseResult() {
      phraseResult.value = null;
      phraseCopied.value = false;
    }
    function copyPhraseResult() {
      if (!phraseResult.value)
        return;
      common_vendor.index.setClipboardData({
        data: phraseResult.value.translated,
        success: () => {
          phraseCopied.value = true;
          setTimeout(() => {
            phraseCopied.value = false;
          }, 2e3);
        }
      });
    }
    function usePhraseInText() {
      if (!phraseResult.value)
        return;
      inputText.value = phraseResult.value.source;
      resultText.value = phraseResult.value.translated;
      fromLang.value = "zh";
      toLang.value = phraseTargetLang.value;
      detectedLang.value = "";
      closePhraseResult();
      activeTab.value = "text";
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.unref(rpx)(22),
        b: common_vendor.unref(rpx)(22),
        c: currentLocale.value === "zh" ? 1 : "",
        d: common_vendor.o(($event) => switchUiLang("zh"), "5c"),
        e: common_vendor.unref(rpx)(22),
        f: currentLocale.value === "en" ? 1 : "",
        g: common_vendor.o(($event) => switchUiLang("en"), "31"),
        h: common_vendor.f(tabs.value, (tab, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(tab.label),
            b: tab.key === "history" && common_vendor.unref(history).length
          }, tab.key === "history" && common_vendor.unref(history).length ? {
            c: common_vendor.t(common_vendor.unref(history).length)
          } : {}, {
            d: tab.key,
            e: activeTab.value === tab.key ? 1 : "",
            f: common_vendor.o(($event) => activeTab.value = tab.key, tab.key)
          });
        }),
        i: common_vendor.unref(rpx)(26),
        j: activeTab.value === "text"
      }, activeTab.value === "text" ? common_vendor.e({
        k: recentLangPairs.value.length
      }, recentLangPairs.value.length ? {
        l: common_vendor.unref(rpx)(22),
        m: common_vendor.f(recentLangPairs.value, (pair, k0, i0) => {
          return {
            a: common_vendor.t(pair.fromFlag),
            b: common_vendor.t(pair.toFlag),
            c: pair.key,
            d: fromLang.value === pair.from && toLang.value === pair.to ? 1 : "",
            e: common_vendor.o(($event) => applyLangPair(pair), pair.key)
          };
        }),
        n: common_vendor.unref(rpx)(22)
      } : {}, {
        o: common_vendor.t(fromLangInfo.value.flag),
        p: common_vendor.t(fromLangInfo.value.name),
        q: common_vendor.unref(rpx)(26),
        r: common_vendor.o(($event) => showLangPicker("from"), "e9"),
        s: common_vendor.o(swapLang, "07"),
        t: common_vendor.t(toLangInfo.value.flag),
        v: common_vendor.t(toLangInfo.value.name),
        w: common_vendor.unref(rpx)(26),
        x: common_vendor.o(($event) => showLangPicker("to"), "56"),
        y: common_vendor.unref(t)("translate.placeholder"),
        z: inputText.value,
        A: common_vendor.unref(rpx)(28),
        B: common_vendor.o(onInput, "be"),
        C: common_vendor.t(inputText.value.length),
        D: inputText.value.length > 450 ? 1 : "",
        E: common_vendor.unref(rpx)(22),
        F: inputText.value
      }, inputText.value ? {
        G: common_vendor.unref(rpx)(22),
        H: common_vendor.o(clearInput, "d0")
      } : {}, {
        I: inputText.value
      }, inputText.value ? {
        J: common_vendor.unref(rpx)(22),
        K: common_vendor.o(pasteFromClipboard, "93")
      } : {}, {
        L: translating.value
      }, translating.value ? {} : {}, {
        M: common_vendor.t(translating.value ? common_vendor.unref(t)("translate.translating") : common_vendor.unref(t)("translate.btn")),
        N: translating.value || !inputText.value.trim(),
        O: translating.value || !inputText.value.trim() ? 1 : "",
        P: common_vendor.unref(rpx)(30),
        Q: common_vendor.o(doTranslate, "7e"),
        R: common_vendor.t(common_vendor.unref(t)("translate.result")),
        S: common_vendor.unref(rpx)(22),
        T: detectedLang.value && fromLang.value === "auto"
      }, detectedLang.value && fromLang.value === "auto" ? {
        U: common_vendor.t(langNameByCode(detectedLang.value)),
        V: common_vendor.unref(rpx)(20)
      } : {}, {
        W: resultText.value
      }, resultText.value ? {
        X: common_vendor.o(speakResult, "43"),
        Y: common_vendor.t(copied.value ? "✓ " + common_vendor.unref(t)("translate.copied") : common_vendor.unref(t)("translate.copy")),
        Z: common_vendor.unref(rpx)(22),
        aa: common_vendor.o(copyResult, "f1")
      } : {}, {
        ab: resultText.value
      }, resultText.value ? {
        ac: common_vendor.t(resultText.value),
        ad: common_vendor.unref(rpx)(30)
      } : {
        ae: common_vendor.t(common_vendor.unref(t)("translate.noResult")),
        af: common_vendor.unref(rpx)(26)
      }, {
        ag: resultText.value ? 1 : "",
        ah: resultText.value
      }, resultText.value ? {
        ai: common_vendor.unref(rpx)(22),
        aj: common_vendor.t(inputText.value.trim().length),
        ak: common_vendor.t(resultText.value.length),
        al: common_vendor.unref(rpx)(22)
      } : {}) : {}, {
        am: activeTab.value === "phrases"
      }, activeTab.value === "phrases" ? common_vendor.e({
        an: phraseSearch.value,
        ao: common_vendor.unref(rpx)(26),
        ap: common_vendor.o((e) => phraseSearch.value = e.detail.value, "0b"),
        aq: phraseSearch.value
      }, phraseSearch.value ? {
        ar: common_vendor.o(($event) => phraseSearch.value = "", "16")
      } : {}, {
        as: common_vendor.unref(rpx)(24),
        at: common_vendor.f(common_vendor.unref(phraseTargetLangs), (lang, k0, i0) => {
          return {
            a: common_vendor.t(lang.flag),
            b: common_vendor.t(lang.name),
            c: lang.code,
            d: phraseTargetLang.value === lang.code ? 1 : "",
            e: common_vendor.o(($event) => onPhraseTargetChange(lang.code), lang.code)
          };
        }),
        av: common_vendor.unref(rpx)(22),
        aw: phraseSearch.value && !phraseCategories.value.length && !filteredFavorites.value.length
      }, phraseSearch.value && !phraseCategories.value.length && !filteredFavorites.value.length ? {
        ax: common_vendor.t(phraseSearch.value),
        ay: common_vendor.unref(rpx)(26)
      } : {}, {
        az: filteredFavorites.value.length
      }, filteredFavorites.value.length ? {
        aA: common_vendor.unref(rpx)(26),
        aB: common_vendor.f(filteredFavorites.value, (phrase, k0, i0) => {
          return {
            a: common_vendor.t(phrase.zh),
            b: common_vendor.t(phrase.en),
            c: common_vendor.o(($event) => common_vendor.unref(toggleFavorite)({
              ...phrase,
              category: "fav"
            }), phrase.zh),
            d: phrase.zh,
            e: common_vendor.o(($event) => translatePhrase(phrase), phrase.zh)
          };
        }),
        aC: common_vendor.unref(rpx)(28),
        aD: common_vendor.unref(rpx)(24)
      } : {}, {
        aE: common_vendor.t(common_vendor.unref(t)("translate.phrases.tapToTranslate")),
        aF: common_vendor.unref(rpx)(24),
        aG: common_vendor.f(phraseCategories.value, (cat, k0, i0) => {
          return {
            a: common_vendor.t(cat.icon),
            b: common_vendor.t(common_vendor.unref(t)(`translate.phrases.${cat.key}`)),
            c: common_vendor.f(cat.phrases, (phrase, k1, i1) => {
              return {
                a: common_vendor.t(phrase.zh),
                b: common_vendor.t(phrase.en),
                c: common_vendor.t(common_vendor.unref(isFavorite)(phrase.zh) ? "⭐" : "☆"),
                d: common_vendor.unref(isFavorite)(phrase.zh) ? 1 : "",
                e: common_vendor.o(($event) => common_vendor.unref(toggleFavorite)({
                  ...phrase,
                  category: cat.key
                }), phrase.zh),
                f: phrase.zh,
                g: common_vendor.o(($event) => translatePhrase(phrase), phrase.zh),
                h: common_vendor.o(($event) => common_vendor.unref(toggleFavorite)({
                  ...phrase,
                  category: cat.key
                }), phrase.zh)
              };
            }),
            d: cat.key
          };
        }),
        aH: common_vendor.unref(rpx)(26),
        aI: common_vendor.unref(rpx)(28),
        aJ: common_vendor.unref(rpx)(24)
      }) : {}, {
        aK: activeTab.value === "history"
      }, activeTab.value === "history" ? common_vendor.e({
        aL: common_vendor.unref(history).length === 0
      }, common_vendor.unref(history).length === 0 ? {
        aM: common_vendor.t(common_vendor.unref(t)("translate.history.empty")),
        aN: common_vendor.unref(rpx)(28),
        aO: common_vendor.unref(rpx)(24)
      } : common_vendor.e({
        aP: common_vendor.t(historyFilter.value ? filteredHistory.value.length + "/" : ""),
        aQ: common_vendor.t(common_vendor.unref(history).length),
        aR: common_vendor.unref(rpx)(24),
        aS: common_vendor.unref(rpx)(24),
        aT: common_vendor.o(onClearHistory, "a6"),
        aU: historyLangPairs.value.length > 1
      }, historyLangPairs.value.length > 1 ? {
        aV: common_vendor.unref(rpx)(22),
        aW: historyFilter.value === "" ? 1 : "",
        aX: common_vendor.o(($event) => historyFilter.value = "", "23"),
        aY: common_vendor.f(historyLangPairs.value, (pair, k0, i0) => {
          return {
            a: common_vendor.t(pair),
            b: pair,
            c: historyFilter.value === pair ? 1 : "",
            d: common_vendor.o(($event) => historyFilter.value = pair, pair)
          };
        }),
        aZ: common_vendor.unref(rpx)(22)
      } : {}, {
        ba: filteredHistory.value.length === 0
      }, filteredHistory.value.length === 0 ? {
        bb: common_vendor.unref(rpx)(26)
      } : {}, {
        bc: common_vendor.f(filteredHistory.value, (item, k0, i0) => {
          return {
            a: common_vendor.t(langNameByCode(item.from)),
            b: common_vendor.t(langNameByCode(item.to)),
            c: common_vendor.t(common_vendor.unref(formatTime)(item.timestamp)),
            d: common_vendor.t(item.sourceText),
            e: common_vendor.t(item.translatedText),
            f: common_vendor.o(($event) => common_vendor.unref(removeHistory)(item.id), item.id),
            g: item.id,
            h: common_vendor.o(($event) => reuseHistory(item), item.id),
            i: common_vendor.o(($event) => onHistoryLongPress(item), item.id)
          };
        }),
        bd: common_vendor.unref(rpx)(20),
        be: common_vendor.unref(rpx)(20),
        bf: common_vendor.unref(rpx)(28),
        bg: common_vendor.unref(rpx)(26),
        bh: common_vendor.unref(rpx)(22),
        bi: common_vendor.unref(rpx)(22)
      })) : {}, {
        bj: phraseResult.value
      }, phraseResult.value ? {
        bk: common_vendor.unref(rpx)(28),
        bl: common_vendor.o(closePhraseResult, "cd"),
        bm: common_vendor.t(phraseResult.value.source),
        bn: common_vendor.unref(rpx)(28),
        bo: common_vendor.t(langNameByCode(phraseTargetLang.value)),
        bp: common_vendor.unref(rpx)(22),
        bq: common_vendor.t(phraseResult.value.translated),
        br: common_vendor.unref(rpx)(34),
        bs: common_vendor.t(phraseCopied.value ? "✓ 已复制" : "📋 复制"),
        bt: common_vendor.unref(rpx)(26),
        bv: common_vendor.o(copyPhraseResult, "ff"),
        bw: common_vendor.unref(rpx)(26),
        bx: common_vendor.o(usePhraseInText, "f7"),
        by: common_vendor.o(() => {
        }, "31"),
        bz: common_vendor.o(closePhraseResult, "aa")
      } : {}, {
        bA: langPickerVisible.value
      }, langPickerVisible.value ? {
        bB: common_vendor.t(pickerTarget.value === "from" ? "选择源语言" : "选择目标语言"),
        bC: common_vendor.unref(rpx)(28),
        bD: common_vendor.o(($event) => langPickerVisible.value = false, "39"),
        bE: common_vendor.f(pickerLangs.value, (lang, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(lang.flag),
            b: common_vendor.t(lang.name),
            c: currentPickerLang.value === lang.code
          }, currentPickerLang.value === lang.code ? {} : {}, {
            d: lang.code,
            e: currentPickerLang.value === lang.code ? 1 : "",
            f: common_vendor.o(($event) => selectLang(lang.code), lang.code)
          });
        }),
        bF: common_vendor.unref(rpx)(28),
        bG: common_vendor.o(() => {
        }, "c5"),
        bH: common_vendor.o(($event) => langPickerVisible.value = false, "f4")
      } : {}, {
        bI: common_vendor.p({
          active: "language"
        })
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-143e30ec"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/language/language.js.map
