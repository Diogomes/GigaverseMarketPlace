import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { SessionUser } from '../../auth/AuthProvider'
import { jsonResponse, mockApi, renderApp } from '../../test/authTestUtils'
import { CustomerDetailPage } from './CustomerDetailPage'
import { DashboardPage } from './DashboardPage'
import { OrderDetailPage } from './OrdersPages'
import { StaffPage } from './SystemPages'

const admin: SessionUser = { id: 1, email: 'admin@exemplo.com', fullName: 'Admin', role: 'ADMIN' }
const operator: SessionUser = { id: 2, email: 'op@exemplo.com', fullName: 'Operadora', role: 'OPERATOR' }

const kpis = (gross: number) => ({
  grossSales: gross,
  netSales: 360,
  cancelledAfterPayment: 80,
  shipping: 25,
  paidOrders: 4,
  averageTicket: 96.25,
  ordersPlaced: 6,
})

const dashboard = {
  from: '2026-10-02',
  to: '2026-10-31',
  timezone: 'America/Recife',
  kpis: kpis(465),
  previousFrom: '2026-09-02',
  previousTo: '2026-10-01',
  previous: kpis(0),
  sales: [{ day: '2026-10-31', grossSales: 465, netSales: 360 }],
  ordersByStatus: [
    { status: 'PENDING_PAYMENT', count: 1 },
    { status: 'PAID', count: 1 },
    { status: 'IN_PRODUCTION', count: 1 },
    { status: 'DELIVERED', count: 2 },
    { status: 'CANCELLED', count: 1 },
  ],
  topProducts: [{ productId: 7, name: 'Dragão Azul', sku: 'GV-1', quantity: 4, revenue: 160 }],
  customers: { buyers: 3, newBuyers: 2, returningBuyers: 1, signups: 1 },
  lowStock: [{ productId: 9, name: 'Quase Esgotado', sku: 'GV-9', onHand: 1, reserved: 0, available: 1, threshold: 2 }],
  lowStockTotal: 1,
  awaitingAction: [{ status: 'PENDING_PAYMENT', count: 1 }],
  oldestAwaiting: [
    { number: 1001, placedAt: '2026-10-30T12:00:00Z', status: 'PENDING_PAYMENT', customerName: 'Cliente A', total: 30 },
  ],
}

const order = (status: string, allowedNext: string[], canConfirmPayment: boolean) => ({
  order: {
    number: 1001,
    createdAt: '2026-10-30T12:00:00Z',
    status,
    items: [
      {
        productSlug: 'dragao',
        productName: 'Dragão Azul',
        sku: 'GV-1',
        optionsSummary: null,
        unitPrice: 30,
        quantity: 1,
        lineTotal: 30,
      },
    ],
    subtotal: 30,
    shippingTotal: 0,
    discountTotal: 0,
    total: 30,
    currency: 'BRL',
    paymentMethod: 'PIX',
    paymentStatus: 'PENDING',
    paymentInstructions: null,
    shippingMethod: 'PICKUP',
    shippingAddress: null,
    customerNote: null,
    history: [],
    canCancel: true,
    productionDays: null,
  },
  customerId: 5,
  customerName: 'Cliente A',
  customerEmail: 'a@exemplo.com',
  paidAt: null,
  cancellationReason: null,
  history: [
    { from: null, to: 'PENDING_PAYMENT', at: '2026-10-30T12:00:00Z', note: null, actorId: null, actorName: null },
  ],
  allowedNext,
  canConfirmPayment,
})

afterEach(() => vi.restoreAllMocks())

describe('DashboardPage', () => {
  it('shows server-computed KPIs, definitions and the no-baseline state', async () => {
    const fetchSpy = mockApi(admin, (url) =>
      url.includes('/api/v1/admin/dashboard') ? jsonResponse(200, dashboard) : undefined,
    )
    renderApp(<DashboardPage />)
    expect(await screen.findByText('R$ 465,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 360,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 96,25')).toBeInTheDocument()
    expect(screen.getByText(/R\$ 80,00 cancelados após pagamento/)).toBeInTheDocument()
    expect(screen.getAllByText('Sem base de comparação').length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: 'Definição: Vendas líquidas' })).toBeInTheDocument()
    expect(screen.getByText('Quase Esgotado')).toBeInTheDocument()
    // 7 status da API agrupados nas 5 etapas da legenda
    expect(screen.getByText('Pago / em produção').closest('li')).toHaveTextContent('2')
    // período enviado ao servidor como datas ISO
    expect(
      fetchSpy.mock.calls.some(([u]) => /dashboard\?from=\d{4}-\d{2}-\d{2}&to=\d{4}-\d{2}-\d{2}/.test(String(u))),
    ).toBe(true)
  })

  it('shows the API error instead of numbers', async () => {
    mockApi(admin, (url) =>
      url.includes('/admin/dashboard') ? jsonResponse(400, { detail: 'Período máximo de 366 dias.' }) : undefined,
    )
    renderApp(<DashboardPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Período máximo de 366 dias.')
  })
})

