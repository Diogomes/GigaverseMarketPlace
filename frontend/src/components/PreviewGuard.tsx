import { ShieldAlert } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { SESSION_EXPIRED_FLAG, useAuth, type Role } from '../auth/AuthProvider'
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
    let expired = false
    try {
      expired = sessionStorage.getItem(SESSION_EXPIRED_FLAG) === '1'
    } catch {
      // ignora
    }
    return <Navigate to={`/entrar?next=${next}${expired ? '&expirada=1' : ''}`} replace />
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
