<script setup lang="ts">
import type { LayerConfig } from '../types/config'

const props = defineProps<{
  layers: LayerConfig[]
  selectedIndex: number | null
}>()

const emit = defineEmits<{
  (e: 'select', idx: number): void
  (e: 'add', type: 'text' | 'image'): void
  (e: 'remove', idx: number): void
  (e: 'moveUp', idx: number): void
  (e: 'moveDown', idx: number): void
}>()
</script>

<template>
  <div class="layer-panel">
    <div class="panel-header">
      <span>图层</span>
      <div class="add-btns">
        <button class="btn-add" @click="emit('add', 'text')">+ 文字</button>
        <button class="btn-add" @click="emit('add', 'image')">+ 图片</button>
      </div>
    </div>
    <div class="layer-list">
      <div
        v-for="(layer, idx) in layers"
        :key="idx"
        class="layer-item"
        :class="{ selected: idx === selectedIndex }"
        @click="emit('select', idx)"
      >
        <span class="layer-icon">{{ layer.type === 'text' ? 'T' : '🖼' }}</span>
        <span class="layer-name">
          {{ layer.type === 'text' ? (layer.content || '文字图层') : (layer.file || '图片图层') }}
        </span>
        <div class="layer-actions">
          <button class="btn-icon" title="上移" @click.stop="emit('moveUp', idx)">↑</button>
          <button class="btn-icon" title="下移" @click.stop="emit('moveDown', idx)">↓</button>
          <button class="btn-icon btn-del" title="删除" @click.stop="emit('remove', idx)">✕</button>
        </div>
      </div>
      <div v-if="layers.length === 0" class="empty-hint">暂无图层，点击上方按钮添加</div>
    </div>
  </div>
</template>

<style scoped>
.layer-panel {
  width: 220px;
  background: #16213e;
  border-right: 1px solid #2a2a4e;
  display: flex;
  flex-direction: column;
}
.panel-header {
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 600;
  border-bottom: 1px solid #2a2a4e;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.add-btns {
  display: flex;
  gap: 4px;
}
.btn-add {
  background: #6c63ff;
  color: #fff;
  font-size: 11px;
  padding: 3px 7px;
}
.btn-add:hover { background: #5a52e0; }
.layer-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
}
.layer-item {
  display: flex;
  align-items: center;
  padding: 7px 12px;
  cursor: pointer;
  gap: 8px;
  font-size: 12px;
  border-left: 3px solid transparent;
}
.layer-item:hover { background: #1e2a4a; }
.layer-item.selected {
  background: #1e2a4a;
  border-left-color: #6c63ff;
}
.layer-icon {
  font-size: 13px;
  width: 18px;
  text-align: center;
  flex-shrink: 0;
}
.layer-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #ccc;
}
.layer-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.1s;
}
.layer-item:hover .layer-actions { opacity: 1; }
.btn-icon {
  background: #2a2a4e;
  color: #aaa;
  padding: 2px 5px;
  font-size: 11px;
}
.btn-icon:hover { background: #3a3a6e; color: #fff; }
.btn-del:hover { background: #8b2020; color: #fff; }
.empty-hint {
  text-align: center;
  color: #555;
  font-size: 12px;
  padding: 20px;
}
</style>
