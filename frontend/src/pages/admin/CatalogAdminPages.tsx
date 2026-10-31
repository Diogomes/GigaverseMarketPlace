import { Boxes, Pencil, Plus, Tags, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import {
  useAdminCategories,
  useAdminProduct,
  useAdminProducts,
  useCatalogActions,
  type AdminCategory,
} from '../../api/admin'
import { useAuth } from '../../auth/AuthProvider'
import {
  ActionError,
  errorMessage,
  Loading,
  Pagination,
  QueryError,
  rowClass,
  SearchInput,
  tableWrapClass,
  theadClass,
} from '../../components/admin/AdminUi'
import { StockPanel } from '../../components/admin/StockPanel'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { EmptyState } from '../../components/ui/EmptyState'
import { controlClass } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { useDebounced } from '../../lib/useDebounced'

/** Estoque dos produtos de pronta entrega, com filtro de estoque baixo e lançamentos. */
export function StockPage() {
  const [q, setQ] = useState('')
  const [lowOnly, setLowOnly] = useState(false)
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<number | null>(null)
  const search = useDebounced(q.trim())
  const { data, error, isPending, refetch } = useAdminProducts({
    q: search || undefined,
    type: 'READY',
    lowStock: lowOnly || undefined,
    page,
    size: 20,
  })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Estoque" description="Saldos dos produtos de pronta entrega e histórico de movimentações." />
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="flex-1">
          <SearchInput
            value={q}
            onChange={(v) => {
              setQ(v)
              setPage(0)
            }}
            label="Buscar produto"
            placeholder="Buscar por nome ou SKU…"
          />
        </div>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={lowOnly}
            onChange={(e) => {
              setLowOnly(e.target.checked)
              setPage(0)
            }}
            className="size-4"
          />
          Somente estoque baixo
        </label>
      </div>
      <div className="grid gap-6 xl:grid-cols-[1fr_26rem]">
        <div className="flex min-w-0 flex-col gap-3">
          {error ? (
            <QueryError error={error} onRetry={() => void refetch()} />
          ) : isPending ? (
            <Loading />
          ) : data.content.length === 0 ? (
            <EmptyState icon={Boxes} title={lowOnly ? 'Nenhum item com estoque baixo' : 'Nenhum produto encontrado'} />
          ) : (
            <>
              <div className={tableWrapClass}>
                <table className="w-full min-w-[32rem] text-sm">
                  <caption className="sr-only">Estoque por produto</caption>
                  <thead className={theadClass}>
                    <tr>
                      <th scope="col" className="px-4 py-3 font-medium">
                        Produto
                      </th>
                      <th scope="col" className="px-4 py-3 text-right font-medium">
                        Em estoque
                      </th>
                      <th scope="col" className="px-4 py-3 font-medium">
                        Situação
                      </th>
                      <th scope="col" className="px-4 py-3 text-right font-medium">
                        <span className="sr-only">Ações</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((p) => (
                      <tr key={p.id} className={`${rowClass} ${selected === p.id ? 'bg-primary/5' : ''}`}>
                        <td className="px-4 py-3">
                          <p className="font-medium">{p.name}</p>
                          <p className="text-xs text-fg-muted">{p.sku}</p>
                        </td>
                        <td className="px-4 py-3 text-right font-semibold tabular-nums">{p.stockOnHand}</td>
                        <td className="px-4 py-3">
                          {p.lowStock ? <Badge tone="warning">Estoque baixo</Badge> : <Badge tone="success">OK</Badge>}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            type="button"
                            className={buttonClass('outline', 'sm')}
                            aria-pressed={selected === p.id}
                            onClick={() => setSelected(p.id)}
                          >
                            Movimentar
                          </button>
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
        </div>
        <StockSide productId={selected} />
      </div>
    </div>
  )
}

function StockSide({ productId }: { productId: number | null }) {
  const { data, isPending, error } = useAdminProduct(productId === null ? undefined : String(productId))
  if (productId === null) {
    return (
      <Card>
        <p className="text-sm text-fg-muted">Selecione um produto para lançar entradas, ajustes e ver o histórico.</p>
      </Card>
    )
  }
  return (
    <Card>
      {error ? (
        <ActionError message={errorMessage(error)} />
      ) : isPending ? (
        <Loading />
      ) : (
        <>
          <h2 className="font-semibold">
            <Link to={`/admin/produtos/${data.id}`} className="hover:text-accent hover:underline">
              {data.name}
            </Link>
          </h2>
          <p className="text-xs text-fg-muted">{data.sku}</p>
          <div className="mt-4">
            <StockPanel productId={data.id} stock={data.stock} />
          </div>
        </>
      )}
    </Card>
  )
}

/** Categorias: criar, renomear, ativar/desativar; excluir (somente ADMIN, sem produtos). */
export function CategoriesPage() {
  const { user } = useAuth()
  const { data, error, isPending, refetch } = useAdminCategories()
  const actions = useCatalogActions()
  const [editing, setEditing] = useState<AdminCategory | 'new' | null>(null)
  const [deleting, setDeleting] = useState<AdminCategory | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Categorias"
        description="Organização do catálogo exibida na loja."
        actions={
          <button type="button" className={buttonClass('primary')} onClick={() => setEditing('new')}>
            <Plus aria-hidden className="size-4" /> Nova categoria
          </button>
        }
      />
      {notice && <Notice>{notice}</Notice>}
      <ActionError message={failure} />
      {editing && (
        <CategoryForm
          category={editing === 'new' ? null : editing}
          onDone={(message) => {
            setEditing(null)
            setNotice(message)
          }}
          onCancel={() => setEditing(null)}
        />
      )}
      {error ? (
        <QueryError error={error} onRetry={() => void refetch()} />
      ) : isPending ? (
        <Loading />
      ) : data.length === 0 ? (
        <EmptyState icon={Tags} title="Nenhuma categoria" description="Crie a primeira categoria do catálogo." />
      ) : (
        <div className={tableWrapClass}>
          <table className="w-full min-w-[36rem] text-sm">
            <caption className="sr-only">Categorias</caption>
            <thead className={theadClass}>
              <tr>
                <th scope="col" className="px-4 py-3 font-medium">
                  Nome
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Slug
                </th>
                <th scope="col" className="px-4 py-3 text-right font-medium">
                  Ordem
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Situação
                </th>
                <th scope="col" className="px-4 py-3 text-right font-medium">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody>
              {data.map((c) => (
                <tr key={c.id} className={rowClass}>
                  <td className="px-4 py-3 font-medium">{c.name}</td>
                  <td className="px-4 py-3 text-fg-muted">{c.slug}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{c.sortOrder}</td>
                  <td className="px-4 py-3">
                    <Badge tone={c.active ? 'success' : 'neutral'}>{c.active ? 'Ativa' : 'Inativa'}</Badge>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-1">
                      <button
                        type="button"
                        className={buttonClass('ghost', 'sm', 'size-9 px-0')}
                        aria-label={`Editar ${c.name}`}
                        onClick={() => setEditing(c)}
                      >
                        <Pencil aria-hidden className="size-4" />
                      </button>
                      {user?.role === 'ADMIN' && (
                        <button
                          type="button"
                          className={buttonClass('ghost', 'sm', 'size-9 px-0')}
                          aria-label={`Excluir ${c.name}`}
                          onClick={() => setDeleting(c)}
                        >
                          <Trash2 aria-hidden className="size-4" />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <ConfirmDialog
        open={deleting !== null}
        title="Excluir categoria?"
        description={`"${deleting?.name ?? ''}" será excluída. Só é possível excluir categorias sem produtos; caso contrário, desative-a.`}
        confirmLabel="Excluir"
        onCancel={() => setDeleting(null)}
        onConfirm={() => {
          const c = deleting
          setDeleting(null)
          setFailure(null)
          if (c)
            actions
              .deleteCategory(c.id)
              .then(() => setNotice(`Categoria "${c.name}" excluída.`))
              .catch((e) => setFailure(errorMessage(e)))
        }}
      />
    </div>
  )
}

function CategoryForm({
  category,
  onDone,
  onCancel,
}: {
  category: AdminCategory | null
  onDone: (message: string) => void
  onCancel: () => void
}) {
  const actions = useCatalogActions()
  const [name, setName] = useState(category?.name ?? '')
  const [description, setDescription] = useState(category?.description ?? '')
  const [sortOrder, setSortOrder] = useState(String(category?.sortOrder ?? 0))
  const [active, setActive] = useState(category?.active ?? true)
  const [failure, setFailure] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setFailure(null)
    if (!name.trim()) return setFailure('Informe o nome.')
    if (!/^\d{1,4}$/.test(sortOrder)) return setFailure('Ordem: número inteiro maior ou igual a zero.')
    setBusy(true)
    try {
      await actions.saveCategory(category?.id ?? null, {
        name: name.trim(),
        slug: category?.slug,
        description: description.trim() || undefined,
        sortOrder: Number(sortOrder),
        active,
      })
      onDone(category ? 'Categoria atualizada.' : 'Categoria criada.')
    } catch (err) {
      setFailure(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card>
      <form
        onSubmit={submit}
        className="grid gap-3 sm:grid-cols-2"
        aria-label={category ? 'Editar categoria' : 'Nova categoria'}
      >
        <label className="text-sm">
          <span className="mb-1 block font-medium">Nome</span>
          <input
            value={name}
            maxLength={80}
            onChange={(e) => setName(e.target.value)}
            className={`${controlClass} h-10`}
          />
        </label>
        <label className="text-sm">
          <span className="mb-1 block font-medium">Ordem de exibição</span>
          <input
            type="number"
            min={0}
            value={sortOrder}
            onChange={(e) => setSortOrder(e.target.value)}
            className={`${controlClass} h-10`}
          />
        </label>
        <label className="text-sm sm:col-span-2">
          <span className="mb-1 block font-medium">Descrição (opcional)</span>
          <input
            value={description}
            maxLength={300}
            onChange={(e) => setDescription(e.target.value)}
            className={`${controlClass} h-10`}
          />
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} className="size-4" />
          Ativa (visível na loja)
        </label>
        <div className="flex justify-end gap-2 sm:col-span-2">
          <ActionError message={failure} />
          <button type="button" className={buttonClass('outline')} onClick={onCancel}>
            Cancelar
          </button>
          <button type="submit" className={buttonClass('primary')} disabled={busy}>
            {busy ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>
    </Card>
  )
}
