import type { ApiImage } from '../../api/types'
import { demoImageFor } from '../../data/store'
import { cn } from '../../lib/cn'
import { productImage, type ProductImageVariant } from '../../lib/images'
import { ProductVisual } from './ProductVisual'

/**
 * Imagem de produto do catálogo real: usa a foto enviada pela loja (API); em desenvolvimento,
 * sem foto cadastrada, usa a imagem de demonstração do mesmo slug; senão, placeholder identificado.
 */
export function CatalogImage({
  slug,
  name,
  image,
  variant,
  priority = false,
  fallbackVariant = 0,
  fallbackSize = 'md',
  className,
}: {
  slug: string | null
  name: string
  image?: ApiImage | null
  variant: ProductImageVariant
  priority?: boolean
  fallbackVariant?: number
  fallbackSize?: 'xs' | 'sm' | 'md' | 'lg'
  className?: string
}) {
  const loading = priority ? 'eager' : 'lazy'
  const classes = cn('bg-surface-muted object-cover', className)
  if (image) {
    return (
      <img
        src={image.url}
        alt={image.alt}
        width={image.width ?? undefined}
        height={image.height ?? undefined}
        loading={loading}
        decoding="async"
        className={classes}
      />
    )
  }
  const demo = demoImageFor(slug)
  if (demo) {
    const { src, width, height } = productImage(demo.base, variant)
    return (
      <img
        src={src}
        alt={demo.alt}
        width={width}
        height={height}
        loading={loading}
        decoding="async"
        fetchPriority={priority ? 'high' : undefined}
        className={classes}
      />
    )
  }
  return <ProductVisual name={name} variant={fallbackVariant} size={fallbackSize} className={className} />
}
