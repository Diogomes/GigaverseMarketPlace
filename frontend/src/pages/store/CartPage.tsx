import { AlertTriangle, ShoppingCart, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../../auth/AuthProvider'
import { useCart } from '../../cart/CartProvider'
import { buttonClass } from '../../components/ui/button'
import { CatalogImage } from '../../components/ui/CatalogImage'
import { EmptyState } from '../../components/ui/EmptyState'
import { controlClass } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { ApiError } from '../../lib/api'
import { formatPrice } from '../../lib/format'
import { usePageMeta } from '../../lib/seo'

function QuantitySelect({
  value,
  max,
  onChange,
  label,
}: {
  value: number
  max: number
  onChange: (v: number) => void
  label: string
}) {
  const options = Array.from({ length: Math.max(1, Math.min(max || value, 20)) }, (_, i) => i + 1)
  if (!options.includes(value)) options.push(value)
  return (
    <label>
      <span className="sr-only">{label}</span>
      <select value={value} onChange={(e) => onChange(Number(e.target.value))} className={`${controlClass} h-9 w-20`}>
        {options.map((n) => (
          <option key={n} value={n}>
            {n}
          </option>
        ))}
      </select>
    </label>
  )
}

export function CartPage() {
  usePageMeta('Carrinho')
  const { user } = useAuth()
  const { serverCart, localItems, loading, updateQuantity, remove, updateLocal, removeLocal } = useCart()
  const [error, setError] = useState<string | null>(null)

  async function run(action: () => Promise<void>) {
    setError(null)
    try {
      await action()
    } catch (e) {
      setError(
        e instanceof ApiError ? (e.fieldErrors[0]?.message ?? e.message) : 'Não foi possível atualizar o carrinho.',
      )
    }
  }

  const empty = user ? !serverCart?.items.length : !localItems.length

  return (
    <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold">Carrinho</h1>
      {error && (
        <div className="mt-4">
          <Notice>{error}</Notice>
        </div>
      )}
      {loading ? (
        <p role="status" className="mt-8 text-sm text-fg-muted">
          Carregando…
        </p>
      ) : empty ? (
        <div className="mt-8">
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
      ) : (
        <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_20rem]">
          <ul className="flex flex-col divide-y divide-border rounded-card border border-border bg-surface">
            {user
              ? serverCart!.items.map((item, i) => (
                  <li key={item.id} className="flex flex-wrap items-center gap-4 p-4">
                    <CatalogImage
                      slug={item.productSlug}
                      name={item.productName}
                      image={
                        item.imageUrl
                          ? { id: 0, url: item.imageUrl, alt: item.productName, width: null, height: null }
                          : null
                      }
                      variant="square"
                      fallbackVariant={i}
                      fallbackSize="sm"
                      className="size-20 shrink-0 rounded-lg"
                    />
                    <div className="min-w-0 flex-1">
                      <Link to={`/produtos/${item.productSlug}`} className="font-semibold hover:text-accent">
                        {item.productName}
                      </Link>
                      {item.options.length > 0 && (
                        <p className="text-sm text-fg-muted">
                          {item.options.map((o) => `${o.group}: ${o.label}`).join(' · ')}
                        </p>
                      )}
                      <p className="text-sm text-fg-muted">{formatPrice(item.unitPrice)} cada</p>
                      {item.problem && (
                        <p className="mt-1 flex items-center gap-1 text-sm text-danger">
                          <AlertTriangle aria-hidden className="size-4" /> {item.problem}
                        </p>
                      )}
                    </div>
                    <QuantitySelect
                      label={`Quantidade de ${item.productName}`}
                      value={item.quantity}
                      max={item.maxQuantity}
                      onChange={(q) => void run(() => updateQuantity(item.id, q))}
                    />
                    <span className="w-24 text-right font-semibold">{formatPrice(item.lineTotal)}</span>
                    <button
                      type="button"
                      onClick={() => void run(() => remove(item.id))}
                      className="inline-flex size-9 items-center justify-center rounded-lg text-fg-muted hover:bg-danger/10 hover:text-danger"
                      aria-label={`Remover ${item.productName}`}
                    >
                      <Trash2 aria-hidden className="size-4" />
                    </button>
                  </li>
                ))
              : localItems.map((item, i) => (
                  <li key={`${item.productSlug}-${i}`} className="flex flex-wrap items-center gap-4 p-4">
                    <CatalogImage
                      slug={item.productSlug}
                      name={item.name}
                      variant="square"
                      fallbackVariant={i}
                      fallbackSize="sm"
                      className="size-20 shrink-0 rounded-lg"
                    />
                    <div className="min-w-0 flex-1">
                      <Link to={`/produtos/${item.productSlug}`} className="font-semibold hover:text-accent">
                        {item.name}
                      </Link>
                      {item.optionsLabel && <p className="text-sm text-fg-muted">{item.optionsLabel}</p>}
                      <p className="text-sm text-fg-muted">{formatPrice(item.unitPrice)} cada (referência)</p>
                    </div>
                    <QuantitySelect
                      label={`Quantidade de ${item.name}`}
                      value={item.quantity}
                      max={20}
                      onChange={(q) => updateLocal(i, q)}
                    />
                    <button
                      type="button"
                      onClick={() => removeLocal(i)}
                      className="inline-flex size-9 items-center justify-center rounded-lg text-fg-muted hover:bg-danger/10 hover:text-danger"
                      aria-label={`Remover ${item.name}`}
                    >
                      <Trash2 aria-hidden className="size-4" />
                    </button>
                  </li>
                ))}
          </ul>

          <aside className="h-fit rounded-card border border-border bg-surface p-5 shadow-card">
            <h2 className="font-semibold">Resumo</h2>
            {user ? (
              <>
                <dl className="mt-4 flex justify-between text-sm">
                  <dt className="text-fg-muted">Subtotal ({serverCart!.itemCount} itens)</dt>
                  <dd className="font-semibold">{formatPrice(serverCart!.subtotal)}</dd>
                </dl>
                <p className="mt-2 text-xs text-fg-muted">Frete calculado na próxima etapa.</p>
                {serverCart!.hasProblems ? (
                  <p className="mt-4 text-sm text-danger">Ajuste os itens marcados para continuar.</p>
                ) : (
                  <Link to="/checkout" className={buttonClass('primary', 'lg', 'mt-4 w-full')}>
                    Finalizar compra
                  </Link>
                )}
              </>
            ) : (
              <>
                <p className="mt-4 text-sm text-fg-muted">
                  Os preços são confirmados pelo sistema quando você entra na sua conta.
                </p>
                <Link to="/entrar?next=%2Fcheckout" className={buttonClass('primary', 'lg', 'mt-4 w-full')}>
                  Entrar para finalizar
                </Link>
                <Link
                  to="/cadastro"
                  className="mt-3 block text-center text-sm font-semibold text-accent hover:underline"
                >
                  Criar conta
                </Link>
              </>
            )}
          </aside>
        </div>
      )}
    </div>
  )
}
