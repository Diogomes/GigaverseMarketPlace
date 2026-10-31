import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Cart } from '../../api/types'
import { jsonResponse, mockApi, renderApp } from '../../test/authTestUtils'
import { CheckoutPage } from './CheckoutPage'

const user = { id: 7, email: 'ana@exemplo.com', fullName: 'Ana Teste', role: 'CUSTOMER' as const }
const cart: Cart = {
  items: [
    {
      id: 1,
      productSlug: 'raptor-articulado',
      productName: 'Raptor Articulado',
      imageUrl: null,
      options: [],
      unitPrice: 129.9,
      quantity: 2,
      lineTotal: 259.8,
      maxQuantity: 20,
      problem: null,
    },
  ],
  itemCount: 2,
  subtotal: 259.8,
  currency: 'BRL',
  hasProblems: false,
}

describe('CheckoutPage', () => {
  it('places the order with an idempotency key and the total shown to the customer', async () => {
    let checkoutInit: RequestInit | undefined
    mockApi(user, (url, init) => {
      if (url.endsWith('/api/v1/cart')) return jsonResponse(200, cart)
      if (url.endsWith('/account/addresses')) return jsonResponse(200, [])
      if (url.includes('/checkout/shipping-options'))
        return jsonResponse(200, {
          originPostalCode: '53625816',
          options: [{ method: 'PICKUP', label: 'Retirada', price: 0, minDays: null, maxDays: null, demoValues: false }],
        })
      if (url.endsWith('/api/v1/checkout') && init?.method === 'POST') {
        checkoutInit = init
        return jsonResponse(201, { number: 1042 })
      }
      return undefined
    })
    renderApp(<CheckoutPage />, {
      route: '/checkout',
      extraRoutes: [['/conta/pedidos/:n', <p key="ok">Pedido criado</p>]],
    })

    await userEvent.click(await screen.findByRole('button', { name: 'Confirmar pedido' }))
    expect(await screen.findByText('Pedido criado')).toBeInTheDocument()
    const headers = checkoutInit!.headers as Record<string, string>
    expect(headers['Idempotency-Key']).toMatch(/^[A-Za-z0-9-]{8,64}$/)
    expect(JSON.parse(String(checkoutInit!.body))).toEqual(
      expect.objectContaining({ shippingMethod: 'PICKUP', paymentMethod: 'PIX', expectedTotal: 259.8 }),
    )
  })

  it('explains when prices changed (409) and refreshes the cart', async () => {
    let cartReads = 0
    mockApi(user, (url, init) => {
      if (url.endsWith('/api/v1/cart')) {
        cartReads++
        return jsonResponse(200, cart)
      }
      if (url.endsWith('/account/addresses')) return jsonResponse(200, [])
      if (url.includes('/checkout/shipping-options'))
        return jsonResponse(200, {
          originPostalCode: '53625816',
          options: [{ method: 'PICKUP', label: 'Retirada', price: 0, minDays: null, maxDays: null, demoValues: false }],
        })
      if (url.endsWith('/api/v1/checkout') && init?.method === 'POST')
        return jsonResponse(409, {
          detail: 'Os preços, a disponibilidade ou o frete mudaram. Revise o pedido: o total agora é R$ 270,00.',
        })
      return undefined
    })
    renderApp(<CheckoutPage />, { route: '/checkout' })
    await userEvent.click(await screen.findByRole('button', { name: 'Confirmar pedido' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('R$ 270,00')
    await waitFor(() => expect(cartReads).toBeGreaterThanOrEqual(2))
  })

  it('never asks for card data', async () => {
    mockApi(user, (url) => {
      if (url.endsWith('/api/v1/cart')) return jsonResponse(200, cart)
      if (url.endsWith('/account/addresses')) return jsonResponse(200, [])
      if (url.includes('/checkout/shipping-options'))
        return jsonResponse(200, { originPostalCode: '53625816', options: [] })
      return undefined
    })
    renderApp(<CheckoutPage />, { route: '/checkout' })
    await screen.findByRole('button', { name: 'Confirmar pedido' })
    expect(screen.queryByLabelText(/número do cartão|cvv|validade/i)).not.toBeInTheDocument()
  })
})
