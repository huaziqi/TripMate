# 翻译页发音功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为翻译结果区和常用短语弹出层添加发音按钮，复用腾讯云 TTS 后端接口，支持中文和英文发音。

**Architecture:** 后端 `TtsRequestDTO` 新增 `lang` 字段，`TtsServiceImpl` 根据语言选择音色；前端新增 `api/tts.ts` 封装接口调用，在 `language.vue` 中实现 `speakResult()` 和 `speakPhrase()`，用 `uni.createInnerAudioContext()` 播放返回的音频 URL。

**Tech Stack:** Spring Boot 4 / Java 21（后端）、UniApp + Vue 3 Composition API + TypeScript（前端）

## Global Constraints

- 后端字数限制保持 150 字（前端截断，不修改后端限制逻辑）
- 仅支持 `zh`（中文）和 `en`（英文）TTS；其余语言前端提示"暂不支持该语言发音"
- 前端 API base URL 为 `http://localhost:8080`，通过 `useApi` composable 发请求
- 音频播放用模块级单例 `audioCtx`，每次播放前销毁上一个实例
- 不修改后端 `/api/tts/synthesize` 的路由和鉴权配置（已放行）

---

## 文件变更总览

| 操作 | 文件 |
|------|------|
| Modify | `src/main/java/com/LHZ/TripMate/dto/tts/TtsRequestDTO.java` |
| Modify | `src/main/java/com/LHZ/TripMate/service/impl/TtsServiceImpl.java` |
| Create | `frontend/api/tts.ts` |
| Modify | `frontend/pages/language/language.vue` |

---

### Task 1: 后端扩展 TTS 接口支持语言参数

**Files:**
- Modify: `src/main/java/com/LHZ/TripMate/dto/tts/TtsRequestDTO.java`
- Modify: `src/main/java/com/LHZ/TripMate/service/impl/TtsServiceImpl.java`

**Interfaces:**
- Produces: `POST /api/tts/synthesize` 接受 `{ text: string, lang?: string }` → `{ audioUrl, sessionId }`
- `lang` 为空时默认 `"zh"`；传 `"en"` 时用英文音色；其余抛异常

- [ ] **Step 1: 修改 TtsRequestDTO，新增 lang 字段**

打开 `src/main/java/com/LHZ/TripMate/dto/tts/TtsRequestDTO.java`，将全部内容替换为：

```java
package com.LHZ.TripMate.dto.tts;

import jakarta.validation.constraints.NotBlank;

public class TtsRequestDTO {

    @NotBlank(message = "合成文本不能为空")
    private String text;

    private String lang = "zh";

    public TtsRequestDTO() {}

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang != null ? lang : "zh"; }
}
```

- [ ] **Step 2: 修改 TtsServiceImpl，根据 lang 选择音色**

打开 `src/main/java/com/LHZ/TripMate/service/impl/TtsServiceImpl.java`，将 `synthesize()` 方法中：

```java
ttsRequest.setVoiceType(voiceType);
ttsRequest.setPrimaryLanguage(1L);
```

替换为：

```java
String lang = request.getLang() == null ? "zh" : request.getLang().toLowerCase();
switch (lang) {
    case "zh" -> {
        ttsRequest.setVoiceType(101001L);
        ttsRequest.setPrimaryLanguage(1L);
    }
    case "en" -> {
        ttsRequest.setVoiceType(101016L);
        ttsRequest.setPrimaryLanguage(2L);
    }
    default -> throw new RuntimeException("不支持该语言的语音合成：" + lang);
}
```

注意：同时删除原来从 `@Value` 注入 `voiceType` 的字段和注解（`@Value("${tencent.tts.voice-type:101001}") private Long voiceType;`），因为音色现在由 lang 决定，不再从配置读取。

- [ ] **Step 3: 启动后端，手动验证中文 TTS**

运行：
```bash
./mvnw spring-boot:run
```

用 curl 或 Postman 测试：
```bash
curl -X POST http://localhost:8080/api/tts/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text":"你好，欢迎使用旅伴","lang":"zh"}'
```

预期：返回 `{ "code": 200, "data": { "audioUrl": "http://...", "sessionId": "..." } }`

- [ ] **Step 4: 验证英文 TTS**

```bash
curl -X POST http://localhost:8080/api/tts/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello, welcome to TripMate","lang":"en"}'
```

预期：返回 200，`audioUrl` 可在浏览器打开播放，发音为英文女声。

- [ ] **Step 5: 验证不支持语言返回错误**

