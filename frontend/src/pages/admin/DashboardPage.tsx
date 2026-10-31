import {
  AlertTriangle,
  CircleDollarSign,
  Info,
  Receipt,
  ShoppingBag,
  TrendingDown,
  TrendingUp,
  Wallet,
  type LucideIcon,
} from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { useDashboard, type Kpis } from '../../api/admin'
import { Loading, QueryError } from '../../components/admin/AdminUi'
import { SalesChart } from '../../components/charts/SalesChart'
import { StatusBreakdown } from '../../components/charts/StatusBreakdown'
import { ORDER_STATUS_UI } from '../../components/store/orderStatus'
import { Badge } from '../../components/ui/Badge'
import { controlClass } from '../../components/ui/Field'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { formatDate, formatDateTime, formatPrice, isoDay } from '../../lib/format'

/** Definições exibidas nos cards — as mesmas de docs/METRICS.md e do backend (DashboardService). */
const METRIC_DEFINITIONS = {
  grossSales:
    'Soma do total (produtos + frete) dos pedidos com pagamento confirmado no período, pela data do pagamento. Inclui pedidos cancelados depois de pagos.',
  netSales:
    'Vendas brutas menos pedidos cancelados após o pagamento (estorno) e menos o frete: o valor dos produtos efetivamente vendidos.',
  paidOrders: 'Pedidos com pagamento confirmado no período que não foram cancelados.',
  averageTicket: 'Total (com frete) dos pedidos pagos válidos dividido pela quantidade desses pedidos.',
} as const

type Preset = '7' | '30' | '90' | 'custom'

function rangeFor(days: number): { from: string; to: string } {
  const to = new Date()
  const from = new Date(to.getFullYear(), to.getMonth(), to.getDate() - (days - 1))
  return { from: isoDay(from), to: isoDay(to) }
}

/** Variação percentual sobre o período anterior de mesmo tamanho; null quando não há base. */
function change(current: Kpis, previous: Kpis, key: keyof Kpis): number | null {
  if (previous[key] === 0) return null
  return ((current[key] - previous[key]) / previous[key]) * 100
}

function Kpi({
  icon: Icon,
  label,
  value,
  delta,
  definition,
  sub,
}: {
  icon: LucideIcon
  label: string
  value: string
  delta: number | null
  definition: string
  sub?: string
}) {
  const up = (delta ?? 0) >= 0
  return (
    <Card>
      <div className="flex items-start justify-between gap-3">
        <span className="inline-flex size-10 items-center justify-center rounded-lg bg-primary/10 text-accent">
          <Icon aria-hidden className="size-5" />
        </span>
        <span className="group relative">
          <button type="button" className="text-fg-muted hover:text-fg" aria-label={`Definição: ${label}`}>
            <Info aria-hidden className="size-4" />
          </button>
          <span
            role="tooltip"
            className="pointer-events-none absolute right-0 top-6 z-20 hidden w-64 rounded-lg border border-border bg-bg-elevated p-3 text-xs text-fg-muted shadow-card group-focus-within:block group-hover:block"
          >
            {definition}
          </span>
        </span>
      </div>
      <p className="mt-3 text-sm text-fg-muted">{label}</p>
      <p className="mt-1 text-2xl font-bold tabular-nums">{value}</p>
      {sub && <p className="text-xs text-fg-muted">{sub}</p>}
      <p className="mt-1 flex items-center gap-1 text-xs text-fg-muted">
        {delta === null ? (
          'Sem base de comparação'
        ) : (
          <>
            {up ? (
              <TrendingUp aria-hidden className="size-3.5 text-success" />
            ) : (
              <TrendingDown aria-hidden className="size-3.5 text-danger" />
            )}
            <span className={up ? 'text-success' : 'text-danger'}>
              {up ? '+' : ''}
              {delta.toFixed(0)}%
            </span>
            vs. período anterior
          </>
        )}
      </p>
    </Card>
  )
}

