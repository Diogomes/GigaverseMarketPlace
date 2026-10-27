import { Lock, Plus, X } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { DemoBanner } from '../../components/ui/DemoBanner'
import { controlClass } from '../../components/ui/Field'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { CUSTOMER_STATUS, ORDER_STATUS } from '../../data/labels'
import { customerSummary, getCustomer, listCustomerOrders, today } from '../../data/store'
import type { Customer } from '../../data/types'
import { formatBRL, formatDate, initials } from '../../lib/format'
import { NotFoundPage } from '../PlaceholderPage'

export function CustomerDetailPage() {
  const { id = '' } = useParams()
  const customer = getCustomer(id)
  if (!customer) return <NotFoundPage />
  return <CustomerDetail key={customer.id} customer={customer} />
}

function CustomerDetail({ customer }: { customer: Customer }) {
  // Estado local apenas para pré-visualização; persistência e auditoria na Fase 09.
  const [notes, setNotes] = useState(customer.notes)
  const [tags, setTags] = useState(customer.tags)
  const [noteText, setNoteText] = useState('')
  const [tagText, setTagText] = useState('')
  const summary = customerSummary(customer.id)
  const orders = listCustomerOrders(customer.id)

  return (
    <div className="flex flex-col gap-6">
      <Link to="/admin/clientes" className="text-sm font-semibold text-accent hover:underline">
        ← Clientes
      </Link>
      <PageHeader
        title={customer.name}
        description={`Cliente desde ${formatDate(customer.createdAt)}`}
        actions={<Badge tone={CUSTOMER_STATUS[customer.status].tone}>{CUSTOMER_STATUS[customer.status].label}</Badge>}
      />
      <DemoBanner />

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
              </div>
            </div>
            <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
              <div>
                <dt className="text-fg-muted">Pedidos</dt>
                <dd className="text-lg font-semibold">{summary.orderCount}</dd>
              </div>
              <div>
                <dt className="text-fg-muted">Total gasto</dt>
                <dd className="text-lg font-semibold">{formatBRL(summary.totalSpentCents)}</dd>
              </div>
              <div className="col-span-2">
                <dt className="text-fg-muted">Última compra</dt>
                <dd>{summary.lastOrderAt ? formatDate(summary.lastOrderAt) : '—'}</dd>
              </div>
            </dl>
          </Card>

          <Card>
            <h2 className="font-semibold">Etiquetas</h2>
            <ul className="mt-3 flex flex-wrap gap-2">
              {tags.map((t) => (
                <li
                  key={t}
                  className="inline-flex items-center gap-1 rounded-full border border-border bg-surface-muted px-2.5 py-0.5 text-xs"
                >
                  {t}
                  <button
                    type="button"
                    aria-label={`Remover etiqueta ${t}`}
                    onClick={() => setTags(tags.filter((x) => x !== t))}
                  >
                    <X aria-hidden className="size-3" />
                  </button>
                </li>
              ))}
              {tags.length === 0 && <li className="text-sm text-fg-muted">Nenhuma etiqueta.</li>}
            </ul>
            <form
              className="mt-3 flex gap-2"
              onSubmit={(e) => {
                e.preventDefault()
                const t = tagText.trim()
                if (t && !tags.includes(t)) setTags([...tags, t])
                setTagText('')
              }}
            >
              <label className="flex-1">
                <span className="sr-only">Nova etiqueta</span>
                <input
                  value={tagText}
                  onChange={(e) => setTagText(e.target.value)}
                  maxLength={30}
                  placeholder="Nova etiqueta"
                  className={`${controlClass} h-9`}
                />
              </label>
              <button type="submit" className={buttonClass('outline', 'sm')} aria-label="Adicionar etiqueta">
                <Plus aria-hidden className="size-4" />
              </button>
            </form>
          </Card>
        </div>

        <div className="flex flex-col gap-6">
          <Card>
            <h2 className="font-semibold">Histórico de pedidos</h2>
            {orders.length === 0 ? (
              <p className="mt-4 text-sm text-fg-muted">Nenhum pedido.</p>
            ) : (
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
                    {orders.map((o) => (
                      <tr key={o.number} className="border-t border-border">
                        <td className="py-2 font-medium">#{o.number}</td>
                        <td className="py-2 text-fg-muted">{formatDate(o.createdAt)}</td>
                        <td className="py-2">
                          <Badge tone={ORDER_STATUS[o.status].tone}>{ORDER_STATUS[o.status].label}</Badge>
                        </td>
                        <td className="py-2 text-right tabular-nums">{formatBRL(o.totalCents)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card>
            <h2 className="flex items-center gap-2 font-semibold">
              <Lock aria-hidden className="size-4 text-fg-muted" /> Notas internas
            </h2>
            <p className="text-xs text-fg-muted">Visíveis apenas para a equipe autorizada — nunca para o cliente.</p>
            <ul className="mt-4 flex flex-col gap-3">
              {notes.map((n) => (
                <li key={n.id} className="rounded-lg border border-border bg-surface-muted p-3 text-sm">
                  <p>{n.text}</p>
                  <p className="mt-1 text-xs text-fg-muted">
                    {n.author} · {formatDate(n.at)}
                  </p>
                </li>
              ))}
              {notes.length === 0 && <li className="text-sm text-fg-muted">Nenhuma nota.</li>}
            </ul>
            <form
              className="mt-4 flex flex-col gap-2"
              onSubmit={(e) => {
                e.preventDefault()
                const text = noteText.trim()
                if (!text) return
                setNotes([{ id: crypto.randomUUID(), author: 'Você', at: today(), text }, ...notes])
                setNoteText('')
              }}
            >
              <label htmlFor="new-note" className="sr-only">
                Nova nota interna
              </label>
              <textarea
                id="new-note"
                value={noteText}
                onChange={(e) => setNoteText(e.target.value)}
                maxLength={1000}
                placeholder="Escreva uma nota interna…"
                className={`${controlClass} min-h-20 py-2`}
              />
              <p className="text-xs text-fg-muted">Evite registrar dados sensíveis desnecessários.</p>
              <button type="submit" className={buttonClass('primary', 'sm', 'self-end')}>
                Adicionar nota
              </button>
            </form>
          </Card>
        </div>
      </div>
    </div>
  )
}
