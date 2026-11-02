import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Cart } from '../../api/types'
import { jsonResponse, mockApi, renderApp } from '../../test/authTestUtils'
import { CartPage } from './CartPage'

const customer = { id: 7, email: 'cliente@exemplo.com', fullName: 'Cliente Fictício', role: 'CUSTOMER' as const }

const line = (over: Partial<Cart['items'][number]> = {}): Cart['items'][number] => ({
  id: 1,
  productSlug: 'vaso-ficticio',
  productName: 'Vaso Fictício',
  imageUrl: null,
  options: [],
  unitPrice: 40,
  quantity: 1,
  lineTotal: 40,
  maxQuantity: 10,
  problem: null,
  ...over,
})

const cartOf = (items: Cart['items']): Cart => ({
  items,
  itemCount: items.reduce((n, i) => n + i.quantity, 0),
  subtotal: items.reduce((s, i) => s + i.lineTotal, 0),
  currency: 'BRL',
  hasProblems: items.some((i) => i.problem),
})

afterEach(() => {
  vi.restoreAllMocks()
  localStorage.clear()
})

describe('CartPage (cliente logado)', () => {
  it('mostra itens, opções e o subtotal calculado pelo servidor', async () => {
    mockApi(customer, (url) =>
      url.endsWith('/api/v1/cart')
        ? jsonResponse(
            200,
            cartOf([
              line(),
              line({
                id: 2,
                productSlug: 'dragao',
                productName: 'Dragão Fictício',
                options: [{ group: 'Tamanho', label: 'G', priceDelta: 40 }],
                unitPrice: 220,
                quantity: 2,
                lineTotal: 440,
              }),
            ]),
          )
        : undefined,
    )
    renderApp(<CartPage />, { route: '/carrinho' })
    expect(await screen.findByRole('link', { name: 'Dragão Fictício' })).toBeInTheDocument()
    expect(screen.getByText('Tamanho: G')).toBeInTheDocument()
    expect(screen.getByText('R$ 440,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 480,00')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Finalizar compra' })).toHaveAttribute('href', '/checkout')
  })

  it('altera a quantidade e remove itens pela API e reflete o novo total', async () => {
    const calls: Array<{ method: string; url: string; body?: string }> = []
    let current = cartOf([line({ quantity: 1, lineTotal: 40 })])
    mockApi(customer, (url, init) => {
      if (!url.includes('/api/v1/cart')) return undefined
      const method = init?.method ?? 'GET'
      if (method !== 'GET') calls.push({ method, url, body: init?.body as string | undefined })
      if (method === 'PUT') current = cartOf([line({ quantity: 3, lineTotal: 120 })])
      if (method === 'DELETE') current = cartOf([])
      return jsonResponse(200, current)
    })
    renderApp(<CartPage />, { route: '/carrinho' })

    await userEvent.selectOptions(await screen.findByLabelText('Quantidade de Vaso Fictício'), '3')
    expect(await screen.findAllByText('R$ 120,00', undefined, { timeout: 3000 })).toHaveLength(2) // linha + subtotal
    expect(calls[0]).toEqual(expect.objectContaining({ method: 'PUT', url: expect.stringContaining('/cart/items/1') }))
    expect(JSON.parse(calls[0].body!)).toEqual({ quantity: 3 })

    await userEvent.click(screen.getByRole('button', { name: 'Remover Vaso Fictício' }))
    await waitFor(() => expect(screen.queryByRole('link', { name: 'Vaso Fictício' })).not.toBeInTheDocument())
    expect(calls[1]).toEqual(expect.objectContaining({ method: 'DELETE' }))
  })

  it('mostra o problema do item (ex.: sem estoque) que o servidor apontou', async () => {
    mockApi(customer, (url) =>
      url.endsWith('/api/v1/cart')
        ? jsonResponse(200, cartOf([line({ problem: 'Quantidade indisponível (máximo 1).', quantity: 2 })]))
        : undefined,
    )
    renderApp(<CartPage />, { route: '/carrinho' })
    expect(await screen.findByText('Quantidade indisponível (máximo 1).')).toBeInTheDocument()
  })

  it('carrinho vazio convida a ver produtos', async () => {
    mockApi(customer, (url) => (url.endsWith('/api/v1/cart') ? jsonResponse(200, cartOf([])) : undefined))
    renderApp(<CartPage />, { route: '/carrinho' })
    expect(await screen.findByText(/carrinho está vazio|nenhum item/i)).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Finalizar compra' })).not.toBeInTheDocument()
  })
})

describe('CartPage (visitante)', () => {
  it('lê o carrinho do navegador, permite remover e pede login para finalizar', async () => {
    localStorage.setItem(
      'gv3d-cart',
      JSON.stringify([
        {
          productSlug: 'vaso-ficticio',
          name: 'Vaso Fictício',
          unitPrice: 40,
          quantity: 2,
          optionValueIds: [],
          optionsLabel: null,
        },
      ]),
    )
    mockApi(null)
    renderApp(<CartPage />, { route: '/carrinho' })
    expect(await screen.findByRole('link', { name: 'Entrar para finalizar' })).toBeInTheDocument()
    const name = await screen.findAllByText('Vaso Fictício')
    expect(name.length).toBeGreaterThan(0)
    await userEvent.click(screen.getByRole('button', { name: 'Remover Vaso Fictício' }))
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Remover Vaso Fictício' })).not.toBeInTheDocument())
  })
})
