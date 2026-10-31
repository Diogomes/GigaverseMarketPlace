import { screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { Cart } from '../api/types'
import { jsonResponse, mockApi, renderApp } from '../test/authTestUtils'
import { useCart } from './CartProvider'

const user = { id: 1, email: 'ana@exemplo.com', fullName: 'Ana', role: 'CUSTOMER' as const }
const merged: Cart = { items: [], itemCount: 1, subtotal: 89.9, currency: 'BRL', hasProblems: false }

function Count() {
  const { count } = useCart()
  return <p>itens: {count}</p>
}

describe('CartProvider', () => {
  it('merges the visitor cart once after login, clearing the browser copy before sending', async () => {
    localStorage.setItem(
      'gv3d-cart',
      JSON.stringify([
        {
          productSlug: 'miniatura-para-pintar',
          optionValueIds: [],
          quantity: 1,
          name: 'Miniatura',
          unitPrice: 89.9,
          optionsLabel: null,
        },
      ]),
    )
    let storedWhenSent: string | null = 'não enviado'
    const fetchSpy = mockApi(user, (url, init) => {
      if (url.endsWith('/cart/merge') && init?.method === 'POST') {
        storedWhenSent = localStorage.getItem('gv3d-cart')
        return jsonResponse(200, merged)
      }
      if (url.endsWith('/api/v1/cart')) return jsonResponse(200, merged)
      return undefined
    })
    renderApp(<Count />)
    expect(await screen.findByText('itens: 1')).toBeInTheDocument()
    await waitFor(() => expect(fetchSpy.mock.calls.filter(([u]) => String(u).endsWith('/cart/merge'))).toHaveLength(1))
    expect(storedWhenSent).toBeNull()
    expect(localStorage.getItem('gv3d-cart')).toBeNull()
  })
})
