# Badge 3D Viewer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在点击已解锁勋章时弹出可手指旋转的 3D 金属硬币勋章查看器，替换原有的 2D BadgeCard 展示。

**Architecture:** 用 `threejs-miniprogram` 适配 Three.js 在微信小程序 WebGL Canvas 上运行，创建独立的 `Badge3DViewer.vue` 组件，内含圆柱硬币模型（正面 emoji 纹理、侧面稀有度金属色），手指拖拽旋转、松手后自动缓慢自旋。`badges.vue` 的详情弹窗直接嵌入该组件。

**Tech Stack:** `three` + `threejs-miniprogram`（WeChat Mini Program WebGL 适配层）；UniApp Vue 3 Composition API；`<canvas type="webgl">`；`wx.createOffscreenCanvas`（emoji 纹理）

---

## 文件清单

| 文件 | 操作 | 职责 |
|------|------|------|
| `frontend/package.json` | 修改 | 添加 `three` 和 `threejs-miniprogram` 依赖 |
| `frontend/components/Badge3DViewer/Badge3DViewer.vue` | 新建 | 3D 硬币渲染组件，含触摸旋转逻辑 |
| `frontend/pages/badges/badges.vue` | 修改 | 详情弹窗中用 Badge3DViewer 替换 BadgeCard |

---

## Task 1：安装依赖

**Files:**
- Modify: `frontend/package.json`

- [ ] **Step 1: 在 frontend/ 目录安装 three 和 threejs-miniprogram**

```powershell
cd "D:\code\作业\综合实践2\TripMate\frontend"
npm install three@0.160.0 threejs-miniprogram@0.4.3
```

预期输出：`added N packages`，无 ERROR。

- [ ] **Step 2: 确认 package.json 中出现两个依赖**

```powershell
Get-Content "D:\code\作业\综合实践2\TripMate\frontend\package.json"
```

预期：dependencies 中有 `"three"` 和 `"threejs-miniprogram"`。

- [ ] **Step 3: Commit**

```powershell
git -C "D:\code\作业\综合实践2\TripMate" add frontend/package.json frontend/package-lock.json
git -C "D:\code\作业\综合实践2\TripMate" commit -m "chore: add three.js and threejs-miniprogram dependencies"
```

---

## Task 2：Badge3DViewer 组件

**Files:**
- Create: `frontend/components/Badge3DViewer/Badge3DViewer.vue`

组件职责：
- 接收 `badge: BadgeDTO` prop
- 用 `<canvas type="webgl">` + `threejs-miniprogram` 渲染 3D 硬币
- 硬币正面：绘有 emoji 的 canvas 纹理；侧面：稀有度金属渐变色；背面：深色
- 触摸拖拽旋转；松手 2 秒后恢复自动慢旋

- [ ] **Step 1: 创建 Badge3DViewer.vue**

```vue
<!-- frontend/components/Badge3DViewer/Badge3DViewer.vue -->
<template>
  <view class="viewer">
    <canvas
      type="webgl"
      :id="canvasId"
      class="canvas"
      @touchstart="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
    />
    <text class="hint">拖动旋转</text>
  </view>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import type { BadgeDTO } from '@/api/badge'

const props = defineProps<{ badge: BadgeDTO }>()

// 用 badge.id 保证同页面多次挂载时 canvas id 唯一
const canvasId = `badge3d-${props.badge.id}`

const RARITY_COLOR: Record<string, number> = {
  COMMON: 0x9e9e9e,
  RARE: 0x2196f3,
  EPIC: 0x9c27b0,
  LEGENDARY: 0xffd700
}

// Three.js 运行时变量，不需要响应式
let THREE: any
let renderer: any
let scene: any
let camera: any
let coin: any
let rafId: number
let autoSpin = true
let touchPrev = { x: 0, y: 0 }

onMounted(() => {
  const query = uni.createSelectorQuery()
  query.select(`#${canvasId}`).node().exec((res: any[]) => {
    const canvas = res[0]?.node
    if (!canvas) return
    initScene(canvas)
  })
})

