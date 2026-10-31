import { useQuery, useQueryClient } from '@tanstack/react-query'
import { CreditCard, MapPin, QrCode, ShoppingCart, Store } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAddressActions, useAddresses } from '../../api/account'
import type { OrderDetail, PaymentMethod, ShippingMethod, ShippingQuote } from '../../api/types'
import { CART_KEY } from '../../cart/cartKeys'
import { useCart } from '../../cart/CartProvider'
import { AddressForm } from '../../components/account/AddressForm'
import { buttonClass } from '../../components/ui/button'
import { EmptyState } from '../../components/ui/EmptyState'
import { Notice } from '../../components/ui/Notice'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { apiGet, apiSend, ApiError } from '../../lib/api'
import { cn } from '../../lib/cn'
import { formatPrice } from '../../lib/format'
import { usePageMeta } from '../../lib/seo'

type QuotesResponse = { originPostalCode: string; options: ShippingQuote[] }

function newKey() {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

const optionClass = (active: boolean) =>
  cn(
    'flex cursor-pointer items-start gap-3 rounded-lg border p-4 transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent',
    active ? 'border-accent bg-primary/10' : 'border-border hover:bg-surface-muted',
  )

export function CheckoutPage() {
  usePageMeta('Finalizar compra')
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { serverCart, loading } = useCart()
  const { data: addresses = [], isPending: loadingAddresses } = useAddresses()
  const addressActions = useAddressActions()

  const [method, setMethod] = useState<ShippingMethod>('PICKUP')
  const [chosenAddressId, setAddressId] = useState<number | null>(null)
  const [payment, setPayment] = useState<PaymentMethod>('PIX')
  const [note, setNote] = useState('')
  const [addingAddress, setAddingAddress] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  // mesma chave em reenvios (duplo clique / falha de rede) → o servidor não duplica o pedido
  const [idempotencyKey, setIdempotencyKey] = useState(newKey)

  // sem escolha explícita, usa o endereço principal (ou o primeiro)
  const addressId = chosenAddressId ?? (addresses.find((a) => a.isDefault) ?? addresses[0])?.id ?? null

  const { data: quotes } = useQuery({
    queryKey: ['checkout', 'shipping', addressId],
    queryFn: () =>
      apiGet<QuotesResponse>(`/api/v1/checkout/shipping-options${addressId ? `?addressId=${addressId}` : ''}`),
  })
  const pickup = quotes?.options.find((o) => o.method === 'PICKUP')
  const delivery = quotes?.options.find((o) => o.method === 'DELIVERY')
  const shippingPrice = method === 'DELIVERY' ? (delivery?.price ?? 0) : 0
  const subtotal = serverCart?.subtotal ?? 0
  const total = Math.round((subtotal + shippingPrice) * 100) / 100
  const canSubmit =
    !!serverCart?.items.length &&
    !serverCart.hasProblems &&
    (method === 'PICKUP' ? !!pickup : !!delivery && addressId !== null)

  async function placeOrder() {
    setError(null)
    setSubmitting(true)
    try {
      const order = await apiSend<OrderDetail>(
        'POST',
        '/api/v1/checkout',
        {
          shippingMethod: method,
          addressId: method === 'DELIVERY' ? addressId : null,
          paymentMethod: payment,
          customerNote: note.trim() || null,
          expectedTotal: total,
        },
        { 'Idempotency-Key': idempotencyKey },
      )
      setIdempotencyKey(newKey())
      await queryClient.invalidateQueries({ queryKey: CART_KEY })
      navigate(`/conta/pedidos/${order.number}?novo=1`, { replace: true })
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        // preço/estoque mudou: atualiza o carrinho e pede revisão (nova tentativa com nova chave)
        setIdempotencyKey(newKey())
        await queryClient.invalidateQueries({ queryKey: CART_KEY })
      }
      setError(
        e instanceof ApiError
          ? (e.fieldErrors[0]?.message ?? e.message)
          : 'Não foi possível concluir. Tente novamente.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <p role="status" className="px-4 py-24 text-center text-sm text-fg-muted">
        Carregando…
      </p>
    )
  }
  if (!serverCart?.items.length) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-12">
        <EmptyState
          icon={ShoppingCart}
          title="Seu carrinho está vazio"
          action={
            <Link to="/produtos" className={buttonClass('primary')}>
              Ver produtos
            </Link>
          }
        />
      </div>
    )
  }

  return (
    <div className="mx-auto grid max-w-6xl gap-8 px-4 py-10 sm:px-6 lg:grid-cols-[1fr_22rem]">
      <div className="flex flex-col gap-6">
        <PageHeader title="Finalizar compra" description="Revise a entrega, o pagamento e confirme o pedido." />
        {serverCart.hasProblems && (
          <Notice>
            Alguns itens mudaram de preço ou disponibilidade.{' '}
            <Link to="/carrinho" className="font-semibold underline">
              Revise o carrinho
            </Link>
            .
          </Notice>
        )}

        <Card>
          <h2 className="font-semibold">1. Entrega</h2>
          <fieldset className="mt-4 grid gap-3">
            <legend className="sr-only">Forma de entrega</legend>
            {pickup && (
              <label className={optionClass(method === 'PICKUP')}>
                <input
                  type="radio"
                  name="shipping"
                  className="sr-only"
                  checked={method === 'PICKUP'}
                  onChange={() => setMethod('PICKUP')}
                />
                <Store aria-hidden className="mt-0.5 size-5 text-accent" />
                <span className="flex-1">
                  <span className="block font-medium">{pickup.label}</span>
                  <span className="text-sm text-fg-muted">Sem custo de frete.</span>
                </span>
                <span className="font-semibold">Grátis</span>
              </label>
            )}
            <label className={optionClass(method === 'DELIVERY')}>
              <input
                type="radio"
                name="shipping"
                className="sr-only"
                checked={method === 'DELIVERY'}
                onChange={() => setMethod('DELIVERY')}
              />
              <MapPin aria-hidden className="mt-0.5 size-5 text-accent" />
              <span className="flex-1">
                <span className="block font-medium">Entrega no endereço</span>
                <span className="text-sm text-fg-muted">
                  {delivery
                    ? `${delivery.label} · ${delivery.minDays}–${delivery.maxDays} dias úteis após a postagem${delivery.demoValues ? ' · valores de demonstração' : ''}`
                    : addressId
                      ? 'Ainda não entregamos neste CEP — escolha a retirada ou fale conosco.'
                      : 'Cadastre um endereço para calcular.'}
                </span>
              </span>
              {delivery && <span className="font-semibold">{formatPrice(delivery.price)}</span>}
            </label>
          </fieldset>

          {method === 'DELIVERY' && (
            <div className="mt-4">
              {loadingAddresses ? (
                <p className="text-sm text-fg-muted">Carregando endereços…</p>
              ) : (
                <fieldset className="grid gap-2">
                  <legend className="mb-2 text-sm font-medium">Endereço de entrega</legend>
                  {addresses.map((a) => (
                    <label key={a.id} className={optionClass(addressId === a.id)}>
                      <input
                        type="radio"
                        name="address"
                        className="sr-only"
                        checked={addressId === a.id}
                        onChange={() => setAddressId(a.id)}
                      />
                      <span className="text-sm">
                        <span className="block font-medium">
                          {a.label}
                          {a.isDefault ? ' · principal' : ''}
                        </span>
                        <span className="text-fg-muted">
                          {a.recipientName} — {a.street}, {a.number}
                          {a.complement ? `, ${a.complement}` : ''} · {a.district} · {a.city}/{a.state} · CEP{' '}
                          {a.postalCode}
                        </span>
                      </span>
                    </label>
                  ))}
                </fieldset>
              )}
              {addingAddress ? (
                <div className="mt-4 rounded-lg border border-border p-4">
                  <AddressForm
                    submitLabel="Salvar endereço"
                    onCancel={() => setAddingAddress(false)}
                    onSubmit={async (data) => {
                      const created = await addressActions.create(data)
                      setAddressId(created.id)
                      setAddingAddress(false)
                    }}
                  />
                </div>
              ) : (
                <button
                  type="button"
                  className={buttonClass('outline', 'sm', 'mt-3')}
                  onClick={() => setAddingAddress(true)}
                >
                  Adicionar endereço
                </button>
              )}
            </div>
          )}
        </Card>

        <Card>
          <h2 className="font-semibold">2. Pagamento</h2>
          <fieldset className="mt-4 grid gap-3 sm:grid-cols-2">
            <legend className="sr-only">Forma de pagamento</legend>
            <label className={optionClass(payment === 'PIX')}>
              <input
                type="radio"
                name="payment"
                className="sr-only"
                checked={payment === 'PIX'}
                onChange={() => setPayment('PIX')}
              />
              <QrCode aria-hidden className="mt-0.5 size-5 text-accent" />
              <span>
                <span className="block font-medium">PIX</span>
                <span className="text-sm text-fg-muted">Enviamos a chave PIX por e-mail após o pedido.</span>
              </span>
            </label>
            <label className={optionClass(payment === 'CARD')}>
              <input
                type="radio"
                name="payment"
                className="sr-only"
                checked={payment === 'CARD'}
                onChange={() => setPayment('CARD')}
              />
              <CreditCard aria-hidden className="mt-0.5 size-5 text-accent" />
              <span>
                <span className="block font-medium">Cartão</span>
                <span className="text-sm text-fg-muted">Enviamos um link de pagamento seguro por e-mail.</span>
              </span>
            </label>
          </fieldset>
          <p className="mt-3 text-xs text-fg-muted">
            Nenhum dado de cartão é digitado ou armazenado neste site. O pedido é confirmado quando a equipe conferir o
            pagamento.
          </p>
        </Card>

        <Card>
          <label htmlFor="note" className="font-semibold">
            Observações (opcional)
          </label>
          <textarea
            id="note"
            value={note}
            onChange={(e) => setNote(e.target.value.slice(0, 500))}
            maxLength={500}
            className="mt-3 min-h-20 w-full rounded-lg border border-border bg-bg-elevated px-3 py-2 text-sm"
            placeholder="Ex.: melhor horário para contato"
          />
        </Card>
      </div>

      <aside className="h-fit rounded-card border border-border bg-surface p-5 shadow-card lg:sticky lg:top-20">
        <h2 className="font-semibold">Resumo do pedido</h2>
        <ul className="mt-4 flex flex-col gap-2 text-sm">
          {serverCart.items.map((i) => (
            <li key={i.id} className="flex justify-between gap-3">
              <span className="min-w-0 truncate">
                {i.quantity}× {i.productName}
              </span>
              <span className="shrink-0">{formatPrice(i.lineTotal)}</span>
            </li>
          ))}
        </ul>
        <dl className="mt-4 space-y-1 border-t border-border pt-4 text-sm">
          <div className="flex justify-between">
            <dt className="text-fg-muted">Subtotal</dt>
            <dd>{formatPrice(subtotal)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-fg-muted">Frete</dt>
            <dd>{method === 'PICKUP' ? 'Grátis' : delivery ? formatPrice(delivery.price) : '—'}</dd>
          </div>
          <div className="flex justify-between text-base font-semibold">
            <dt>Total</dt>
            <dd>{formatPrice(total)}</dd>
          </div>
        </dl>
        {error && (
          <p
            role="alert"
            className="mt-4 rounded-lg border border-danger/40 bg-danger/10 px-3 py-2 text-sm text-danger"
          >
            {error}
          </p>
        )}
        <button
          type="button"
          disabled={!canSubmit || submitting}
          onClick={() => void placeOrder()}
          className={buttonClass('primary', 'lg', 'mt-4 w-full')}
        >
          {submitting ? 'Enviando pedido…' : 'Confirmar pedido'}
        </button>
        <p className="mt-3 text-xs text-fg-muted">
          Ao confirmar, você concorda com a{' '}
          <Link to="/politica-comercial" className="underline">
            política comercial
          </Link>{' '}
          (rascunho em revisão).
        </p>
      </aside>
    </div>
  )
}
