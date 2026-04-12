export interface FontConfig {
  file: string
  size: number
  color: string
  style: 'plain' | 'bold' | 'italic' | 'bold_italic'
}

export interface TextLayer {
  type: 'text'
  content: string
  x: number
  y: number
  font: FontConfig
}

export interface ImageLayer {
  type: 'image'
  file: string
  x: number
  y: number
  width: number
  height: number
}

export type LayerConfig = TextLayer | ImageLayer

export interface ImageConfig {
  background: string
  width: number
  height: number
  layers: LayerConfig[]
}

export interface ResourceStore {
  images: Map<string, HTMLImageElement>
  fonts: Map<string, FontFace>
  imageDataUrls: Map<string, string>
}
