import { ArrowRight, Boxes, Gem, Lamp, Sparkles, Wrench, type LucideIcon } from 'lucide-react'
import { Link } from 'react-router'
import { InstagramLink } from '../components/InstagramLink'
import { ProductCard } from '../components/store/ProductCard'
import { buttonClass } from '../components/ui/button'
import { DemoBanner } from '../components/ui/DemoBanner'
import { EmptyState } from '../components/ui/EmptyState'
import { listCategories, listFeaturedProducts } from '../data/store'

const CATEGORY_ICONS: Record<string, LucideIcon> = {
  colecionaveis: Gem,
  articulados: Boxes,
  decoracao: Lamp,
  acessorios: Wrench,
  personalizados: Sparkles,
}

/** Telas 1 (escuro) e 3 (claro) da referência visual. */
export function HomePage() {
  const categories = listCategories()
  const featured = listFeaturedProducts()

  return (
    <>
      <section className="relative overflow-hidden">
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 bg-[radial-gradient(60%_60%_at_75%_40%,rgb(47_123_255/0.18),transparent_70%)]"
        />
        <div className="relative mx-auto grid max-w-7xl items-center gap-12 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:py-24">
          <div>
            <h1 className="text-5xl font-bold tracking-tight sm:text-6xl">
              Seu universo <br />
              em <span className="text-accent">3D</span>
            </h1>
            <p className="mt-6 max-w-xl text-lg text-fg-muted">
              Impressão 3D de alta qualidade, peças personalizadas, colecionáveis e muito mais.
            </p>
            <div className="mt-8 flex flex-wrap gap-4">
              <Link to="/produtos" className={buttonClass('primary', 'lg', 'shadow-glow')}>
                Ver produtos <ArrowRight aria-hidden className="size-4" />
              </Link>
              <Link to="/personalizados" className={buttonClass('outline', 'lg')}>
                Personalizados
              </Link>
            </div>
          </div>

          <div className="flex flex-col items-center gap-8 lg:items-end">
            {/* Arte oficial da marca até o lojista fornecer fotos de produtos. */}
            <img
              src="/brand/gigaverse-logo-512.png"
              alt="Logo Gigaverse3D: hexágono prateado com contorno azul e planeta em órbita"
              width={512}
              height={512}
              className="w-full max-w-sm rounded-[2rem] shadow-card"
            />
            <InstagramLink />
          </div>
        </div>
      </section>

      <section aria-labelledby="categorias" className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <h2 id="categorias" className="text-2xl font-bold">
          Categorias
        </h2>
        {categories.length === 0 ? (
          <div className="mt-6">
            <EmptyState icon={Boxes} title="Categorias em breve" description="O catálogo está sendo preparado." />
          </div>
        ) : (
          <ul className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
            {categories.map((c) => {
              const Icon = CATEGORY_ICONS[c.slug] ?? Boxes
              return (
                <li key={c.slug}>
                  <Link
                    to={`/produtos?categoria=${c.slug}`}
                    className="flex h-full flex-col gap-3 rounded-card border border-border bg-surface p-4 shadow-card transition-colors hover:border-accent"
                  >
                    <span className="inline-flex size-10 items-center justify-center rounded-lg bg-primary/10 text-accent">
                      <Icon aria-hidden className="size-5" />
                    </span>
                    <span className="font-semibold">{c.name}</span>
                    <span className="text-xs text-fg-muted">{c.description}</span>
                  </Link>
                </li>
              )
            })}
          </ul>
        )}
      </section>

      <section aria-labelledby="destaques" className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <div className="flex items-end justify-between gap-4">
          <h2 id="destaques" className="text-2xl font-bold">
            Produtos em destaque
          </h2>
          <Link to="/produtos" className="text-sm font-semibold text-accent hover:underline">
            Ver todos
          </Link>
        </div>
        <div className="mt-4">
          <DemoBanner>Produtos fictícios para visualizar o layout; o catálogo real vem da API na Fase 07.</DemoBanner>
        </div>
        {featured.length === 0 ? (
          <div className="mt-6">
            <EmptyState icon={Gem} title="Nenhum produto em destaque" description="Volte em breve para conferir." />
          </div>
        ) : (
          <div className="mt-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
            {featured.map((p, i) => (
              <ProductCard key={p.id} product={p} index={i} />
            ))}
          </div>
        )}
      </section>

      <section aria-labelledby="personalizados" className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <div className="relative overflow-hidden rounded-card border border-border bg-surface p-8 shadow-card sm:p-12">
          <div
            aria-hidden
            className="pointer-events-none absolute inset-0 bg-[radial-gradient(50%_80%_at_100%_50%,rgb(47_123_255/0.18),transparent_70%)]"
          />
          <div className="relative max-w-2xl">
            <p className="text-sm font-semibold uppercase tracking-widest text-accent">Personalizados</p>
            <h2 id="personalizados" className="mt-2 text-3xl font-bold">
              Tem uma ideia? A gente imprime.
            </h2>
            <p className="mt-4 text-fg-muted">
              Miniaturas, peças técnicas, presentes e projetos sob medida. Conte o que você precisa e receba um
              orçamento antes de qualquer pagamento.
            </p>
            <Link to="/personalizados" className={buttonClass('primary', 'lg', 'mt-6')}>
              Como funciona <ArrowRight aria-hidden className="size-4" />
            </Link>
          </div>
        </div>
      </section>
    </>
  )
}