onUnmounted(() => {
  if (rafId) cancelAnimationFrame(rafId)
  renderer?.dispose()
})

function initScene(canvas: any) {
  const { createScopedThreejs } = require('threejs-miniprogram')
  THREE = createScopedThreejs(canvas)

  const W = 600
  const H = 600
  canvas.width = W
  canvas.height = H

  // Scene & background
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x1a1a2e)

  // Camera
  camera = new THREE.PerspectiveCamera(40, W / H, 0.1, 100)
  camera.position.set(0, 0, 5)

  // Renderer
  renderer = new THREE.WebGLRenderer({ canvas, antialias: true })
  renderer.setSize(W, H)
  renderer.setPixelRatio(1)

  // Lights
  scene.add(new THREE.AmbientLight(0xffffff, 0.6))
  const keyLight = new THREE.DirectionalLight(0xffffff, 1.2)
  keyLight.position.set(3, 4, 5)
  scene.add(keyLight)
  const rimLight = new THREE.DirectionalLight(0xffffff, 0.5)
  rimLight.position.set(-4, -2, -3)
  scene.add(rimLight)

  // Coin geometry: thin cylinder
  const geo = new THREE.CylinderGeometry(1.2, 1.2, 0.18, 64)

  // Rarity color
  const color = RARITY_COLOR[props.badge.rarity] ?? 0x9e9e9e

  // Emoji texture on offscreen 2D canvas
  const offscreen = wx.createOffscreenCanvas({ type: '2d', width: 256, height: 256 })
  const ctx = offscreen.getContext('2d')
  // Circle background with rarity color
  const hex = '#' + color.toString(16).padStart(6, '0')
  ctx.fillStyle = hex
  ctx.beginPath()
  ctx.arc(128, 128, 128, 0, Math.PI * 2)
  ctx.fill()
  // Emoji
  ctx.font = '108px serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(props.badge.icon, 128, 128)
  const faceTexture = new THREE.CanvasTexture(offscreen)

  // Material array: [side, top=front, bottom=back]
  const sideMat = new THREE.MeshStandardMaterial({
    color,
    metalness: 0.9,
    roughness: 0.15
  })
  const frontMat = new THREE.MeshStandardMaterial({
    map: faceTexture,
    metalness: 0.3,
    roughness: 0.4
  })
  const backMat = new THREE.MeshStandardMaterial({
    color: 0x222222,
    metalness: 0.8,
    roughness: 0.2
  })

  coin = new THREE.Mesh(geo, [sideMat, frontMat, backMat])
  // Rotate so front face (top of cylinder) faces camera
  coin.rotation.x = Math.PI / 2
  scene.add(coin)

  // Particle shimmer ring
  const ringGeo = new THREE.TorusGeometry(1.5, 0.04, 8, 64)
  const ringMat = new THREE.MeshStandardMaterial({ color, metalness: 1, roughness: 0 })
  const ring = new THREE.Mesh(ringGeo, ringMat)
  ring.rotation.x = Math.PI / 2
  scene.add(ring)

  animate()
}

function animate() {
  rafId = requestAnimationFrame(animate)
  if (autoSpin && coin) {
    coin.rotation.z += 0.008
  }
  renderer?.render(scene, camera)
}

function onTouchStart(e: any) {
  autoSpin = false
  const t = e.touches[0]
  touchPrev = { x: t.clientX, y: t.clientY }
}

function onTouchMove(e: any) {
  if (!coin) return
  const t = e.touches[0]
  const dx = t.clientX - touchPrev.x
  const dy = t.clientY - touchPrev.y
  coin.rotation.z += dx * 0.012
  coin.rotation.x += dy * 0.012
  touchPrev = { x: t.clientX, y: t.clientY }
}

function onTouchEnd() {
  setTimeout(() => { autoSpin = true }, 2000)
}
</script>