```bash
curl -X POST http://localhost:8080/api/tts/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text":"Bonjour","lang":"fr"}'
```

预期：返回非 200（RuntimeException 会被 Spring 全局异常处理捕获，或 500），`message` 含"不支持该语言的语音合成"。

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/LHZ/TripMate/dto/tts/TtsRequestDTO.java
git add src/main/java/com/LHZ/TripMate/service/impl/TtsServiceImpl.java
git commit -m "feat(tts): support lang param to select zh/en voice type"
```

---

### Task 2: 前端新增 TTS API 封装

**Files:**
- Create: `frontend/api/tts.ts`

**Interfaces:**
- Produces: `synthesizeSpeech(text: string, lang: string): Promise<ApiResponse<{ audioUrl: string; sessionId: string }>>`
- 供 Task 3 的 `language.vue` 调用

- [ ] **Step 1: 创建 frontend/api/tts.ts**

```typescript
import { useApi } from '@/utils/useApi'

const { post } = useApi()

export interface TtsResult {
  audioUrl: string
  sessionId: string
}

export function synthesizeSpeech(text: string, lang: string) {
  return post<TtsResult>('/api/tts/synthesize', { text, lang } as any)
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/api/tts.ts
git commit -m "feat(tts): add synthesizeSpeech API wrapper"
```

---

### Task 3: 翻译结果区发音功能

**Files:**
- Modify: `frontend/pages/language/language.vue`

**Interfaces:**
- Consumes: `synthesizeSpeech(text, lang)` from `frontend/api/tts.ts`
- Consumes: `resultText` ref（翻译结果文本）、`toLang` ref（目标语言代码）

- [ ] **Step 1: 在 script setup 顶部引入 synthesizeSpeech**

在 `language.vue` 的 `<script setup>` 里，在现有 import 末尾添加：

```typescript
import { synthesizeSpeech } from '@/api/tts'
```

- [ ] **Step 2: 添加音频播放单例和 speaking 状态**

在 `<script setup>` 中，在 `const copied = ref(false)` 之后添加：

```typescript
const speaking = ref(false)
let audioCtx: UniApp.InnerAudioContext | null = null

function playAudio(url: string) {
  if (audioCtx) {
    audioCtx.stop()
    audioCtx.destroy()
    audioCtx = null
  }
  audioCtx = uni.createInnerAudioContext()
  audioCtx.src = url
  audioCtx.onEnded(() => { speaking.value = false })
  audioCtx.onError(() => {
    speaking.value = false
    uni.showToast({ title: '发音失败，请稍后再试', icon: 'none' })
  })
  audioCtx.play()
}
```

- [ ] **Step 3: 替换 speakResult() 实现**

找到现有的 `speakResult()` 函数（约第 605 行）：

```typescript
function speakResult() {
  // 微信小程序使用 wx.createInnerAudioContext 或文字转语音暂无免费 API
  // 这里只用 Toast 提示，预留扩展
  uni.showToast({ title: '朗读功能即将上线', icon: 'none' })
}
```

替换为：

```typescript
async function speakResult() {
  if (!resultText.value || speaking.value) return
  const supportedLangs = ['zh', 'en']
  if (!supportedLangs.includes(toLang.value)) {
    uni.showToast({ title: '暂不支持该语言发音', icon: 'none' })
    return
  }
  let text = resultText.value
  if (text.length > 150) {
    text = text.slice(0, 150)
    uni.showToast({ title: '文本过长，仅朗读前150字', icon: 'none' })
  }
  speaking.value = true
  try {
    const res = await synthesizeSpeech(text, toLang.value)
    if (res.code === 200 && res.data?.audioUrl) {
      playAudio(res.data.audioUrl)
    } else {
      speaking.value = false
      uni.showToast({ title: '发音失败，请稍后再试', icon: 'none' })
    }
  } catch {
    speaking.value = false
    uni.showToast({ title: '发音失败，请稍后再试', icon: 'none' })
  }
}
```

- [ ] **Step 4: 更新🔊按钮 UI，显示 speaking 状态**

找到 template 中现有的图标按钮（约第 122 行）：

```html
<view class="icon-btn" @click="speakResult">
  <text class="icon-btn-text">🔊</text>
</view>
```

替换为：

```html
<view class="icon-btn" :class="{ speaking: speaking }" @click="speakResult">
  <text class="icon-btn-text">{{ speaking ? '⏸' : '🔊' }}</text>
</view>
```

- [ ] **Step 5: 给 speaking 状态添加样式**

在 `<style scoped>` 末尾，`.icon-btn` 相关样式后添加：

```css
.icon-btn.speaking {
  background: #d6e4ff;
}
```

- [ ] **Step 6: 在微信开发者工具中验证**

1. 在 HBuilderX 编译到微信开发者工具
2. 输入中文文本，点击"翻译"（目标语言选英文）
3. 翻译完成后点击🔊按钮
4. 预期：按钮变为⏸，播放英文发音，结束后恢复🔊
5. 将目标语言改为日语，翻译后点🔊
6. 预期：toast 显示"暂不支持该语言发音"

- [ ] **Step 7: 提交**

```bash
git add frontend/pages/language/language.vue
git commit -m "feat(language): implement speakResult with TTS backend"
```

---

### Task 4: 常用短语弹出层发音功能

**Files:**
- Modify: `frontend/pages/language/language.vue`

**Interfaces:**
- Consumes: `synthesizeSpeech(text, lang)` from `frontend/api/tts.ts`
- Consumes: `phraseResult` ref（`{ source, translated }`）、`phraseTargetLang` ref（目标语言代码）
- Consumes: `playAudio(url)` 函数（Task 3 中已定义）、`speaking` ref（Task 3 中已定义）

- [ ] **Step 1: 添加 speakPhrase() 函数**

在 `language.vue` 的 `<script setup>` 中，在 `closePhraseResult()` 函数之前添加：

```typescript
async function speakPhrase() {
  if (!phraseResult.value || speaking.value) return
  const supportedLangs = ['zh', 'en']
  if (!supportedLangs.includes(phraseTargetLang.value)) {
    uni.showToast({ title: '暂不支持该语言发音', icon: 'none' })
    return
  }
  let text = phraseResult.value.translated
  if (text.length > 150) {
    text = text.slice(0, 150)
    uni.showToast({ title: '文本过长，仅朗读前150字', icon: 'none' })
  }
  speaking.value = true
  try {
    const res = await synthesizeSpeech(text, phraseTargetLang.value)
    if (res.code === 200 && res.data?.audioUrl) {
      playAudio(res.data.audioUrl)
    } else {
      speaking.value = false
      uni.showToast({ title: '发音失败，请稍后再试', icon: 'none' })
    }
  } catch {
    speaking.value = false
    uni.showToast({ title: '发音失败，请稍后再试', icon: 'none' })
  }
}
```

- [ ] **Step 2: 在短语弹出层 sheet-actions 中添加🔊按钮**

找到 template 中的短语翻译弹出层 `sheet-actions`（约第 335 行）：

```html
<view class="sheet-actions">
  <button class="sheet-btn secondary" :style="{ fontSize: rpx(26) }" @click="copyPhraseResult">
    {{ phraseCopied ? '✓ 已复制' : '📋 复制' }}
  </button>
  <button class="sheet-btn primary" :style="{ fontSize: rpx(26) }" @click="usePhraseInText">
    在翻译页使用
  </button>
</view>
```

替换为：

```html
<view class="sheet-actions">
  <view class="icon-btn sheet-speak-btn" :class="{ speaking: speaking }" @click="speakPhrase">
    <text class="icon-btn-text">{{ speaking ? '⏸' : '🔊' }}</text>
  </view>
  <button class="sheet-btn secondary" :style="{ fontSize: rpx(26) }" @click="copyPhraseResult">
    {{ phraseCopied ? '✓ 已复制' : '📋 复制' }}
  </button>
  <button class="sheet-btn primary" :style="{ fontSize: rpx(26) }" @click="usePhraseInText">
    在翻译页使用
  </button>
</view>
```

- [ ] **Step 3: 添加 sheet-speak-btn 样式**

在 `<style scoped>` 的 `.sheet-actions` 部分后添加：

```css
.sheet-speak-btn {
  flex-shrink: 0;
  align-self: center;
}
```

- [ ] **Step 4: 在微信开发者工具中验证**

1. 切换到"常用短语"标签，目标语言选英文
2. 点击任意短语（如"你好"），弹出翻译结果弹窗
3. 点击🔊按钮
4. 预期：播放"Hello"的英文发音，按钮变⏸，结束后恢复🔊
5. 将目标语言改为日语，再点短语后点🔊
6. 预期：toast"暂不支持该语言发音"

- [ ] **Step 5: 提交**

```bash
git add frontend/pages/language/language.vue
git commit -m "feat(language): add speak button to phrase result sheet"
```
