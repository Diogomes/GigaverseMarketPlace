import { useQueryClient } from '@tanstack/react-query'
import { Check, ChevronLeft, ChevronRight, MapPin, Package, PartyPopper, Plus, Star, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { useAddressActions, useAddresses, useMyOrder, useMyOrders } from '../../api/account'
import type { Address } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { AddressForm } from '../../components/account/AddressForm'
import { InstagramLink } from '../../components/InstagramLink'
import { OrderCard } from '../../components/store/OrderCard'
import { ORDER_STATUS_UI } from '../../components/store/orderStatus'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { CatalogImage } from '../../components/ui/CatalogImage'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { EmptyState } from '../../components/ui/EmptyState'
import { Notice } from '../../components/ui/Notice'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { apiSend, ApiError } from '../../lib/api'
import { cn } from '../../lib/cn'
import { formatDateTime, formatPrice } from '../../lib/format'
import { usePageMeta } from '../../lib/seo'
import { NotFoundPage } from '../PlaceholderPage'

function Loading() {
  return (
    <p role="status" className="text-sm text-fg-muted">
      Carregando…
    </p>
  )
}

function LoadError() {
  return <Notice>Não foi possível carregar. Verifique sua conexão e tente novamente.</Notice>
}

// ------------------------------------------------------------------ resumo

export function AccountOverviewPage() {
  usePageMeta('Minha conta')
  const { user } = useAuth()
  const { data: orders, isPending, isError } = useMyOrders(0, 3)
  const { data: addresses } = useAddresses()
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={`Olá, ${user?.fullName.split(' ')[0] ?? 'cliente'}!`} description="Resumo da sua conta." />
      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <p className="text-sm text-fg-muted">Pedidos</p>
          <p className="mt-1 text-2xl font-bold">{orders?.totalElements ?? '—'}</p>
        </Card>
        <Card>
          <p className="text-sm text-fg-muted">Endereços</p>
          <p className="mt-1 text-2xl font-bold">{addresses?.length ?? '—'}</p>
        </Card>
      </div>
      <section aria-labelledby="recentes">
        <div className="flex items-center justify-between">
          <h2 id="recentes" className="text-lg font-semibold">
            Pedidos recentes
          </h2>
          <Link to="/conta/pedidos" className="text-sm font-semibold text-accent hover:underline">
            Ver todos
          </Link>
        </div>
        <div className="mt-4 flex flex-col gap-3">
          {isPending ? (
            <Loading />
          ) : isError ? (
            <LoadError />
          ) : orders.content.length === 0 ? (
            <EmptyState
              icon={Package}
              title="Você ainda não fez pedidos"
              action={
                <Link to="/produtos" className={buttonClass('primary')}>
                  Ver produtos
                </Link>
              }
            />
          ) : (
            orders.content.map((o) => <OrderCard key={o.number} order={o} />)
          )}
        </div>
      </section>
    </div>
  )
}

// ------------------------------------------------------------------ pedidos

const TABS: Array<{ key: string; label: string; group?: 'ACTIVE' | 'DELIVERED' | 'CANCELLED' }> = [
  { key: 'todos', label: 'Todos' },
  { key: 'andamento', label: 'Em andamento', group: 'ACTIVE' },
  { key: 'entregues', label: 'Entregues', group: 'DELIVERED' },
  { key: 'cancelados', label: 'Cancelados', group: 'CANCELLED' },
]

