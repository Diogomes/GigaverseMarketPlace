import { Clock, Minus, PackageCheck, Plus, ShieldCheck, ShoppingCart, Truck } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { DemoBanner } from '../../components/ui/DemoBanner'
import { Notice } from '../../components/ui/Notice'
import { DemoTag } from '../../components/ui/DemoTag'
import { ProductMedia } from '../../components/ui/ProductMedia'
import { PRODUCT_TYPE } from '../../data/labels'
import { effectivePrice, getCategory, getPublicProduct, listRelatedProducts } from '../../data/store'
import type { Product } from '../../data/types'
import { cn } from '../../lib/cn'
import type { ProductImageVariant } from '../../lib/images'
import { formatBRL } from '../../lib/format'
import { NotFoundPage } from '../PlaceholderPage'

function availability(p: Product) {
  if (p.type === 'PERSONALIZADO') return { tone: 'accent' as const, label: 'Sob orçamento' }
  if (p.type === 'SOB_ENCOMENDA' || p.stock === 0) return { tone: 'warning' as const, label: 'Sob encomenda' }
  return { tone: 'success' as const, label: 'Em estoque' }
}

/** Tela 2 da referência visual. */
export function ProductPage() {
  const { slug = '' } = useParams()
  const product = getPublicProduct(slug)
  if (!product) return <NotFoundPage />
  return <ProductDetail key={product.id} product={product} />
}

