import { Lock, Plus, Trash2, X } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import {
  useAdminOrders,
  useCrmActions,
  useCustomer,
  useCustomerNotes,
  useTags,
  type CustomerDetail,
} from '../../api/admin'
import { useAuth } from '../../auth/AuthProvider'
import { ActionError, errorMessage, Loading, Pagination, QueryError } from '../../components/admin/AdminUi'
import { CUSTOMER_STATUS_UI } from '../../components/admin/labels'
import { ORDER_STATUS_UI } from '../../components/store/orderStatus'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { controlClass } from '../../components/ui/Field'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { ApiError } from '../../lib/api'
import { formatDateTime, formatInstantDate, formatPrice, initials } from '../../lib/format'
import { NotFoundPage } from '../PlaceholderPage'

export function CustomerDetailPage() {
  const { id = '' } = useParams()
  const { data, error, isPending, refetch } = useCustomer(id)
  if (error instanceof ApiError && error.status === 404) return <NotFoundPage />
  if (error) return <QueryError error={error} onRetry={() => void refetch()} />
  if (isPending) return <Loading />
  return <CustomerView key={data.id} customer={data} />
}

function CustomerView({ customer }: { customer: CustomerDetail }) {
  return (
    <div className="flex flex-col gap-6">
      <Link to="/admin/clientes" className="text-sm font-semibold text-accent hover:underline">
        ← Clientes
      </Link>
      <PageHeader
        title={customer.name}
        description={`Cliente desde ${formatInstantDate(customer.createdAt)}`}
        actions={
          <Badge tone={CUSTOMER_STATUS_UI[customer.status].tone}>{CUSTOMER_STATUS_UI[customer.status].label}</Badge>
        }
      />

      <div className="grid gap-6 xl:grid-cols-[20rem_1fr]">
        <div className="flex flex-col gap-6">
          <Card>
            <div className="flex items-center gap-3">
              <span className="inline-flex size-12 items-center justify-center rounded-full bg-primary/15 font-semibold text-accent">
                {initials(customer.name)}
              </span>
              <div className="min-w-0">
                <p className="truncate font-semibold">{customer.name}</p>
                <p className="truncate text-sm text-fg-muted">{customer.email}</p>
                {customer.phone && <p className="text-sm text-fg-muted">{customer.phone}</p>}
              </div>
            </div>
            <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
              <div>
                <dt className="text-fg-muted">Compras</dt>
                <dd className="text-lg font-semibold tabular-nums">{customer.paidOrders}</dd>
              </div>
              <div>
                <dt className="text-fg-muted">Total gasto</dt>
                <dd className="text-lg font-semibold tabular-nums">{formatPrice(customer.totalSpent)}</dd>
              </div>
              <div>
                <dt className="text-fg-muted">Ticket médio</dt>
                <dd className="tabular-nums">{formatPrice(customer.averageTicket)}</dd>
              </div>
              <div>
                <dt className="text-fg-muted">Pedidos (todos)</dt>
                <dd className="tabular-nums">{customer.orders}</dd>
              </div>
              <div>
                <dt className="text-fg-muted">Primeira compra</dt>
                <dd>{customer.firstPurchaseAt ? formatInstantDate(customer.firstPurchaseAt) : '—'}</dd>
              </div>
              <div>
                <dt className="text-fg-muted">Última compra</dt>
                <dd>{customer.lastPurchaseAt ? formatInstantDate(customer.lastPurchaseAt) : '—'}</dd>
              </div>
              <div className="col-span-2">
                <dt className="text-fg-muted">E-mails de marketing</dt>
                <dd>{customer.marketingOptIn ? 'Aceita receber' : 'Não aceitou — não enviar ofertas'}</dd>
              </div>
            </dl>
          </Card>
          <TagsCard customer={customer} />
        </div>

        <div className="flex min-w-0 flex-col gap-6">
          <OrdersCard customerId={customer.id} />
          <NotesCard customerId={customer.id} />
        </div>
      </div>
    </div>
  )
}

