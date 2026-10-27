import { Link } from 'react-router'
import { ORDER_STATUS } from '../../data/labels'
import { imageForProduct } from '../../data/store'
import type { Order } from '../../data/types'
import { formatBRL, formatDate } from '../../lib/format'
import { Badge } from '../ui/Badge'
import { buttonClass } from '../ui/button'
import { ProductMedia } from '../ui/ProductMedia'

export function OrderCard({ order }: { order: Order }) {
  const status = ORDER_STATUS[order.status]
  const first = order.items[0]
  return (
    <article className="rounded-card border border-border bg-surface p-4 shadow-card">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <h3 className="font-semibold">Pedido #{order.number}</h3>
        <span className="text-sm text-fg-muted">{formatDate(order.createdAt)}</span>
        <Badge tone={status.tone}>{status.label}</Badge>
        <span className="ml-auto font-semibold">{formatBRL(order.totalCents)}</span>
      </div>
      <div className="mt-3 flex flex-wrap items-center gap-3">
        <ProductMedia
          name={first.name}
          image={imageForProduct(first.productSlug)}
          variant="square"
          fallbackSize="xs"
          className="size-12 shrink-0 rounded-lg"
        />
        <div className="min-w-0 flex-1 text-sm">
          <p className="truncate font-medium">{first.name}</p>
          <p className="truncate text-fg-muted">
            {first.variant ?? 'Padrão'}
            {order.items.length > 1 && ` · +${order.items.length - 1} item(ns)`}
          </p>
        </div>
        <Link to={`/conta/pedidos/${order.number}`} className={buttonClass('outline', 'sm')}>
          Ver detalhes
        </Link>
      </div>
    </article>
  )
}
