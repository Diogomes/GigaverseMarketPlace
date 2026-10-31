import { Link } from 'react-router'
import { InstagramIcon } from './icons/InstagramIcon'
import { ApiStatus } from '../features/status/ApiStatus'
import { INSTAGRAM_URL } from '../lib/links'

const CURRENT_YEAR = new Date().getFullYear()

export function SiteFooter() {
  return (
    <footer className="border-t border-border bg-bg-elevated">
      <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 py-8 text-sm text-fg-muted sm:flex-row sm:items-center sm:justify-between sm:px-6">
        <div className="flex flex-col gap-3">
          <p>© {CURRENT_YEAR} Gigaverse3D. Impressão 3D • Tecnologia • Universo.</p>
          <nav aria-label="Institucional">
            <ul className="flex flex-wrap gap-x-4 gap-y-1">
              {[
                ['/sobre', 'Sobre'],
                ['/contato', 'Contato'],
                ['/perguntas-frequentes', 'Perguntas frequentes'],
                ['/politica-comercial', 'Política comercial'],
                ['/privacidade', 'Privacidade'],
                ['/termos', 'Termos'],
              ].map(([to, label]) => (
                <li key={to}>
                  <Link to={to} className="hover:text-fg">
                    {label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        </div>
        <div className="flex items-center gap-6">
          {import.meta.env.DEV && <ApiStatus />}
          <a
            href={INSTAGRAM_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 hover:text-fg"
          >
            <InstagramIcon aria-hidden className="size-4" />
            Instagram<span className="sr-only"> (abre em nova aba)</span>
          </a>
        </div>
      </div>
    </footer>
  )
}
