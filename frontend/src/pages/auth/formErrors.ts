import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError } from '../../lib/api'

/**
 * Leva erros de campo da API (RFC 9457 `errors[]`) para o formulário.
 * Devolve a mensagem geral a exibir (ou null se tudo foi atribuído a campos conhecidos).
 */
export function applyApiErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  knownFields: Array<Path<T>>,
): string | null {
  if (!(error instanceof ApiError)) return 'Não foi possível conectar ao servidor. Tente novamente.'
  let unassigned = false
  for (const fe of error.fieldErrors) {
    if ((knownFields as string[]).includes(fe.field)) {
      setError(fe.field as Path<T>, { type: 'server', message: fe.message })
    } else {
      unassigned = true
    }
  }
  return error.fieldErrors.length === 0 || unassigned ? error.message : null
}

/** Só aceita redirecionamentos internos (evita open redirect via ?next=). */
export function safeNext(next: string | null): string | null {
  return next && next.startsWith('/') && !next.startsWith('//') && !next.startsWith('/\\') ? next : null
}
