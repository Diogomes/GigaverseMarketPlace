import type { ImageSet } from '../../data/types'
import { cn } from '../../lib/cn'
import { productImage, type ProductImageVariant } from '../../lib/images'
import { ProductVisual } from './ProductVisual'

/**
 * Imagem do produto com dimensões declaradas (evita layout shift) e object-fit cover.
 * Sem imagem cadastrada, cai no placeholder identificado "Foto pendente".
 */
export function ProductMedia({
  name,
  image,
  variant,
  priority = false,
  fallbackVariant = 0,
  fallbackSize = 'md',
  className,
}: {
  name: string
  image?: ImageSet
  variant: ProductImageVariant
  priority?: boolean
  fallbackVariant?: number
  fallbackSize?: 'xs' | 'sm' | 'md' | 'lg'
  className?: string
}) {
  if (!image) {
    return <ProductVisual name={name} variant={fallbackVariant} size={fallbackSize} className={className} />
  }
  const { src, width, height } = productImage(image.base, variant)
  return (
    <img
      src={src}
      width={width}
      height={height}
      alt={image.alt}
      loading={priority ? 'eager' : 'lazy'}
      decoding="async"
      fetchPriority={priority ? 'high' : undefined}
      className={cn('bg-surface-muted object-cover', className)}
    />
  )
}
