import { ArrowRight, Boxes, Gem, Lamp, Paintbrush, Sparkles, Wrench, type LucideIcon } from 'lucide-react'
import { Link } from 'react-router'
import { InstagramLink } from '../components/InstagramLink'
import { ProductCard } from '../components/store/ProductCard'
import { buttonClass } from '../components/ui/button'
import { DemoBanner } from '../components/ui/DemoBanner'
import { EmptyState } from '../components/ui/EmptyState'
import { CardsSkeleton } from '../components/ui/Skeleton'
import { useCategories, useProducts } from '../api/catalog'
import { demoCategoryImage } from '../data/store'
import { usePageMeta } from '../lib/seo'
import { BANNERS, categoryImage, HERO_IMAGES } from '../lib/images'

const CATEGORY_ICONS: Record<string, LucideIcon> = {
  colecionaveis: Gem,
  articulados: Boxes,
  miniaturas: Paintbrush,
  decoracao: Lamp,
  acessorios: Wrench,
  personalizados: Sparkles,
}

const COLLECTION_BANNERS = [
  {
    to: '/produtos?categoria=colecionaveis',
    title: 'Colecionáveis',
    text: 'Estátuas e displays com acabamento detalhado.',
    image: BANNERS.collectibles,
  },
  {
    to: '/produtos?categoria=articulados',
    title: 'Impressão articulada',
    text: 'Peças com juntas móveis, impressas em uma só etapa.',
    image: BANNERS.articulated,
  },
]

