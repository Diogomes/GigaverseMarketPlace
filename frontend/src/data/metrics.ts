import { isoDay } from '../lib/format'
import { listAdminProducts, listOrders, today } from './store'
import type { Order, OrderStatus, Product } from './types'

/**
 * Definições das métricas do dashboard (proposta D-P12 — validar com o proprietário).
 * Um pedido é considerado "pago" quando está em produção, enviado ou entregue.
 */
export const METRIC_DEFINITIONS = {
  sales:
    'Soma do total (produtos + frete) dos pedidos pagos criados no período. Não inclui pedidos aguardando pagamento nem cancelados.',
  orders: 'Quantidade de pedidos criados no período, em qualquer status.',
  avgTicket: 'Vendas do período divididas pelo número de pedidos pagos no período.',
  customers: 'Clientes distintos com ao menos um pedido pago no período.',
} as const

const PAID: OrderStatus[] = ['EM_PRODUCAO', 'ENVIADO', 'ENTREGUE']
export const isPaid = (o: Order) => PAID.includes(o.status)

export type PeriodDays = 7 | 30 | 90

function shiftDay(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number)
  return isoDay(new Date(y, m - 1, d + days))
}

function inRange(o: Order, from: string, to: string) {
  return o.createdAt >= from && o.createdAt <= to
}

type Kpis = { salesCents: number; orders: number; avgTicketCents: number; customers: number }

function kpisFor(orders: Order[]): Kpis {
  const paid = orders.filter(isPaid)
  const salesCents = paid.reduce((s, o) => s + o.totalCents, 0)
  return {
    salesCents,
    orders: orders.length,
    avgTicketCents: paid.length ? Math.round(salesCents / paid.length) : 0,
    customers: new Set(paid.map((o) => o.customerId)).size,
  }
}

/** Variação percentual em relação ao período anterior de mesmo tamanho; null se não há base. */
function delta(current: number, previous: number): number | null {
  if (previous === 0) return null
  return ((current - previous) / previous) * 100
}

export type DashboardData = {
  from: string
  to: string
  kpis: Kpis
  deltas: Record<keyof Kpis, number | null>
  dailySales: Array<{ day: string; cents: number }>
  statusCounts: Array<{ status: OrderStatus; count: number }>
  topProducts: Array<{ slug: string; name: string; quantity: number; revenueCents: number }>
  pendingOrders: Order[]
  lowStock: Product[]
}

export function getDashboard(period: PeriodDays): DashboardData {
  const to = today()
  const from = shiftDay(to, -(period - 1))
  const prevTo = shiftDay(from, -1)
  const prevFrom = shiftDay(prevTo, -(period - 1))
  const all = listOrders()
  const current = all.filter((o) => inRange(o, from, to))
  const previous = all.filter((o) => inRange(o, prevFrom, prevTo))
  const kpis = kpisFor(current)
  const prev = kpisFor(previous)

  const dailySales = Array.from({ length: period }, (_, i) => {
    const day = shiftDay(from, i)
    const cents = current.filter((o) => o.createdAt === day && isPaid(o)).reduce((s, o) => s + o.totalCents, 0)
    return { day, cents }
  })

  const statusOrder: OrderStatus[] = ['AGUARDANDO_PAGAMENTO', 'EM_PRODUCAO', 'ENVIADO', 'ENTREGUE', 'CANCELADO']
  const statusCounts = statusOrder.map((status) => ({
    status,
    count: current.filter((o) => o.status === status).length,
  }))

  const byProduct = new Map<string, { slug: string; name: string; quantity: number; revenueCents: number }>()
  for (const o of current.filter(isPaid)) {
    for (const it of o.items) {
      const row = byProduct.get(it.productSlug) ?? { slug: it.productSlug, name: it.name, quantity: 0, revenueCents: 0 }
      row.quantity += it.quantity
      row.revenueCents += it.unitCents * it.quantity
      byProduct.set(it.productSlug, row)
    }
  }
  const topProducts = [...byProduct.values()]
    .sort((a, b) => b.quantity - a.quantity || b.revenueCents - a.revenueCents)
    .slice(0, 5)

  const pendingOrders = all.filter((o) => o.status === 'AGUARDANDO_PAGAMENTO' || o.status === 'EM_PRODUCAO').slice(0, 5)
  const lowStock = listAdminProducts().filter(
    (p) => p.status === 'ATIVO' && p.type === 'PRONTO' && p.stock <= p.lowStockThreshold,
  )

  return {
    from,
    to,
    kpis,
    deltas: {
      salesCents: delta(kpis.salesCents, prev.salesCents),
      orders: delta(kpis.orders, prev.orders),
      avgTicketCents: delta(kpis.avgTicketCents, prev.avgTicketCents),
      customers: delta(kpis.customers, prev.customers),
    },
    dailySales,
    statusCounts,
    topProducts,
    pendingOrders,
    lowStock,
  }
}
