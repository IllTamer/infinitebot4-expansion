import type { ImageConfig, LayerConfig, TextLayer, ImageLayer } from '../types/config'
import type { ResourceStore } from '../types/config'

export function renderToCanvas(
  canvas: HTMLCanvasElement,
  config: ImageConfig,
  resources: ResourceStore,
  selectedIndex: number | null
) {
  const ctx = canvas.getContext('2d')!
  const w = config.width || 800
  const h = config.height || 600
  canvas.width = w
  canvas.height = h

  ctx.clearRect(0, 0, w, h)

  // 背景
  const bgImg = resources.images.get(config.background)
  if (bgImg) {
    ctx.drawImage(bgImg, 0, 0, w, h)
  } else {
    ctx.fillStyle = '#2a2a3e'
    ctx.fillRect(0, 0, w, h)
    ctx.fillStyle = '#444'
    ctx.font = '14px sans-serif'
    ctx.fillText('背景图未加载: ' + config.background, 10, 20)
  }

  // 图层
  config.layers.forEach((layer, idx) => {
    if (layer.type === 'text') {
      drawTextLayer(ctx, layer as TextLayer, resources)
    } else if (layer.type === 'image') {
      drawImageLayer(ctx, layer as ImageLayer, resources)
    }

    // 选中高亮
    if (idx === selectedIndex) {
      const bounds = getLayerBounds(layer, resources, ctx)
      ctx.save()
      ctx.strokeStyle = '#6c63ff'
      ctx.lineWidth = 2
      ctx.setLineDash([5, 3])
      ctx.strokeRect(bounds.x - 2, bounds.y - 2, bounds.w + 4, bounds.h + 4)
      ctx.restore()
    }
  })
}

function drawTextLayer(ctx: CanvasRenderingContext2D, layer: TextLayer, resources: ResourceStore) {
  const f = layer.font
  const fontStyle = f.style === 'bold' ? 'bold'
    : f.style === 'italic' ? 'italic'
    : f.style === 'bold_italic' ? 'bold italic'
    : 'normal'
  // 使用已注册的字体名（FontFace 注册时用文件名作为 family）
  const fontFamily = resources.fonts.has(f.file) ? `"${f.file}"` : 'sans-serif'
  ctx.font = `${fontStyle} ${f.size}px ${fontFamily}`
  ctx.fillStyle = f.color || '#ffffff'
  ctx.fillText(layer.content || '', layer.x, layer.y)
}

function drawImageLayer(ctx: CanvasRenderingContext2D, layer: ImageLayer, resources: ResourceStore) {
  const img = resources.images.get(layer.file)
  if (!img) return
  const w = layer.width > 0 ? layer.width : img.naturalWidth
  const h = layer.height > 0 ? layer.height : img.naturalHeight
  ctx.drawImage(img, layer.x, layer.y, w, h)
}

function getLayerBounds(
  layer: LayerConfig,
  resources: ResourceStore,
  ctx: CanvasRenderingContext2D
): { x: number; y: number; w: number; h: number } {
  if (layer.type === 'text') {
    const tl = layer as TextLayer
    const f = tl.font
    const fontStyle = f.style === 'bold' ? 'bold' : f.style === 'italic' ? 'italic' : 'normal'
    const fontFamily = resources.fonts.has(f.file) ? `"${f.file}"` : 'sans-serif'
    ctx.font = `${fontStyle} ${f.size}px ${fontFamily}`
    const metrics = ctx.measureText(tl.content || '')
    return { x: tl.x, y: tl.y - f.size, w: metrics.width, h: f.size * 1.2 }
  } else {
    const il = layer as ImageLayer
    const img = resources.images.get(il.file)
    const w = il.width > 0 ? il.width : (img?.naturalWidth ?? 50)
    const h = il.height > 0 ? il.height : (img?.naturalHeight ?? 50)
    return { x: il.x, y: il.y, w, h }
  }
}

export function hitTest(
  x: number,
  y: number,
  config: ImageConfig,
  resources: ResourceStore,
  ctx: CanvasRenderingContext2D
): number | null {
  // 从最上层往下检测
  for (let i = config.layers.length - 1; i >= 0; i--) {
    const bounds = getLayerBounds(config.layers[i], resources, ctx)
    if (x >= bounds.x && x <= bounds.x + bounds.w && y >= bounds.y && y <= bounds.y + bounds.h) {
      return i
    }
  }
  return null
}