function TagsCard({ customer }: { customer: CustomerDetail }) {
  const { user } = useAuth()
  const tags = useTags()
  const actions = useCrmActions()
  const [selected, setSelected] = useState('')
  const [newTag, setNewTag] = useState('')
  const [failure, setFailure] = useState<string | null>(null)
  const current = customer.tags.map((t) => t.id)
  const available = tags.data?.filter((t) => !current.includes(t.id)) ?? []

  async function apply(ids: number[]) {
    setFailure(null)
    try {
      await actions.setTags(customer.id, ids)
    } catch (e) {
      setFailure(errorMessage(e))
    }
  }

  async function create(e: React.FormEvent) {
    e.preventDefault()
    const name = newTag.trim()
    if (name.length < 2) return setFailure('A etiqueta precisa de ao menos 2 caracteres.')
    setFailure(null)
    try {
      const tag = await actions.createTag(name)
      await actions.setTags(customer.id, [...current, tag.id])
      setNewTag('')
    } catch (err) {
      setFailure(errorMessage(err))
    }
  }

  return (
    <Card>
      <h2 className="font-semibold">Etiquetas</h2>
      <ul className="mt-3 flex flex-wrap gap-2">
        {customer.tags.map((t) => (
          <li
            key={t.id}
            className="inline-flex items-center gap-1 rounded-full border border-border bg-surface-muted px-2.5 py-0.5 text-xs"
          >
            {t.name}
            <button
              type="button"
              aria-label={`Remover etiqueta ${t.name}`}
              onClick={() => void apply(current.filter((id) => id !== t.id))}
            >
              <X aria-hidden className="size-3" />
            </button>
          </li>
        ))}
        {customer.tags.length === 0 && <li className="text-sm text-fg-muted">Nenhuma etiqueta.</li>}
      </ul>
      {available.length > 0 && (
        <div className="mt-3 flex gap-2">
          <label className="flex-1">
            <span className="sr-only">Adicionar etiqueta existente</span>
            <select value={selected} onChange={(e) => setSelected(e.target.value)} className={`${controlClass} h-9`}>
              <option value="">Adicionar etiqueta…</option>
              {available.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            className={buttonClass('outline', 'sm')}
            disabled={!selected}
            onClick={() => {
              void apply([...current, Number(selected)])
              setSelected('')
            }}
          >
            Adicionar
          </button>
        </div>
      )}
      <form className="mt-3 flex gap-2" onSubmit={create}>
        <label className="flex-1">
          <span className="sr-only">Nova etiqueta</span>
          <input
            value={newTag}
            onChange={(e) => setNewTag(e.target.value)}
            maxLength={30}
            placeholder="Criar etiqueta"
            className={`${controlClass} h-9`}
          />
        </label>
        <button type="submit" className={buttonClass('outline', 'sm')} aria-label="Criar e adicionar etiqueta">
          <Plus aria-hidden className="size-4" />
        </button>
      </form>
      <ActionError message={failure} />
      <p className="mt-2 text-xs text-fg-muted">
        Use etiquetas comerciais (ex.: “Atacado”, “Evento”). Não registre saúde, religião, política ou outros dados
        sensíveis.
      </p>
      {user?.role !== 'ADMIN' && (
        <p className="mt-1 text-xs text-fg-muted">Excluir etiquetas é restrito a administradores.</p>
      )}
    </Card>
  )
}

function OrdersCard({ customerId }: { customerId: number }) {
  const [page, setPage] = useState(0)
  const { data, error, isPending } = useAdminOrders({ customerId, page, size: 10 })
  return (
    <Card>
      <div className="flex items-center justify-between">
        <h2 className="font-semibold">Histórico de pedidos</h2>
        <Link to={`/admin/pedidos?cliente=${customerId}`} className="text-xs font-semibold text-accent hover:underline">
          Abrir em Pedidos
        </Link>
      </div>
      {error ? (
        <ActionError message={errorMessage(error)} />
      ) : isPending ? (
        <Loading />
      ) : data.content.length === 0 ? (
        <p className="mt-4 text-sm text-fg-muted">Nenhum pedido.</p>
      ) : (
        <>
          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[28rem] text-sm">
              <thead className="text-left text-xs uppercase tracking-wide text-fg-muted">
                <tr>
                  <th scope="col" className="py-2 font-medium">
                    Pedido
                  </th>
                  <th scope="col" className="py-2 font-medium">
                    Data
                  </th>
                  <th scope="col" className="py-2 font-medium">
                    Status
                  </th>
                  <th scope="col" className="py-2 text-right font-medium">
                    Total
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((o) => (
                  <tr key={o.number} className="border-t border-border">
                    <td className="py-2">
                      <Link to={`/admin/pedidos/${o.number}`} className="font-medium text-accent hover:underline">
                        #{o.number}
                      </Link>
                    </td>
                    <td className="py-2 text-fg-muted">{formatDateTime(o.createdAt)}</td>
                    <td className="py-2">
                      <Badge tone={ORDER_STATUS_UI[o.status].tone}>{ORDER_STATUS_UI[o.status].label}</Badge>
                    </td>
                    <td className="py-2 text-right tabular-nums">{formatPrice(o.total)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            className="mt-3"
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPage={setPage}
          />
        </>
      )}
    </Card>
  )
}

function NotesCard({ customerId }: { customerId: number }) {
  const { user } = useAuth()
  const [page, setPage] = useState(0)
  const notes = useCustomerNotes(String(customerId), page)
  const actions = useCrmActions()
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const [removing, setRemoving] = useState<number | null>(null)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    const body = text.trim()
    if (!body) return
    setBusy(true)
    setFailure(null)
    try {
      await actions.addNote(customerId, body)
      setText('')
      setPage(0)
    } catch (err) {
      setFailure(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card>
      <h2 className="flex items-center gap-2 font-semibold">
        <Lock aria-hidden className="size-4 text-fg-muted" /> Notas internas
      </h2>
      <p className="text-xs text-fg-muted">Visíveis apenas para a equipe — nunca para o cliente.</p>
      <form className="mt-4 flex flex-col gap-2" onSubmit={submit}>
        <label htmlFor="new-note" className="sr-only">
          Nova nota interna
        </label>
        <textarea
          id="new-note"
          value={text}
          onChange={(e) => setText(e.target.value)}
          maxLength={2000}
          placeholder="Escreva uma nota interna…"
          className={`${controlClass} min-h-20 py-2`}
        />
        <p className="text-xs text-fg-muted">Registre só o necessário para o atendimento; evite dados sensíveis.</p>
        <ActionError message={failure} />
        <button type="submit" className={buttonClass('primary', 'sm', 'self-end')} disabled={busy || !text.trim()}>
          {busy ? 'Salvando…' : 'Adicionar nota'}
        </button>
      </form>
      {notes.error ? (
        <ActionError message={errorMessage(notes.error)} />
      ) : notes.isPending ? (
        <Loading />
      ) : notes.data.content.length === 0 ? (
        <p className="mt-4 text-sm text-fg-muted">Nenhuma nota.</p>
      ) : (
        <>
          <ul className="mt-4 flex flex-col gap-3">
            {notes.data.content.map((n) => (
              <li key={n.id} className="rounded-lg border border-border bg-surface-muted p-3 text-sm">
                <p className="whitespace-pre-line">{n.body}</p>
                <p className="mt-1 flex items-center justify-between text-xs text-fg-muted">
                  <span>
                    {n.authorName ?? 'Usuário removido'} · {formatDateTime(n.createdAt)}
                  </span>
                  {user?.role === 'ADMIN' && (
                    <button
                      type="button"
                      className="inline-flex items-center gap-1 hover:text-danger"
                      onClick={() => setRemoving(n.id)}
                    >
                      <Trash2 aria-hidden className="size-3.5" /> Remover
                    </button>
                  )}
                </p>
              </li>
            ))}
          </ul>
          <Pagination
            className="mt-3"
            page={notes.data.page}
            totalPages={notes.data.totalPages}
            totalElements={notes.data.totalElements}
            onPage={setPage}
          />
        </>
      )}
      <ConfirmDialog
        open={removing !== null}
        title="Remover nota?"
        description="A nota deixa de aparecer para a equipe. A remoção fica registrada na auditoria."
        confirmLabel="Remover"
        onCancel={() => setRemoving(null)}
        onConfirm={() => {
          const noteId = removing
          setRemoving(null)
          if (noteId !== null) actions.deleteNote(customerId, noteId).catch((e) => setFailure(errorMessage(e)))
        }}
      />
    </Card>
  )
}