/** Telas 1 (escuro) e 3 (claro) da referência visual. */
export function HomePage() {
  usePageMeta(undefined, 'Impressão 3D de alta qualidade, peças personalizadas, colecionáveis e muito mais.')
  const { data: categories = [], isPending: loadingCategories } = useCategories()
  const { data: featuredPage, isPending: loadingFeatured, isError } = useProducts({ featured: true, size: 4 })
  const featured = featuredPage?.content ?? []
  const hero = HERO_IMAGES.main

  return (
    <>
      <section className="relative isolate overflow-hidden">
        {/* Imagem provisória do pacote de assets; o texto fica em HTML sobre um gradiente do tema. */}
        <img
          src={hero.src}
          width={hero.width}
          height={hero.height}
          alt={hero.alt}
          fetchPriority="high"
          className="absolute inset-0 -z-10 h-full w-full object-cover object-[70%_center]"
        />
        <div
          aria-hidden
          className="absolute inset-0 -z-10 bg-gradient-to-r from-bg via-bg/85 to-bg/60 lg:via-bg/70 lg:to-transparent"
        />
        <div aria-hidden className="absolute inset-x-0 bottom-0 -z-10 h-24 bg-gradient-to-t from-bg to-transparent" />
        <div className="mx-auto grid max-w-7xl items-end gap-10 px-4 py-20 sm:px-6 lg:grid-cols-[1fr_auto] lg:py-28">
          <div className="max-w-xl">
            <h1 className="text-5xl font-bold tracking-tight sm:text-6xl">
              Seu universo <br />
              em <span className="text-accent">3D</span>
            </h1>
            <p className="mt-6 text-lg text-fg-muted">
              Impressão 3D de alta qualidade, peças personalizadas, colecionáveis e muito mais.
            </p>
            <div className="mt-8 flex flex-wrap gap-4">
              <Link to="/produtos" className={buttonClass('primary', 'lg', 'shadow-glow')}>
                Ver produtos <ArrowRight aria-hidden className="size-4" />
              </Link>
              <Link to="/personalizados" className={buttonClass('outline', 'lg', 'bg-bg/60 backdrop-blur')}>
                Personalizados
              </Link>
            </div>
          </div>
          <div className="lg:justify-self-end">
            <InstagramLink />
          </div>
        </div>
      </section>

      <section aria-labelledby="categorias" className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <h2 id="categorias" className="text-2xl font-bold">
          Categorias
        </h2>
        {loadingCategories ? (
          <CardsSkeleton count={6} aspect="aspect-[3/2]" />
        ) : categories.length === 0 ? (
          <div className="mt-6">
            <EmptyState icon={Boxes} title="Categorias em breve" description="O catálogo está sendo preparado." />
          </div>
        ) : (
          <ul className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-3">
            {categories.map((c) => {
              const Icon = CATEGORY_ICONS[c.slug] ?? Boxes
              const base = demoCategoryImage(c.slug)
              const img = base ? categoryImage(base) : null
              return (
                <li key={c.slug}>
                  <Link
                    to={`/produtos?categoria=${c.slug}`}
                    className="group relative flex aspect-[3/2] flex-col justify-end overflow-hidden rounded-card border border-border bg-surface shadow-card transition-colors hover:border-accent"
                  >
                    {img ? (
                      <img
                        src={img.src}
                        width={img.width}
                        height={img.height}
                        alt=""
                        loading="lazy"
                        decoding="async"
                        className="absolute inset-0 h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                      />
                    ) : (
                      <span
                        aria-hidden
                        className="absolute inset-0 flex items-center justify-center bg-gradient-to-br from-[#0b1f4d] via-[#123a8f] to-[#06101f]"
                      >
                        <Icon className="size-12 text-white/70" strokeWidth={1.25} />
                      </span>
                    )}
                    <span
                      aria-hidden
                      className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/30 to-transparent"
                    />
                    <span className="relative p-4 text-white">
                      <span className="block text-lg font-semibold">{c.name}</span>
                      <span className="hidden text-xs text-white/80 sm:block">{c.description}</span>
                    </span>
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
          <DemoBanner>
            Catálogo de demonstração do ambiente de desenvolvimento (vem da API; imagens ilustrativas).
          </DemoBanner>
        </div>
        {loadingFeatured ? (
          <CardsSkeleton count={4} aspect="aspect-[4/5]" />
        ) : isError ? (
          <div className="mt-6">
            <EmptyState
              icon={Gem}
              title="Não foi possível carregar os destaques"
              description="Tente novamente em instantes."
            />
          </div>
        ) : featured.length === 0 ? (
          <div className="mt-6">
            <EmptyState icon={Gem} title="Nenhum produto em destaque" description="Volte em breve para conferir." />
          </div>
        ) : (
          <div className="mt-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
            {featured.map((p, i) => (
              <ProductCard key={p.slug} product={p} index={i} />
            ))}
          </div>
        )}
      </section>

      <section aria-label="Coleções" className="mx-auto grid max-w-7xl gap-4 px-4 py-12 sm:px-6 md:grid-cols-2">
        {COLLECTION_BANNERS.map((b) => (
          <Link
            key={b.to}
            to={b.to}
            className="group relative flex aspect-[16/7] flex-col justify-end overflow-hidden rounded-card border border-border shadow-card"
          >
            <img
              src={b.image.src}
              width={b.image.width}
              height={b.image.height}
              alt=""
              loading="lazy"
              decoding="async"
              className="absolute inset-0 h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
            />
            <span aria-hidden className="absolute inset-0 bg-gradient-to-r from-black/85 via-black/45 to-transparent" />
            <span className="relative max-w-xs p-6 text-white">
              <span className="block text-2xl font-bold">{b.title}</span>
              <span className="mt-1 block text-sm text-white/80">{b.text}</span>
              <span className="mt-3 inline-flex items-center gap-1 text-sm font-semibold text-[#7db4ff]">
                Explorar <ArrowRight aria-hidden className="size-4" />
              </span>
            </span>
          </Link>
        ))}
      </section>

      <section aria-labelledby="personalizados" className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <div className="relative isolate overflow-hidden rounded-card border border-border bg-surface shadow-card">
          <img
            src={BANNERS.customPainting.src}
            width={BANNERS.customPainting.width}
            height={BANNERS.customPainting.height}
            alt=""
            loading="lazy"
            decoding="async"
            className="absolute inset-y-0 right-0 -z-10 h-full w-full object-cover object-right md:w-2/3"
          />
          <div
            aria-hidden
            className="absolute inset-0 -z-10 bg-gradient-to-r from-surface via-surface/90 to-surface/40 md:via-surface/80 md:to-transparent"
          />
          <div className="max-w-xl p-8 sm:p-12">
            <p className="text-sm font-semibold uppercase tracking-widest text-accent">Personalizados</p>
            <h2 id="personalizados" className="mt-2 text-3xl font-bold">
              Tem uma ideia? A gente imprime.
            </h2>
            <p className="mt-4 text-fg-muted">
              Miniaturas, pintura personalizada, presentes e projetos sob medida. Conte o que você precisa e receba um
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