describe('OrderDetailPage', () => {
  it('offers only the transitions the server allows and payment confirmation only to ADMIN', async () => {
    mockApi(operator, (url) =>
      url.endsWith('/api/v1/admin/orders/1001')
        ? jsonResponse(200, order('PENDING_PAYMENT', ['CANCELLED'], true))
        : undefined,
    )
    renderApp(<p>outra</p>, {
      route: '/admin/pedidos/1001',
      extraRoutes: [['/admin/pedidos/:number', <OrderDetailPage key="rota" />]],
    })
    expect(await screen.findByText(/A confirmação de pagamento é feita por um administrador/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Confirmar pagamento/ })).not.toBeInTheDocument()
    const select = screen.getByRole('combobox', { name: 'Novo status' })
    expect(
      within(select)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['Selecione…', 'Cancelado'])
  })

  it('requires a reason to cancel and sends it to the API', async () => {
    const calls: Array<{ url: string; body: unknown }> = []
    mockApi(admin, (url, init) => {
      if (url.endsWith('/api/v1/admin/orders/1001/status') && init?.method === 'POST') {
        calls.push({ url, body: JSON.parse(String(init.body)) })
        return jsonResponse(200, order('CANCELLED', [], false).order)
      }
      if (url.endsWith('/api/v1/admin/orders/1001'))
        return jsonResponse(200, order('PENDING_PAYMENT', ['CANCELLED'], true))
      return undefined
    })
    renderApp(<p>outra</p>, {
      route: '/admin/pedidos/1001',
      extraRoutes: [['/admin/pedidos/:number', <OrderDetailPage key="rota" />]],
    })
    const user = userEvent.setup()
    await user.selectOptions(await screen.findByRole('combobox', { name: 'Novo status' }), 'CANCELLED')
    await user.click(screen.getByRole('button', { name: 'Aplicar' }))
    expect(await screen.findByText('Informe o motivo do cancelamento.')).toBeInTheDocument()
    expect(calls).toHaveLength(0)
    await user.type(screen.getByRole('textbox', { name: /Motivo/ }), 'Cliente desistiu')
    await user.click(screen.getByRole('button', { name: 'Aplicar' }))
    await waitFor(() => expect(calls).toHaveLength(1))
    expect(calls[0].body).toMatchObject({ status: 'CANCELLED', reason: 'Cliente desistiu' })
  })
})

describe('CustomerDetailPage', () => {
  it('posts internal notes and hides delete from operators', async () => {
    const posted: unknown[] = []
    const customer = {
      id: 5,
      name: 'Cliente A',
      email: 'a@exemplo.com',
      phone: null,
      status: 'ACTIVE',
      createdAt: '2026-01-10T12:00:00Z',
      lastLoginAt: null,
      marketingOptIn: false,
      orders: 1,
      paidOrders: 1,
      totalSpent: 30,
      averageTicket: 30,
      firstPurchaseAt: '2026-10-30T12:00:00Z',
      lastPurchaseAt: '2026-10-30T12:00:00Z',
      tags: [],
    }
    const page = (content: unknown[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })
    mockApi(operator, (url, init) => {
      if (url.includes('/customers/5/notes') && init?.method === 'POST') {
        posted.push(JSON.parse(String(init.body)))
        return jsonResponse(201, {
          id: 1,
          body: 'x',
          authorId: 2,
          authorName: 'Operadora',
          createdAt: '2026-10-31T12:00:00Z',
        })
      }
      if (url.includes('/customers/5/notes'))
        return jsonResponse(
          200,
          page([
            {
              id: 1,
              body: 'Prefere retirar no sábado',
              authorId: 2,
              authorName: 'Operadora',
              createdAt: '2026-10-31T12:00:00Z',
            },
          ]),
        )
      if (url.endsWith('/api/v1/admin/customers/5')) return jsonResponse(200, customer)
      if (url.includes('/api/v1/admin/orders')) return jsonResponse(200, page([]))
      if (url.includes('/customer-tags')) return jsonResponse(200, [])
      return undefined
    })
    renderApp(<p>outra</p>, {
      route: '/admin/clientes/5',
      extraRoutes: [['/admin/clientes/:id', <CustomerDetailPage key="rota" />]],
    })
    expect(await screen.findByText('Prefere retirar no sábado')).toBeInTheDocument()
    expect(screen.getByText(/Não aceitou — não enviar ofertas/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Remover/ })).not.toBeInTheDocument()
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Nova nota interna'), 'Ligar na segunda')
    await user.click(screen.getByRole('button', { name: 'Adicionar nota' }))
    await waitFor(() => expect(posted).toEqual([{ body: 'Ligar na segunda' }]))
  })
})

describe('StaffPage', () => {
  it('is closed to operators (the server also returns 403)', async () => {
    const fetchSpy = mockApi(operator)
    renderApp(<StaffPage />)
    expect(await screen.findByText('Área restrita a administradores')).toBeInTheDocument()
    expect(fetchSpy.mock.calls.some(([u]) => String(u).includes('/api/v1/admin/users'))).toBe(false)
  })
})
