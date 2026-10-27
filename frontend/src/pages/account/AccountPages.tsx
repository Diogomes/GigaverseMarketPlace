import { Check, MapPin, Package, Plus } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useAuth } from '../../auth/AuthProvider'
import { InstagramLink } from '../../components/InstagramLink'
import { OrderCard } from '../../components/store/OrderCard'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { DemoBanner } from '../../components/ui/DemoBanner'
import { EmptyState } from '../../components/ui/EmptyState'
import { Notice } from '../../components/ui/Notice'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { ProductMedia } from '../../components/ui/ProductMedia'
import { ORDER_STATUS } from '../../data/labels'
import {
  imageForProduct,
  getCustomerOrder,
  listAddresses,
  listCustomerOrders,
  PREVIEW_CUSTOMER_ID,
} from '../../data/store'
import type { Order } from '../../data/types'
import { cn } from '../../lib/cn'
import { formatBRL, formatDate } from '../../lib/format'
import { NotFoundPage } from '../PlaceholderPage'

const TABS: Array<{ key: string; label: string; match: (o: Order) => boolean }> = [
  { key: 'todos', label: 'Todos', match: () => true },
  {
    key: 'andamento',
    label: 'Em andamento',
    match: (o) => ['AGUARDANDO_PAGAMENTO', 'EM_PRODUCAO', 'ENVIADO'].includes(o.status),
  },
  { key: 'entregues', label: 'Entregues', match: (o) => o.status === 'ENTREGUE' },
  { key: 'cancelados', label: 'Cancelados', match: (o) => o.status === 'CANCELADO' },
]

export function AccountOverviewPage() {
  const { user } = useAuth()
  const orders = listCustomerOrders(PREVIEW_CUSTOMER_ID)
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title={`Olá, ${user?.fullName.split(' ')[0] ?? 'cliente'}!`} description="Resumo da sua conta." />
      <DemoBanner />
      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <p className="text-sm text-fg-muted">Pedidos</p>
          <p className="mt-1 text-2xl font-bold">{orders.length}</p>
        </Card>
        <Card>
          <p className="text-sm text-fg-muted">Em andamento</p>
          <p className="mt-1 text-2xl font-bold">{orders.filter(TABS[1].match).length}</p>
        </Card>
        <Card>
          <p className="text-sm text-fg-muted">Endereços</p>
          <p className="mt-1 text-2xl font-bold">{listAddresses().length}</p>
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
          {orders.length === 0 ? (
            <EmptyState icon={Package} title="Você ainda não fez pedidos" />
          ) : (
            orders.slice(0, 3).map((o) => <OrderCard key={o.number} order={o} />)
          )}
        </div>
      </section>
    </div>
  )
}

export function AccountOrdersPage() {
  const [tab, setTab] = useState('todos')
  const orders = listCustomerOrders(PREVIEW_CUSTOMER_ID)
  const active = TABS.find((t) => t.key === tab) ?? TABS[0]
  const visible = orders.filter(active.match)

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Meus pedidos" description="Acompanhe o status dos seus pedidos." />
      <DemoBanner />
      <div role="tablist" aria-label="Filtrar pedidos" className="flex flex-wrap gap-2">
        {TABS.map((t) => (
          <button
            key={t.key}
            role="tab"
            type="button"
            aria-selected={tab === t.key}
            onClick={() => setTab(t.key)}
            className={cn(
              'rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors',
              tab === t.key ? 'border-accent bg-primary text-on-primary' : 'border-border text-fg-muted hover:text-fg',
            )}
          >
            {t.label} ({orders.filter(t.match).length})
          </button>
        ))}
      </div>
      <div role="tabpanel" className="flex flex-col gap-3">
        {visible.length === 0 ? (
          <EmptyState icon={Package} title="Nenhum pedido nesta categoria" />
        ) : (
          visible.map((o) => <OrderCard key={o.number} order={o} />)
        )}
      </div>
    </div>
  )
}

