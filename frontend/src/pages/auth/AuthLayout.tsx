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
          <div className="absolute inset-0 bg-[radial-gradient(70%_60%_at_60%_35%,rgb(47_123_255/0.35),transparent_70%)]" />
          <img
            src="/brand/gigaverse-emblem.png"
            alt=""
            className="absolute left-1/2 top-[38%] w-64 -translate-x-1/2 -translate-y-1/2 mix-blend-screen [mask-image:radial-gradient(closest-side,black_62%,transparent)]"
          />
          <p className="relative text-right text-2xl font-semibold leading-snug text-white">
            Mais que impressão 3D,
            <br />é o seu <span className="text-[#4c9bff]">universo.</span>
          </p>
        </div>
      </div>
    </div>
  )
}
