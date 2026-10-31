import type { ApiCategory, Page, ProductSummary } from '../api/types'

export function summary(overrides: Partial<ProductSummary> = {}): ProductSummary {
  return {
    slug: 'raptor-articulado',
    name: 'Raptor Articulado',
    categorySlug: 'articulados',
    categoryName: 'Articulados',
    type: 'READY',
    price: 129.9,
    promoPrice: null,
    effectivePrice: 129.9,
    currency: 'BRL',
    availability: 'IN_STOCK',
    productionDays: null,
    featured: true,
    image: null,
    ...overrides,
  }
}

export function page<T>(content: T[]): Page<T> {
  return { content, page: 0, size: 12, totalElements: content.length, totalPages: 1 }
}

export const categories: ApiCategory[] = [
  { slug: 'articulados', name: 'Articulados', description: 'Peças com juntas móveis.', parentSlug: null },
  { slug: 'decoracao', name: 'Decoração', description: 'Objetos para o ambiente.', parentSlug: null },
]
