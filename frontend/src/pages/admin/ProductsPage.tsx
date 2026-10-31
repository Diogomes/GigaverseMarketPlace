import { Archive, ArchiveRestore, PackageSearch, Pencil, Plus } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import {
  useAdminCategories,
  useAdminProducts,
  useCatalogActions,
  type AdminProductListItem,
  type ProductStatus,
} from '../../api/admin'
import { apiGet } from '../../lib/api'
import type { AdminProduct } from '../../api/admin'
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
import { PRODUCT_STATUS_UI, PRODUCT_TYPE_UI } from '../../components/admin/labels'
import { Badge } from '../../components/ui/Badge'
import { CatalogImage } from '../../components/ui/CatalogImage'
import { buttonClass } from '../../components/ui/button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { EmptyState } from '../../components/ui/EmptyState'
import { Notice } from '../../components/ui/Notice'
import { PageHeader } from '../../components/ui/PageHeader'
import { formatPrice } from '../../lib/format'
import { useDebounced } from '../../lib/useDebounced'

/** Tela 7 da referência. */
export function ProductsPage() {
  const [q, setQ] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [status, setStatus] = useState<ProductStatus | ''>('')
  const [page, setPage] = useState(0)
  const [target, setTarget] = useState<AdminProductListItem | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const search = useDebounced(q.trim())
  const categories = useAdminCategories()
  const { data, error, isPending, refetch } = useAdminProducts({
    q: search || undefined,
    categoryId: categoryId ? Number(categoryId) : undefined,
    status: status || undefined,
    page,
    size: 20,
  })
  const actions = useCatalogActions()
  const archiving = target?.status !== 'ARCHIVED'

  async function toggleArchive(p: AdminProductListItem) {
    setTarget(null)
    setFailure(null)
    try {
      // a versão vem do detalhe atual: se outra pessoa editou, o servidor responde 409
      const current = await apiGet<AdminProduct>(`/api/v1/admin/products/${p.id}`)
      const next: ProductStatus = p.status === 'ARCHIVED' ? 'ACTIVE' : 'ARCHIVED'
      await actions.changeStatus(p.id, next, current.version)
      setNotice(next === 'ARCHIVED' ? `"${p.name}" foi arquivado.` : `"${p.name}" está ativo na loja.`)
    } catch (e) {
      setFailure(errorMessage(e))
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Produtos"
        description="Gerencie seus produtos e estoque."
        actions={
          <Link to="/admin/produtos/novo" className={buttonClass('primary')}>
            <Plus aria-hidden className="size-4" /> Novo produto
          </Link>
        }
      />
      {notice && <Notice>{notice}</Notice>}
      <ActionError message={failure} />

      <div className="grid gap-3 sm:grid-cols-[1fr_auto_auto]">
        <SearchInput
          value={q}
          onChange={(v) => {
            setQ(v)
            setPage(0)
          }}
          label="Buscar produto por nome ou SKU"
          placeholder="Buscar por nome ou SKU…"
        />
        <FilterSelect
          label="Categoria"
          value={categoryId}
          onChange={(v) => {
            setCategoryId(v)
            setPage(0)
          }}
        >
          <option value="">Todas as categorias</option>
          {categories.data?.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect
          label="Status"
          value={status}
          onChange={(v) => {
            setStatus(v as ProductStatus | '')
            setPage(0)
          }}
        >
          <option value="">Todos os status</option>
          {Object.entries(PRODUCT_STATUS_UI).map(([k, v]) => (
            <option key={k} value={k}>
              {v.label}
            </option>
          ))}
        </FilterSelect>
      </div>

      {error ? (
        <QueryError error={error} onRetry={() => void refetch()} />
      ) : isPending ? (
        <Loading />
      ) : data.content.length === 0 ? (
        <EmptyState
          icon={PackageSearch}
          title="Nenhum produto encontrado"
          description="Ajuste a busca ou os filtros."
        />
      ) : (
        <>
          <div className={tableWrapClass}>
            <table className="w-full min-w-[48rem] text-sm">
              <caption className="sr-only">Lista de produtos</caption>
              <thead className={theadClass}>
                <tr>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Produto
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Categoria
                  </th>
                  <th scope="col" className="px-4 py-3 text-right font-medium">
                    Preço
                  </th>
                  <th scope="col" className="px-4 py-3 text-right font-medium">
                    Estoque
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Status
                  </th>
                  <th scope="col" className="px-4 py-3 text-right font-medium">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((p, i) => (
                  <tr key={p.id} className={rowClass}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <CatalogImage
                          slug={p.slug}
                          name={p.name}
                          image={p.imageUrl ? { id: 0, url: p.imageUrl, alt: '', width: null, height: null } : null}
                          variant="square"
                          fallbackVariant={i}
                          fallbackSize="xs"
                          className="size-10 shrink-0 rounded-lg"
                        />
                        <div>
                          <p className="font-medium">{p.name}</p>
                          <p className="text-xs text-fg-muted">
                            {p.sku} · {PRODUCT_TYPE_UI[p.type]}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-fg-muted">{p.categoryName}</td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      {formatPrice(p.promoPrice ?? p.price)}
                      {p.promoPrice !== null && (
                        <span className="block text-xs text-fg-muted line-through">{formatPrice(p.price)}</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      {p.type === 'READY' ? (
                        <span className={p.lowStock ? 'font-semibold text-danger' : ''}>
                          {p.stockOnHand}
                          {p.lowStock && <span className="sr-only"> (estoque baixo)</span>}
                        </span>
                      ) : (
                        <span className="text-fg-muted">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <Badge tone={PRODUCT_STATUS_UI[p.status].tone}>{PRODUCT_STATUS_UI[p.status].label}</Badge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-1">
                        <Link
                          to={`/admin/produtos/${p.id}`}
                          className={buttonClass('ghost', 'sm', 'size-9 px-0')}
                          aria-label={`Editar ${p.name}`}
                        >
                          <Pencil aria-hidden className="size-4" />
                        </Link>
                        <button
                          type="button"
                          className={buttonClass('ghost', 'sm', 'size-9 px-0')}
                          aria-label={p.status === 'ARCHIVED' ? `Reativar ${p.name}` : `Arquivar ${p.name}`}
                          onClick={() => setTarget(p)}
                        >
                          {p.status === 'ARCHIVED' ? (
                            <ArchiveRestore aria-hidden className="size-4" />
                          ) : (
                            <Archive aria-hidden className="size-4" />
                          )}
                        </button>
                      </div>
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

      <ConfirmDialog
        open={target !== null}
        title={archiving ? 'Arquivar produto?' : 'Reativar produto?'}
        description={
          archiving
            ? `"${target?.name ?? ''}" deixará de aparecer na loja, mas continuará no histórico de pedidos. Você pode reativá-lo depois.`
            : `"${target?.name ?? ''}" voltará a aparecer na loja.`
        }
        confirmLabel={archiving ? 'Arquivar' : 'Reativar'}
        onCancel={() => setTarget(null)}
        onConfirm={() => target && void toggleArchive(target)}
      />
    </div>
  )
}
