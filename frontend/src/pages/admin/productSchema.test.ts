import { describe, expect, it } from 'vitest'
import type { AdminProduct } from '../../api/admin'
import { productSchema, toForm, toRequest, type ProductFormInput } from './productSchema'

const valid: ProductFormInput = {
  name: 'Dragão Articulado',
  sku: 'GV-ART-001',
  categoryId: '3',
  description: 'Descrição suficientemente longa.',
  price: '189,90',
  promoPrice: '',
  type: 'READY',
  lowStockThreshold: '2',
  productionDays: '',
  status: 'ACTIVE',
  featured: false,
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
    expect(errorPaths({ ...valid, type: 'MADE_TO_ORDER' })).toContain('productionDays')
    expect(errorPaths({ ...valid, type: 'MADE_TO_ORDER', productionDays: '7' })).toEqual([])
  })

  it('rejects invalid price, negative threshold and lowercase SKU', () => {
    const paths = errorPaths({ ...valid, price: '0', lowStockThreshold: '-1', sku: 'gv-1' })
    expect(paths).toEqual(expect.arrayContaining(['price', 'lowStockThreshold', 'sku']))
  })
})

describe('toRequest', () => {
  it('converts BRL text to decimal reais, keeps options and the read version', () => {
    const existing = {
      slug: 'dragao',
      version: 4,
      options: [{ name: 'Tamanho', values: [{ id: 9, label: '20 cm', priceDelta: 60 }] }],
    } as unknown as AdminProduct
    const body = toRequest({ ...valid, price: '1.234,56', promoPrice: '999,90' }, existing)
    expect(body.price).toBe(1234.56)
    expect(body.promoPrice).toBe(999.9)
    expect(body.categoryId).toBe(3)
    expect(body.productionDays).toBeNull()
    expect(body.version).toBe(4)
    expect(body.options).toEqual([{ name: 'Tamanho', values: [{ label: '20 cm', priceDelta: 60 }] }])
  })

  it('round-trips an existing product through the form', () => {
    const p = {
      price: 349.9,
      promoPrice: null,
      stock: { lowStockThreshold: 3 },
      categoryId: 1,
    } as unknown as AdminProduct
    expect(toForm(p)).toMatchObject({ price: '349,90', promoPrice: '', lowStockThreshold: '3', categoryId: '1' })
  })
})