export function AccountOrdersPage() {
  usePageMeta('Meus pedidos')
  const [tab, setTab] = useState('todos')
  const [page, setPage] = useState(0)
  const active = TABS.find((t) => t.key === tab) ?? TABS[0]
  const { data, isPending, isError } = useMyOrders(page, 10, active.group)

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Meus pedidos" description="Acompanhe o status dos seus pedidos." />
      <div role="tablist" aria-label="Filtrar pedidos" className="flex flex-wrap gap-2">
        {TABS.map((t) => (
          <button
            key={t.key}
            role="tab"
            type="button"
            aria-selected={tab === t.key}
            onClick={() => {
              setTab(t.key)
              setPage(0)
            }}
            className={cn(
              'rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors',
              tab === t.key ? 'border-accent bg-primary text-on-primary' : 'border-border text-fg-muted hover:text-fg',
            )}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div role="tabpanel" className="flex flex-col gap-3">
        {isPending ? (
          <Loading />
        ) : isError ? (
          <LoadError />
        ) : data.content.length === 0 ? (
          <EmptyState icon={Package} title="Nenhum pedido nesta categoria" />
        ) : (
          data.content.map((o) => <OrderCard key={o.number} order={o} />)
        )}
      </div>
      {data && data.totalPages > 1 && (
        <nav aria-label="Paginação" className="flex items-center justify-center gap-2">
          <button
            type="button"
            className={buttonClass('outline', 'sm')}
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            <ChevronLeft aria-hidden className="size-4" /> Anterior
          </button>
          <span className="px-3 text-sm text-fg-muted">
            Página {page + 1} de {data.totalPages}
          </span>
          <button
            type="button"
            className={buttonClass('outline', 'sm')}
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage(page + 1)}
          >
            Próxima <ChevronRight aria-hidden className="size-4" />
          </button>
        </nav>
      )}
    </div>
  )
}

const PAYMENT_STATUS: Record<string, string> = {
  PENDING: 'Aguardando pagamento',
  AUTHORIZED: 'Autorizado',
  PAID: 'Pago',
  FAILED: 'Falhou',
  CANCELLED: 'Cancelado',
  REFUNDED: 'Estornado',
}

export function AccountOrderDetailPage() {
  const { number = '' } = useParams()
  const [params] = useSearchParams()
  const queryClient = useQueryClient()
  const { data: order, isPending, error } = useMyOrder(number)
  const [confirming, setConfirming] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  usePageMeta(`Pedido #${number}`)

  if (isPending) return <Loading />
  if (error instanceof ApiError && error.status === 404) return <NotFoundPage />
  if (!order) return <LoadError />

  const status = ORDER_STATUS_UI[order.status]

  async function cancel() {
    setConfirming(false)
    setMessage(null)
    try {
      const updated = await apiSend('POST', `/api/v1/account/orders/${order!.number}/cancel`)
      queryClient.setQueryData(['account', 'order', number], updated)
      await queryClient.invalidateQueries({ queryKey: ['account', 'orders'] })
      setMessage('Pedido cancelado.')
    } catch (e) {
      setMessage(e instanceof ApiError ? e.message : 'Não foi possível cancelar.')
    }
  }

  return (
    <div className="flex flex-col gap-6">
      {params.get('novo') && order.status === 'PENDING_PAYMENT' && (
        <p className="flex items-center gap-2 rounded-lg border border-success/40 bg-success/10 px-4 py-3 text-sm">
          <PartyPopper aria-hidden className="size-5 text-success" />
          <span>
            <strong>Pedido recebido!</strong> Enviamos as instruções de pagamento para o seu e-mail.
          </span>
        </p>
      )}
      <PageHeader
        title={`Pedido #${order.number}`}
        description={`Realizado em ${formatDateTime(order.createdAt)}`}
        actions={<Badge tone={status.tone}>{status.label}</Badge>}
      />
      {message && <Notice>{message}</Notice>}

      {order.status === 'PENDING_PAYMENT' && order.paymentInstructions && (
        <Card className="border-amber-500/40">
          <h2 className="font-semibold">Pagamento — {order.paymentMethod === 'PIX' ? 'PIX' : 'Cartão'}</h2>
          <p className="mt-2 text-sm text-fg-muted">{order.paymentInstructions}</p>
          <p className="mt-2 text-xs text-fg-muted">
            Pedidos sem pagamento identificado em até 72 horas são cancelados automaticamente.
          </p>
        </Card>
      )}

      <div className="grid gap-6 xl:grid-cols-[1fr_20rem]">
        <Card>
          <h2 className="font-semibold">Itens</h2>
          <ul className="mt-4 divide-y divide-border">
            {order.items.map((it, i) => (
              <li key={i} className="flex items-center gap-3 py-3">
                <CatalogImage
                  slug={it.productSlug}
                  name={it.productName}
                  variant="square"
                  fallbackVariant={i}
                  fallbackSize="xs"
                  className="size-12 shrink-0 rounded-lg"
                />
                <div className="min-w-0 flex-1 text-sm">
                  <p className="font-medium">
                    {it.productSlug ? (
                      <Link to={`/produtos/${it.productSlug}`} className="hover:text-accent">
                        {it.productName}
                      </Link>
                    ) : (
                      it.productName
                    )}
                  </p>
                  <p className="text-fg-muted">
                    {it.optionsSummary ?? 'Padrão'} · Qtd. {it.quantity} · {formatPrice(it.unitPrice)} cada
                  </p>
                </div>
                <span className="text-sm font-semibold">{formatPrice(it.lineTotal)}</span>
              </li>
            ))}
          </ul>
          <dl className="mt-4 space-y-1 border-t border-border pt-4 text-sm">
            <div className="flex justify-between">
              <dt className="text-fg-muted">Subtotal</dt>
              <dd>{formatPrice(order.subtotal)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-fg-muted">Frete</dt>
              <dd>{order.shippingTotal ? formatPrice(order.shippingTotal) : 'Grátis'}</dd>
            </div>
            <div className="flex justify-between text-base font-semibold">
              <dt>Total</dt>
              <dd>{formatPrice(order.total)}</dd>
            </div>
          </dl>
        </Card>

        <div className="flex flex-col gap-6">
          <Card>
            <h2 className="font-semibold">Acompanhamento</h2>
            <ol className="mt-4 flex flex-col gap-3">
              {order.history.map((h, i) => (
                <li key={i} className="flex items-start gap-3 text-sm">
                  <span className="mt-0.5 inline-flex size-5 shrink-0 items-center justify-center rounded-full bg-primary text-on-primary">
                    <Check aria-hidden className="size-3" />
                  </span>
                  <span>
                    <span className="block font-medium">{ORDER_STATUS_UI[h.status].label}</span>
                    <span className="text-fg-muted">{formatDateTime(h.at)}</span>
                    {h.note && <span className="block text-fg-muted">{h.note}</span>}
                  </span>
                </li>
              ))}
            </ol>
          </Card>
          <Card>
            <h2 className="font-semibold">{order.shippingMethod === 'PICKUP' ? 'Retirada' : 'Entrega'}</h2>
            {order.shippingAddress ? (
              <p className="mt-2 text-sm text-fg-muted">
                {order.shippingAddress.recipientName}
                <br />
                {order.shippingAddress.street}, {order.shippingAddress.number}
                {order.shippingAddress.complement ? ` — ${order.shippingAddress.complement}` : ''}
                <br />
                {order.shippingAddress.district} · {order.shippingAddress.city}/{order.shippingAddress.state} · CEP{' '}
                {order.shippingAddress.postalCode}
              </p>
            ) : (
              <p className="mt-2 text-sm text-fg-muted">Local e horário combinados com a equipe após a confirmação.</p>
            )}
            <p className="mt-3 text-sm">
              <span className="text-fg-muted">Pagamento: </span>
              {order.paymentMethod === 'PIX' ? 'PIX' : 'Cartão'} ·{' '}
              {order.paymentStatus ? PAYMENT_STATUS[order.paymentStatus] : '—'}
            </p>
          </Card>
          {order.canCancel && (
            <button type="button" className={buttonClass('danger')} onClick={() => setConfirming(true)}>
              Cancelar pedido
            </button>
          )}
        </div>
      </div>
      <Link to="/conta/pedidos" className="text-sm font-semibold text-accent hover:underline">
        ← Voltar para meus pedidos
      </Link>
      <ConfirmDialog
        open={confirming}
        title="Cancelar pedido?"
        description="O pedido será cancelado e os itens voltam ao estoque. Esta ação não pode ser desfeita."
        confirmLabel="Cancelar pedido"
        onCancel={() => setConfirming(false)}
        onConfirm={() => void cancel()}
      />
    </div>
  )
}

// ------------------------------------------------------------------ endereços

export function AccountAddressesPage() {
  usePageMeta('Endereços')
  const { data: addresses, isPending, isError } = useAddresses()
  const actions = useAddressActions()
  const [editing, setEditing] = useState<Address | 'new' | null>(null)
  const [removing, setRemoving] = useState<Address | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  async function run(action: () => Promise<void>, done: string) {
    setMessage(null)
    try {
      await action()
      setMessage(done)
    } catch (e) {
      setMessage(e instanceof ApiError ? e.message : 'Não foi possível concluir.')
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Endereços"
        description="Gerencie seus endereços de entrega."
        actions={
          editing === null && (
            <button type="button" className={buttonClass('primary')} onClick={() => setEditing('new')}>
              <Plus aria-hidden className="size-4" /> Adicionar endereço
            </button>
          )
        }
      />
      {message && <Notice>{message}</Notice>}
      {editing !== null && (
        <Card>
          <h2 className="mb-4 font-semibold">{editing === 'new' ? 'Novo endereço' : `Editar: ${editing.label}`}</h2>
          <AddressForm
            initial={editing === 'new' ? undefined : editing}
            submitLabel="Salvar"
            onCancel={() => setEditing(null)}
            onSubmit={async (data) => {
              if (editing === 'new') await actions.create(data)
              else await actions.update(editing.id, data)
              setEditing(null)
              setMessage('Endereço salvo.')
            }}
          />
        </Card>
      )}
      {isPending ? (
        <Loading />
      ) : isError ? (
        <LoadError />
      ) : addresses.length === 0 ? (
        <EmptyState icon={MapPin} title="Nenhum endereço cadastrado" />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {addresses.map((a) => (
            <Card key={a.id}>
              <div className="flex items-center gap-2">
                <h2 className="font-semibold">{a.label}</h2>
                {a.isDefault && <Badge tone="accent">Principal</Badge>}
              </div>
              <p className="mt-2 text-sm text-fg-muted">
                {a.recipientName}
                <br />
                {a.street}, {a.number}
                {a.complement ? ` — ${a.complement}` : ''}
                <br />
                {a.district} · {a.city}/{a.state} · CEP {a.postalCode.slice(0, 5)}-{a.postalCode.slice(5)}
              </p>
              <div className="mt-4 flex flex-wrap gap-2">
                <button type="button" className={buttonClass('outline', 'sm')} onClick={() => setEditing(a)}>
                  Editar
                </button>
                {!a.isDefault && (
                  <button
                    type="button"
                    className={buttonClass('ghost', 'sm')}
                    onClick={() => void run(() => actions.makeDefault(a.id), 'Endereço principal atualizado.')}
                  >
                    <Star aria-hidden className="size-4" /> Tornar principal
                  </button>
                )}
                <button
                  type="button"
                  className={buttonClass('ghost', 'sm', 'text-danger')}
                  onClick={() => setRemoving(a)}
                  aria-label={`Remover endereço ${a.label}`}
                >
                  <Trash2 aria-hidden className="size-4" /> Remover
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}
      <ConfirmDialog
        open={removing !== null}
        title="Remover endereço?"
        description={`"${removing?.label ?? ''}" deixará de aparecer na sua conta. Pedidos antigos não são afetados.`}
        confirmLabel="Remover"
        onCancel={() => setRemoving(null)}
        onConfirm={() => {
          const target = removing!
          setRemoving(null)
          void run(() => actions.remove(target.id), 'Endereço removido.')
        }}
      />
    </div>
  )
}

// ------------------------------------------------------------------ suporte

export function AccountSupportPage() {
  usePageMeta('Suporte')
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Suporte" description="Fale com a equipe Gigaverse3D." />
      <Card>
        <p className="text-sm text-fg-muted">
          Enquanto os canais de atendimento (e-mail/WhatsApp) não são definidos, fale conosco pelo Instagram. Informe o
          número do pedido, se for o caso.
        </p>
        <div className="mt-4">
          <InstagramLink />
        </div>
      </Card>
    </div>
  )
}
