import { Eye } from 'lucide-react'
import type { ReactNode } from 'react'
import { Navigate } from 'react-router'
import { DEMO_MODE } from '../data/store'

/**
 * Até a autenticação real (Fase 05), as áreas privadas só abrem em modo de pré-visualização
 * no ambiente de desenvolvimento. Em produção, redireciona para o login.
 * Lembrete: isto é navegação, não autorização — a proteção real fica no backend.
 */
export function PreviewGuard({ children }: { children: ReactNode }) {
  if (!DEMO_MODE) return <Navigate to="/entrar" replace />
  return children
}

export function PreviewRibbon({ area }: { area: string }) {
  return (
    <p className="flex items-center gap-2 border-b border-amber-500/40 bg-amber-500/10 px-4 py-1.5 text-xs text-amber-800 dark:text-amber-200">
      <Eye aria-hidden className="size-3.5 shrink-0" />
      Pré-visualização da {area} sem login — dados de demonstração (autenticação real na Fase 05).
    </p>
  )
}
