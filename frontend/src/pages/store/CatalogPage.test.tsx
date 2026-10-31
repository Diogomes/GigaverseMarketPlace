import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { categories, page, summary } from '../../test/catalogFixtures'
import { jsonResponse, mockApi, renderApp } from '../../test/authTestUtils'
import { CatalogPage } from './CatalogPage'

const all = [
  summary(),
  summary({ slug: 'luminaria-lua-3d', name: 'Luminária Lua 3D', categorySlug: 'decoracao', categoryName: 'Decoração' }),
]

function api() {
  return mockApi(null, (url) => {
    if (url.includes('/public/categories')) return jsonResponse(200, categories)
    if (url.includes('/public/products')) {
      const params = new URL(url, 'http://x').searchParams
      const q = params.get('q')?.toLowerCase()
      const cat = params.get('category')
      const result = all.filter(
        (p) => (!q || p.name.toLowerCase().includes(q.slice(0, 5))) && (!cat || p.categorySlug === cat),
      )
      return jsonResponse(200, page(result))
    }
    return undefined
  })
}

describe('CatalogPage', () => {
  it('lists products from the API', async () => {
    api()
    renderApp(<CatalogPage />, { route: '/produtos' })
    expect(await screen.findByText('Raptor Articulado')).toBeInTheDocument()
    expect(screen.getByText('2 produtos encontrados')).toBeInTheDocument()
  })

  it('sends search and category to the API', async () => {
    const fetchSpy = api()
    renderApp(<CatalogPage />, { route: '/produtos?categoria=decoracao' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Decoração' })).toBeInTheDocument()
    await userEvent.type(screen.getByRole('searchbox'), 'luminária')
    await waitFor(() =>
      expect(
        fetchSpy.mock.calls.some(([u]) => String(u).includes('q=lumin') && String(u).includes('category=decoracao')),
      ).toBe(true),
    )
    expect(await screen.findByText('Luminária Lua 3D')).toBeInTheDocument()
  })

  it('shows an error state with retry when the API fails', async () => {
    mockApi(null, (url) =>
      url.includes('/public/products')
        ? jsonResponse(500, {})
        : url.includes('categories')
          ? jsonResponse(200, [])
          : undefined,
    )
    renderApp(<CatalogPage />, { route: '/produtos' })
    expect(await screen.findByText('Não foi possível carregar os produtos')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument()
  })
})
