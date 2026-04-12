<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'
import type { ImageConfig } from '../types/config'
import type { ResourceStore } from '../types/config'
import { renderToCanvas, hitTest } from '../composables/useCanvas'

const props = defineProps<{
  config: ImageConfig
  resources: ResourceStore
  selectedIndex: number | null
}>()

const emit = defineEmits<{
  (e: 'select', idx: number | null): void
  (e: 'move', idx: number, x: number, y: number): void
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
let dragging = false
let dragIdx = -1
let dragOffX = 0
let dragOffY = 0

function redraw() {
  if (!canvasRef.value) return
  renderToCanvas(canvasRef.value, props.config, props.resources, props.selectedIndex)
}

watch(() => [props.config, props.resources, props.selectedIndex], redraw, { deep: true })
onMounted(() => nextTick(redraw))

function onMouseDown(e: MouseEvent) {
  if (!canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  const scaleX = canvasRef.value.width / rect.width
  const scaleY = canvasRef.value.height / rect.height
  const x = (e.clientX - rect.left) * scaleX
  const y = (e.clientY - rect.top) * scaleY
  const ctx = canvasRef.value.getContext('2d')!
  const idx = hitTest(x, y, props.config, props.resources, ctx)
  emit('select', idx)
  if (idx !== null) {
    dragging = true
    dragIdx = idx
    const layer = props.config.layers[idx]
    dragOffX = x - layer.x
    dragOffY = y - layer.y
  }
}

function onMouseMove(e: MouseEvent) {
  if (!dragging || !canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  const scaleX = canvasRef.value.width / rect.width
  const scaleY = canvasRef.value.height / rect.height
  const x = Math.round((e.clientX - rect.left) * scaleX - dragOffX)
  const y = Math.round((e.clientY - rect.top) * scaleY - dragOffY)
  emit('move', dragIdx, x, y)
}

function onMouseUp() {
  dragging = false
}
</script>

<template>
  <div class="canvas-wrap">
    <canvas
      ref="canvasRef"
      @mousedown="onMouseDown"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
      @mouseleave="onMouseUp"
    />
  </div>
</template>

<style scoped>
.canvas-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  background: #111122;
  padding: 16px;
}
canvas {
  max-width: 100%;
  max-height: 100%;
  box-shadow: 0 4px 24px rgba(0,0,0,0.5);
  cursor: crosshair;
}
</style>
