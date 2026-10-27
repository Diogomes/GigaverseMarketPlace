import { useEffect, useRef } from 'react'
import { buttonClass } from './button'

/** Confirmação para ações destrutivas, baseada no <dialog> nativo (foco e Esc acessíveis). */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  onConfirm,
  onCancel,
}: {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  onConfirm: () => void
  onCancel: () => void
}) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal?.()
    if (!open && dialog.open) dialog.close?.()
  }, [open])

  return (
    <dialog
      ref={ref}
      onCancel={(e) => {
        e.preventDefault()
        onCancel()
      }}
      aria-labelledby="confirm-title"
      className="m-auto w-[min(28rem,calc(100vw-2rem))] rounded-card border border-border bg-surface p-6 text-fg shadow-card backdrop:bg-black/60"
    >
      <h2 id="confirm-title" className="text-lg font-semibold">
        {title}
      </h2>
      <p className="mt-2 text-sm text-fg-muted">{description}</p>
      <div className="mt-6 flex justify-end gap-3">
        <button type="button" className={buttonClass('outline')} onClick={onCancel}>
          Cancelar
        </button>
        <button type="button" className={buttonClass('danger')} onClick={onConfirm}>
          {confirmLabel}
        </button>
      </div>
    </dialog>
  )
}
