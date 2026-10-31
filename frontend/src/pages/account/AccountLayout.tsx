import { Headset, House, LogOut, MapPin, Package, UserRound, type LucideIcon } from 'lucide-react'
import { NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../../auth/AuthProvider'
import { cn } from '../../lib/cn'

const LINKS: Array<{ to: string; label: string; icon: LucideIcon; end?: boolean }> = [
  { to: '/conta', label: 'Minha conta', icon: House, end: true },
  { to: '/conta/pedidos', label: 'Meus pedidos', icon: Package },
  { to: '/conta/enderecos', label: 'Endereços', icon: MapPin },
  { to: '/conta/dados', label: 'Meus dados', icon: UserRound },
  { to: '/conta/suporte', label: 'Suporte', icon: Headset },
]

/** Tela 4 da referência: navegação lateral no desktop, abas roláveis no celular. */
export function AccountLayout() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    cn(
      'flex shrink-0 items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
      isActive ? 'bg-primary text-on-primary' : 'text-fg-muted hover:bg-surface-muted hover:text-fg',
    )

  return (
    <>
      <div className="mx-auto grid max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:grid-cols-[15rem_1fr]">
        <aside className="lg:self-start lg:rounded-card lg:border lg:border-border lg:bg-surface lg:p-3 lg:shadow-card">
          <nav aria-label="Minha conta">
            <ul className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-2 lg:mx-0 lg:flex-col lg:gap-1 lg:overflow-visible lg:p-0">
              {LINKS.map(({ to, label, icon: Icon, end }) => (
                <li key={to}>
                  <NavLink to={to} end={end} className={linkClass}>
                    <Icon aria-hidden className="size-4" />
                    {label}
                  </NavLink>
                </li>
              ))}
              <li className="lg:mt-2 lg:border-t lg:border-border lg:pt-2">
                <button
                  type="button"
                  onClick={() => void logout().then(() => navigate('/'))}
                  className="flex shrink-0 items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-danger hover:bg-danger/10"
                >
                  <LogOut aria-hidden className="size-4" />
                  Sair
                </button>
              </li>
            </ul>
          </nav>
        </aside>
        <div className="min-w-0">
          <Outlet />
        </div>
      </div>
    </>
  )
}
