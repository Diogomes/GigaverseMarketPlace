import { useState } from 'react'
import { ORDER_STATUS, ORDER_STATUS_ORDER } from '../../data/labels'
import type { OrderStatus } from '../../data/types'

/** Cor fixa por status (segue a entidade, nunca a posição). */
const STATUS_COLOR: Record<OrderStatus, string> = Object.fromEntries(
  ORDER_STATUS_ORDER.map((s, i) => [s, `var(--viz-${i + 1})`]),
) as Record<OrderStatus, string>

/** Parte-do-todo como barra empilhada única + legenda com rótulo, quantidade e percentual. */
export function StatusBreakdown({ data }: { data: Array<{ status: OrderStatus; count: number }> }) {
  const [hover, setHover] = useState<OrderStatus | null>(null)
  const total = data.reduce((s, d) => s + d.count, 0)
  const pct = (n: number) => (total ? Math.round((n / total) * 100) : 0)
  const visible = data.filter((d) => d.count > 0)

  return (
    <div>
      <p className="text-3xl font-bold tabular-nums">{total}</p>
      <p className="text-xs text-fg-muted">pedidos no período</p>
      {total > 0 && (
        <div className="mt-4 flex h-3 gap-[2px]" role="img" aria-label="Distribuição de pedidos por status">
          {visible.map((d, i) => (
            <div
              key={d.status}
              onPointerEnter={() => setHover(d.status)}
              onPointerLeave={() => setHover(null)}
              className={`h-full transition-opacity ${i === 0 ? 'rounded-l' : ''} ${i === visible.length - 1 ? 'rounded-r' : ''} ${hover && hover !== d.status ? 'opacity-40' : ''}`}
              style={{ width: `${(d.count / total) * 100}%`, background: STATUS_COLOR[d.status] }}
            />
          ))}
        </div>
      )}
      <ul className="mt-4 flex flex-col gap-2 text-sm">
        {data.map((d) => (
          <li
            key={d.status}
            onPointerEnter={() => setHover(d.status)}
            onPointerLeave={() => setHover(null)}
            className={`flex items-center gap-2 rounded px-1 ${hover === d.status ? 'bg-surface-muted' : ''}`}
          >
            <span aria-hidden className="size-2.5 rounded-full" style={{ background: STATUS_COLOR[d.status] }} />
            <span className="flex-1 text-fg-muted">{ORDER_STATUS[d.status].label}</span>
            <span className="font-semibold tabular-nums">{d.count}</span>
            <span className="w-10 text-right text-xs tabular-nums text-fg-muted">{pct(d.count)}%</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
