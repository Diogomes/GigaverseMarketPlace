/**
 * Caminhos dos assets de demonstração em public/images (origem e regras em docs/IMAGE_ASSETS.md).
 * Quando as fotos reais chegarem (upload na Fase 04), a API passará a fornecer as URLs.
 */
export type ProductImageVariant = 'card' | 'square' | 'detail'

const VARIANTS: Record<ProductImageVariant, { suffix: string; width: number; height: number }> = {
  card: { suffix: 'card-600x750', width: 600, height: 750 },
  square: { suffix: 'square-800x800', width: 800, height: 800 },
  detail: { suffix: 'detail-1200x1200', width: 1200, height: 1200 },
}

export function productImage(base: string, variant: ProductImageVariant) {
  const v = VARIANTS[variant]
  return { src: `/images/products/${base}-${v.suffix}.jpg`, width: v.width, height: v.height }
}

export function categoryImage(base: string) {
  return { src: `/images/categories/${base}-category-900x600.jpg`, width: 900, height: 600 }
}

export const HERO_IMAGES = {
  main: {
    src: '/images/hero/hero-blue-dragon-1920x720.jpg',
    width: 1920,
    height: 720,
    alt: 'Escultura de dragão mecânico azul impressa em 3D (imagem ilustrativa)',
  },
  alternate: {
    src: '/images/hero/hero-fantasy-warrior-1920x720.jpg',
    width: 1920,
    height: 720,
    alt: 'Miniatura pintada de guerreira com escudo (imagem ilustrativa)',
  },
} as const

export const BANNERS = {
  collectibles: { src: '/images/banners/banner-collectibles-1600x600.jpg', width: 1600, height: 600 },
  articulated: { src: '/images/banners/banner-articulated-raptor-1600x600.jpg', width: 1600, height: 600 },
  customPainting: { src: '/images/banners/banner-custom-painting-1600x600.jpg', width: 1600, height: 600 },
} as const
