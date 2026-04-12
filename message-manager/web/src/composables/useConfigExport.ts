import type { ImageConfig, LayerConfig } from '../types/config'
import yaml from 'js-yaml'

export function exportYaml(config: ImageConfig): string {
  const obj: Record<string, unknown> = {
    background: config.background,
  }
  if (config.width > 0) obj.width = config.width
  if (config.height > 0) obj.height = config.height

  obj.layers = config.layers.map(layer => {
    if (layer.type === 'text') {
      return {
        type: 'text',
        content: layer.content,
        x: layer.x,
        y: layer.y,
        font: {
          file: layer.font.file,
          size: layer.font.size,
          color: layer.font.color,
          style: layer.font.style,
        },
      }
    } else {
      const il: Record<string, unknown> = {
        type: 'image',
        file: layer.file,
        x: layer.x,
        y: layer.y,
      }
      if (layer.width > 0) il.width = layer.width
      if (layer.height > 0) il.height = layer.height
      return il
    }
  })

  return yaml.dump({ image: obj }, { indent: 2, lineWidth: -1 })
}
