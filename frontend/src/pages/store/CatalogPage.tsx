import { ChevronLeft, ChevronRight, PackageSearch, Search } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ProductCard } from '../../components/store/ProductCard'
import { buttonClass } from '../../components/ui/button'
import { DemoBanner } from '../../components/ui/DemoBanner'
import { EmptyState } from '../../components/ui/EmptyState'
import { controlClass } from '../../components/ui/Field'
import { getCategory, listCategories, listPublicProducts, type ProductSort } from '../../data/store'

const PAGE_SIZE = 8
const SORTS: Array<{ value: ProductSort; label: string }> = [
  { value: 'relevancia', label: 'Destaques' },
  { value: 'menor-preco', label: 'Menor preço' },
  { value: 'maior-preco', label: 'Maior preço' },
  { value: 'nome', label: 'Nome (A–Z)' },
]

export function CatalogPage() {
  const [params, setParams] = useSearchParams()
  const searchRef = useRef<HTMLInputElement>(null)
  const q = params.get('q') ?? ''
  const category = params.get('categoria') ?? ''
  const sort = (params.get('ordem') as ProductSort | null) ?? 'relevancia'
  const page = Math.max(1, Number(params.get('pagina')) || 1)

  useEffect(() => {
    if (params.get('busca')) searchRef.current?.focus()
  }, [params])

  const all = listPublicProducts({ q, category: category || undefined, sort })
  const pages = Math.max(1, Math.ceil(all.length / PAGE_SIZE))
  const current = Math.min(page, pages)
  const items = all.slice((current - 1) * PAGE_SIZE, current * PAGE_SIZE)
  const categoryName = getCategory(category)?.name

  function update(key: string, value: string) {
    const next = new URLSearchParams(params)
    next.delete('busca')
    if (value) next.set(key, value)
    else next.delete(key)
    if (key !== 'pagina') next.delete('pagina')
    setParams(next, { replace: true })
  }

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
        <DemoBanner>Produtos fictícios para visualizar o layout; o catálogo real vem da API na Fase 07.</DemoBanner>
      </div>

      <form role="search" className="mt-6 grid gap-3 sm:grid-cols-[1fr_auto_auto]" onSubmit={(e) => e.preventDefault()}>
        <label className="relative">
          <span className="sr-only">Buscar produtos</span>
          <Search aria-hidden className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-fg-muted" />
          <input
            ref={searchRef}
            type="search"
            value={q}
            onChange={(e) => update('q', e.target.value)}
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
            {listCategories().map((c) => (
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

      <p className="mt-4 text-sm text-fg-muted" aria-live="polite">
        {all.length} {all.length === 1 ? 'produto encontrado' : 'produtos encontrados'}
      </p>

      {items.length === 0 ? (
        <div className="mt-6">
          <EmptyState
            icon={PackageSearch}
            title="Nenhum produto encontrado"
            description="Tente outro termo de busca ou remova os filtros."
            action={
              <button type="button" className={buttonClass('outline')} onClick={() => setParams({}, { replace: true })}>
                Limpar filtros
              </button>
            }
          />
        </div>
      ) : (
        <div className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
          {items.map((p, i) => (
            <ProductCard key={p.id} product={p} index={i} />
          ))}
        </div>
      )}

      {pages > 1 && (
        <nav aria-label="Paginação" className="mt-8 flex items-center justify-center gap-2">
          <button
            type="button"
            className={buttonClass('outline', 'sm')}
            disabled={current === 1}
            onClick={() => update('pagina', String(current - 1))}
          >
            <ChevronLeft aria-hidden className="size-4" /> Anterior
          </button>
          <span className="px-3 text-sm text-fg-muted">
            Página {current} de {pages}
          </span>
          <button
            type="button"
            className={buttonClass('outline', 'sm')}
            disabled={current === pages}
            onClick={() => update('pagina', String(current + 1))}
          >
            Próxima <ChevronRight aria-hidden className="size-4" />
          </button>
        </nav>
      )}
    </div>
  )
}
