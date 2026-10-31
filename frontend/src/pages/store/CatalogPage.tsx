import { ChevronLeft, ChevronRight, PackageSearch, Search, WifiOff } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useCategories, useProducts, type ProductSort } from '../../api/catalog'
import { ProductCard } from '../../components/store/ProductCard'
import { buttonClass } from '../../components/ui/button'
import { DemoBanner } from '../../components/ui/DemoBanner'
import { EmptyState } from '../../components/ui/EmptyState'
import { controlClass } from '../../components/ui/Field'
import { CardsSkeleton } from '../../components/ui/Skeleton'
import { usePageMeta } from '../../lib/seo'

const PAGE_SIZE = 12
const SORTS: Array<{ value: ProductSort; label: string }> = [
  { value: 'relevance', label: 'Destaques' },
  { value: 'price_asc', label: 'Menor preço' },
  { value: 'price_desc', label: 'Maior preço' },
  { value: 'name', label: 'Nome (A–Z)' },
  { value: 'newest', label: 'Novidades' },
]

export function CatalogPage() {
  const [params, setParams] = useSearchParams()
  const searchRef = useRef<HTMLInputElement>(null)
  const q = params.get('q') ?? ''
  const category = params.get('categoria') ?? ''
  const sortParam = params.get('ordem') as ProductSort | null
  const sort = SORTS.some((s) => s.value === sortParam) ? (sortParam as ProductSort) : 'relevance'
  const page = Math.max(1, Number(params.get('pagina')) || 1)
  // a busca digitada só vai para a URL/API após uma pausa (evita uma requisição por tecla)
  const [typed, setTyped] = useState(q)

  const { data: categories = [] } = useCategories()
  const { data, isPending, isError, isFetching, refetch } = useProducts({
    q: q || undefined,
    category: category || undefined,
    sort,
    page: page - 1,
    size: PAGE_SIZE,
  })
  const categoryName = categories.find((c) => c.slug === category)?.name
  usePageMeta(categoryName ?? 'Produtos', categories.find((c) => c.slug === category)?.description)

  useEffect(() => {
    if (params.get('busca')) searchRef.current?.focus()
  }, [params])

  function update(key: string, value: string) {
    const next = new URLSearchParams(params)
    next.delete('busca')
    if (value) next.set(key, value)
    else next.delete(key)
    if (key !== 'pagina') next.delete('pagina')
    setParams(next, { replace: true })
  }

  useEffect(() => {
    if (typed.trim() === q) return
    const t = setTimeout(() => {
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          next.delete('busca')
          next.delete('pagina')
          if (typed.trim()) next.set('q', typed.trim())
          else next.delete('q')
          return next
        },
        { replace: true },
      )
    }, 350)
    return () => clearTimeout(t)
  }, [typed, q, setParams])

  const items = data?.content ?? []
  const pages = data?.totalPages ?? 1

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <nav aria-label="Trilha de navegação" className="text-sm text-fg-muted">
        <Link to="/" className="hover:text-fg">
          Início
        </Link>{' '}
        / <span className="text-fg">{categoryName ?? 'Produtos'}</span>
      </nav>
      <h1 className="mt-2 text-3xl font-bold">{categoryName ?? 'Produtos'}</h1>

      <div className="mt-4">
        <DemoBanner>Catálogo de demonstração do ambiente de desenvolvimento (imagens ilustrativas).</DemoBanner>
      </div>

      <form role="search" className="mt-6 grid gap-3 sm:grid-cols-[1fr_auto_auto]" onSubmit={(e) => e.preventDefault()}>
        <label className="relative">
          <span className="sr-only">Buscar produtos</span>
          <Search aria-hidden className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-fg-muted" />
          <input
            ref={searchRef}
            type="search"
            value={typed}
            maxLength={100}
            onChange={(e) => setTyped(e.target.value)}
            placeholder="Buscar produtos…"
            className={`${controlClass} h-10 pl-9`}
          />
        </label>
        <label>
          <span className="sr-only">Categoria</span>
          <select
            value={category}
            onChange={(e) => update('categoria', e.target.value)}
            className={`${controlClass} h-10`}
          >
            <option value="">Todas as categorias</option>
            {categories.map((c) => (
              <option key={c.slug} value={c.slug}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span className="sr-only">Ordenar por</span>
          <select value={sort} onChange={(e) => update('ordem', e.target.value)} className={`${controlClass} h-10`}>
            {SORTS.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
        </label>
      </form>

      {data && (
        <p className="mt-4 text-sm text-fg-muted" aria-live="polite">
          {data.totalElements} {data.totalElements === 1 ? 'produto encontrado' : 'produtos encontrados'}
          {isFetching && ' · atualizando…'}
        </p>
      )}

      {isPending ? (
        <CardsSkeleton count={8} aspect="aspect-[4/5]" />
      ) : isError ? (
        <div className="mt-6">
          <EmptyState
            icon={WifiOff}
            title="Não foi possível carregar os produtos"
            description="Verifique sua conexão e tente novamente."
            action={
              <button type="button" className={buttonClass('outline')} onClick={() => void refetch()}>
                Tentar novamente
              </button>
            }
          />
        </div>
      ) : items.length === 0 ? (
        <div className="mt-6">
          <EmptyState
            icon={PackageSearch}
            title="Nenhum produto encontrado"
            description="Tente outro termo de busca ou remova os filtros."
            action={
              <button
                type="button"
                className={buttonClass('outline')}
                onClick={() => {
                  setTyped('')
                  setParams({}, { replace: true })
                }}
              >
                Limpar filtros
              </button>
            }
          />
        </div>
      ) : (
        <div className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
          {items.map((p, i) => (
            <ProductCard key={p.slug} product={p} index={i} />
          ))}
        </div>
      )}

      {pages > 1 && (
        <nav aria-label="Paginação" className="mt-8 flex items-center justify-center gap-2">
          <button
            type="button"
            className={buttonClass('outline', 'sm')}
            disabled={page <= 1}
            onClick={() => update('pagina', String(page - 1))}
          >
            <ChevronLeft aria-hidden className="size-4" /> Anterior
          </button>
          <span className="px-3 text-sm text-fg-muted">
            Página {page} de {pages}
          </span>
          <button
            type="button"
            className={buttonClass('outline', 'sm')}
            disabled={page >= pages}
            onClick={() => update('pagina', String(page + 1))}
          >
            Próxima <ChevronRight aria-hidden className="size-4" />
          </button>
        </nav>
      )}
    </div>
  )
}
