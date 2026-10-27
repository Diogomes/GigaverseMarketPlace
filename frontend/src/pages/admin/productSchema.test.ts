import { describe, expect, it } from 'vitest'
import { productSchema, type ProductFormInput } from './productSchema'

const valid: ProductFormInput = {
  name: 'Dragão Articulado',
  sku: 'GV-ART-001',
  categorySlug: 'articulados',
  description: 'Descrição suficientemente longa.',
  price: '189,90',
  promoPrice: '',
  type: 'PRONTO',
  stock: '10',
  productionDays: '',
  status: 'ATIVO',
  images: undefined,
}

function errorPaths(input: ProductFormInput) {
  const r = productSchema.safeParse(input)
  return r.success ? [] : r.error.issues.map((i) => i.path.join('.'))
}

describe('productSchema', () => {
  it('accepts a valid product', () => {
    expect(errorPaths(valid)).toEqual([])
  })

  it('requires promo price to be lower than the regular price', () => {
    expect(errorPaths({ ...valid, promoPrice: '200,00' })).toContain('promoPrice')
    expect(errorPaths({ ...valid, promoPrice: '150,00' })).toEqual([])
  })

  it('requires production time for made-to-order products', () => {
    expect(errorPaths({ ...valid, type: 'SOB_ENCOMENDA' })).toContain('productionDays')
    expect(errorPaths({ ...valid, type: 'SOB_ENCOMENDA', productionDays: '7' })).toEqual([])
  })

  it('rejects invalid price, negative stock and lowercase SKU', () => {
    const paths = errorPaths({ ...valid, price: '0', stock: '-1', sku: 'gv-1' })
    expect(paths).toEqual(expect.arrayContaining(['price', 'stock', 'sku']))
  })
})
