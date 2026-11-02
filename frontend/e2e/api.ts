import { expect, type Page } from '@playwright/test'

/**
 * Chamadas à API com a sessão do navegador (mesmos cookies), para montar cenários com dados fictícios
 * sem depender da interface — ex.: criar um produto com estoque exato antes de testar a compra.
 * Requer que a página esteja logada como ADMIN.
 */

async function csrf(page: Page): Promise<string> {
  const res = await page.request.get('/api/v1/auth/csrf')
  expect(res.ok()).toBeTruthy()
  const cookie = (await page.context().cookies()).find((c) => c.name === 'XSRF-TOKEN')
  return decodeURIComponent(cookie?.value ?? ((await res.json()) as { token: string }).token)
}

export async function apiPost<T>(page: Page, path: string, data: unknown, method: 'POST' | 'PUT' = 'POST') {
  const res = await page.request.fetch(path, { method, data, headers: { 'X-XSRF-TOKEN': await csrf(page) } })
  if (!res.ok()) throw new Error(`${method} ${path} → ${res.status()} ${await res.text()}`)
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export async function apiGetJson<T>(page: Page, path: string): Promise<T> {
  const res = await page.request.get(path)
  if (!res.ok()) throw new Error(`GET ${path} → ${res.status()}`)
  return (await res.json()) as T
}

type AdminProduct = {
  id: number
  slug: string
  name: string
  version: number
  price: number
  stock: { onHand: number; available: number } | null
}

export type NewProduct = {
  name: string
  price: number
  promoPrice?: number | null
  stock?: number
  type?: 'READY' | 'MADE_TO_ORDER'
  status?: 'ACTIVE' | 'DRAFT'
}

let seq = 0
const uid = () => `${Date.now() % 1e8}${++seq}`

/** Cria produto fictício (e lança estoque inicial) via API administrativa. */
export async function createProduct(page: Page, p: NewProduct): Promise<AdminProduct> {
  const categories = await apiGetJson<Array<{ id: number; active: boolean }>>(page, '/api/v1/admin/categories')
  const category = categories.find((c) => c.active)!
  const type = p.type ?? 'READY'
  const created = await apiPost<AdminProduct>(page, '/api/v1/admin/products', {
    name: `${p.name} ${uid()}`,
    sku: `GV-E2E-${uid()}`,
    description: 'Produto fictício criado pelos testes automatizados.',
    categoryId: category.id,
    type,
    status: p.status ?? 'ACTIVE',
    price: p.price,
    promoPrice: p.promoPrice ?? null,
    productionDays: type === 'MADE_TO_ORDER' ? 7 : null,
    featured: false,
    lowStockThreshold: 0,
    options: [],
  })
  if (type === 'READY' && p.stock) {
    await apiPost(page, `/api/v1/admin/products/${created.id}/stock-movements`, {
      type: 'PURCHASE',
      quantity: p.stock,
      reason: 'Estoque inicial (teste)',
    })
  }
  return getProduct(page, created.id)
}

export const getProduct = (page: Page, id: number) => apiGetJson<AdminProduct>(page, `/api/v1/admin/products/${id}`)

/** Altera só o preço (mantendo o restante) — simula o admin mudando o valor com o cliente no meio da compra. */
export async function changePrice(page: Page, id: number, price: number) {
  const full = await apiGetJson<Record<string, unknown> & { categoryId: number; version: number }>(
    page,
    `/api/v1/admin/products/${id}`,
  )
  await apiPost(
    page,
    `/api/v1/admin/products/${id}`,
    {
      name: full.name,
      slug: full.slug,
      sku: full.sku,
      description: full.description,
      categoryId: full.categoryId,
      type: full.type,
      status: full.status,
      price,
      promoPrice: null,
      productionDays: full.productionDays,
      featured: full.featured,
      lowStockThreshold: (full.stock as { lowStockThreshold: number } | null)?.lowStockThreshold ?? 0,
      options: [],
      version: full.version,
    },
    'PUT',
  )
}
