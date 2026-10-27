import { FlaskConical, ShieldAlert } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { useAuth, type Role } from '../auth/AuthProvider'
import { DEMO_MODE } from '../data/store'
import { buttonClass } from './ui/button'

/**
 * Exige sessão (e, opcionalmente, um dos papéis). É só navegação/UX:
 * a autorização real é feita pelo backend em cada requisição.
 */
export function RequireAuth({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <p role="status" className="px-4 py-24 text-center text-sm text-fg-muted">
        Verificando sessão…
      </p>
    )
  }
  if (!user) {
    const next = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/entrar?next=${next}`} replace />
  }
  if (roles && !roles.includes(user.role)) {
    return (
      <section className="mx-auto max-w-xl px-4 py-24 text-center">
        <ShieldAlert aria-hidden className="mx-auto size-10 text-danger" />
        <h1 className="mt-4 text-2xl font-bold">Acesso restrito</h1>
        <p className="mt-2 text-fg-muted">Sua conta não tem permissão para acessar esta área.</p>
        <Link to="/" className={buttonClass('primary', 'md', 'mt-6')}>
          Voltar para a loja
        </Link>
      </section>
    )
  }
  return children
}

/** Faixa de aviso nas áreas que ainda exibem dados de demonstração (pedidos, métricas, CRM). */
export function DemoRibbon({ area }: { area: string }) {
  if (!DEMO_MODE) return null
  return (
    <p className="flex items-center gap-2 border-b border-amber-500/40 bg-amber-500/10 px-4 py-1.5 text-xs text-amber-800 dark:text-amber-200">
      <FlaskConical aria-hidden className="size-3.5 shrink-0" />
      {area}: sessão real; pedidos, métricas e clientes exibidos ainda são de demonstração (APIs nas Fases 06–09).
    </p>
  )
}
