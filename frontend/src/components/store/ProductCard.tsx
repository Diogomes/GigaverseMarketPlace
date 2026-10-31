import { Link } from 'react-router'
import type { ProductSummary } from '../../api/types'
import { formatPrice } from '../../lib/format'
import { Badge } from '../ui/Badge'
import { CatalogImage } from '../ui/CatalogImage'
import { DemoTag } from '../ui/DemoTag'
import { AVAILABILITY } from './availability'

export function ProductCard({ product, index = 0 }: { product: ProductSummary; index?: number }) {
  const availability = AVAILABILITY[product.availability]
  return (
    <article className="group relative flex flex-col overflow-hidden rounded-card border border-border bg-surface shadow-card transition-colors hover:border-accent">
      <div className="relative overflow-hidden">
        {/* Cards em 4:5, como os recortes *-card-600x750 */}
        <CatalogImage
          slug={product.slug}
          name={product.name}
          image={product.image}
          variant="card"
          fallbackVariant={index}
          className="aspect-[4/5] w-full transition-transform duration-300 group-hover:scale-[1.03]"
        />
        <DemoTag className="absolute left-2 top-2" />
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <div className="flex flex-wrap gap-2">
          {product.availability !== 'IN_STOCK' && <Badge tone={availability.tone}>{availability.label}</Badge>}
          {product.promoPrice !== null && <Badge tone="success">Promoção</Badge>}
        </div>
        <h3 className="font-semibold leading-snug">
          <Link to={`/produtos/${product.slug}`} className="after:absolute after:inset-0">
            {product.name}
          </Link>
        </h3>
        <p className="mt-auto flex items-baseline gap-2">
          <span className="text-lg font-bold text-accent">{formatPrice(product.effectivePrice)}</span>
          {product.promoPrice !== null && (
            <span className="text-sm text-fg-muted line-through">{formatPrice(product.price)}</span>
          )}
        </p>
      </div>
    </article>
  )
}
