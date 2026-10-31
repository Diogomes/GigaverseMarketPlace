import {
  BarChart3,
  Boxes,
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  History,
  Package,
  PanelLeftClose,
  PanelLeftOpen,
  Settings,
  ShieldCheck,
  ShoppingBag,
  Tags,
  Users,
  X,
  type LucideIcon,
} from 'lucide-react'
import { Suspense, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { initials } from '../../lib/format'
import { useAuth } from '../../auth/AuthProvider'
import { ThemeToggle } from '../../components/ThemeToggle'
import { cn } from '../../lib/cn'

const ADMIN_LINKS: Array<{ to: string; label: string; icon: LucideIcon; end?: boolean; adminOnly?: boolean }> = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/admin/pedidos', label: 'Pedidos', icon: ShoppingBag },
  { to: '/admin/produtos', label: 'Produtos', icon: Package },
  { to: '/admin/clientes', label: 'Clientes', icon: Users },
  { to: '/admin/estoque', label: 'Estoque', icon: Boxes },
  { to: '/admin/categorias', label: 'Categorias', icon: Tags },
  { to: '/admin/conteudo', label: 'Conteúdo', icon: FileText },
  { to: '/admin/relatorios', label: 'Relatórios', icon: BarChart3 },
  { to: '/admin/configuracoes', label: 'Configurações', icon: Settings },
  { to: '/admin/equipe', label: 'Equipe', icon: ShieldCheck, adminOnly: true },
  { to: '/admin/auditoria', label: 'Auditoria', icon: History, adminOnly: true },
]

function SidebarNav({ collapsed, onNavigate }: { collapsed: boolean; onNavigate?: () => void }) {
  const { user } = useAuth()
  // esconder o link é só conveniência: o servidor recusa (403) quem não é ADMIN
  const links = ADMIN_LINKS.filter((l) => !l.adminOnly || user?.role === 'ADMIN')
  return (
    <nav aria-label="Administração">
      <ul className="flex flex-col gap-1">
        {links.map(({ to, label, icon: Icon, end }) => (
          <li key={to}>
            <NavLink
              to={to}
              end={end}
              onClick={onNavigate}
              title={collapsed ? label : undefined}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  collapsed && 'justify-center px-0',
                  isActive ? 'bg-primary text-on-primary' : 'text-fg-muted hover:bg-surface-muted hover:text-fg',
                )
              }
            >
              <Icon aria-hidden className="size-4 shrink-0" />
              <span className={collapsed ? 'sr-only' : ''}>{label}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}

function SidebarLogo({ collapsed }: { collapsed: boolean }) {
  return (
    <Link to="/admin" className="flex items-center gap-2 rounded-lg bg-logo-chip p-1.5" aria-label="Painel Gigaverse3D">
      <img src="/brand/gigaverse-emblem.png" alt="" className="size-8 shrink-0" />
      {!collapsed && <img src="/brand/gigaverse-wordmark.png" alt="" className="h-4 w-auto" />}
    </Link>
  )
}

/** Tela 6 da referência: sidebar recolhível no desktop, gaveta no celular, barra superior. */
export function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="flex min-h-dvh">
      <a
        href="#admin-conteudo"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-on-primary"
      >
        Pular para o conteúdo
      </a>
      <aside
        className={cn(
          'sticky top-0 hidden h-dvh shrink-0 flex-col gap-6 border-r border-border bg-bg-elevated p-3 lg:flex',
          collapsed ? 'w-[4.25rem]' : 'w-60',
        )}
      >
        <SidebarLogo collapsed={collapsed} />
        <SidebarNav collapsed={collapsed} />
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Menu administrativo">
          <button
            type="button"
            aria-label="Fechar menu"
            className="absolute inset-0 bg-black/60"
            onClick={() => setMobileOpen(false)}
          />
          <div className="relative flex h-full w-72 flex-col gap-6 bg-bg-elevated p-4 shadow-card">
            <div className="flex items-center justify-between">
              <SidebarLogo collapsed={false} />
              <button
                type="button"
                className="inline-flex size-10 items-center justify-center rounded-lg text-fg-muted"
                aria-label="Fechar menu"
                onClick={() => setMobileOpen(false)}
              >
                <X aria-hidden className="size-5" />
              </button>
            </div>
            <SidebarNav collapsed={false} onNavigate={() => setMobileOpen(false)} />
          </div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-bg/90 px-4 backdrop-blur sm:px-6">
          <button
            type="button"
            className="inline-flex size-10 items-center justify-center rounded-lg text-fg-muted hover:bg-surface-muted lg:hidden"
            aria-label="Abrir menu"
            onClick={() => setMobileOpen(true)}
          >
            <Menu aria-hidden className="size-5" />
          </button>
          <button
            type="button"
            className="hidden size-10 items-center justify-center rounded-lg text-fg-muted hover:bg-surface-muted lg:inline-flex"
            aria-label={collapsed ? 'Expandir menu lateral' : 'Recolher menu lateral'}
            aria-pressed={collapsed}
            onClick={() => setCollapsed((c) => !c)}
          >
            {collapsed ? (
              <PanelLeftOpen aria-hidden className="size-5" />
            ) : (
              <PanelLeftClose aria-hidden className="size-5" />
            )}
          </button>
          <span className="text-sm font-medium text-fg-muted">Painel administrativo</span>
          <div className="ml-auto flex items-center gap-3">
            <ThemeToggle />
            <div className="flex items-center gap-2">
              <span className="inline-flex size-9 items-center justify-center rounded-full bg-primary/15 text-sm font-semibold text-accent">
                {initials(user?.fullName ?? '')}
              </span>
              <span className="hidden text-sm leading-tight sm:block">
                <span className="block font-medium">{user?.fullName}</span>
                <span className="block text-xs text-fg-muted">
                  {user?.role === 'ADMIN' ? 'Administrador' : 'Operador'}
                </span>
              </span>
            </div>
            <button
              type="button"
              onClick={() => void logout().then(() => navigate('/'))}
              className="inline-flex size-10 items-center justify-center rounded-lg text-fg-muted hover:bg-surface-muted hover:text-danger"
              aria-label="Sair"
              title="Sair"
            >
              <LogOut aria-hidden className="size-5" />
            </button>
          </div>
        </header>
        <main id="admin-conteudo" className="flex-1 p-4 sm:p-6">
          <Suspense
            fallback={
              <p role="status" className="py-24 text-center text-sm text-fg-muted">
                Carregando…
              </p>
            }
          >
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  )
}
