<script setup lang="ts">
import { ref } from 'vue'
import { useResources } from '../composables/useResources'

const { imageFileNames, fontFileNames, statusMsg, loadImageFolder, loadFontFolder } = useResources()

const imgInput = ref<HTMLInputElement | null>(null)
const fontInput = ref<HTMLInputElement | null>(null)

async function onImageFiles(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (files) await loadImageFolder(files)
}

async function onFontFiles(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (files) await loadFontFolder(files)
}
</script>

<template>
  <div class="resource-panel">
    <div class="res-section">
      <div class="res-title">
        <span>图片资源 ({{ imageFileNames.length }})</span>
        <button class="btn-load" @click="imgInput?.click()">选择文件夹</button>
        <input ref="imgInput" type="file" multiple accept=".png,.jpg,.jpeg,.gif,.webp"
          style="display:none" @change="onImageFiles" />
      </div>
      <div class="file-list">
        <div v-for="f in imageFileNames" :key="f" class="file-item">{{ f }}</div>
        <div v-if="imageFileNames.length === 0" class="empty">未加载</div>
      </div>
    </div>
    <div class="res-section">
      <div class="res-title">
        <span>字体资源 ({{ fontFileNames.length }})</span>
        <button class="btn-load" @click="fontInput?.click()">选择文件夹</button>
        <input ref="fontInput" type="file" multiple accept=".ttf,.otf"
          style="display:none" @change="onFontFiles" />
      </div>
      <div class="file-list">
        <div v-for="f in fontFileNames" :key="f" class="file-item">{{ f }}</div>
        <div v-if="fontFileNames.length === 0" class="empty">未加载</div>
      </div>
    </div>
    <div v-if="statusMsg" class="status">{{ statusMsg }}</div>
  </div>
</template>

<style scoped>
.resource-panel {
  background: #16213e;
  border-bottom: 1px solid #2a2a4e;
  display: flex;
  gap: 0;
  flex-shrink: 0;
}
.res-section {
  flex: 1;
  padding: 8px 12px;
  border-right: 1px solid #2a2a4e;
}
.res-section:last-of-type { border-right: none; }
.res-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #aaa;
}
.btn-load {
  background: #2a2a4e;
  color: #aaa;
  font-size: 11px;
  padding: 2px 8px;
}
.btn-load:hover { background: #3a3a6e; color: #fff; }
.file-list {
  max-height: 60px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.file-item {
  background: #2a2a4e;
  border-radius: 3px;
  padding: 2px 6px;
  font-size: 11px;
  color: #ccc;
}
.empty {
  font-size: 11px;
  color: #555;
}
.status {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 11px;
  color: #6c63ff;
}
</style>
