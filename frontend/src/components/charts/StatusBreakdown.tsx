import { useState } from 'react'
import type { OrderStatus } from '../../api/types'

/**
 * Os 7 status da API agrupados em 5 etapas, uma por cor da paleta categórica validada
 * (--viz-1…5). A cor segue a etapa, nunca a posição; nenhuma cor nova é gerada.
 */
const GROUPS: Array<{ key: string; label: string; detail?: string; statuses: OrderStatus[] }> = [
  { key: 'pending', label: 'Aguardando pagamento', statuses: ['PENDING_PAYMENT'] },
  { key: 'production', label: 'Pago / em produção', statuses: ['PAID', 'IN_PRODUCTION'] },
  {
    key: 'dispatch',
    label: 'Pronto ou enviado',
    detail: 'retirada ou entrega',
    statuses: ['READY_FOR_PICKUP', 'SHIPPED'],
  },
  { key: 'delivered', label: 'Entregue', statuses: ['DELIVERED'] },
  { key: 'cancelled', label: 'Cancelado', statuses: ['CANCELLED'] },
]

const COLOR: Record<string, string> = Object.fromEntries(GROUPS.map((g, i) => [g.key, `var(--viz-${i + 1})`]))

/** Parte-do-todo como barra empilhada única + legenda com rótulo, quantidade e percentual. */
export function StatusBreakdown({ data }: { data: Array<{ status: OrderStatus; count: number }> }) {
  const [hover, setHover] = useState<string | null>(null)
  const rows = GROUPS.map((g) => ({
    ...g,
    count: data.filter((d) => g.statuses.includes(d.status)).reduce((s, d) => s + d.count, 0),
  }))
  const total = rows.reduce((s, d) => s + d.count, 0)
  const pct = (n: number) => (total ? Math.round((n / total) * 100) : 0)
  const visible = rows.filter((d) => d.count > 0)

  return (
    <div>
      <p className="text-3xl font-bold tabular-nums">{total}</p>
      <p className="text-xs text-fg-muted">pedidos criados no período</p>
      {total > 0 && (
        <div
          className="mt-4 flex h-3 gap-[2px]"
          role="img"
          aria-label={`Distribuição por etapa: ${visible.map((d) => `${d.label} ${d.count}`).join(', ')}`}
        >
          {visible.map((d, i) => (
            <div
              key={d.key}
              title={`${d.label}: ${d.count} (${pct(d.count)}%)`}
              onPointerEnter={() => setHover(d.key)}
              onPointerLeave={() => setHover(null)}
              className={`h-full transition-opacity ${i === 0 ? 'rounded-l' : ''} ${i === visible.length - 1 ? 'rounded-r' : ''} ${hover && hover !== d.key ? 'opacity-40' : ''}`}
              style={{ width: `${(d.count / total) * 100}%`, background: COLOR[d.key] }}
            />
          ))}
        </div>
      )}
      <ul className="mt-4 flex flex-col gap-2 text-sm">
        {rows.map((d) => (
          <li
            key={d.key}
            onPointerEnter={() => setHover(d.key)}
            onPointerLeave={() => setHover(null)}
            className={`flex items-center gap-2 rounded px-1 ${hover === d.key ? 'bg-surface-muted' : ''}`}
          >
            <span aria-hidden className="size-2.5 shrink-0 rounded-full" style={{ background: COLOR[d.key] }} />
            <span className="flex-1 text-fg-muted">
              {d.label}
              {d.detail && <span className="text-xs"> ({d.detail})</span>}
            </span>
            <span className="font-semibold tabular-nums">{d.count}</span>
            <span className="w-10 text-right text-xs tabular-nums text-fg-muted">{pct(d.count)}%</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
