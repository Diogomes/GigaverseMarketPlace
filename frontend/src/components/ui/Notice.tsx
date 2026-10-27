import { Info } from 'lucide-react'
import type { ReactNode } from 'react'

/** Mensagem informativa anunciada a leitores de tela (ex.: recurso que chega numa fase futura). */
export function Notice({ children }: { children: ReactNode }) {
  return (
    <p
      role="status"
      className="flex items-start gap-2 rounded-lg border border-primary/30 bg-primary/10 px-3 py-2 text-sm text-fg"
    >
      <Info aria-hidden className="mt-0.5 size-4 shrink-0 text-accent" />
      <span>{children}</span>
    </p>
  )
}
