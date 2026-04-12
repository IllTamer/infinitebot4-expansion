<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import type { ImageConfig, LayerConfig, TextLayer, ImageLayer } from './types/config'
import { useResources } from './composables/useResources'
import EditorCanvas from './components/EditorCanvas.vue'
import LayerPanel from './components/LayerPanel.vue'
import PropertyPanel from './components/PropertyPanel.vue'
import ResourcePanel from './components/ResourcePanel.vue'
import ConfigOutput from './components/ConfigOutput.vue'

const { resources, imageFileNames, fontFileNames } = useResources()

const config = reactive<ImageConfig>({
  background: '',
  width: 800,
  height: 600,
  layers: [],
})

const selectedIndex = ref<number | null>(null)

const selectedLayer = computed<LayerConfig | null>(() =>
  selectedIndex.value !== null ? config.layers[selectedIndex.value] : null
)

// 图层操作
function addLayer(type: 'text' | 'image') {
  if (type === 'text') {
    config.layers.push({
      type: 'text',
      content: '文字内容',
      x: 50,
      y: 50,
      font: { file: '', size: 24, color: '#ffffff', style: 'plain' },
    } as TextLayer)
  } else {
    config.layers.push({
      type: 'image',
      file: '',
      x: 0,
      y: 0,
      width: 0,
      height: 0,
    } as ImageLayer)
  }
  selectedIndex.value = config.layers.length - 1
}

function removeLayer(idx: number) {
  config.layers.splice(idx, 1)
  if (selectedIndex.value !== null && selectedIndex.value >= config.layers.length) {
    selectedIndex.value = config.layers.length > 0 ? config.layers.length - 1 : null
  }
}

function moveLayerUp(idx: number) {
  if (idx <= 0) return
  const tmp = config.layers[idx]
  config.layers[idx] = config.layers[idx - 1]
  config.layers[idx - 1] = tmp
  if (selectedIndex.value === idx) selectedIndex.value = idx - 1
}

function moveLayerDown(idx: number) {
  if (idx >= config.layers.length - 1) return
  const tmp = config.layers[idx]
  config.layers[idx] = config.layers[idx + 1]
  config.layers[idx + 1] = tmp
  if (selectedIndex.value === idx) selectedIndex.value = idx + 1
}

// 属性更新
function updateBg(field: 'background' | 'width' | 'height', value: string | number) {
  (config as Record<string, unknown>)[field] = value
}

function updateLayer(field: string, value: unknown) {
  if (selectedIndex.value === null) return
  const layer = config.layers[selectedIndex.value] as Record<string, unknown>
  // 支持 "font.file" 这样的嵌套路径
  const parts = field.split('.')
  if (parts.length === 1) {
    layer[field] = value
  } else {
    let obj = layer
    for (let i = 0; i < parts.length - 1; i++) {
      obj = obj[parts[i]] as Record<string, unknown>
    }
    obj[parts[parts.length - 1]] = value
  }
}

// 拖拽移动
function onMove(idx: number, x: number, y: number) {
  const layer = config.layers[idx] as Record<string, unknown>
  layer.x = x
  layer.y = y
}
</script>

<template>
  <div class="app">
    <div class="topbar">
      <span class="title">MessageManager 图片编辑器</span>
      <span class="hint">拖拽图层可移动位置 · 点击图层选中编辑</span>
    </div>
    <ResourcePanel />
    <div class="main">
      <LayerPanel
        :layers="config.layers"
        :selected-index="selectedIndex"
        @select="selectedIndex = $event"
        @add="addLayer"
        @remove="removeLayer"
        @move-up="moveLayerUp"
        @move-down="moveLayerDown"
      />
      <div class="center">
        <EditorCanvas
          :config="config"
          :resources="resources"
          :selected-index="selectedIndex"
          @select="selectedIndex = $event"
          @move="onMove"
        />
        <ConfigOutput :config="config" />
      </div>
      <PropertyPanel
        :config="config"
        :selected-layer="selectedLayer"
        :image-files="imageFileNames"
        :font-files="fontFileNames"
        @update-bg="updateBg"
        @update-layer="updateLayer"
      />
    </div>
  </div>
</template>

<style scoped>
.app {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.topbar {
  background: #0f0f23;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #2a2a4e;
  flex-shrink: 0;
}
.title {
  font-size: 14px;
  font-weight: 700;
  color: #6c63ff;
}
.hint {
  font-size: 11px;
  color: #555;
}
.main {
  flex: 1;
  display: flex;
  overflow: hidden;
}
.center {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>
