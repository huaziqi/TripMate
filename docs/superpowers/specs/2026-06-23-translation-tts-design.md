# 翻译页发音功能设计文档

**日期：** 2026-06-23

## 目标

为翻译页的**翻译结果**和**常用短语弹出层**添加发音（TTS）功能，复用已有的腾讯云 TTS 后端接口。

## 范围

- 翻译结果区🔊按钮（已有 UI，需实现逻辑）
- 常用短语翻译结果弹出层新增🔊按钮
- 不涉及历史记录区、原文输入区

---

## 后端改动

### 1. `TtsRequestDTO` 新增 `lang` 字段

```java
@NotBlank
private String text;

private String lang = "zh"; // 可选，默认中文
```

### 2. `TtsServiceImpl.synthesize()` 语言分支

| lang | voiceType | PrimaryLanguage | 说明 |
|------|-----------|-----------------|------|
| `zh` | 101001 | 1 | 中文女声（智瑜） |
| `en` | 101016 | 2 | 英文女声（Vivian） |
| 其他 | — | — | 抛 RuntimeException，前端提示"暂不支持该语言发音" |

字数限制保持 150 字不变；前端负责截断并给用户提示。

---

## 前端改动

### 1. 新增 `frontend/api/tts.ts`

```ts
export function synthesizeSpeech(text: string, lang: string) {
  return post<{ audioUrl: string; sessionId: string }>('/api/tts/synthesize', { text, lang })
}
```

### 2. `language.vue` — 翻译结果区

- `speakResult()` 调用 `synthesizeSpeech(resultText.value, toLang.value)`
- 超 150 字截断，toast 提示"文本过长，仅朗读前150字"
- 后端报错（不支持语言）catch 后 toast "暂不支持该语言发音"
- 播放中按钮高亮，播放结束恢复

### 3. `language.vue` — 常用短语弹出层

- `sheet-actions` 行左侧新增🔊图标按钮
- 点击调用 `speakPhrase()` → `synthesizeSpeech(phraseResult.translated, phraseTargetLang.value)`
- 同样的截断 + 错误处理逻辑

### 4. 音频播放单例

```ts
let audioCtx: UniApp.InnerAudioContext | null = null

function playAudio(url: string) {
  if (audioCtx) { audioCtx.stop(); audioCtx.destroy() }
  audioCtx = uni.createInnerAudioContext()
  audioCtx.src = url
  audioCtx.play()
}
```

- 每次播放前销毁上一个实例，防止叠播
- `speaking` ref 控制按钮状态，`onEnded` / `onError` 回调重置

---

## 错误处理

| 场景 | 处理 |
|------|------|
| 不支持的语言（ja/ko/fr 等） | toast "暂不支持该语言发音" |
| 文本超 150 字 | 截断后朗读，toast "文本过长，仅朗读前150字" |
| 网络/后端异常 | toast "发音失败，请稍后再试" |
| 无翻译结果时点击 | 按钮不可见（v-if 控制） |
