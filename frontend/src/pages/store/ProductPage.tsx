import { Clock, Minus, PackageCheck, Plus, ShieldCheck, ShoppingCart, Truck } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useProduct, useProducts } from '../../api/catalog'
import type { ProductDetail } from '../../api/types'
import { useCart } from '../../cart/CartProvider'
import { AVAILABILITY } from '../../components/store/availability'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { CatalogImage } from '../../components/ui/CatalogImage'
import { DemoBanner } from '../../components/ui/DemoBanner'
import { DemoTag } from '../../components/ui/DemoTag'
import { Notice } from '../../components/ui/Notice'
import { ApiError } from '../../lib/api'
import { cn } from '../../lib/cn'
import { formatPrice } from '../../lib/format'
import type { ProductImageVariant } from '../../lib/images'
import { JsonLd } from '../../components/JsonLd'
import { usePageMeta } from '../../lib/seo'
import { NotFoundPage } from '../PlaceholderPage'

const SCHEMA_AVAILABILITY = {
  IN_STOCK: 'https://schema.org/InStock',
  OUT_OF_STOCK: 'https://schema.org/OutOfStock',
  MADE_TO_ORDER: 'https://schema.org/PreOrder',
  ON_REQUEST: 'https://schema.org/PreOrder',
} as const

/** Tela 2 da referência. */
export function ProductPage() {
  const { slug = '' } = useParams()
  const { data, isPending, error } = useProduct(slug)
  if (isPending) {
    return (
      <p role="status" className="px-4 py-24 text-center text-sm text-fg-muted">
        Carregando produto…
      </p>
    )
  }
  if (error instanceof ApiError && error.status === 404) return <NotFoundPage />
  if (!data) {
    return (
      <div className="mx-auto max-w-xl px-4 py-24">
        <Notice>Não foi possível carregar o produto. Tente novamente em instantes.</Notice>
      </div>
    )
  }
  return <ProductDetailView key={data.slug} product={data} />
}