function ProductDetail({ product }: { product: Product }) {
  const [view, setView] = useState(0)
  const [selected, setSelected] = useState<Record<string, number>>({})
  const [quantity, setQuantity] = useState(1)
  const [notice, setNotice] = useState(false)

  const category = getCategory(product.categorySlug)
  const related = listRelatedProducts(product)
  const status = availability(product)
  const extra = product.options.reduce((sum, g) => sum + (g.values[selected[g.name] ?? 0].extraCents ?? 0), 0)
  const unit = effectivePrice(product) + extra
  const maxQty = product.type === 'PRONTO' && product.stock > 0 ? product.stock : 10
  // Com imagem: os recortes do asset viram as vistas da galeria; sem imagem, placeholders identificados.
  const views: Array<{ label: string; variant: ProductImageVariant }> = product.image
    ? [
        { label: 'Detalhe', variant: 'detail' },
        { label: 'Vista ampla', variant: 'card' },
        { label: 'Enquadramento quadrado', variant: 'square' },
      ]
    : ['Frente', 'Lateral', 'Detalhe', 'Base'].map((label) => ({ label, variant: 'square' as const }))

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
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
          {category && (
            <li>
              <Link to={`/produtos?categoria=${category.slug}`} className="text-accent hover:underline">
                {category.name}
              </Link>{' '}
              /
            </li>
          )}
          <li aria-current="page" className="text-fg">
            {product.name}
          </li>
        </ol>
      </nav>

      <div className="mt-4">
        <DemoBanner>Produto fictício para visualizar o layout.</DemoBanner>
      </div>

      <div className="mt-6 grid gap-8 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)_18rem]">
        {/* Galeria */}
        <div className="flex flex-col-reverse gap-3 sm:flex-row">
          <ul className="flex gap-3 sm:flex-col" aria-label="Miniaturas">
            {views.map(({ label, variant }, i) => (
              <li key={label}>
                <button
                  type="button"
                  onClick={() => setView(i)}
                  aria-label={`Ver imagem: ${label}`}
                  aria-pressed={view === i}
                  className={cn(
                    'block overflow-hidden rounded-lg border-2',
                    view === i ? 'border-accent' : 'border-transparent opacity-70 hover:opacity-100',
                  )}
                >
                  <ProductMedia
                    name={`${product.name} — ${label}`}
                    image={product.image}
                    variant={variant}
                    fallbackVariant={i}
                    fallbackSize="xs"
                    className="size-16"
                  />
                </button>
              </li>
            ))}
          </ul>
          <div className="relative flex-1">
            <ProductMedia
              name={`${product.name} — ${views[view].label}`}
              image={product.image}
              variant={views[view].variant}
              priority
              fallbackVariant={view}
              fallbackSize="lg"
              className="aspect-square w-full rounded-card"
            />
            <DemoTag className="absolute left-3 top-3" />
          </div>
        </div>

        {/* Compra */}
        <div>
          <h1 className="text-3xl font-bold">{product.name}</h1>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Badge tone={status.tone}>{status.label}</Badge>
            {product.type !== 'PRONTO' && <Badge>{PRODUCT_TYPE[product.type]}</Badge>}
          </div>
          <p className="mt-4 flex items-baseline gap-3">
            <span className="text-3xl font-bold text-accent">{formatBRL(unit)}</span>
            {product.promoPriceCents && (
              <span className="text-fg-muted line-through">{formatBRL(product.priceCents + extra)}</span>
            )}
          </p>
          {product.type === 'PERSONALIZADO' && (
            <p className="mt-1 text-sm text-fg-muted">Valor de referência; o preço final é definido no orçamento.</p>
          )}
          <p className="mt-4 text-fg-muted">{product.description}</p>

          {product.options.map((group) => (
            <fieldset key={group.name} className="mt-6">
              <legend className="text-sm font-semibold">{group.name}</legend>
              <div className="mt-2 flex flex-wrap gap-2">
                {group.values.map((v, i) => {
                  const active = (selected[group.name] ?? 0) === i
                  return (
                    <label
                      key={v.label}
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
                        onChange={() => setSelected((s) => ({ ...s, [group.name]: i }))}
                      />
                      {v.label}
                      {v.extraCents ? ` (+${formatBRL(v.extraCents)})` : ''}
                    </label>
                  )
                })}
              </div>
            </fieldset>
          ))}

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
            <button type="button" className={buttonClass('primary', 'lg', 'flex-1')} onClick={() => setNotice(true)}>
              <ShoppingCart aria-hidden className="size-5" />
              {product.type === 'PERSONALIZADO' ? 'Solicitar orçamento' : 'Adicionar ao carrinho'}
            </button>
          </div>
          {notice && (
            <div className="mt-4">
              <Notice>O carrinho e o checkout serão habilitados na Fase 06. Nenhum pedido foi criado.</Notice>
            </div>
          )}
        </div>

        {/* Relacionados */}
        <aside aria-labelledby="relacionados" className="rounded-card border border-border bg-surface p-4 shadow-card">
          <h2 id="relacionados" className="font-semibold">
            Também pode te interessar
          </h2>
          <ul className="mt-4 flex flex-col gap-4">
            {related.map((p, i) => (
              <li key={p.id}>
                <Link to={`/produtos/${p.slug}`} className="group flex items-center gap-3">
                  <ProductMedia
                    name={p.name}
                    image={p.image}
                    variant="square"
                    fallbackVariant={i + 1}
                    fallbackSize="sm"
                    className="size-16 shrink-0 rounded-lg"
                  />
                  <span>
                    <span className="block text-sm font-medium group-hover:text-accent">{p.name}</span>
                    <span className="block text-sm font-semibold text-accent">{formatBRL(effectivePrice(p))}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </aside>
      </div>

      <section
        aria-label="Informações do produto"
        className="mt-12 grid gap-4 border-t border-border pt-8 md:grid-cols-3"
      >
        <Info icon={Clock} title="Produção">
          {product.productionDays
            ? `Produzido após a confirmação do pedido, em até ${product.productionDays} dias úteis.`
            : 'Item pronto para envio após a confirmação do pagamento.'}
        </Info>
        <Info icon={Truck} title="Envio">
          Prazo e valor do frete são calculados no checkout pelo CEP de entrega (em configuração).
        </Info>
        <Info icon={PackageCheck} title="Cuidados">
          Evite exposição prolongada ao sol e a fontes de calor. Limpe com pano seco e macio.
        </Info>
      </section>
      <p className="mt-6 flex items-center gap-2 text-xs text-fg-muted">
        <ShieldCheck aria-hidden className="size-4" /> Pagamento via PIX e cartão — integração em configuração.
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
