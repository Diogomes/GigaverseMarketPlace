import { Link } from 'react-router'
import { PRODUCT_TYPE } from '../../data/labels'
import { effectivePrice } from '../../data/store'
import type { Product } from '../../data/types'
import { formatBRL } from '../../lib/format'
import { Badge } from '../ui/Badge'
import { DemoTag } from '../ui/DemoTag'
import { ProductMedia } from '../ui/ProductMedia'

export function ProductCard({ product, index = 0 }: { product: Product; index?: number }) {
  const price = effectivePrice(product)
  return (
    <article className="group relative flex flex-col overflow-hidden rounded-card border border-border bg-surface shadow-card transition-colors hover:border-accent">
      <div className="relative overflow-hidden">
        {/* Cards em 4:5, como os recortes *-card-600x750 */}
        <ProductMedia
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
          {product.type !== 'PRONTO' && <Badge tone="accent">{PRODUCT_TYPE[product.type]}</Badge>}
          {product.promoPriceCents && <Badge tone="success">Promoção</Badge>}
        </div>
        <h3 className="font-semibold leading-snug">
          <Link to={`/produtos/${product.slug}`} className="after:absolute after:inset-0">
            {product.name}
          </Link>
        </h3>
        <p className="mt-auto flex items-baseline gap-2">
          <span className="text-lg font-bold text-accent">{formatBRL(price)}</span>
          {product.promoPriceCents && (
            <span className="text-sm text-fg-muted line-through">{formatBRL(product.priceCents)}</span>
          )}
        </p>
      </div>
    </article>
  )
}