function ProductDetailView({ product }: { product: ProductDetail }) {
  const { add } = useCart()
  const [view, setView] = useState(0)
  // um valor por grupo de opção (o primeiro já vem escolhido)
  const [selected, setSelected] = useState<Record<string, number>>(() =>
    Object.fromEntries(product.options.map((g) => [g.name, g.values[0]?.id])),
  )
  const [quantity, setQuantity] = useState(1)
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null)
  const [adding, setAdding] = useState(false)
  usePageMeta(product.seoTitle ?? product.name, product.seoDescription ?? product.description)

  const { data: relatedPage } = useProducts({ category: product.categorySlug, size: 4 })
  const related = (relatedPage?.content ?? []).filter((p) => p.slug !== product.slug).slice(0, 3)

  const status = AVAILABILITY[product.availability]
  const extra = product.options.reduce(
    (sum, g) => sum + (g.values.find((v) => v.id === selected[g.name])?.priceDelta ?? 0),
    0,
  )
  const unit = product.effectivePrice + extra
  const purchasable = product.availability === 'IN_STOCK' || product.availability === 'MADE_TO_ORDER'
  const maxQty = product.availability === 'MADE_TO_ORDER' ? 10 : 99

  const views: Array<{ label: string; variant: ProductImageVariant; imageIndex: number }> = product.images.length
    ? product.images.map((img, i) => ({ label: img.alt || `Foto ${i + 1}`, variant: 'detail', imageIndex: i }))
    : [
        { label: 'Detalhe', variant: 'detail', imageIndex: -1 },
        { label: 'Vista ampla', variant: 'card', imageIndex: -1 },
        { label: 'Enquadramento quadrado', variant: 'square', imageIndex: -1 },
      ]
  const current = views[view] ?? views[0]

  async function addToCart() {
    setAdding(true)
    setMessage(null)
    try {
      const optionValueIds = Object.values(selected).filter((v): v is number => typeof v === 'number')
      await add({
        productSlug: product.slug,
        optionValueIds,
        quantity,
        name: product.name,
        unitPrice: unit,
        optionsLabel:
          product.options
            .map((g) => `${g.name}: ${g.values.find((v) => v.id === selected[g.name])?.label ?? ''}`)
            .join(' · ') || null,
      })
      setMessage({ ok: true, text: 'Adicionado ao carrinho.' })
    } catch (e) {
      setMessage({
        ok: false,
        text: e instanceof ApiError ? (e.fieldErrors[0]?.message ?? e.message) : 'Não foi possível adicionar.',
      })
    } finally {
      setAdding(false)
    }
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <JsonLd
        data={{
          '@context': 'https://schema.org',
          '@type': 'Product',
          name: product.name,
          description: product.description,
          image: product.images.map((i) => i.url),
          offers: {
            '@type': 'Offer',
            priceCurrency: 'BRL',
            price: product.effectivePrice.toFixed(2),
            availability: SCHEMA_AVAILABILITY[product.availability],
          },
        }}
      />
      <nav aria-label="Trilha de navegação" className="text-sm text-fg-muted">
        <ol className="flex flex-wrap gap-1">
          <li>
            <Link to="/" className="hover:text-fg">
              Início
            </Link>{' '}
            /
          </li>
          <li>
            <Link to="/produtos" className="hover:text-fg">
              Produtos
            </Link>{' '}
            /
          </li>
          <li>
            <Link to={`/produtos?categoria=${product.categorySlug}`} className="text-accent hover:underline">
              {product.categoryName}
            </Link>{' '}
            /
          </li>
          <li aria-current="page" className="text-fg">
            {product.name}
          </li>
        </ol>
      </nav>

      <div className="mt-4">
        <DemoBanner>Produto do catálogo de demonstração (imagem ilustrativa).</DemoBanner>
      </div>

      <div className="mt-6 grid gap-8 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)_18rem]">
        <div className="flex flex-col-reverse gap-3 sm:flex-row">
          <ul className="flex gap-3 sm:flex-col" aria-label="Miniaturas">
            {views.map((v, i) => (
              <li key={v.label + i}>
                <button
                  type="button"
                  onClick={() => setView(i)}
                  aria-label={`Ver imagem: ${v.label}`}
                  aria-pressed={view === i}
                  className={cn(
                    'block overflow-hidden rounded-lg border-2',
                    view === i ? 'border-accent' : 'border-transparent opacity-70 hover:opacity-100',
                  )}
                >
                  <CatalogImage
                    slug={product.slug}
                    name={`${product.name} — ${v.label}`}
                    image={v.imageIndex >= 0 ? product.images[v.imageIndex] : null}
                    variant={v.variant}
                    fallbackVariant={i}
                    fallbackSize="xs"
                    className="size-16"
                  />
                </button>
              </li>
            ))}
          </ul>
          <div className="relative flex-1">
            <CatalogImage
              slug={product.slug}
              name={`${product.name} — ${current.label}`}
              image={current.imageIndex >= 0 ? product.images[current.imageIndex] : null}
              variant={current.variant}
              priority
              fallbackVariant={view}
              fallbackSize="lg"
              className="aspect-square w-full rounded-card"
            />
            <DemoTag className="absolute left-3 top-3" />
          </div>
        </div>

        <div>
          <h1 className="text-3xl font-bold">{product.name}</h1>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Badge tone={status.tone}>{status.label}</Badge>
          </div>
          <p className="mt-4 flex items-baseline gap-3">
            <span className="text-3xl font-bold text-accent">{formatPrice(unit)}</span>
            {product.promoPrice !== null && (
              <span className="text-fg-muted line-through">{formatPrice(product.price + extra)}</span>
            )}
          </p>
          {product.type === 'CUSTOM' && (
            <p className="mt-1 text-sm text-fg-muted">Valor de referência; o preço final é definido no orçamento.</p>
          )}
          <p className="mt-4 whitespace-pre-line text-fg-muted">{product.description}</p>

          {product.options.map((group) => (
            <fieldset key={group.name} className="mt-6">
              <legend className="text-sm font-semibold">{group.name}</legend>
              <div className="mt-2 flex flex-wrap gap-2">
                {group.values.map((v) => {
                  const active = selected[group.name] === v.id
                  return (
                    <label
                      key={v.id}
                      className={cn(
                        'cursor-pointer rounded-lg border px-4 py-2 text-sm font-medium transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent',
                        active ? 'border-accent bg-primary text-on-primary' : 'border-border hover:bg-surface-muted',
                      )}
                    >
                      <input
                        type="radio"
                        name={group.name}
                        className="sr-only"
                        checked={active}
                        onChange={() => setSelected((s) => ({ ...s, [group.name]: v.id }))}
                      />
                      {v.label}
                      {v.priceDelta > 0 ? ` (+${formatPrice(v.priceDelta)})` : ''}
                    </label>
                  )
                })}
              </div>
            </fieldset>
          ))}

          {purchasable ? (
            <div className="mt-6 flex flex-wrap items-center gap-3">
              <div className="flex items-center rounded-lg border border-border" role="group" aria-label="Quantidade">
                <button
                  type="button"
                  className="inline-flex size-11 items-center justify-center text-fg-muted hover:text-fg disabled:opacity-40"
                  onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                  disabled={quantity <= 1}
                  aria-label="Diminuir quantidade"
                >
                  <Minus aria-hidden className="size-4" />
                </button>
                <output className="w-10 text-center font-semibold" aria-live="polite">
                  {quantity}
                </output>
                <button
                  type="button"
                  className="inline-flex size-11 items-center justify-center text-fg-muted hover:text-fg disabled:opacity-40"
                  onClick={() => setQuantity((q) => Math.min(maxQty, q + 1))}
                  disabled={quantity >= maxQty}
                  aria-label="Aumentar quantidade"
                >
                  <Plus aria-hidden className="size-4" />
                </button>
              </div>
              <button
                type="button"
                className={buttonClass('primary', 'lg', 'flex-1')}
                onClick={() => void addToCart()}
                disabled={adding}
              >
                <ShoppingCart aria-hidden className="size-5" />
                {adding ? 'Adicionando…' : 'Adicionar ao carrinho'}
              </button>
            </div>
          ) : product.type === 'CUSTOM' ? (
            <Link to="/personalizados" className={buttonClass('primary', 'lg', 'mt-6')}>
              Solicitar orçamento
            </Link>
          ) : (
            <p className="mt-6 text-sm text-fg-muted">Produto esgotado no momento.</p>
          )}
          {message && (
            <p role="status" className={cn('mt-4 text-sm', message.ok ? 'text-success' : 'text-danger')}>
              {message.text}{' '}
              {message.ok && (
                <Link to="/carrinho" className="font-semibold text-accent underline">
                  Ver carrinho
                </Link>
              )}
            </p>
          )}
        </div>

        <aside aria-labelledby="relacionados" className="rounded-card border border-border bg-surface p-4 shadow-card">
          <h2 id="relacionados" className="font-semibold">
            Também pode te interessar
          </h2>
          <ul className="mt-4 flex flex-col gap-4">
            {related.map((p, i) => (
              <li key={p.slug}>
                <Link to={`/produtos/${p.slug}`} className="group flex items-center gap-3">
                  <CatalogImage
                    slug={p.slug}
                    name={p.name}
                    image={p.image}
                    variant="square"
                    fallbackVariant={i + 1}
                    fallbackSize="sm"
                    className="size-16 shrink-0 rounded-lg"
                  />
                  <span>
                    <span className="block text-sm font-medium group-hover:text-accent">{p.name}</span>
                    <span className="block text-sm font-semibold text-accent">{formatPrice(p.effectivePrice)}</span>
                  </span>
                </Link>
              </li>
            ))}
            {related.length === 0 && <li className="text-sm text-fg-muted">Sem outros itens nesta categoria.</li>}
          </ul>
        </aside>
      </div>

      <section
        aria-label="Informações do produto"
        className="mt-12 grid gap-4 border-t border-border pt-8 md:grid-cols-3"
      >
        <Info icon={Clock} title="Produção">
          {product.productionDays
            ? `Produzido após a confirmação do pagamento, em até ${product.productionDays} dias úteis.`
            : 'Item pronto para envio após a confirmação do pagamento.'}
        </Info>
        <Info icon={Truck} title="Envio">
          Entrega conforme o CEP (calculada no checkout) ou retirada combinada com a equipe.
        </Info>
        <Info icon={PackageCheck} title="Cuidados">
          Evite exposição prolongada ao sol e a fontes de calor. Limpe com pano seco e macio.
        </Info>
      </section>
      <p className="mt-6 flex items-center gap-2 text-xs text-fg-muted">
        <ShieldCheck aria-hidden className="size-4" /> PIX ou cartão — cobrança enviada pela equipe após o pedido;
        nenhum dado de cartão é coletado neste site.
      </p>
    </div>
  )
}

function Info({ icon: Icon, title, children }: { icon: typeof Clock; title: string; children: string }) {
  return (
    <div className="flex gap-3">
      <Icon aria-hidden className="mt-0.5 size-6 shrink-0 text-accent" />
      <div>
        <h3 className="font-semibold">{title}</h3>
        <p className="mt-1 text-sm text-fg-muted">{children}</p>
      </div>
    </div>
  )
}
