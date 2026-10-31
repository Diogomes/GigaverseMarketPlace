import { useState } from 'react'
import { useCatalogActions, useStockHistory } from '../../api/admin'
import { formatDateTime } from '../../lib/format'
import { buttonClass } from '../ui/button'
import { controlClass } from '../ui/Field'
import { Notice } from '../ui/Notice'
import { ActionError, errorMessage, Loading, Pagination } from './AdminUi'
import { MOVEMENT_UI } from './labels'

type ManualType = 'PURCHASE' | 'ADJUSTMENT' | 'RETURN'

/** Saldo, lançamento manual (auditado no servidor) e histórico de movimentações de um produto. */
export function StockPanel({
  productId,
  stock,
}: {
  productId: number
  stock: { onHand: number; reserved: number; available: number; lowStockThreshold: number; low: boolean } | null
}) {
  const [page, setPage] = useState(0)
  const [type, setType] = useState<ManualType>('PURCHASE')
  const [quantity, setQuantity] = useState('')
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const [done, setDone] = useState<string | null>(null)
  const history = useStockHistory(productId, page)
  const actions = useCatalogActions()

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setFailure(null)
    setDone(null)
    const n = Number(quantity)
    if (!Number.isInteger(n) || n === 0 || Math.abs(n) > 100000) {
      setFailure('Informe uma quantidade inteira diferente de zero (negativa para saída no ajuste).')
      return
    }
    if (type !== 'ADJUSTMENT' && n < 0) {
      setFailure('Entradas e devoluções usam quantidade positiva. Para reduzir o saldo, use "Ajuste".')
      return
    }
    if (type === 'ADJUSTMENT' && !reason.trim()) {
      setFailure('Informe o motivo do ajuste (ex.: contagem, avaria).')
      return
    }
    setBusy(true)
    try {
      await actions.adjustStock(productId, type, n, reason.trim())
      setDone('Movimentação registrada.')
      setQuantity('')
      setReason('')
      setPage(0)
    } catch (err) {
      setFailure(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      {stock && (
        <dl className="grid grid-cols-3 gap-3 text-sm">
          <div>
            <dt className="text-fg-muted">Em estoque</dt>
            <dd className="text-xl font-semibold tabular-nums">{stock.onHand}</dd>
          </div>
          <div>
            <dt className="text-fg-muted">Disponível</dt>
            <dd className={`text-xl font-semibold tabular-nums ${stock.low ? 'text-danger' : ''}`}>
              {stock.available}
            </dd>
          </div>
          <div>
            <dt className="text-fg-muted">Mínimo</dt>
            <dd className="text-xl font-semibold tabular-nums">{stock.lowStockThreshold}</dd>
          </div>
        </dl>
      )}
      <form onSubmit={submit} className="grid gap-2 sm:grid-cols-[auto_6rem_1fr_auto]" aria-label="Lançar movimentação">
        <label>
          <span className="sr-only">Tipo de movimentação</span>
          <select
            value={type}
            onChange={(e) => setType(e.target.value as ManualType)}
            className={`${controlClass} h-10`}
          >
            <option value="PURCHASE">Entrada</option>
            <option value="RETURN">Devolução</option>
            <option value="ADJUSTMENT">Ajuste (+/−)</option>
          </select>
        </label>
        <label>
          <span className="sr-only">Quantidade</span>
          <input
            type="number"
            inputMode="numeric"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            placeholder="Qtd."
            className={`${controlClass} h-10`}
          />
        </label>
        <label>
          <span className="sr-only">Motivo</span>
          <input
            value={reason}
            maxLength={255}
            onChange={(e) => setReason(e.target.value)}
            placeholder={type === 'ADJUSTMENT' ? 'Motivo (obrigatório)' : 'Observação (opcional)'}
            className={`${controlClass} h-10`}
          />
        </label>
        <button type="submit" className={buttonClass('primary', 'md')} disabled={busy}>
          {busy ? 'Salvando…' : 'Lançar'}
        </button>
      </form>
      <ActionError message={failure} />
      {done && <Notice>{done}</Notice>}

      <div>
        <h3 className="text-sm font-semibold">Histórico</h3>
        {history.isPending ? (
          <Loading />
        ) : history.error ? (
          <ActionError message={errorMessage(history.error)} />
        ) : history.data.content.length === 0 ? (
          <p className="mt-2 text-sm text-fg-muted">Nenhuma movimentação.</p>
        ) : (
          <>
            <ul className="mt-2 flex flex-col divide-y divide-border text-sm">
              {history.data.content.map((m) => (
                <li key={m.id} className="flex items-start justify-between gap-3 py-2">
                  <span>
                    <span className="block font-medium">
                      {MOVEMENT_UI[m.type]}
                      {m.orderId !== null && <span className="font-normal text-fg-muted"> · pedido</span>}
                    </span>
                    <span className="text-xs text-fg-muted">
                      {formatDateTime(m.createdAt)}
                      {m.reason && ` · ${m.reason}`}
                    </span>
                  </span>
                  <span className="shrink-0 text-right tabular-nums">
                    <span className={m.quantityDelta < 0 ? 'text-danger' : 'text-success'}>
                      {m.quantityDelta > 0 ? '+' : ''}
                      {m.quantityDelta}
                    </span>
                    <span className="block text-xs text-fg-muted">saldo {m.quantityAfter}</span>
                  </span>
                </li>
              ))}
            </ul>
            <Pagination
              className="mt-2"
              page={history.data.page}
              totalPages={history.data.totalPages}
              totalElements={history.data.totalElements}
              onPage={setPage}
            />
          </>
        )}
      </div>
    </div>
  )
}
