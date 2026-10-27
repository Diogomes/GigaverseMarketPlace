import { Link } from 'react-router'

/** Página honesta para rotas cujo conteúdo chega em fases futuras. */
export function PlaceholderPage({ title, phase }: { title: string; phase: string }) {
  return (
    <section className="mx-auto max-w-3xl px-4 py-24 text-center sm:px-6">
      <h1 className="text-3xl font-bold">{title}</h1>
      <p className="mt-4 text-fg-muted">Esta área está em desenvolvimento ({phase}).</p>
      <Link to="/" className="mt-8 inline-block font-semibold text-accent hover:underline">
        Voltar para o início
      </Link>
    </section>
  )
}

export function NotFoundPage() {
  return (
    <section className="mx-auto max-w-3xl px-4 py-24 text-center sm:px-6">
      <p className="text-sm font-semibold uppercase tracking-widest text-accent">Erro 404</p>
      <h1 className="mt-2 text-3xl font-bold">Página não encontrada</h1>
      <p className="mt-4 text-fg-muted">O endereço acessado não existe ou foi movido.</p>
      <Link to="/" className="mt-8 inline-block font-semibold text-accent hover:underline">
        Voltar para o início
      </Link>
    </section>
  )
}
