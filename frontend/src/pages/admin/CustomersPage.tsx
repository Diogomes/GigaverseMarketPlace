import { Download, Users } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import {
  exportCustomers,
  useCustomers,
  useTags,
  type CustomerSort,
  type CustomerStatus,
  type Segment,
} from '../../api/admin'
import { useAuth } from '../../auth/AuthProvider'
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
import { CUSTOMER_STATUS_UI, SEGMENT_UI } from '../../components/admin/labels'
import { Badge } from '../../components/ui/Badge'
import { buttonClass } from '../../components/ui/button'
import { EmptyState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { formatInstantDate, formatPrice, initials } from '../../lib/format'
import { useDebounced } from '../../lib/useDebounced'

const SORT_UI: Record<CustomerSort, string> = {
  LAST_PURCHASE: 'Última compra',
  TOTAL_SPENT: 'Maior valor gasto',
  CREATED_AT: 'Cadastro mais recente',
  NAME: 'Nome',
}

/** Tela 8 da referência. */
export function CustomersPage() {
  const { user } = useAuth()
  const [q, setQ] = useState('')
  const [status, setStatus] = useState<CustomerStatus | ''>('')
  const [segment, setSegment] = useState<Segment | ''>('')
  const [tagId, setTagId] = useState('')
  const [sort, setSort] = useState<CustomerSort>('LAST_PURCHASE')
  const [page, setPage] = useState(0)
  const [failure, setFailure] = useState<string | null>(null)
  const search = useDebounced(q.trim())
  const tags = useTags()
  const filters = {
    q: search || undefined,
    status: status || undefined,
    segment: segment || undefined,
    tagId: tagId ? Number(tagId) : undefined,
  }
  const { data, error, isPending, refetch } = useCustomers({ ...filters, sort, page, size: 20 })
  const reset = () => setPage(0)

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Clientes"
        description="Gerencie seus clientes e relacionamentos."
        actions={
          user?.role === 'ADMIN' && (
            <button
              type="button"
              className={buttonClass('outline')}
              onClick={() => {
                setFailure(null)
                exportCustomers(filters).catch((e) => setFailure(errorMessage(e)))
              }}
            >
              <Download aria-hidden className="size-4" /> Exportar CSV
            </button>
          )
        }
      />
      <ActionError message={failure} />

      <div className="grid gap-3 md:grid-cols-[1fr_auto_auto_auto_auto]">
        <SearchInput
          value={q}
          onChange={(v) => {
            setQ(v)
            reset()
          }}
          label="Buscar cliente por nome, e-mail ou código"
          placeholder="Nome, e-mail ou código…"
        />
        <FilterSelect
          label="Segmento"
          value={segment}
          onChange={(v) => {
            setSegment(v as Segment | '')
            reset()
          }}
        >
          <option value="">Todos os segmentos</option>
          {Object.entries(SEGMENT_UI).map(([k, v]) => (
            <option key={k} value={k}>
              {v}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect
          label="Etiqueta"
          value={tagId}
          onChange={(v) => {
            setTagId(v)
            reset()
          }}
        >
          <option value="">Todas as etiquetas</option>
          {tags.data?.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name} ({t.customers})
            </option>
          ))}
        </FilterSelect>
        <FilterSelect
          label="Status"
          value={status}
          onChange={(v) => {
            setStatus(v as CustomerStatus | '')
            reset()
          }}
        >
          <option value="">Todos os status</option>
          {Object.entries(CUSTOMER_STATUS_UI).map(([k, v]) => (
            <option key={k} value={k}>
              {v.label}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect label="Ordenar por" value={sort} onChange={(v) => setSort(v as CustomerSort)}>
          {Object.entries(SORT_UI).map(([k, v]) => (
            <option key={k} value={k}>
              {v}
            </option>
          ))}
        </FilterSelect>
      </div>

      {error ? (
        <QueryError error={error} onRetry={() => void refetch()} />
      ) : isPending ? (
        <Loading />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Users} title="Nenhum cliente encontrado" description="Ajuste a busca ou os filtros." />
      ) : (
        <>
          <div className={tableWrapClass}>
            <table className="w-full min-w-[48rem] text-sm">
              <caption className="sr-only">Lista de clientes</caption>
              <thead className={theadClass}>
                <tr>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Cliente
                  </th>
                  <th scope="col" className="px-4 py-3 text-right font-medium">
                    Compras
                  </th>
                  <th scope="col" className="px-4 py-3 text-right font-medium">
                    Total gasto
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Última compra
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Etiquetas
                  </th>
                  <th scope="col" className="px-4 py-3 font-medium">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((c) => (
                  <tr key={c.id} className={rowClass}>
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
                    <td className="px-4 py-3 text-right tabular-nums">{c.paidOrders}</td>
                    <td className="px-4 py-3 text-right tabular-nums">{formatPrice(c.totalSpent)}</td>
                    <td className="px-4 py-3 text-fg-muted">
                      {c.lastPurchaseAt ? formatInstantDate(c.lastPurchaseAt) : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <span className="flex flex-wrap gap-1">
                        {c.tags.map((t) => (
                          <Badge key={t.id}>{t.name}</Badge>
                        ))}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <Badge tone={CUSTOMER_STATUS_UI[c.status].tone}>{CUSTOMER_STATUS_UI[c.status].label}</Badge>
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
          <p className="text-xs text-fg-muted">
            Compras = pedidos pagos e não cancelados. A última compra usa a data de confirmação do pagamento.
          </p>
        </>
      )}
    </div>
  )
}
