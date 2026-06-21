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
import { createScopedThreejs } from 'threejs-miniprogram'
import type { BadgeDTO } from '@/api/badge'

const props = defineProps<{ badge: BadgeDTO }>()

const canvasId = `badge3d-${props.badge.id}`

const RARITY_COLOR: Record<string, number> = {
  COMMON: 0x9e9e9e,
  RARE: 0x2196f3,
  EPIC: 0x9c27b0,
  LEGENDARY: 0xffd700
}

let THREE: any
let renderer: any
let scene: any
let camera: any
let coin: any
let rafId: number
let canvasNode: any   // keep reference for canvas.requestAnimationFrame / cancelAnimationFrame
let autoSpin = true
let touchPrev = { x: 0, y: 0 }

onMounted(() => {
  const query = uni.createSelectorQuery()
  query.select(`#${canvasId}`).node().exec((res: any[]) => {
    const canvas = res[0]?.node
    if (!canvas) return
    canvasNode = canvas
    initScene(canvas)
  })
})

onUnmounted(() => {
  if (rafId && canvasNode) canvasNode.cancelAnimationFrame(rafId)
  renderer?.dispose()
})

function initScene(canvas: any) {
  THREE = createScopedThreejs(canvas)

  const W = 600
  const H = 600
  canvas.width = W
  canvas.height = H

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x1a1a2e)

  camera = new THREE.PerspectiveCamera(40, W / H, 0.1, 100)
  camera.position.set(0, 0, 5)

  renderer = new THREE.WebGLRenderer({ canvas, antialias: true })
  renderer.setSize(W, H)
  renderer.setPixelRatio(1)

  scene.add(new THREE.AmbientLight(0xffffff, 0.6))
  const keyLight = new THREE.DirectionalLight(0xffffff, 1.2)
  keyLight.position.set(3, 4, 5)
  scene.add(keyLight)
  const rimLight = new THREE.DirectionalLight(0xffffff, 0.5)
  rimLight.position.set(-4, -2, -3)
  scene.add(rimLight)

  const geo = new THREE.CylinderGeometry(1.2, 1.2, 0.18, 64)
  const color = RARITY_COLOR[props.badge.rarity] ?? 0x9e9e9e

  // Build emoji texture via offscreen canvas
  const offscreen = wx.createOffscreenCanvas({ type: '2d', width: 256, height: 256 })
  const ctx = offscreen.getContext('2d')
  const hex = '#' + color.toString(16).padStart(6, '0')
  ctx.fillStyle = hex
  ctx.beginPath()
  ctx.arc(128, 128, 128, 0, Math.PI * 2)
  ctx.fill()
  ctx.font = '108px serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(props.badge.icon, 128, 128)
  const faceTexture = new THREE.CanvasTexture(offscreen)

  const sideMat = new THREE.MeshStandardMaterial({ color, metalness: 0.9, roughness: 0.15 })
  const frontMat = new THREE.MeshStandardMaterial({ map: faceTexture, metalness: 0.3, roughness: 0.4 })
  const backMat = new THREE.MeshStandardMaterial({ color: 0x222222, metalness: 0.8, roughness: 0.2 })

  coin = new THREE.Mesh(geo, [sideMat, frontMat, backMat])
  coin.rotation.x = Math.PI / 2
  scene.add(coin)

  const ringGeo = new THREE.TorusGeometry(1.5, 0.04, 8, 64)
  const ringMat = new THREE.MeshStandardMaterial({ color, metalness: 1, roughness: 0 })
  const ring = new THREE.Mesh(ringGeo, ringMat)
  ring.rotation.x = Math.PI / 2
  scene.add(ring)

  animate()
}

function animate() {
  // Must use canvas.requestAnimationFrame in WeChat Mini Program — the global RAF does not exist
  rafId = canvasNode.requestAnimationFrame(animate)
  if (autoSpin && coin) coin.rotation.z += 0.008
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
