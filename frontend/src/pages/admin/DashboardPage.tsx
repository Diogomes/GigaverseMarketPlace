import {
  AlertTriangle,
  CircleDollarSign,
  Info,
  Receipt,
  ShoppingBag,
  TrendingDown,
  TrendingUp,
  Users,
  type LucideIcon,
} from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { SalesChart } from '../../components/charts/SalesChart'
import { StatusBreakdown } from '../../components/charts/StatusBreakdown'
import { Badge } from '../../components/ui/Badge'
import { DemoBanner } from '../../components/ui/DemoBanner'
import { controlClass } from '../../components/ui/Field'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { ORDER_STATUS } from '../../data/labels'
import { getDashboard, METRIC_DEFINITIONS, type PeriodDays } from '../../data/metrics'
import { getCustomer } from '../../data/store'
import { formatBRL, formatDate } from '../../lib/format'

function Kpi({
  icon: Icon,
  label,
  value,
  delta,
  definition,
}: {
  icon: LucideIcon
  label: string
  value: string
  delta: number | null
  definition: string
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

/** Tela 6 da referência. Métricas calculadas a partir dos pedidos (definições em METRIC_DEFINITIONS). */
export function DashboardPage() {
  const [period, setPeriod] = useState<PeriodDays>(30)
  const d = getDashboard(period)
  const maxQty = Math.max(1, ...d.topProducts.map((p) => p.quantity))

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Dashboard"
        description="Visão geral da sua loja."
        actions={
          <label className="flex items-center gap-2 text-sm">
            <span className="text-fg-muted">Período</span>
            <select
              value={period}
              onChange={(e) => setPeriod(Number(e.target.value) as PeriodDays)}
              className={`${controlClass} h-10 w-auto`}
            >
              <option value={7}>Últimos 7 dias</option>
              <option value={30}>Últimos 30 dias</option>
              <option value={90}>Últimos 90 dias</option>
            </select>
          </label>
        }
      />
      <DemoBanner>Os números abaixo são calculados sobre pedidos fictícios.</DemoBanner>
      <p className="text-xs text-fg-muted">
        {formatDate(d.from)} – {formatDate(d.to)}
      </p>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Kpi
          icon={CircleDollarSign}
          label="Vendas"
          value={formatBRL(d.kpis.salesCents)}
          delta={d.deltas.salesCents}
          definition={METRIC_DEFINITIONS.sales}
        />
        <Kpi
          icon={ShoppingBag}
          label="Pedidos"
          value={String(d.kpis.orders)}
          delta={d.deltas.orders}
          definition={METRIC_DEFINITIONS.orders}
        />
        <Kpi
          icon={Receipt}
          label="Ticket médio"
          value={formatBRL(d.kpis.avgTicketCents)}
          delta={d.deltas.avgTicketCents}
          definition={METRIC_DEFINITIONS.avgTicket}
        />
        <Kpi
          icon={Users}
          label="Clientes compradores"
          value={String(d.kpis.customers)}
          delta={d.deltas.customers}
          definition={METRIC_DEFINITIONS.customers}
        />
      </div>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <Card className="min-w-0">
          <h2 className="font-semibold">Vendas por dia</h2>
          <p className="text-xs text-fg-muted">Pedidos pagos, em R$</p>
          <div className="mt-4">
            <SalesChart data={d.dailySales} />
          </div>
        </Card>
        <Card>
          <h2 className="font-semibold">Status dos pedidos</h2>
          <div className="mt-4">
            <StatusBreakdown data={d.statusCounts} />
          </div>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card>
          <h2 className="font-semibold">Mais vendidos</h2>
          {d.topProducts.length === 0 ? (
            <p className="mt-4 text-sm text-fg-muted">Sem vendas no período.</p>
          ) : (
            <ol className="mt-4 flex flex-col gap-3">
              {d.topProducts.map((p) => (
                <li key={p.slug} className="text-sm">
                  <div className="flex justify-between gap-2">
                    <span className="truncate">{p.name}</span>
                    <span className="shrink-0 font-semibold tabular-nums">{p.quantity} un.</span>
                  </div>
                  <div className="mt-1 h-1.5 rounded-full bg-surface-muted">
                    <div
                      className="h-full rounded-full"
                      style={{ width: `${(p.quantity / maxQty) * 100}%`, background: 'var(--viz-1)' }}
                    />
                  </div>
                </li>
              ))}
            </ol>
          )}
        </Card>
        <Card>
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">Aguardando ação</h2>
            <Link to="/admin/pedidos" className="text-xs font-semibold text-accent hover:underline">
              Ver pedidos
            </Link>
          </div>
          {d.pendingOrders.length === 0 ? (
            <p className="mt-4 text-sm text-fg-muted">Nenhum pedido pendente.</p>
          ) : (
            <ul className="mt-4 flex flex-col divide-y divide-border">
              {d.pendingOrders.map((o) => (
                <li key={o.number} className="flex items-center justify-between gap-2 py-2 text-sm">
                  <span>
                    <span className="block font-medium">#{o.number}</span>
                    <span className="text-xs text-fg-muted">{getCustomer(o.customerId)?.name}</span>
                  </span>
                  <Badge tone={ORDER_STATUS[o.status].tone}>{ORDER_STATUS[o.status].label}</Badge>
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
            <p className="mt-4 text-sm text-fg-muted">Nenhum item abaixo do mínimo.</p>
          ) : (
            <ul className="mt-4 flex flex-col divide-y divide-border">
              {d.lowStock.map((p) => (
                <li key={p.id} className="flex items-center justify-between gap-2 py-2 text-sm">
                  <span>
                    <span className="block font-medium">{p.name}</span>
                    <span className="text-xs text-fg-muted">
                      {p.sku} · mínimo {p.lowStockThreshold}
                    </span>
                  </span>
                  <Badge tone={p.stock === 0 ? 'danger' : 'warning'}>{p.stock} un.</Badge>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  )
}
