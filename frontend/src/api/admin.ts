import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiDownload, apiGet, apiSend } from '../lib/api'
import type { OrderDetail, OrderStatus, Page, PaymentMethod, ProductType, ShippingMethod } from './types'

/** Tipos espelhando os DTOs administrativos (backend/…/admin, crm, catalog/api, order/api). */

function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const p = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.set(k, String(v))
  })
  const s = p.toString()
  return s ? `?${s}` : ''
}

// ------------------------------------------------------------------ dashboard

export type Kpis = {
  grossSales: number
  netSales: number
  cancelledAfterPayment: number
  shipping: number
  paidOrders: number
  averageTicket: number
  ordersPlaced: number
}

export type StatusCount = { status: OrderStatus; count: number }

export type Dashboard = {
  from: string
  to: string
  timezone: string
  kpis: Kpis
  previousFrom: string
  previousTo: string
  previous: Kpis
  sales: Array<{ day: string; grossSales: number; netSales: number }>
  ordersByStatus: StatusCount[]
  topProducts: Array<{ productId: number | null; name: string; sku: string; quantity: number; revenue: number }>
  customers: { buyers: number; newBuyers: number; returningBuyers: number; signups: number }
  lowStock: Array<{
    productId: number
    name: string
    sku: string
    onHand: number
    reserved: number
    available: number
    threshold: number
  }>
  lowStockTotal: number
  awaitingAction: StatusCount[]
  oldestAwaiting: Array<{ number: number; placedAt: string; status: OrderStatus; customerName: string; total: number }>
}

export function useDashboard(from: string, to: string) {
  return useQuery({
    queryKey: ['admin', 'dashboard', from, to],
    queryFn: () => apiGet<Dashboard>(`/api/v1/admin/dashboard${qs({ from, to })}`),
    placeholderData: keepPreviousData,
  })
}

// ------------------------------------------------------------------ catálogo

export type ProductStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'

export type AdminCategory = {
  id: number
  slug: string
  name: string
  description: string | null
  parentId: number | null
  sortOrder: number
  active: boolean
}

export type AdminProductListItem = {
  id: number
  slug: string
  sku: string
  name: string
  categoryName: string
  type: ProductType
  status: ProductStatus
  price: number
  promoPrice: number | null
  stockOnHand: number
  lowStock: boolean
  imageUrl: string | null
  updatedAt: string
}

export type AdminImage = {
  id: number
  url: string
  alt: string
  contentType: string
  sizeBytes: number
  width: number | null
  height: number | null
  sortOrder: number
}

export type AdminProduct = {
  id: number
  slug: string
  sku: string
  name: string
  description: string
  categoryId: number
  categorySlug: string
  type: ProductType
  status: ProductStatus
  price: number
  promoPrice: number | null
  currency: 'BRL'
  productionDays: number | null
  weightGrams: number | null
  lengthMm: number | null
  widthMm: number | null
  heightMm: number | null
  featured: boolean
  seoTitle: string | null
  seoDescription: string | null
  stock: { onHand: number; reserved: number; available: number; lowStockThreshold: number; low: boolean } | null
  options: Array<{ name: string; values: Array<{ id: number; label: string; priceDelta: number }> }>
  images: AdminImage[]
  version: number
  createdAt: string
  updatedAt: string
}

export type ProductRequest = {
  name: string
  slug?: string
  sku: string
  description: string
  categoryId: number
  type: ProductType
  status: ProductStatus
  price: number
  promoPrice: number | null
  productionDays: number | null
  featured: boolean
  lowStockThreshold: number
  options: Array<{ name: string; values: Array<{ label: string; priceDelta: number }> }>
  version?: number
}

export type StockMovement = {
  id: number
  type: 'INITIAL' | 'PURCHASE' | 'ADJUSTMENT' | 'SALE' | 'RETURN' | 'CANCELLATION'
  quantityDelta: number
  quantityAfter: number
  reason: string | null
  orderId: number | null
  createdAt: string
}

