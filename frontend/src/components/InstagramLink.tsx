import { InstagramIcon } from './icons/InstagramIcon'
import { INSTAGRAM_HANDLE, INSTAGRAM_URL } from '../lib/links'

export function InstagramLink() {
  return (
    <a
      href={INSTAGRAM_URL}
      target="_blank"
      rel="noopener noreferrer"
      className="group inline-flex items-center gap-3 rounded-card border border-border bg-surface/80 p-3 pr-5 shadow-card transition-colors hover:border-accent"
    >
      <span className="inline-flex size-11 items-center justify-center rounded-xl bg-gradient-to-br from-[#f58529] via-[#dd2a7b] to-[#8134af] text-white">
        <InstagramIcon aria-hidden className="size-6" />
      </span>
      <span className="text-sm leading-tight">
        <span className="block text-fg-muted">Acompanhe nossos trabalhos no Instagram</span>
        <span className="block font-semibold text-accent group-hover:underline">{INSTAGRAM_HANDLE}</span>
        <span className="sr-only">(abre em nova aba)</span>
      </span>
    </a>
  )
}
