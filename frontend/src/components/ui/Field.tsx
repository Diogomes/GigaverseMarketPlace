import type { InputHTMLAttributes, ReactNode, Ref, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

export const controlClass =
  'w-full rounded-lg border border-border bg-bg-elevated px-3 text-sm text-fg placeholder:text-fg-muted/70 focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30 aria-[invalid=true]:border-danger disabled:opacity-60'

type FieldShellProps = { id: string; label: string; error?: string; hint?: ReactNode; children: ReactNode }

function FieldShell({ id, label, error, hint, children }: FieldShellProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      {children}
      {error ? (
        <p id={`${id}-error`} className="text-xs text-danger" role="alert">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-hint`} className="text-xs text-fg-muted">
            {hint}
          </p>
        )
      )}
    </div>
  )
}

function describedBy(id: string, error?: string, hint?: ReactNode) {
  if (error) return `${id}-error`
  return hint ? `${id}-hint` : undefined
}

type InputFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  id: string
  label: string
  error?: string
  hint?: ReactNode
  ref?: Ref<HTMLInputElement>
}

export function InputField({ id, label, error, hint, className, ...props }: InputFieldProps) {
  return (
    <FieldShell id={id} label={label} error={error} hint={hint}>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(id, error, hint)}
        className={cn(controlClass, 'h-10', className)}
        {...props}
      />
    </FieldShell>
  )
}

type SelectFieldProps = SelectHTMLAttributes<HTMLSelectElement> & {
  id: string
  label: string
  error?: string
  hint?: ReactNode
  ref?: Ref<HTMLSelectElement>
}

export function SelectField({ id, label, error, hint, className, children, ...props }: SelectFieldProps) {
  return (
    <FieldShell id={id} label={label} error={error} hint={hint}>
      <select
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(id, error, hint)}
        className={cn(controlClass, 'h-10', className)}
        {...props}
      >
        {children}
      </select>
    </FieldShell>
  )
}

type TextareaFieldProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  id: string
  label: string
  error?: string
  hint?: ReactNode
  ref?: Ref<HTMLTextAreaElement>
}

export function TextareaField({ id, label, error, hint, className, ...props }: TextareaFieldProps) {
  return (
    <FieldShell id={id} label={label} error={error} hint={hint}>
      <textarea
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(id, error, hint)}
        className={cn(controlClass, 'min-h-28 py-2', className)}
        {...props}
      />
    </FieldShell>
  )
}
