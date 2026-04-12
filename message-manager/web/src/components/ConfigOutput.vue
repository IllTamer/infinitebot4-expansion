<script setup lang="ts">
import { ref } from 'vue'
import type { ImageConfig } from '../types/config'
import { exportYaml } from '../composables/useConfigExport'

const props = defineProps<{ config: ImageConfig }>()

const copied = ref(false)

function getYaml() {
  return exportYaml(props.config)
}

async function copyYaml() {
  try {
    await navigator.clipboard.writeText(getYaml())
    copied.value = true
    setTimeout(() => { copied.value = false }, 1500)
  } catch {
    // fallback
    const ta = document.createElement('textarea')
    ta.value = getYaml()
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    copied.value = true
    setTimeout(() => { copied.value = false }, 1500)
  }
}
</script>

<template>
  <div class="config-output">
    <div class="output-header">
      <span>配置预览</span>
      <button class="btn-copy" :class="{ copied }" @click="copyYaml">
        {{ copied ? '已复制!' : '复制配置' }}
      </button>
    </div>
    <pre class="yaml-code">{{ getYaml() }}</pre>
  </div>
</template>

<style scoped>
.config-output {
  background: #16213e;
  border-top: 1px solid #2a2a4e;
  display: flex;
  flex-direction: column;
  height: 200px;
}
.output-header {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  border-bottom: 1px solid #2a2a4e;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.btn-copy {
  background: #6c63ff;
  color: #fff;
  font-size: 11px;
  padding: 3px 10px;
}
.btn-copy:hover { background: #5a52e0; }
.btn-copy.copied { background: #2e7d32; }
.yaml-code {
  flex: 1;
  overflow: auto;
  padding: 8px 12px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  color: #a8d8a8;
  white-space: pre;
  line-height: 1.5;
}
</style>
