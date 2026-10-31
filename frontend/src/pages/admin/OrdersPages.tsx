import { CheckCircle2, Download, ShoppingBag } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { exportOrders, useAdminOrder, useAdminOrders, useOrderActions, type AdminOrder } from '../../api/admin'
import type { OrderStatus } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import {
  ActionError,
  errorMessage,
  FilterSelect,
  Loading,
  Pagination,
  QueryError,
  rowClass,
  SearchInput,
  tableWrapClass,
  theadClass,
} from '../../components/admin/AdminUi'
import { ORDER_STATUS_UI } from '../../components/store/orderStatus'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { EmptyState } from '../../components/ui/EmptyState'
import { controlClass } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { ApiError } from '../../lib/api'
import { formatDateTime, formatPrice } from '../../lib/format'
import { useDebounced } from '../../lib/useDebounced'
import { NotFoundPage } from '../PlaceholderPage'

const PAYMENT = { PIX: 'PIX', CARD: 'Cartão' } as const
const SHIPPING = { DELIVERY: 'Entrega', PICKUP: 'Retirada' } as const

/** Lista filtrável por status, período (data de criação) e cliente/número. */
export function OrdersPage() {
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const status = (params.get('status') ?? '') as OrderStatus | ''
  const customerId = params.get('cliente') ? Number(params.get('cliente')) : undefined
  const [q, setQ] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)
  const [failure, setFailure] = useState<string | null>(null)
  const search = useDebounced(q.trim())
  const filters = {
    status: status || undefined,
    q: search || undefined,
    customerId,
    from: from || undefined,
    to: to || undefined,
  }
  const { data, error, isPending, refetch } = useAdminOrders({ ...filters, page, size: 20 })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Pedidos"
        description="Acompanhe e atualize os pedidos da loja."
        actions={
          user?.role === 'ADMIN' && (
            <button
              type="button"
              className={buttonClass('outline')}
              onClick={() => {
                setFailure(null)
                exportOrders(filters).catch((e) => setFailure(errorMessage(e)))
              }}
            >
              <Download aria-hidden className="size-4" /> Exportar CSV
            </button>
          )
        }
      />
      <ActionError message={failure} />
      {customerId && (
        <Notice>
          Mostrando pedidos de um cliente.{' '}
          <button
            type="button"
            className="font-semibold text-accent hover:underline"
            onClick={() => {
              params.delete('cliente')
              setParams(params)
            }}
          >
            Ver todos
          </button>
        </Notice>
      )}
      <div className="grid gap-3 md:grid-cols-[1fr_auto_auto_auto]">
        <SearchInput
          value={q}
          onChange={(v) => {
            setQ(v)
            setPage(0)
          }}
          label="Buscar por número, nome ou e-mail"
          placeholder="Número, nome ou e-mail…"
        />
        <FilterSelect
          label="Status"
          value={status}
          onChange={(v) => {
            if (v) params.set('status', v)
            else params.delete('status')
            setParams(params)
            setPage(0)
          }}
        >
          <option value="">Todos os status</option>
          {Object.entries(ORDER_STATUS_UI).map(([k, v]) => (
            <option key={k} value={k}>
              {v.label}
            </option>
          ))}
        </FilterSelect>
        <label>
          <span className="sr-only">Criados a partir de</span>
          <input
            type="date"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value)
              setPage(0)
            }}
            className={`${controlClass} h-10`}
          />
        </label>
        <label>
          <span className="sr-only">Criados até</span>
          <input
            type="date"
            value={to}
            onChange={(e) => {
              setTo(e.target.value)
              setPage(0)
            }}
            className={`${controlClass} h-10`}
          />
        </label>
      </div>

      {error ? (
        <QueryError error={error} onRetry={() => void refetch()} />
      ) : isPending ? (
        <Loading />
      ) : data.content.length === 0 ? (
        <EmptyState icon={ShoppingBag} title="Nenhum pedido encontrado" description="Ajuste a busca ou os filtros." />
      ) : (
        <>
          <div className={tableWrapClass}>
            <table className="w-full min-w-[48rem] text-sm">
              <caption className="sr-only">Pedidos</caption>
              <thead className={theadClass}>
                <tr>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Pedido
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Cliente
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Pagamento / entrega
                  </th>
                  <th scope="col" className="px-4 py-3 text-right font-medium">
                    Total
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((o) => (
                  <tr key={o.number} className={rowClass}>
                    <td className="px-4 py-3">
                      <Link to={`/admin/pedidos/${o.number}`} className="font-medium text-accent hover:underline">
                        #{o.number}
                      </Link>
                      <span className="block text-xs text-fg-muted">{formatDateTime(o.createdAt)}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="block">{o.customerName}</span>
                      <span className="text-xs text-fg-muted">{o.customerEmail}</span>
                    </td>
                    <td className="px-4 py-3 text-fg-muted">
                      {PAYMENT[o.paymentMethod]} · {SHIPPING[o.shippingMethod]}
                      <span className="block text-xs">
                        {o.itemCount} {o.itemCount === 1 ? 'item' : 'itens'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">{formatPrice(o.total)}</td>
                    <td className="px-4 py-3">
                      <Badge tone={ORDER_STATUS_UI[o.status].tone}>{ORDER_STATUS_UI[o.status].label}</Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPage={setPage}
          />
        </>
      )}
    </div>
  )
}

export function OrderDetailPage() {
  const { number = '' } = useParams()
  const { data, error, isPending, refetch } = useAdminOrder(number)
  if (error instanceof ApiError && error.status === 404) return <NotFoundPage />
  if (error) return <QueryError error={error} onRetry={() => void refetch()} />
  if (isPending) return <Loading />
  return <OrderDetail key={data.order.number} data={data} />
}

function OrderDetail({ data }: { data: AdminOrder }) {
  const { order } = data
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const actions = useOrderActions(order.number)
  const [target, setTarget] = useState<OrderStatus | ''>('')
  const [reason, setReason] = useState('')
  const [tracking, setTracking] = useState('')
  const [reference, setReference] = useState('')
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const [done, setDone] = useState<string | null>(null)
  const paidCancel = target === 'CANCELLED' && order.status !== 'PENDING_PAYMENT'

  async function run(action: () => Promise<void>, message: string) {
    setBusy(true)
    setFailure(null)
    setDone(null)
    try {
      await action()
      setDone(message)
      setTarget('')
      setReason('')
      setTracking('')
      setReference('')
    } catch (e) {
      setFailure(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  function submitStatus(e: React.FormEvent) {
    e.preventDefault()
    if (!target) return
    if (target === 'CANCELLED' && !reason.trim()) {
      setFailure('Informe o motivo do cancelamento.')
      return
    }
    void run(
      () => actions.changeStatus(target, reason.trim() || undefined, tracking.trim() || undefined),
      `Status alterado para "${ORDER_STATUS_UI[target].label}".`,
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <Link to="/admin/pedidos" className="text-sm font-semibold text-accent hover:underline">
        ← Pedidos
      </Link>
      <PageHeader
        title={`Pedido #${order.number}`}
        description={`Criado em ${formatDateTime(order.createdAt)}`}
        actions={<Badge tone={ORDER_STATUS_UI[order.status].tone}>{ORDER_STATUS_UI[order.status].label}</Badge>}
      />
      {done && <Notice>{done}</Notice>}
      <ActionError message={failure} />

      <div className="grid gap-6 xl:grid-cols-[1fr_22rem]">
        <div className="flex min-w-0 flex-col gap-6">
          <Card>
            <h2 className="font-semibold">Itens</h2>
            <div className="mt-3 overflow-x-auto">
              <table className="w-full min-w-[28rem] text-sm">
                <thead className="text-left text-xs uppercase tracking-wide text-fg-muted">
                  <tr>
                    <th scope="col" className="py-2 font-medium">
                      Produto
                    </th>
                    <th scope="col" className="py-2 text-right font-medium">
                      Qtd.
                    </th>
                    <th scope="col" className="py-2 text-right font-medium">
                      Unitário
                    </th>
                    <th scope="col" className="py-2 text-right font-medium">
                      Total
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {order.items.map((i) => (
                    <tr key={`${i.sku}-${i.optionsSummary}`} className="border-t border-border">
                      <td className="py-2">
                        {i.productName}
                        <span className="block text-xs text-fg-muted">
                          {i.sku}
                          {i.optionsSummary && ` · ${i.optionsSummary}`}
                        </span>
                      </td>
                      <td className="py-2 text-right tabular-nums">{i.quantity}</td>
                      <td className="py-2 text-right tabular-nums">{formatPrice(i.unitPrice)}</td>
                      <td className="py-2 text-right tabular-nums">{formatPrice(i.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <dl className="mt-4 ml-auto grid max-w-xs grid-cols-2 gap-1 text-sm">
              <dt className="text-fg-muted">Subtotal</dt>
              <dd className="text-right tabular-nums">{formatPrice(order.subtotal)}</dd>
              <dt className="text-fg-muted">Frete</dt>
              <dd className="text-right tabular-nums">{formatPrice(order.shippingTotal)}</dd>
              {order.discountTotal > 0 && (
                <>
                  <dt className="text-fg-muted">Desconto</dt>
                  <dd className="text-right tabular-nums">−{formatPrice(order.discountTotal)}</dd>
                </>
              )}
              <dt className="font-semibold">Total</dt>
              <dd className="text-right font-semibold tabular-nums">{formatPrice(order.total)}</dd>
            </dl>
          </Card>

          <Card>
            <h2 className="font-semibold">Histórico</h2>
            <ol className="mt-3 flex flex-col gap-3 border-l border-border pl-4 text-sm">
              {data.history.map((h, i) => (
                <li key={`${h.at}-${i}`}>
                  <p className="font-medium">{ORDER_STATUS_UI[h.to].label}</p>
                  <p className="text-xs text-fg-muted">
                    {formatDateTime(h.at)} · {h.actorName ?? (h.actorId ? 'Usuário removido' : 'Sistema ou cliente')}
                  </p>
                  {h.note && <p className="mt-0.5 text-fg-muted">{h.note}</p>}
                </li>
              ))}
            </ol>
          </Card>
        </div>

        <div className="flex flex-col gap-6">
          <Card>
            <h2 className="font-semibold">Cliente</h2>
            <p className="mt-2 text-sm">
              <Link to={`/admin/clientes/${data.customerId}`} className="font-medium text-accent hover:underline">
                {data.customerName}
              </Link>
              <span className="block text-fg-muted">{data.customerEmail}</span>
            </p>
            {order.customerNote && (
              <p className="mt-3 rounded-lg bg-surface-muted p-2 text-sm">
                <span className="block text-xs text-fg-muted">Observação do cliente</span>
                {order.customerNote}
              </p>
            )}
          </Card>
          <Card>
            <h2 className="font-semibold">{SHIPPING[order.shippingMethod]}</h2>
            {order.shippingAddress ? (
              <address className="mt-2 text-sm not-italic text-fg-muted">
                {order.shippingAddress.recipientName}
                <br />
                {order.shippingAddress.street}, {order.shippingAddress.number}
                {order.shippingAddress.complement && ` — ${order.shippingAddress.complement}`}
                <br />
                {order.shippingAddress.district} · {order.shippingAddress.city}/{order.shippingAddress.state}
                <br />
                CEP {order.shippingAddress.postalCode.replace(/^(\d{5})(\d{3})$/, '$1-$2')}
              </address>
            ) : (
              <p className="mt-2 text-sm text-fg-muted">Retirada combinada após a confirmação.</p>
            )}
          </Card>
          <Card>
            <h2 className="font-semibold">Pagamento</h2>
            <p className="mt-2 text-sm text-fg-muted">
              {PAYMENT[order.paymentMethod]}
              {data.paidAt && ` · confirmado em ${formatDateTime(data.paidAt)}`}
            </p>
            {data.canConfirmPayment &&
              (isAdmin ? (
                <form
                  className="mt-3 flex flex-col gap-2"
                  onSubmit={(e) => {
                    e.preventDefault()
                    void run(() => actions.confirmPayment(reference.trim() || undefined), 'Pagamento confirmado.')
                  }}
                >
                  <label className="text-sm">
                    <span className="mb-1 block font-medium">Referência (ex.: ID do PIX)</span>
                    <input
                      value={reference}
                      maxLength={120}
                      onChange={(e) => setReference(e.target.value)}
                      className={`${controlClass} h-10`}
                    />
                  </label>
                  <p className="text-xs text-fg-muted">
                    Confirme somente após conferir o valor de {formatPrice(order.total)} no extrato.
                  </p>
                  <button type="submit" className={buttonClass('primary', 'md')} disabled={busy}>
                    <CheckCircle2 aria-hidden className="size-4" /> Confirmar pagamento
                  </button>
                </form>
              ) : (
                <p className="mt-2 text-xs text-fg-muted">A confirmação de pagamento é feita por um administrador.</p>
              ))}
          </Card>
          {data.allowedNext.length > 0 && (
            <Card>
              <h2 className="font-semibold">Atualizar status</h2>
              <form onSubmit={submitStatus} className="mt-3 flex flex-col gap-3">
                <label className="text-sm">
                  <span className="mb-1 block font-medium">Novo status</span>
                  <select
                    value={target}
                    onChange={(e) => setTarget(e.target.value as OrderStatus | '')}
                    className={`${controlClass} h-10`}
                  >
                    <option value="">Selecione…</option>
                    {data.allowedNext.map((s) => (
                      <option key={s} value={s}>
                        {ORDER_STATUS_UI[s].label}
                      </option>
                    ))}
                  </select>
                </label>
                {target === 'SHIPPED' && (
                  <label className="text-sm">
                    <span className="mb-1 block font-medium">Código de rastreio (opcional)</span>
                    <input
                      value={tracking}
                      maxLength={60}
                      onChange={(e) => setTracking(e.target.value)}
                      className={`${controlClass} h-10`}
                    />
                  </label>
                )}
                {target && (
                  <label className="text-sm">
                    <span className="mb-1 block font-medium">
                      {target === 'CANCELLED' ? 'Motivo (obrigatório)' : 'Observação (opcional)'}
                    </span>
                    <textarea
                      value={reason}
                      maxLength={500}
                      onChange={(e) => setReason(e.target.value)}
                      className={`${controlClass} min-h-16 py-2`}
                    />
                  </label>
                )}
                {paidCancel && (
                  <p className="text-xs text-danger">
                    {isAdmin
                      ? 'Pedido já pago: o estoque é devolvido e o estorno ao cliente deve ser feito manualmente.'
                      : 'Somente um administrador pode cancelar pedido já pago.'}
                  </p>
                )}
                <button
                  type="submit"
                  className={buttonClass(target === 'CANCELLED' ? 'danger' : 'primary', 'md')}
                  disabled={busy || !target || (paidCancel && !isAdmin)}
                >
                  {busy ? 'Salvando…' : 'Aplicar'}
                </button>
              </form>
            </Card>
          )}
        </div>
      </div>
    </div>
  )
}
