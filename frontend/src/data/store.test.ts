import { describe, expect, it } from 'vitest'
import { effectivePrice, getPublicProduct, listPublicProducts } from './store'

describe('catalog (demo data)', () => {
  it('never exposes draft or archived products publicly', () => {
    expect(listPublicProducts().every((p) => p.status === 'ATIVO')).toBe(true)
    expect(getPublicProduct('chaveiro-hexagono')).toBeUndefined()
  })

  it('filters by category and search term', () => {
    expect(listPublicProducts({ category: 'decoracao' }).every((p) => p.categorySlug === 'decoracao')).toBe(true)
    expect(listPublicProducts({ q: 'dragão' }).map((p) => p.slug)).toEqual(['dragao-mecanico-azul'])
  })

  it('sorts by effective price', () => {
    const prices = listPublicProducts({ sort: 'menor-preco' }).map(effectivePrice)
    expect(prices).toEqual([...prices].sort((a, b) => a - b))
  })
})
