/**
 * Catálogo de demonstração usado só em desenvolvimento para dar imagens aos produtos do seed
 * (por slug). Pedidos, clientes e métricas vêm da API real desde a Fase 09.
 */
import { demoCategories, demoProducts } from './demo'
import type { Category, ImageSet, Product } from './types'

export const DEMO_MODE = import.meta.env.DEV

const categories = (): Category[] => (DEMO_MODE ? demoCategories : [])
const products = (): Product[] => (DEMO_MODE ? demoProducts : [])

export function listCategories(): Category[] {
  return categories()
}

export function getCategory(slug: string): Category | undefined {
  return categories().find((c) => c.slug === slug)
}

export type ProductSort = 'relevancia' | 'menor-preco' | 'maior-preco' | 'nome'

export function effectivePrice(p: Product): number {
  return p.promoPriceCents ?? p.priceCents
}

/** Catálogo público: somente produtos ATIVOS. */
export function listPublicProducts(filter: { q?: string; category?: string; sort?: ProductSort } = {}): Product[] {
  const q = filter.q?.trim().toLowerCase()
  const result = products().filter(
    (p) =>
      p.status === 'ATIVO' &&
      (!filter.category || p.categorySlug === filter.category) &&
      (!q || p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q)),
  )
  switch (filter.sort) {
    case 'menor-preco':
      return result.sort((a, b) => effectivePrice(a) - effectivePrice(b))
    case 'maior-preco':
      return result.sort((a, b) => effectivePrice(b) - effectivePrice(a))
    case 'nome':
      return result.sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'))
    default:
      return result.sort((a, b) => Number(b.featured) - Number(a.featured))
  }
}

export function listFeaturedProducts(): Product[] {
  return listPublicProducts().filter((p) => p.featured)
}

export function getPublicProduct(slug: string): Product | undefined {
  return products().find((p) => p.slug === slug && p.status === 'ATIVO')
}

export function listRelatedProducts(product: Product, limit = 3): Product[] {
  const others = listPublicProducts().filter((p) => p.id !== product.id)
  const same = others.filter((p) => p.categorySlug === product.categorySlug)
  return [...same, ...others.filter((p) => p.categorySlug !== product.categorySlug)].slice(0, limit)
}

/** Imagem de demonstração por slug (só em desenvolvimento; em produção, sempre undefined). */
export function demoImageFor(slug: string | null | undefined): ImageSet | undefined {
  return DEMO_MODE && slug ? demoProducts.find((p) => p.slug === slug)?.image : undefined
}

export function demoCategoryImage(slug: string): string | undefined {
  return DEMO_MODE ? demoCategories.find((c) => c.slug === slug)?.image : undefined
}

/** Imagem do produto referenciado por um item de pedido (snapshot não guarda imagem). */
export function imageForProduct(slug: string): ImageSet | undefined {
  return products().find((p) => p.slug === slug)?.image
}
