import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { SessionUser } from '../../auth/AuthProvider'
import { jsonResponse, mockApi, renderApp } from '../../test/authTestUtils'
import { ProductFormPage } from './ProductFormPage'

const admin: SessionUser = { id: 1, email: 'admin@exemplo.com', fullName: 'Admin', role: 'ADMIN' }

const product = {
  id: 7,
  slug: 'vaso-teste',
  sku: 'GV-T-1',
  name: 'Vaso Teste',
  description: 'Descrição longa o bastante.',
  categoryId: 3,
  categorySlug: 'decoracao',
  type: 'READY',
  status: 'ACTIVE',
  price: 49.9,
  promoPrice: null,
  currency: 'BRL',
  productionDays: null,
  weightGrams: null,
  lengthMm: null,
  widthMm: null,
  heightMm: null,
  featured: false,
  seoTitle: null,
  seoDescription: null,
  stock: { onHand: 0, reserved: 0, available: 0, lowStockThreshold: 2, low: true },
  options: [],
  images: [],
  version: 0,
  createdAt: '2026-11-01T12:00:00Z',
  updatedAt: '2026-11-01T12:00:00Z',
}

afterEach(() => vi.restoreAllMocks())

describe('ProductFormPage (edição)', () => {
  function setup() {
    const fetchSpy = mockApi(admin, (url, init) => {
      if (url.endsWith('/api/v1/admin/products/7')) return jsonResponse(200, product)
      if (url.endsWith('/api/v1/admin/categories'))
        return jsonResponse(200, [{ id: 3, slug: 'decoracao', name: 'Decoração', active: true }])
      if (url.includes('/stock-movements') && init?.method === 'POST') return new Response(null, { status: 201 })
      if (url.includes('/stock-movements'))
        return jsonResponse(200, { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })
      return undefined
    })
    const view = renderApp(<p>outra</p>, {
      route: '/admin/produtos/7',
      extraRoutes: [['/admin/produtos/:id', <ProductFormPage key="rota" />]],
    })
    return { fetchSpy, ...view }
  }

  it('não aninha formulários (imagens/estoque têm form próprio) e mantém a categoria selecionada', async () => {
    const { container } = setup()
    expect(await screen.findByRole('heading', { name: 'Editar: Vaso Teste' })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByLabelText('Categoria')).toHaveValue('3'))
    expect(container.querySelectorAll('form form')).toHaveLength(0)
    // campos da coluna lateral continuam ligados ao formulário do produto
    expect(screen.getByLabelText('Preço (R$)')).toHaveAttribute('form', 'product-form')
    expect(screen.getByRole('button', { name: 'Salvar produto' })).toHaveAttribute('form', 'product-form')
  })

  it('lança estoque pelo painel sem enviar o formulário do produto', async () => {
    const { fetchSpy } = setup()
    const qty = await screen.findByLabelText('Quantidade')
    await userEvent.type(qty, '5')
    await userEvent.click(screen.getByRole('button', { name: 'Lançar' }))
    expect(await screen.findByText('Movimentação registrada.')).toBeInTheDocument()
    const posts = fetchSpy.mock.calls.filter(([, init]) => init?.method === 'POST' || init?.method === 'PUT')
    expect(posts.map(([u]) => String(u))).toEqual([expect.stringContaining('/api/v1/admin/products/7/stock-movements')])
  })
})
