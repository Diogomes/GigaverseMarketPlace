import { AlertTriangle, ChevronLeft, ChevronRight, Search } from 'lucide-react'
import type { ReactNode } from 'react'
import { ApiError } from '../../lib/api'
import { cn } from '../../lib/cn'
import { buttonClass } from '../ui/button'
import { controlClass } from '../ui/Field'

export const tableWrapClass = 'overflow-x-auto rounded-card border border-border bg-surface shadow-card'
export const theadClass = 'border-b border-border text-left text-xs uppercase tracking-wide text-fg-muted'
export const rowClass = 'border-b border-border last:border-0 hover:bg-surface-muted/50'

// oxlint-disable-next-line react/only-export-components -- utilitário pequeno, usado junto com os componentes
export function errorMessage(error: unknown): string {
  return error instanceof ApiError || error instanceof Error ? error.message : 'Algo deu errado. Tente novamente.'
}

/** Erro de carregamento com opção de tentar de novo. */
export function QueryError({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  return (
    <div
      role="alert"
      className="flex flex-col items-center gap-3 rounded-card border border-danger/30 px-6 py-10 text-center"
    >
      <AlertTriangle aria-hidden className="size-8 text-danger" />
      <p className="text-sm">{errorMessage(error)}</p>
      {onRetry && (
        <button type="button" className={buttonClass('outline', 'sm')} onClick={onRetry}>
          Tentar novamente
        </button>
      )}
    </div>
  )
}

export function Loading({ label = 'Carregando…' }: { label?: string }) {
  return (
    <p role="status" className="py-12 text-center text-sm text-fg-muted">
      {label}
    </p>
  )
}

/** Mensagem de erro de uma ação (salvar, mudar status…). */
export function ActionError({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <p role="alert" className="rounded-lg border border-danger/30 bg-danger/10 px-3 py-2 text-sm text-danger">
      {message}
    </p>
  )
}

export function SearchInput({
  value,
  onChange,
  label,
  placeholder,
}: {
  value: string
  onChange: (v: string) => void
  label: string
  placeholder?: string
}) {
  return (
    <label className="relative">
      <span className="sr-only">{label}</span>
      <Search aria-hidden className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-fg-muted" />
      <input
        type="search"
        value={value}
        maxLength={100}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className={`${controlClass} h-10 pl-9`}
      />
    </label>
  )
}

export function FilterSelect({
  label,
  value,
  onChange,
  children,
  className,
}: {
  label: string
  value: string
  onChange: (v: string) => void
  children: ReactNode
  className?: string
}) {
  return (
    <label className={className}>
      <span className="sr-only">{label}</span>
      <select value={value} onChange={(e) => onChange(e.target.value)} className={`${controlClass} h-10`}>
        {children}
      </select>
    </label>
  )
}

/** Paginação simples (anterior/próxima) com total de registros. */
export function Pagination({
  page,
  totalPages,
  totalElements,
  onPage,
  className,
}: {
  page: number
  totalPages: number
  totalElements: number
  onPage: (p: number) => void
  className?: string
}) {
  if (totalElements === 0) return null
  return (
    <nav aria-label="Paginação" className={cn('flex items-center justify-between gap-3 text-sm', className)}>
      <span className="text-fg-muted">
        {totalElements} {totalElements === 1 ? 'registro' : 'registros'} · página {page + 1} de{' '}
        {Math.max(1, totalPages)}
      </span>
      <span className="flex gap-2">
        <button
          type="button"
          className={buttonClass('outline', 'sm')}
          disabled={page === 0}
          onClick={() => onPage(page - 1)}
        >
          <ChevronLeft aria-hidden className="size-4" /> Anterior
        </button>
        <button
          type="button"
          className={buttonClass('outline', 'sm')}
          disabled={page + 1 >= totalPages}
          onClick={() => onPage(page + 1)}
        >
          Próxima <ChevronRight aria-hidden className="size-4" />
        </button>
      </span>
    </nav>
  )
}
