import { Archive, PackageSearch, Pencil, Plus, Search } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { DemoBanner } from '../../components/ui/DemoBanner'
import { EmptyState } from '../../components/ui/EmptyState'
import { controlClass } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { PageHeader } from '../../components/ui/PageHeader'
import { ProductMedia } from '../../components/ui/ProductMedia'
import { PRODUCT_STATUS, PRODUCT_TYPE } from '../../data/labels'
import { getCategory, listAdminProducts, listCategories } from '../../data/store'
import type { Product, ProductStatus } from '../../data/types'
import { formatBRL } from '../../lib/format'

/** Tela 7 da referência. */
export function ProductsPage() {
  const [q, setQ] = useState('')
  const [category, setCategory] = useState('')
  const [status, setStatus] = useState<ProductStatus | ''>('')
  const [archiving, setArchiving] = useState<Product | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const term = q.trim().toLowerCase()
  const products = listAdminProducts().filter(
    (p) =>
      (!term || p.name.toLowerCase().includes(term) || p.sku.toLowerCase().includes(term)) &&
      (!category || p.categorySlug === category) &&
      (!status || p.status === status),
  )

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
      <DemoBanner />
      {notice && <Notice>{notice}</Notice>}

      <div className="grid gap-3 sm:grid-cols-[1fr_auto_auto]">
        <label className="relative">
          <span className="sr-only">Buscar produto por nome ou SKU</span>
          <Search aria-hidden className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-fg-muted" />
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Buscar por nome ou SKU…"
            className={`${controlClass} h-10 pl-9`}
          />
        </label>
        <label>
          <span className="sr-only">Categoria</span>
          <select value={category} onChange={(e) => setCategory(e.target.value)} className={`${controlClass} h-10`}>
            <option value="">Todas as categorias</option>
            {listCategories().map((c) => (
              <option key={c.slug} value={c.slug}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span className="sr-only">Status</span>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as ProductStatus | '')}
            className={`${controlClass} h-10`}
          >
            <option value="">Todos os status</option>
            {Object.entries(PRODUCT_STATUS).map(([k, v]) => (
              <option key={k} value={k}>
                {v.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {products.length === 0 ? (
        <EmptyState
          icon={PackageSearch}
          title="Nenhum produto encontrado"
          description="Ajuste a busca ou os filtros."
        />
      ) : (
        <div className="overflow-x-auto rounded-card border border-border bg-surface shadow-card">
          <table className="w-full min-w-[48rem] text-sm">
            <caption className="sr-only">Lista de produtos</caption>
            <thead className="border-b border-border text-left text-xs uppercase tracking-wide text-fg-muted">
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
              {products.map((p, i) => (
                <tr key={p.id} className="border-b border-border last:border-0 hover:bg-surface-muted/50">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <ProductMedia
                        name={p.name}
                        image={p.image}
                        variant="square"
                        fallbackVariant={i}
                        fallbackSize="xs"
                        className="size-10 shrink-0 rounded-lg"
                      />
                      <div>
                        <p className="font-medium">{p.name}</p>
                        <p className="text-xs text-fg-muted">
                          {p.sku} · {PRODUCT_TYPE[p.type]}
                        </p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-fg-muted">{getCategory(p.categorySlug)?.name}</td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    {formatBRL(p.promoPriceCents ?? p.priceCents)}
                    {p.promoPriceCents && (
                      <span className="block text-xs text-fg-muted line-through">{formatBRL(p.priceCents)}</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    {p.type === 'PRONTO' ? (
                      <span className={p.stock <= p.lowStockThreshold ? 'font-semibold text-danger' : ''}>
                        {p.stock}
                      </span>
                    ) : (
                      <span className="text-fg-muted">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <Badge tone={PRODUCT_STATUS[p.status].tone}>{PRODUCT_STATUS[p.status].label}</Badge>
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
                        aria-label={`Arquivar ${p.name}`}
                        onClick={() => setArchiving(p)}
                      >
                        <Archive aria-hidden className="size-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={archiving !== null}
        title="Arquivar produto?"
        description={`"${archiving?.name ?? ''}" deixará de aparecer na loja, mas continuará no histórico de pedidos. Você pode reativá-lo depois.`}
        confirmLabel="Arquivar"
        onCancel={() => setArchiving(null)}
        onConfirm={() => {
          setArchiving(null)
          setNotice('Arquivamento será salvo quando a API de catálogo estiver disponível (Fase 04). Nada foi alterado.')
        }}
      />
    </div>
  )
}