export type AdminProductFilters = {
  q?: string
  categoryId?: number
  status?: ProductStatus
  type?: ProductType
  lowStock?: boolean
  page?: number
  size?: number
}

export function useAdminCategories() {
  return useQuery({
    queryKey: ['admin', 'categories'],
    queryFn: () => apiGet<AdminCategory[]>('/api/v1/admin/categories'),
  })
}

export function useAdminProducts(filters: AdminProductFilters) {
  return useQuery({
    queryKey: ['admin', 'products', filters],
    queryFn: () => apiGet<Page<AdminProductListItem>>(`/api/v1/admin/products${qs(filters)}`),
    placeholderData: keepPreviousData,
  })
}

export function useAdminProduct(id: string | undefined) {
  return useQuery({
    queryKey: ['admin', 'product', id],
    queryFn: () => apiGet<AdminProduct>(`/api/v1/admin/products/${id}`),
    enabled: Boolean(id),
    retry: false,
  })
}

export function useStockHistory(productId: number | null, page = 0) {
  return useQuery({
    queryKey: ['admin', 'stock', productId, page],
    queryFn: () =>
      apiGet<Page<StockMovement>>(`/api/v1/admin/products/${productId}/stock-movements?page=${page}&size=10`),
    enabled: productId !== null,
  })
}

/** Escritas do catálogo; invalidam as listas em cache. */
export function useCatalogActions() {
  const client = useQueryClient()
  const refresh = () => client.invalidateQueries({ queryKey: ['admin'] })
  return {
    createProduct: async (body: ProductRequest) => {
      const p = await apiSend<AdminProduct>('POST', '/api/v1/admin/products', body)
      await refresh()
      return p
    },
    updateProduct: async (id: number, body: ProductRequest) => {
      const p = await apiSend<AdminProduct>('PUT', `/api/v1/admin/products/${id}`, body)
      await refresh()
      return p
    },
    changeStatus: async (id: number, status: ProductStatus, version: number) => {
      const p = await apiSend<AdminProduct>('POST', `/api/v1/admin/products/${id}/status`, { status, version })
      await refresh()
      return p
    },
    uploadImage: async (id: number, file: File, altText: string) => {
      const form = new FormData()
      form.set('file', file)
      form.set('altText', altText)
      const p = await apiSend<AdminProduct>('POST', `/api/v1/admin/products/${id}/images`, form)
      await refresh()
      return p
    },
    deleteImage: async (id: number, imageId: number) => {
      await apiSend('DELETE', `/api/v1/admin/products/${id}/images/${imageId}`)
      await refresh()
    },
    adjustStock: async (id: number, type: 'PURCHASE' | 'ADJUSTMENT' | 'RETURN', quantity: number, reason: string) => {
      await apiSend('POST', `/api/v1/admin/products/${id}/stock-movements`, { type, quantity, reason })
      await refresh()
    },
    saveCategory: async (
      id: number | null,
      body: { name: string; slug?: string; description?: string; sortOrder?: number; active?: boolean },
    ) => {
      await apiSend(id ? 'PUT' : 'POST', id ? `/api/v1/admin/categories/${id}` : '/api/v1/admin/categories', body)
      await refresh()
    },
    deleteCategory: async (id: number) => {
      await apiSend('DELETE', `/api/v1/admin/categories/${id}`)
      await refresh()
    },
  }
}

// ------------------------------------------------------------------ pedidos

export type AdminOrderListItem = {
  number: number
  createdAt: string
  status: OrderStatus
  total: number
  customerName: string
  customerEmail: string
  paymentMethod: PaymentMethod
  shippingMethod: ShippingMethod
  itemCount: number
}

export type AdminOrder = {
  order: OrderDetail
  customerId: number
  customerName: string
  customerEmail: string
  paidAt: string | null
  cancellationReason: string | null
  history: Array<{
    from: OrderStatus | null
    to: OrderStatus
    at: string
    note: string | null
    actorId: number | null
    actorName: string | null
  }>
  allowedNext: OrderStatus[]
  canConfirmPayment: boolean
}

