import { describe, expect, it } from 'vitest'
import { getDashboard, isPaid } from './metrics'
import { effectivePrice, getPublicProduct, listOrders, listPublicProducts } from './store'

describe('catalog (demo data)', () => {
  it('never exposes draft or archived products publicly', () => {
    expect(listPublicProducts().every((p) => p.status === 'ATIVO')).toBe(true)
    expect(getPublicProduct('chaveiro-hexagono')).toBeUndefined()
  })

  it('filters by category and search term', () => {
    expect(listPublicProducts({ category: 'decoracao' }).every((p) => p.categorySlug === 'decoracao')).toBe(true)
    expect(listPublicProducts({ q: 'dragão' }).map((p) => p.slug)).toEqual(['dragao-articulado'])
  })

  it('sorts by effective price', () => {
    const prices = listPublicProducts({ sort: 'menor-preco' }).map(effectivePrice)
    expect(prices).toEqual([...prices].sort((a, b) => a - b))
  })
})

describe('dashboard metrics', () => {
  it('computes KPIs consistent with their definitions', () => {
    const d = getDashboard(30)
    const inPeriod = listOrders().filter((o) => o.createdAt >= d.from && o.createdAt <= d.to)
    const paid = inPeriod.filter(isPaid)
    expect(d.kpis.orders).toBe(inPeriod.length)
    expect(d.kpis.salesCents).toBe(paid.reduce((s, o) => s + o.totalCents, 0))
    expect(d.dailySales).toHaveLength(30)
    expect(d.dailySales.reduce((s, p) => s + p.cents, 0)).toBe(d.kpis.salesCents)
    expect(d.statusCounts.reduce((s, c) => s + c.count, 0)).toBe(d.kpis.orders)
  })

  it('uses integer cents for the average ticket', () => {
    expect(Number.isInteger(getDashboard(90).kpis.avgTicketCents)).toBe(true)
  })
})
