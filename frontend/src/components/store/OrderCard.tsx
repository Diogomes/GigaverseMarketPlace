import { Link } from 'react-router'
import type { OrderSummary } from '../../api/types'
import { formatDateTime, formatPrice } from '../../lib/format'
import { Badge } from '../ui/Badge'
import { buttonClass } from '../ui/button'
import { CatalogImage } from '../ui/CatalogImage'
import { ORDER_STATUS_UI } from './orderStatus'

export function OrderCard({ order }: { order: OrderSummary }) {
  const status = ORDER_STATUS_UI[order.status]
  const others = order.itemCount - 1
  return (
    <article className="rounded-card border border-border bg-surface p-4 shadow-card">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <h3 className="font-semibold">Pedido #{order.number}</h3>
        <span className="text-sm text-fg-muted">{formatDateTime(order.createdAt)}</span>
        <Badge tone={status.tone}>{status.label}</Badge>
        <span className="ml-auto font-semibold">{formatPrice(order.total)}</span>
      </div>
      <div className="mt-3 flex flex-wrap items-center gap-3">
        <CatalogImage
          slug={order.firstItemSlug}
          name={order.firstItemName}
          variant="square"
          fallbackSize="xs"
          className="size-12 shrink-0 rounded-lg"
        />
        <div className="min-w-0 flex-1 text-sm">
          <p className="truncate font-medium">{order.firstItemName}</p>
          <p className="text-fg-muted">
            {order.itemCount} {order.itemCount === 1 ? 'item' : 'itens'}
            {others > 0 && ` · e mais ${others}`}
          </p>
        </div>
        <Link to={`/conta/pedidos/${order.number}`} className={buttonClass('outline', 'sm')}>
          Ver detalhes
        </Link>
      </div>
    </article>
  )
}
