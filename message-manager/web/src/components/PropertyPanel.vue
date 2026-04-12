<script setup lang="ts">
import type { LayerConfig, TextLayer, ImageLayer, ImageConfig } from '../types/config'

const props = defineProps<{
  config: ImageConfig
  selectedLayer: LayerConfig | null
  imageFiles: string[]
  fontFiles: string[]
}>()

const emit = defineEmits<{
  (e: 'updateBg', field: 'background' | 'width' | 'height', value: string | number): void
  (e: 'updateLayer', field: string, value: unknown): void
}>()

function isText(layer: LayerConfig | null): layer is TextLayer {
  return layer?.type === 'text'
}
function isImage(layer: LayerConfig | null): layer is ImageLayer {
  return layer?.type === 'image'
}
</script>

<template>
  <div class="prop-panel">
    <div class="panel-header">属性</div>
    <div class="prop-content">
      <!-- 背景属性 -->
      <div class="section-title">画布</div>
      <div class="field">
        <label>背景图片</label>
        <select :value="config.background" @change="emit('updateBg', 'background', ($event.target as HTMLSelectElement).value)">
          <option value="">-- 无背景 --</option>
          <option v-for="f in imageFiles" :key="f" :value="f">{{ f }}</option>
        </select>
      </div>
      <div class="row2">
        <div class="field">
          <label>宽度 (px)</label>
          <input type="number" :value="config.width" min="1"
            @change="emit('updateBg', 'width', Number(($event.target as HTMLInputElement).value))" />
        </div>
        <div class="field">
          <label>高度 (px)</label>
          <input type="number" :value="config.height" min="1"
            @change="emit('updateBg', 'height', Number(($event.target as HTMLInputElement).value))" />
        </div>
      </div>

      <template v-if="selectedLayer">
        <div class="divider" />
        <div class="section-title">{{ selectedLayer.type === 'text' ? '文字图层' : '图片图层' }}</div>

        <!-- 坐标 -->
        <div class="row2">
          <div class="field">
            <label>X</label>
            <input type="number" :value="selectedLayer.x"
              @change="emit('updateLayer', 'x', Number(($event.target as HTMLInputElement).value))" />
          </div>
          <div class="field">
            <label>Y</label>
            <input type="number" :value="selectedLayer.y"
              @change="emit('updateLayer', 'y', Number(($event.target as HTMLInputElement).value))" />
          </div>
        </div>

        <!-- 文字图层属性 -->
        <template v-if="isText(selectedLayer)">
          <div class="field">
            <label>内容（支持 %placeholder%）</label>
            <input type="text" :value="selectedLayer.content"
              @input="emit('updateLayer', 'content', ($event.target as HTMLInputElement).value)" />
          </div>
          <div class="field">
            <label>字体文件</label>
            <select :value="selectedLayer.font.file"
              @change="emit('updateLayer', 'font.file', ($event.target as HTMLSelectElement).value)">
              <option value="">-- 系统默认 --</option>
              <option v-for="f in fontFiles" :key="f" :value="f">{{ f }}</option>
            </select>
          </div>
          <div class="row2">
            <div class="field">
              <label>字号</label>
              <input type="number" :value="selectedLayer.font.size" min="6" max="200"
                @change="emit('updateLayer', 'font.size', Number(($event.target as HTMLInputElement).value))" />
            </div>
            <div class="field">
              <label>样式</label>
              <select :value="selectedLayer.font.style"
                @change="emit('updateLayer', 'font.style', ($event.target as HTMLSelectElement).value)">
                <option value="plain">plain</option>
                <option value="bold">bold</option>
                <option value="italic">italic</option>
                <option value="bold_italic">bold_italic</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label>颜色</label>
            <div class="color-row">
              <input type="color" :value="selectedLayer.font.color.length === 7 ? selectedLayer.font.color : '#ffffff'"
                @input="emit('updateLayer', 'font.color', ($event.target as HTMLInputElement).value)" />
              <input type="text" :value="selectedLayer.font.color"
                @input="emit('updateLayer', 'font.color', ($event.target as HTMLInputElement).value)" />
            </div>
          </div>
        </template>

        <!-- 图片图层属性 -->
        <template v-if="isImage(selectedLayer)">
          <div class="field">
            <label>图片文件</label>
            <select :value="selectedLayer.file"
              @change="emit('updateLayer', 'file', ($event.target as HTMLSelectElement).value)">
              <option value="">-- 选择图片 --</option>
              <option v-for="f in imageFiles" :key="f" :value="f">{{ f }}</option>
            </select>
          </div>
          <div class="row2">
            <div class="field">
              <label>宽度 (0=原始)</label>
              <input type="number" :value="selectedLayer.width" min="0"
                @change="emit('updateLayer', 'width', Number(($event.target as HTMLInputElement).value))" />
            </div>
            <div class="field">
              <label>高度 (0=原始)</label>
              <input type="number" :value="selectedLayer.height" min="0"
                @change="emit('updateLayer', 'height', Number(($event.target as HTMLInputElement).value))" />
            </div>
          </div>
        </template>
      </template>

      <div v-else class="empty-hint">点击画布或图层列表选中图层</div>
    </div>
  </div>
</template>

<style scoped>
.prop-panel {
  width: 240px;
  background: #16213e;
  border-left: 1px solid #2a2a4e;
  display: flex;
  flex-direction: column;
}
.panel-header {
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 600;
  border-bottom: 1px solid #2a2a4e;
}
.prop-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}
.section-title {
  font-size: 11px;
  color: #6c63ff;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
  font-weight: 600;
}
.divider {
  border-top: 1px solid #2a2a4e;
  margin: 12px 0;
}
.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.color-row {
  display: flex;
  gap: 6px;
  align-items: center;
}
.color-row input[type="color"] {
  width: 36px;
  height: 28px;
  padding: 2px;
  flex-shrink: 0;
  cursor: pointer;
}
.empty-hint {
  text-align: center;
  color: #555;
  font-size: 12px;
  padding: 20px 0;
}
</style>