/** Tela 6 da referência. Todos os números vêm do backend (GET /api/v1/admin/dashboard). */
export function DashboardPage() {
  const [preset, setPreset] = useState<Preset>('30')
  const [custom, setCustom] = useState(rangeFor(30))
  const range = preset === 'custom' ? custom : rangeFor(Number(preset))
  const { data: d, error, isPending, refetch } = useDashboard(range.from, range.to)
  const customInvalid = preset === 'custom' && custom.to < custom.from

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Dashboard"
        description="Visão geral da sua loja."
        actions={
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <label className="flex items-center gap-2">
              <span className="text-fg-muted">Período</span>
              <select
                value={preset}
                onChange={(e) => setPreset(e.target.value as Preset)}
                className={`${controlClass} h-10 w-auto`}
              >
                <option value="7">Últimos 7 dias</option>
                <option value="30">Últimos 30 dias</option>
                <option value="90">Últimos 90 dias</option>
                <option value="custom">Personalizado</option>
              </select>
            </label>
            {preset === 'custom' && (
              <>
                <label>
                  <span className="sr-only">Data inicial</span>
                  <input
                    type="date"
                    value={custom.from}
                    max={custom.to}
                    onChange={(e) => e.target.value && setCustom({ ...custom, from: e.target.value })}
                    className={`${controlClass} h-10 w-auto`}
                  />
                </label>
                <span className="text-fg-muted">a</span>
                <label>
                  <span className="sr-only">Data final</span>
                  <input
                    type="date"
                    value={custom.to}
                    min={custom.from}
                    onChange={(e) => e.target.value && setCustom({ ...custom, to: e.target.value })}
                    className={`${controlClass} h-10 w-auto`}
                  />
                </label>
              </>
            )}
          </div>
        }
      />

      {customInvalid ? (
        <p role="alert" className="text-sm text-danger">
          A data final deve ser igual ou posterior à inicial.
        </p>
      ) : error ? (
        <QueryError error={error} onRetry={() => void refetch()} />
      ) : isPending || !d ? (
        <Loading label="Calculando métricas…" />
      ) : (
        <>
          <p className="text-xs text-fg-muted">
            {formatDate(d.from)} – {formatDate(d.to)} · comparado a {formatDate(d.previousFrom)} –{' '}
            {formatDate(d.previousTo)} · horário de Recife
          </p>

          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Kpi
              icon={CircleDollarSign}
              label="Vendas brutas"
              value={formatPrice(d.kpis.grossSales)}
              delta={change(d.kpis, d.previous, 'grossSales')}
              definition={METRIC_DEFINITIONS.grossSales}
              sub={
                d.kpis.cancelledAfterPayment > 0
                  ? `${formatPrice(d.kpis.cancelledAfterPayment)} cancelados após pagamento`
                  : undefined
              }
            />
            <Kpi
              icon={Wallet}
              label="Vendas líquidas"
              value={formatPrice(d.kpis.netSales)}
              delta={change(d.kpis, d.previous, 'netSales')}
              definition={METRIC_DEFINITIONS.netSales}
              sub={`Frete excluído: ${formatPrice(d.kpis.shipping)}`}
            />
            <Kpi
              icon={ShoppingBag}
              label="Pedidos pagos"
              value={String(d.kpis.paidOrders)}
              delta={change(d.kpis, d.previous, 'paidOrders')}
              definition={METRIC_DEFINITIONS.paidOrders}
              sub={`${d.kpis.ordersPlaced} pedidos criados no período`}
            />
            <Kpi
              icon={Receipt}
              label="Ticket médio"
              value={formatPrice(d.kpis.averageTicket)}
              delta={change(d.kpis, d.previous, 'averageTicket')}
              definition={METRIC_DEFINITIONS.averageTicket}
            />
          </div>

          <div className="grid gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
            <Card className="min-w-0">
              <h2 className="font-semibold">Vendas brutas por dia</h2>
              <p className="text-xs text-fg-muted">Pela data do pagamento, em R$</p>
              <div className="mt-4">
                <SalesChart data={d.sales.map((s) => ({ day: s.day, cents: Math.round(s.grossSales * 100) }))} />
              </div>
            </Card>
            <Card>
              <h2 className="font-semibold">Pedidos por etapa</h2>
              <div className="mt-4">
                <StatusBreakdown data={d.ordersByStatus} />
              </div>
            </Card>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Card>
              <h2 className="font-semibold">Mais vendidos</h2>
              {d.topProducts.length === 0 ? (
                <p className="mt-4 text-sm text-fg-muted">Sem vendas no período.</p>
              ) : (
                <ol className="mt-4 flex flex-col gap-3">
                  {d.topProducts.map((p) => (
                    <li key={`${p.productId}-${p.sku}`} className="text-sm">
                      <div className="flex justify-between gap-2">
                        <span className="truncate">{p.name}</span>
                        <span className="shrink-0 font-semibold tabular-nums">{p.quantity} un.</span>
                      </div>
                      <div className="mt-1 h-1.5 rounded-full bg-surface-muted">
                        <div
                          className="h-full rounded-full"
                          style={{
                            width: `${(p.quantity / Math.max(1, d.topProducts[0].quantity)) * 100}%`,
                            background: 'var(--viz-1)',
                          }}
                        />
                      </div>
                      <p className="mt-0.5 text-xs text-fg-muted">{formatPrice(p.revenue)}</p>
                    </li>
                  ))}
                </ol>
              )}
            </Card>
            <Card>
              <h2 className="font-semibold">Clientes</h2>
              <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
                <div>
                  <dt className="text-fg-muted">Compradores</dt>
                  <dd className="text-xl font-semibold tabular-nums">{d.customers.buyers}</dd>
                </div>
                <div>
                  <dt className="text-fg-muted">Novos cadastros</dt>
                  <dd className="text-xl font-semibold tabular-nums">{d.customers.signups}</dd>
                </div>
                <div>
                  <dt className="text-fg-muted">1ª compra</dt>
                  <dd className="text-xl font-semibold tabular-nums">{d.customers.newBuyers}</dd>
                </div>
                <div>
                  <dt className="text-fg-muted">Recorrentes</dt>
                  <dd className="text-xl font-semibold tabular-nums">{d.customers.returningBuyers}</dd>
                </div>
              </dl>
              <p className="mt-3 text-xs text-fg-muted">
                Recorrente: já tinha compra paga antes do período. 1ª compra: a primeira foi no período.
              </p>
            </Card>
            <Card>
              <div className="flex items-center justify-between">
                <h2 className="font-semibold">Aguardando ação</h2>
                <Link to="/admin/pedidos" className="text-xs font-semibold text-accent hover:underline">
                  Ver pedidos
                </Link>
              </div>
              <ul className="mt-3 flex flex-wrap gap-2">
                {d.awaitingAction.map((s) => (
                  <li key={s.status}>
                    <Link to={`/admin/pedidos?status=${s.status}`} className="hover:opacity-80">
                      <Badge tone={ORDER_STATUS_UI[s.status].tone}>
                        {ORDER_STATUS_UI[s.status].label}: {s.count}
                      </Badge>
                    </Link>
                  </li>
                ))}
              </ul>
              {d.oldestAwaiting.length === 0 ? (
                <p className="mt-4 text-sm text-fg-muted">Nenhum pedido pendente.</p>
              ) : (
                <ul className="mt-3 flex flex-col divide-y divide-border">
                  {d.oldestAwaiting.map((o) => (
                    <li key={o.number} className="py-2 text-sm">
                      <Link to={`/admin/pedidos/${o.number}`} className="flex justify-between gap-2 hover:text-accent">
                        <span>
                          <span className="block font-medium">#{o.number}</span>
                          <span className="text-xs text-fg-muted">
                            {o.customerName} · {formatDateTime(o.placedAt)}
                          </span>
                        </span>
                        <span className="shrink-0 tabular-nums">{formatPrice(o.total)}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
            <Card>
              <h2 className="flex items-center gap-2 font-semibold">
                <AlertTriangle aria-hidden className="size-4 text-amber-500" /> Estoque baixo
              </h2>
              {d.lowStock.length === 0 ? (
                <p className="mt-4 text-sm text-fg-muted">Nenhum item no mínimo ou abaixo.</p>
              ) : (
                <ul className="mt-4 flex flex-col divide-y divide-border">
                  {d.lowStock.map((p) => (
                    <li key={p.productId} className="flex items-center justify-between gap-2 py-2 text-sm">
                      <Link to={`/admin/produtos/${p.productId}`} className="min-w-0 hover:text-accent">
                        <span className="block truncate font-medium">{p.name}</span>
                        <span className="text-xs text-fg-muted">
                          {p.sku} · mínimo {p.threshold}
                        </span>
                      </Link>
                      <Badge tone={p.available === 0 ? 'danger' : 'warning'}>{p.available} disp.</Badge>
                    </li>
                  ))}
                </ul>
              )}
              {d.lowStockTotal > d.lowStock.length && (
                <Link to="/admin/estoque" className="mt-2 block text-xs font-semibold text-accent hover:underline">
                  Ver todos ({d.lowStockTotal})
                </Link>
              )}
            </Card>
          </div>
        </>
      )}
    </div>
  )
}