export function AccountOrderDetailPage() {
  const { number = '' } = useParams()
  const order = getCustomerOrder(PREVIEW_CUSTOMER_ID, number)
  const address = listAddresses().find((a) => a.isDefault)
  if (!order) return <NotFoundPage />
  const subtotal = order.totalCents - order.shippingCents

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={`Pedido #${order.number}`}
        description={`Realizado em ${formatDate(order.createdAt)}`}
        actions={<Badge tone={ORDER_STATUS[order.status].tone}>{ORDER_STATUS[order.status].label}</Badge>}
      />
      <DemoBanner />
      <div className="grid gap-6 xl:grid-cols-[1fr_20rem]">
        <Card>
          <h2 className="font-semibold">Itens</h2>
          <ul className="mt-4 divide-y divide-border">
            {order.items.map((it, i) => (
              <li key={i} className="flex items-center gap-3 py-3">
                <ProductMedia
                  name={it.name}
                  image={imageForProduct(it.productSlug)}
                  variant="square"
                  fallbackVariant={i}
                  fallbackSize="xs"
                  className="size-12 shrink-0 rounded-lg"
                />
                <div className="min-w-0 flex-1 text-sm">
                  <p className="font-medium">{it.name}</p>
                  <p className="text-fg-muted">
                    {it.variant ?? 'Padrão'} · Qtd. {it.quantity}
                  </p>
                </div>
                <span className="text-sm font-semibold">{formatBRL(it.unitCents * it.quantity)}</span>
              </li>
            ))}
          </ul>
          <dl className="mt-4 space-y-1 border-t border-border pt-4 text-sm">
            <div className="flex justify-between">
              <dt className="text-fg-muted">Subtotal</dt>
              <dd>{formatBRL(subtotal)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-fg-muted">Frete</dt>
              <dd>{formatBRL(order.shippingCents)}</dd>
            </div>
            <div className="flex justify-between text-base font-semibold">
              <dt>Total</dt>
              <dd>{formatBRL(order.totalCents)}</dd>
            </div>
          </dl>
        </Card>
        <div className="flex flex-col gap-6">
          <Card>
            <h2 className="font-semibold">Acompanhamento</h2>
            <ol className="mt-4 flex flex-col gap-3">
              {order.history.map((h) => (
                <li key={h.status} className="flex items-start gap-3 text-sm">
                  <span className="mt-0.5 inline-flex size-5 items-center justify-center rounded-full bg-primary text-on-primary">
                    <Check aria-hidden className="size-3" />
                  </span>
                  <span>
                    <span className="block font-medium">{ORDER_STATUS[h.status].label}</span>
                    <span className="text-fg-muted">{formatDate(h.at)}</span>
                  </span>
                </li>
              ))}
            </ol>
          </Card>
          {address && (
            <Card>
              <h2 className="font-semibold">Entrega</h2>
              <p className="mt-2 text-sm text-fg-muted">
                {address.recipient}
                <br />
                {address.line} — {address.district}
                <br />
                {address.city}/{address.state} · CEP {address.postalCode}
              </p>
            </Card>
          )}
        </div>
      </div>
      <Link to="/conta/pedidos" className="text-sm font-semibold text-accent hover:underline">
        ← Voltar para meus pedidos
      </Link>
    </div>
  )
}

export function AccountAddressesPage() {
  const [notice, setNotice] = useState(false)
  const addresses = listAddresses()
  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Endereços"
        description="Gerencie seus endereços de entrega."
        actions={
          <button type="button" className={buttonClass('primary')} onClick={() => setNotice(true)}>
            <Plus aria-hidden className="size-4" /> Adicionar endereço
          </button>
        }
      />
      <DemoBanner />
      {notice && <Notice>O cadastro de endereços será habilitado na Fase 05.</Notice>}
      {addresses.length === 0 ? (
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
                {a.recipient}
                <br />
                {a.line} — {a.district}
                <br />
                {a.city}/{a.state} · CEP {a.postalCode}
              </p>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}

export function AccountSupportPage() {
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Suporte" description="Fale com a equipe Gigaverse3D." />
      <Card>
        <p className="text-sm text-fg-muted">
          Enquanto os canais de atendimento (e-mail/WhatsApp) não são definidos, fale conosco pelo Instagram.
        </p>
        <div className="mt-4">
          <InstagramLink />
        </div>
      </Card>
    </div>
  )
}
