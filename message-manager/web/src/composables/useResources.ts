import { ref, reactive } from 'vue'
import type { ResourceStore } from '../types/config'

const resources = reactive<ResourceStore>({
  images: new Map(),
  fonts: new Map(),
  imageDataUrls: new Map(),
})

const imageFileNames = ref<string[]>([])
const fontFileNames = ref<string[]>([])
const loading = ref(false)
const statusMsg = ref('')

async function loadImageFolder(files: FileList) {
  for (const file of Array.from(files)) {
    const lower = file.name.toLowerCase()
    if (!lower.match(/\.(png|jpg|jpeg|gif|webp)$/)) continue
    const dataUrl = await readFileAsDataUrl(file)
    resources.imageDataUrls.set(file.name, dataUrl)
    const img = new Image()
    img.src = dataUrl
    await new Promise<void>(resolve => { img.onload = () => resolve() })
    resources.images.set(file.name, img)
    if (!imageFileNames.value.includes(file.name)) {
      imageFileNames.value.push(file.name)
    }
  }
  statusMsg.value = `已加载 ${resources.images.size} 张图片`
}

async function loadFontFolder(files: FileList) {
  for (const file of Array.from(files)) {
    const lower = file.name.toLowerCase()
    if (!lower.match(/\.(ttf|otf)$/)) continue
    const dataUrl = await readFileAsDataUrl(file)
    const fontFace = new FontFace(file.name, `url(${dataUrl})`)
    try {
      await fontFace.load()
      document.fonts.add(fontFace)
      resources.fonts.set(file.name, fontFace)
      if (!fontFileNames.value.includes(file.name)) {
        fontFileNames.value.push(file.name)
      }
    } catch (e) {
      console.warn('字体加载失败:', file.name, e)
    }
  }
  statusMsg.value = `已加载 ${resources.fonts.size} 个字体`
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target!.result as string)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

export function useResources() {
  return {
    resources,
    imageFileNames,
    fontFileNames,
    loading,
    statusMsg,
    loadImageFolder,
    loadFontFolder,
  }
}
