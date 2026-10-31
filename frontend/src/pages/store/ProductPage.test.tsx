import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { ProductDetail } from '../../api/types'
import { jsonResponse, mockApi, renderApp } from '../../test/authTestUtils'
import { page } from '../../test/catalogFixtures'
import { ProductPage } from './ProductPage'

const detail: ProductDetail = {
  slug: 'dragao-mecanico-azul',
  name: 'Dragão Mecânico Azul',
  description: 'Escultura de dragão.',
  categorySlug: 'colecionaveis',
  categoryName: 'Colecionáveis',
  type: 'READY',
  price: 349.9,
  promoPrice: 319.9,
  effectivePrice: 319.9,
  currency: 'BRL',
  availability: 'IN_STOCK',
  productionDays: null,
  weightGrams: null,
  lengthMm: null,
  widthMm: null,
  heightMm: null,
  seoTitle: null,
  seoDescription: null,
  options: [
    {
      name: 'Tamanho',
      values: [
        { id: 11, label: '10 cm', priceDelta: 0 },
        { id: 12, label: '20 cm', priceDelta: 60 },
      ],
    },
  ],
  images: [],
}

function api() {
  return mockApi(null, (url) => {
    if (url.endsWith('/public/products/dragao-mecanico-azul')) return jsonResponse(200, detail)
    if (url.endsWith('/public/products/nao-existe')) return jsonResponse(404, { detail: 'Produto não encontrado.' })
    if (url.includes('/public/products')) return jsonResponse(200, page([]))
    return undefined
  })
}

describe('ProductPage', () => {
  it('updates the price with the chosen option and adds to the visitor cart', async () => {
    api()
    renderApp(<p>outra</p>, {
      route: '/produtos/dragao-mecanico-azul',
      extraRoutes: [['/produtos/:slug', <ProductPage key="p" />]],
    })
    expect(await screen.findByRole('heading', { level: 1, name: 'Dragão Mecânico Azul' })).toBeInTheDocument()
    expect(screen.getByText('R$ 319,90')).toBeInTheDocument()
    await userEvent.click(screen.getByText(/20 cm/))
    expect(screen.getByText('R$ 379,90')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /adicionar ao carrinho/i }))
    expect(await screen.findByText('Adicionado ao carrinho.')).toBeInTheDocument()
    const stored = JSON.parse(localStorage.getItem('gv3d-cart') ?? '[]')
    expect(stored).toEqual([
      expect.objectContaining({ productSlug: 'dragao-mecanico-azul', optionValueIds: [12], quantity: 1 }),
    ])
  })

  it('emits structured data with the real price and availability only', async () => {
    api()
    renderApp(<p>outra</p>, {
      route: '/produtos/dragao-mecanico-azul',
      extraRoutes: [['/produtos/:slug', <ProductPage key="p" />]],
    })
    await screen.findByRole('heading', { level: 1 })
    const ld = JSON.parse(document.querySelector('script[type="application/ld+json"]')!.textContent!)
    expect(ld.offers).toEqual(
      expect.objectContaining({ price: '319.90', priceCurrency: 'BRL', availability: 'https://schema.org/InStock' }),
    )
    expect(ld.aggregateRating).toBeUndefined()
  })

  it('shows not found for unknown products', async () => {
    api()
    renderApp(<p>outra</p>, {
      route: '/produtos/nao-existe',
      extraRoutes: [['/produtos/:slug', <ProductPage key="p" />]],
    })
    expect(await screen.findByText('Página não encontrada')).toBeInTheDocument()
  })
})
