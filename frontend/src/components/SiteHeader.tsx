import { LayoutDashboard, LogOut, Menu, Search, ShoppingCart, User, X } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthProvider'
import { NAV_LINKS } from '../lib/links'
import { BrandLogo } from './BrandLogo'
import { ThemeToggle } from './ThemeToggle'
import { buttonClass } from './ui/button'

function navClass({ isActive }: { isActive: boolean }) {
  return `relative rounded-md px-3 py-2 text-sm font-medium transition-colors ${
    isActive ? 'text-accent' : 'text-fg-muted hover:text-fg'
  }`
}

const iconButton =
  'inline-flex size-10 items-center justify-center rounded-lg text-fg-muted transition-colors hover:bg-surface-muted hover:text-fg'

export function SiteHeader() {
  const [open, setOpen] = useState(false)
  const { user, isStaff, logout } = useAuth()
  const navigate = useNavigate()
  const firstName = user?.fullName.split(' ')[0]

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-bg/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-2 px-4 sm:px-6">
        <Link to="/" aria-label="Gigaverse3D — página inicial" className="shrink-0">
          <BrandLogo />
        </Link>

        <nav aria-label="Principal" className="ml-4 hidden lg:block">
          <ul className="flex items-center gap-1">
            {NAV_LINKS.map((l) => (
              <li key={l.to}>
                <NavLink to={l.to} end={l.to === '/'} className={navClass}>
                  {l.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <div className="ml-auto flex items-center gap-1">
          {/* Wrapper com display próprio: `hidden` e `inline-flex` na mesma classe conflitam. */}
          <span className="hidden sm:contents">
            <Link to="/produtos?busca=1" className={iconButton} aria-label="Buscar produtos">
              <Search aria-hidden className="size-5" />
            </Link>
            <Link to="/conta" className={iconButton} aria-label="Minha conta">
              <User aria-hidden className="size-5" />
            </Link>
          </span>
          <Link to="/carrinho" className={iconButton} aria-label="Carrinho">
            <ShoppingCart aria-hidden className="size-5" />
          </Link>
          <ThemeToggle />
          <span className="hidden sm:contents">
            {user ? (
              <>
                {isStaff && (
                  <Link to="/admin" className={iconButton} aria-label="Painel administrativo" title="Painel">
                    <LayoutDashboard aria-hidden className="size-5" />
                  </Link>
                )}
                <Link to="/conta" className="ml-1 max-w-32 truncate text-sm font-medium text-fg-muted hover:text-fg">
                  Olá, {firstName}
                </Link>
                <button
                  type="button"
                  onClick={() => void logout().then(() => navigate('/'))}
                  className={iconButton}
                  aria-label="Sair"
                  title="Sair"
                >
                  <LogOut aria-hidden className="size-5" />
                </button>
              </>
            ) : (
              <Link to="/entrar" className={buttonClass('primary', 'md', 'ml-2')}>
                Entrar
              </Link>
            )}
          </span>
          <button
            type="button"
            className={`${iconButton} lg:hidden`}
            aria-expanded={open}
            aria-controls="mobile-nav"
            aria-label={open ? 'Fechar menu' : 'Abrir menu'}
            onClick={() => setOpen((o) => !o)}
          >
            {open ? <X aria-hidden className="size-5" /> : <Menu aria-hidden className="size-5" />}
          </button>
        </div>
      </div>

      {open && (
        <nav id="mobile-nav" aria-label="Principal (celular)" className="border-t border-border lg:hidden">
          <ul className="mx-auto flex max-w-7xl flex-col gap-1 px-4 py-3">
            {[
              ...NAV_LINKS,
              { to: '/conta', label: 'Minha conta' },
              ...(isStaff ? [{ to: '/admin', label: 'Painel' }] : []),
              ...(user ? [] : [{ to: '/entrar', label: 'Entrar' }]),
            ].map((l) => (
              <li key={l.to}>
                <NavLink to={l.to} end={l.to === '/'} className={navClass} onClick={() => setOpen(false)}>
                  {l.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </header>
  )
}