export type AdminOrderFilters = {
  status?: OrderStatus
  q?: string
  customerId?: number
  from?: string
  to?: string
  page?: number
  size?: number
}

export function useAdminOrders(filters: AdminOrderFilters) {
  return useQuery({
    queryKey: ['admin', 'orders', filters],
    queryFn: () => apiGet<Page<AdminOrderListItem>>(`/api/v1/admin/orders${qs(filters)}`),
    placeholderData: keepPreviousData,
  })
}

export function useAdminOrder(number: string) {
  return useQuery({
    queryKey: ['admin', 'order', number],
    queryFn: () => apiGet<AdminOrder>(`/api/v1/admin/orders/${encodeURIComponent(number)}`),
    retry: false,
  })
}

export function useOrderActions(number: number) {
  const client = useQueryClient()
  const refresh = () => client.invalidateQueries({ queryKey: ['admin'] })
  return {
    changeStatus: async (status: OrderStatus, reason?: string, trackingCode?: string) => {
      await apiSend('POST', `/api/v1/admin/orders/${number}/status`, { status, reason, trackingCode })
      await refresh()
    },
    confirmPayment: async (reference?: string, note?: string) => {
      await apiSend('POST', `/api/v1/admin/orders/${number}/confirm-payment`, { reference, note })
      await refresh()
    },
  }
}

export function exportOrders(filters: Omit<AdminOrderFilters, 'page' | 'size'>) {
  return apiDownload(`/api/v1/admin/orders/export${qs(filters)}`, 'pedidos.csv')
}

// ------------------------------------------------------------------ CRM

export type CustomerStatus = 'ACTIVE' | 'DISABLED' | 'ANONYMIZED'
export type Segment = 'NO_PURCHASE' | 'REPEAT' | 'LAPSED'
export type CustomerSort = 'LAST_PURCHASE' | 'TOTAL_SPENT' | 'CREATED_AT' | 'NAME'
export type Tag = { id: number; name: string }

export type CustomerListItem = {
  id: number
  name: string
  email: string
  status: CustomerStatus
  createdAt: string
  paidOrders: number
  totalSpent: number
  lastPurchaseAt: string | null
  tags: Tag[]
}

export type CustomerDetail = {
  id: number
  name: string
  email: string
  phone: string | null
  status: CustomerStatus
  createdAt: string
  lastLoginAt: string | null
  marketingOptIn: boolean
  orders: number
  paidOrders: number
  totalSpent: number
  averageTicket: number
  firstPurchaseAt: string | null
  lastPurchaseAt: string | null
  tags: Tag[]
}

export type Note = { id: number; body: string; authorId: number | null; authorName: string | null; createdAt: string }

export type CustomerFilters = {
  q?: string
  status?: CustomerStatus
  tagId?: number
  segment?: Segment
  sort?: CustomerSort
  page?: number
  size?: number
}

export function useCustomers(filters: CustomerFilters) {
  return useQuery({
    queryKey: ['admin', 'customers', filters],
    queryFn: () => apiGet<Page<CustomerListItem>>(`/api/v1/admin/customers${qs(filters)}`),
    placeholderData: keepPreviousData,
  })
}

export function useCustomer(id: string) {
  return useQuery({
    queryKey: ['admin', 'customer', id],
    queryFn: () => apiGet<CustomerDetail>(`/api/v1/admin/customers/${encodeURIComponent(id)}`),
    retry: false,
  })
}

export function useCustomerNotes(id: string, page = 0) {
  return useQuery({
    queryKey: ['admin', 'customer', id, 'notes', page],
    queryFn: () => apiGet<Page<Note>>(`/api/v1/admin/customers/${encodeURIComponent(id)}/notes?page=${page}&size=20`),
  })
}

