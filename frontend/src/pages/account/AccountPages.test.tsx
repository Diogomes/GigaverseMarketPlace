import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import type { OrderDetail, OrderSummary } from '../../api/types'
import { RequireAuth } from '../../components/PreviewGuard'
import { jsonResponse, mockApi, renderApp } from '../../test/authTestUtils'
import { page } from '../../test/catalogFixtures'
import { LoginPage } from '../auth/LoginPage'
import { AccountAddressesPage, AccountOrderDetailPage, AccountOrdersPage } from './AccountPages'

const user = { id: 3, email: 'ana@exemplo.com', fullName: 'Ana Teste', role: 'CUSTOMER' as const }

const summary: OrderSummary = {
  number: 1042,
  createdAt: '2026-10-27T15:00:00Z',
  status: 'PENDING_PAYMENT',
  total: 259.8,
  itemCount: 2,
  firstItemName: 'Raptor Articulado',
  firstItemSlug: 'raptor-articulado',
}

const detail: OrderDetail = {
  number: 1042,
  createdAt: '2026-10-27T15:00:00Z',
  status: 'PENDING_PAYMENT',
  items: [
    {
      productSlug: 'raptor-articulado',
      productName: 'Raptor Articulado',
      sku: 'GV-ART-002',
      optionsSummary: 'Tamanho: 10 cm',
      unitPrice: 129.9,
      quantity: 2,
      lineTotal: 259.8,
    },
  ],
  subtotal: 259.8,
  shippingTotal: 0,
  discountTotal: 0,
  total: 259.8,
  currency: 'BRL',
  paymentMethod: 'PIX',
  paymentStatus: 'PENDING',
  paymentInstructions: 'Enviaremos a chave PIX pelo e-mail cadastrado.',
  shippingMethod: 'PICKUP',
  shippingAddress: null,
  customerNote: null,
  history: [{ status: 'PENDING_PAYMENT', at: '2026-10-27T15:00:00Z', note: null }],
  canCancel: true,
  productionDays: null,
}

beforeEach(() => sessionStorage.clear())

describe('Meus pedidos', () => {
  it('lists real orders and filters by tab through the API', async () => {
    const fetchSpy = mockApi(user, (url) =>
      url.includes('/account/orders') ? jsonResponse(200, page([summary])) : undefined,
    )
    renderApp(<AccountOrdersPage />, { route: '/conta/pedidos' })
    expect(await screen.findByText('Pedido #1042')).toBeInTheDocument()
    expect(screen.getByText('Aguardando pagamento')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('tab', { name: 'Cancelados' }))
    await waitFor(() => expect(fetchSpy.mock.calls.some(([u]) => String(u).includes('group=CANCELLED'))).toBe(true))
  })

  it('shows payment instructions and lets the customer cancel while pending', async () => {
    const fetchSpy = mockApi(user, (url, init) => {
      if (url.endsWith('/account/orders/1042/cancel') && init?.method === 'POST')
        return jsonResponse(200, { ...detail, status: 'CANCELLED', canCancel: false, paymentInstructions: null })
      if (url.endsWith('/account/orders/1042')) return jsonResponse(200, detail)
      return undefined
    })
    renderApp(<p />, {
      route: '/conta/pedidos/1042?novo=1',
      extraRoutes: [['/conta/pedidos/:number', <AccountOrderDetailPage key="d" />]],
    })
    expect(await screen.findByText(/Pedido recebido/)).toBeInTheDocument()
    expect(screen.getByText(/chave PIX/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar pedido' }))
    const dialogConfirm = await screen.findAllByRole('button', { name: 'Cancelar pedido' })
    await userEvent.click(dialogConfirm[dialogConfirm.length - 1])
    expect(await screen.findByText('Pedido cancelado.')).toBeInTheDocument()
    expect(
      fetchSpy.mock.calls.some(([u, i]) => String(u).endsWith('/cancel') && (i as RequestInit)?.method === 'POST'),
    ).toBe(true)
  })

  it("treats another customer's order as not found", async () => {
    mockApi(user, (url) =>
      url.endsWith('/account/orders/9999') ? jsonResponse(404, { detail: 'Pedido não encontrado.' }) : undefined,
    )
    renderApp(<p />, {
      route: '/conta/pedidos/9999',
      extraRoutes: [['/conta/pedidos/:number', <AccountOrderDetailPage key="d" />]],
    })
    expect(await screen.findByText('Página não encontrada')).toBeInTheDocument()
  })
})

describe('Endereços', () => {
  it('creates an address through the API', async () => {
    let created: unknown
    mockApi(user, (url, init) => {
      if (url.endsWith('/account/addresses') && init?.method === 'POST') {
        created = JSON.parse(String(init.body))
        return jsonResponse(201, { id: 1, ...(created as object), postalCode: '53625816', isDefault: true })
      }
      if (url.endsWith('/account/addresses')) return jsonResponse(200, [])
      return undefined
    })
    renderApp(<AccountAddressesPage />, { route: '/conta/enderecos' })
    await userEvent.click(await screen.findByRole('button', { name: /adicionar endereço/i }))
    await userEvent.type(screen.getByLabelText('Destinatário'), 'Ana Teste')
    await userEvent.type(screen.getByLabelText('CEP'), '53625-816')
    await userEvent.type(screen.getByLabelText('Rua'), 'Rua Exemplo')
    await userEvent.type(screen.getByLabelText('Número'), '10')
    await userEvent.type(screen.getByLabelText('Bairro'), 'Centro')
    await userEvent.type(screen.getByLabelText('Cidade'), 'Cidade Exemplo')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText('Endereço salvo.')).toBeInTheDocument()
    expect(created).toEqual(
      expect.objectContaining({ postalCode: '53625-816', state: 'PE', recipientName: 'Ana Teste' }),
    )
  })
})

describe('Sessão expirada', () => {
  it('sends the customer to login with a clear message when the API answers 401', async () => {
    let meCalls = 0
    mockApi(null, (url) => {
      if (url.endsWith('/auth/me')) {
        meCalls++
        return meCalls === 1 ? jsonResponse(200, user) : new Response(null, { status: 204 })
      }
      if (url.includes('/account/orders')) return jsonResponse(401, {})
      return undefined
    })
    renderApp(
      <RequireAuth>
        <AccountOrdersPage />
      </RequireAuth>,
      { route: '/conta/pedidos', extraRoutes: [['/entrar', <LoginPage key="l" />]] },
    )
    expect(await screen.findByText(/Sua sessão expirou/)).toBeInTheDocument()
  })
})