<style scoped>
.viewer {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}
.canvas {
  width: 300rpx;
  height: 300rpx;
}
.hint {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 8rpx;
}
</style>
```

- [ ] **Step 2: Commit**

```powershell
git -C "D:\code\作业\综合实践2\TripMate" add frontend/components/Badge3DViewer/Badge3DViewer.vue
git -C "D:\code\作业\综合实践2\TripMate" commit -m "feat: add Badge3DViewer component with Three.js WebGL coin"
```

---

## Task 3：badges.vue 弹窗集成 3D 查看器

**Files:**
- Modify: `frontend/pages/badges/badges.vue`

将现有 modal 中的 `<BadgeCard :badge="selectedBadge" size="large" />` 替换为 `<Badge3DViewer>`，并将弹窗背景改为深色以配合 3D 场景。

- [ ] **Step 1: 修改 badges.vue 的 modal 模板**

将 `<!-- 详情弹窗 -->` 整块替换为：

```html
<!-- 详情弹窗 -->
<view v-if="selectedBadge" class="modal-mask" @click.self="selectedBadge = null">
  <view class="modal">
    <Badge3DViewer :badge="selectedBadge" />
    <view class="modal-rarity-tag" :style="{ background: rarityColor(selectedBadge.rarity) }">
      {{ rarityLabel(selectedBadge.rarity) }}
    </view>
    <text class="modal-name">{{ selectedBadge.name }}</text>
    <text class="modal-desc">{{ selectedBadge.description }}</text>
    <text class="modal-date">🗓 获得于 {{ formatDate(selectedBadge.unlockedAt) }}</text>
    <text v-if="selectedBadge.note" class="modal-note">{{ selectedBadge.note }}</text>
    <view class="modal-close" @click="selectedBadge = null">关闭</view>
  </view>
</view>
```

- [ ] **Step 2: 在 script 中添加 Badge3DViewer 的 import**

在现有 import 列表末尾加一行：
```typescript
import Badge3DViewer from '@/components/Badge3DViewer/Badge3DViewer.vue'
```

- [ ] **Step 3: 更新 modal 的 CSS，改为深色背景以配合 3D 场景**

将 `.modal` 样式改为：
```css
.modal {
  background: #1a1a2e;
  border-radius: 24rpx;
  padding: 32rpx 40rpx 40rpx;
  width: 580rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.modal-name { font-size: 36rpx; font-weight: 700; color: #fff; margin-top: 16rpx; }
.modal-desc { font-size: 26rpx; color: rgba(255,255,255,0.7); margin-top: 12rpx; text-align: center; }
.modal-date { font-size: 24rpx; color: rgba(255,255,255,0.5); margin-top: 16rpx; }
.modal-note { font-size: 22rpx; color: rgba(255,255,255,0.4); margin-top: 8rpx; }
.modal-close {
  margin-top: 32rpx; padding: 16rpx 80rpx;
  background: rgba(255,255,255,0.15); color: #fff;
  border-radius: 40rpx; font-size: 28rpx;
}
```

- [ ] **Step 4: Commit**

```powershell
git -C "D:\code\作业\综合实践2\TripMate" add frontend/pages/badges/badges.vue
git -C "D:\code\作业\综合实践2\TripMate" commit -m "feat: integrate Badge3DViewer into badge detail modal"
```

---

## 自检

- [x] `canvasId` 使用 `badge-${badge.id}` 保证唯一性，多次打开不同勋章不冲突
- [x] `onUnmounted` 调用 `cancelAnimationFrame` + `renderer.dispose()` 防止内存泄漏
- [x] `CylinderGeometry` 材质数组顺序：[side=0, top=1, bottom=2]，与 Three.js 规范一致
- [x] `coin.rotation.x = Math.PI / 2` 使硬币正面（cylinder top）朝向相机
- [x] `autoSpin` 在 touchEnd 后延迟 2 秒恢复，避免触摸结束立即旋转
- [x] `wx.createOffscreenCanvas` 在 UniApp 编译到 MP-Weixin 后可用（wx 全局存在）
- [x] `require('threejs-miniprogram')` 在 UniApp 的 CommonJS 模块系统中有效
- [x] Badge3DViewer 是独立组件，badges.vue 通过 `v-if` 控制挂载/卸载，场景自动重建
