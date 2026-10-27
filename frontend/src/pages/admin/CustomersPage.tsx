import { Download, Search, Users } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { DemoBanner } from '../../components/ui/DemoBanner'
import { EmptyState } from '../../components/ui/EmptyState'
import { controlClass } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { PageHeader } from '../../components/ui/PageHeader'
import { CUSTOMER_STATUS } from '../../data/labels'
import { customerSummary, listCustomers } from '../../data/store'
import type { CustomerStatus } from '../../data/types'
import { formatBRL, formatDate, initials } from '../../lib/format'

/** Tela 8 da referência. */
export function CustomersPage() {
  const [q, setQ] = useState('')
  const [status, setStatus] = useState<CustomerStatus | ''>('')
  const [notice, setNotice] = useState(false)
  const term = q.trim().toLowerCase()

  const rows = listCustomers()
    .filter(
      (c) =>
        (!term || c.name.toLowerCase().includes(term) || c.email.toLowerCase().includes(term)) &&
        (!status || c.status === status),
    )
    .map((c) => ({ customer: c, summary: customerSummary(c.id) }))
    .sort((a, b) => b.summary.totalSpentCents - a.summary.totalSpentCents)

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Clientes"
        description="Gerencie seus clientes e relacionamentos."
        actions={
          <button type="button" className={buttonClass('outline')} onClick={() => setNotice(true)}>
            <Download aria-hidden className="size-4" /> Exportar
          </button>
        }
      />
      <DemoBanner>Clientes fictícios (e-mails @exemplo.com).</DemoBanner>
      {notice && (
        <Notice>
          A exportação exigirá permissão específica, terá limite de registros e ficará registrada na auditoria (Fase
          09).
        </Notice>
      )}

      <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
        <label className="relative">
          <span className="sr-only">Buscar cliente por nome ou e-mail</span>
          <Search aria-hidden className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-fg-muted" />
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Buscar cliente…"
            className={`${controlClass} h-10 pl-9`}
          />
        </label>
        <label>
          <span className="sr-only">Status</span>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as CustomerStatus | '')}
            className={`${controlClass} h-10`}
          >
            <option value="">Todos os status</option>
            {Object.entries(CUSTOMER_STATUS).map(([k, v]) => (
              <option key={k} value={k}>
                {v.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {rows.length === 0 ? (
        <EmptyState icon={Users} title="Nenhum cliente encontrado" />
      ) : (
        <div className="overflow-x-auto rounded-card border border-border bg-surface shadow-card">
          <table className="w-full min-w-[44rem] text-sm">
            <caption className="sr-only">Lista de clientes</caption>
            <thead className="border-b border-border text-left text-xs uppercase tracking-wide text-fg-muted">
              <tr>
                <th scope="col" className="px-4 py-3 font-medium">
                  Cliente
                </th>
                <th scope="col" className="px-4 py-3 text-right font-medium">
                  Pedidos
                </th>
                <th scope="col" className="px-4 py-3 text-right font-medium">
                  Total gasto
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Última compra
                </th>
                <th scope="col" className="px-4 py-3 font-medium">
                  Status
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map(({ customer: c, summary: s }) => (
                <tr key={c.id} className="border-b border-border last:border-0 hover:bg-surface-muted/50">
                  <td className="px-4 py-3">
                    <Link to={`/admin/clientes/${c.id}`} className="group flex items-center gap-3">
                      <span className="inline-flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/15 text-xs font-semibold text-accent">
                        {initials(c.name)}
                      </span>
                      <span>
                        <span className="block font-medium group-hover:text-accent group-hover:underline">
                          {c.name}
                        </span>
                        <span className="block text-xs text-fg-muted">{c.email}</span>
                      </span>
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">{s.orderCount}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{formatBRL(s.totalSpentCents)}</td>
                  <td className="px-4 py-3 text-fg-muted">{s.lastOrderAt ? formatDate(s.lastOrderAt) : '—'}</td>
                  <td className="px-4 py-3">
                    <Badge tone={CUSTOMER_STATUS[c.status].tone}>{CUSTOMER_STATUS[c.status].label}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
