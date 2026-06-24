# 独自出发功能设计文档

**日期：** 2026-06-24

## 目标

在匹配页新增"独自出发"按钮，允许用户跳过组队匹配直接进入旅行页，并在旅行页中适配无搭子的独行模式。

## 范围

- `frontend/pages/match/match.vue`：底部按钮区加"独自出发"
- `frontend/pages/trip/trip.vue`：读取 `solo` 参数，适配 UI 和逻辑

---

## match.vue 改动

### 底部按钮区

将现有单按钮布局改为两按钮并列：

| 按钮 | 禁用条件 | 点击行为 |
|------|----------|----------|
| 独自出发（次要样式） | `!selectedSpot` | `uni.redirectTo` 到 trip 页，带 `solo=true` |
| 开始匹配（主要样式） | `!selectedSpot` | 原 `startMatch()` 逻辑不变 |

跳转 URL：
```
/pages/trip/trip?spotId={id}&spotName={encoded}&solo=true
```

`bottom-bar` 改为 `display: flex; gap`，两个按钮各占 `flex: 1`。

---

## trip.vue 改动

### URL 参数

`onLoad` 新增读取 `solo` 参数：
```typescript
const isSolo = ref(false)
// onLoad:
isSolo.value = query?.solo === 'true'
```

### 逻辑适配

| 调用点 | 组队模式 | Solo 模式 |
|--------|----------|-----------|
| `onMounted` → `setMessageHandler` | 调用 | 跳过 |
| `updateMyLocation` → `sendMatch('location')` | 发送 | 跳过 |
| `onDrawEnd` → `sendMatch('drawStroke')` | 发送 | 跳过 |
| `eraseNear` → `sendMatch('eraseStroke')` | 发送 | 跳过 |
| `clearMyStrokes` → `sendMatch('eraseStroke')` | 发送 | 跳过 |
| `onUnmounted` → `disconnectMatch()` | 调用 | 跳过 |
| `leaveTrip` → `sendMatch('leave')` / `disconnectMatch()` | 调用 | 跳过 |

### UI 适配

- 面板"搭子"行：solo 时显示"独自出发"
- 面板"与搭子距离"行：solo 时隐藏（`v-if="!isSolo"`）
- 挑战任务：solo 时过滤掉 `near_partner` 和 `partner_active`，只显示 3 个个人任务

### 挑战任务过滤

```typescript
const visibleChallenges = computed(() =>
  isSolo.value
    ? challenges.value.filter(c => !['near_partner', 'partner_active'].includes(c.id))
    : challenges.value
)
```

template 中将 `v-for="c in challenges"` 改为 `v-for="c in visibleChallenges"`，进度条分母也改用 `visibleChallenges.length`。

---

## 错误处理

- solo 模式下涂鸦、位置等所有 `sendMatch` 调用均通过 `if (!isSolo.value)` 守卫，不会产生 WebSocket 错误
- `leaveTrip` 中跳过 disconnect，直接 `redirectTo` 首页