export function useTags() {
  return useQuery({
    queryKey: ['admin', 'tags'],
    queryFn: () => apiGet<Array<Tag & { customers: number }>>('/api/v1/admin/customer-tags'),
  })
}

export function useCrmActions() {
  const client = useQueryClient()
  const refresh = () => client.invalidateQueries({ queryKey: ['admin'] })
  return {
    addNote: async (customerId: number, body: string) => {
      await apiSend('POST', `/api/v1/admin/customers/${customerId}/notes`, { body })
      await refresh()
    },
    deleteNote: async (customerId: number, noteId: number) => {
      await apiSend('DELETE', `/api/v1/admin/customers/${customerId}/notes/${noteId}`)
      await refresh()
    },
    setTags: async (customerId: number, tagIds: number[]) => {
      await apiSend('PUT', `/api/v1/admin/customers/${customerId}/tags`, { tagIds })
      await refresh()
    },
    createTag: async (name: string) => {
      const tag = await apiSend<Tag>('POST', '/api/v1/admin/customer-tags', { name })
      await refresh()
      return tag
    },
    deleteTag: async (id: number) => {
      await apiSend('DELETE', `/api/v1/admin/customer-tags/${id}`)
      await refresh()
    },
  }
}

export function exportCustomers(filters: Omit<CustomerFilters, 'page' | 'size' | 'sort'>) {
  return apiDownload(`/api/v1/admin/customers/export${qs(filters)}`, 'clientes.csv')
}

// ------------------------------------------------------------------ equipe, auditoria, configurações

export type StaffRole = 'OPERATOR' | 'ADMIN'

export type StaffUser = {
  id: number
  name: string
  email: string
  role: StaffRole
  status: CustomerStatus
  lastLoginAt: string | null
  createdAt: string
}

export function useStaff() {
  return useQuery({ queryKey: ['admin', 'staff'], queryFn: () => apiGet<StaffUser[]>('/api/v1/admin/users') })
}

export function useStaffActions() {
  const client = useQueryClient()
  const refresh = () => client.invalidateQueries({ queryKey: ['admin', 'staff'] })
  return {
    create: async (body: { email: string; fullName: string; role: StaffRole }) => {
      await apiSend('POST', '/api/v1/admin/users', body)
      await refresh()
    },
    invite: (id: number) => apiSend('POST', `/api/v1/admin/users/${id}/invite`),
    changeRole: async (id: number, role: StaffRole) => {
      await apiSend('PUT', `/api/v1/admin/users/${id}/role`, { role })
      await refresh()
    },
    setEnabled: async (id: number, enabled: boolean) => {
      await apiSend('POST', `/api/v1/admin/users/${id}/${enabled ? 'enable' : 'disable'}`)
      await refresh()
    },
  }
}

export type AuditEntry = {
  id: number
  createdAt: string
  actorId: number | null
  actorName: string | null
  actorEmail: string | null
  action: string
  entityType: string
  entityId: string | null
  details: Record<string, unknown> | null
}

export type AuditFilters = { action?: string; entityType?: string; from?: string; to?: string; page?: number }

export function useAudit(filters: AuditFilters) {
  return useQuery({
    queryKey: ['admin', 'audit', filters],
    queryFn: () => apiGet<Page<AuditEntry>>(`/api/v1/admin/audit${qs({ ...filters, size: 25 })}`),
    placeholderData: keepPreviousData,
  })
}

export type Settings = {
  paymentProvider: string
  paymentMethods: PaymentMethod[]
  pendingPaymentExpirationHours: number
  shippingOriginPostalCode: string
  pickupEnabled: boolean
  shippingZones: Array<{
    name: string
    fromPostalCode: string
    toPostalCode: string
    price: number
    minDays: number
    maxDays: number
  }>
  shippingDemoValues: boolean
  storageType: string
  timezone: string
}

export function useSettings() {
  return useQuery({ queryKey: ['admin', 'settings'], queryFn: () => apiGet<Settings>('/api/v1/admin/settings') })
}
