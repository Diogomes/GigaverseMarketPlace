/**
 * Camada de acesso a dados do frontend. Hoje serve dados de demonstração apenas em
 * desenvolvimento; em produção retorna listas vazias (as telas mostram estados vazios).
 * Nas Fases 04–09 estas funções passam a chamar a API real via React Query.
 */
import { isoDay } from '../lib/format'
import { demoAddresses, demoCategories, demoCustomers, demoOrders, demoProducts, DEMO_TODAY } from './demo'
import type { Address, Category, Customer, ImageSet, Order, Product } from './types'

export const DEMO_MODE = import.meta.env.DEV

/** Usuário fictício usado na pré-visualização da área do cliente (sem login real até a Fase 05). */
export const PREVIEW_CUSTOMER_ID = 'c1'

const categories = (): Category[] => (DEMO_MODE ? demoCategories : [])
const products = (): Product[] => (DEMO_MODE ? demoProducts : [])
const orders = (): Order[] => (DEMO_MODE ? demoOrders : [])
const customers = (): Customer[] => (DEMO_MODE ? demoCustomers : [])

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

/** Imagem do produto referenciado por um item de pedido (snapshot não guarda imagem). */
export function imageForProduct(slug: string): ImageSet | undefined {
  return products().find((p) => p.slug === slug)?.image
}

// --- admin ---

export function listAdminProducts(): Product[] {
  return products()
}

export function getAdminProduct(id: string): Product | undefined {
  return products().find((p) => p.id === id)
}

export function listOrders(): Order[] {
  return orders()
}

export function listCustomerOrders(customerId: string): Order[] {
  return orders().filter((o) => o.customerId === customerId)
}

export function getCustomerOrder(customerId: string, number: string): Order | undefined {
  // a checagem de propriedade será feita no backend (Fase 05/08); aqui só simula o mesmo filtro
  return listCustomerOrders(customerId).find((o) => o.number === number)
}

export function listCustomers(): Customer[] {
  return customers()
}

export function getCustomer(id: string): Customer | undefined {
  return customers().find((c) => c.id === id)
}

export type CustomerSummary = { orderCount: number; totalSpentCents: number; lastOrderAt?: string }

/** Total gasto considera apenas pedidos não cancelados. */
export function customerSummary(customerId: string): CustomerSummary {
  const list = listCustomerOrders(customerId).filter((o) => o.status !== 'CANCELADO')
  return {
    orderCount: list.length,
    totalSpentCents: list.reduce((s, o) => s + o.totalCents, 0),
    lastOrderAt: list[0]?.createdAt,
  }
}

export function listAddresses(): Address[] {
  return DEMO_MODE ? demoAddresses : []
}

export function today(): string {
  return isoDay(DEMO_MODE ? DEMO_TODAY : new Date())
}
