import type { ReactNode } from 'react'
import { Link } from 'react-router'

/** Tela 5 da referência: formulário + painel visual (o painel some em telas pequenas). */
export function AuthLayout({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <div className="mx-auto flex max-w-5xl px-4 py-10 sm:px-6 lg:py-16">
      <div className="grid w-full overflow-hidden rounded-card border border-border bg-surface shadow-card lg:grid-cols-2">
        <div className="p-6 sm:p-10">
          <Link
            to="/"
            className="mx-auto flex w-fit flex-col items-center gap-2"
            aria-label="Gigaverse3D — página inicial"
          >
            <span className="rounded-xl bg-logo-chip p-2">
              <img src="/brand/gigaverse-emblem.png" alt="" className="size-14" />
            </span>
            <img
              src="/brand/gigaverse-wordmark.png"
              alt="Gigaverse3D"
              className="h-5 w-auto rounded bg-logo-chip px-1"
            />
          </Link>
          <h1 className="mt-8 text-2xl font-bold">{title}</h1>
          <p className="mt-1 text-sm text-fg-muted">{subtitle}</p>
          <div className="mt-6">{children}</div>
        </div>
        <div aria-hidden className="relative hidden flex-col justify-end overflow-hidden bg-[#05070c] p-10 lg:flex">
          {/* Imagem ilustrativa do pacote de assets (docs/IMAGE_ASSETS.md). */}
          <img
            src="/images/products/sci-fi-knight-detail-1200x1200.jpg"
            width={1200}
            height={1200}
            alt=""
            decoding="async"
            className="absolute inset-0 h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/30 to-black/10" />
          <img src="/brand/gigaverse-emblem.png" alt="" className="relative mb-4 ml-auto size-14 rounded-xl" />
          <p className="relative text-right text-2xl font-semibold leading-snug text-white">
            Mais que impressão 3D,
            <br />é o seu <span className="text-[#4c9bff]">universo.</span>
          </p>
        </div>
      </div>
    </div>
  )
}
