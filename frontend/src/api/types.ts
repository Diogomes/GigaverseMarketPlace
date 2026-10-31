/** Tipos espelhando os DTOs da API (backend/…/api/*Dtos.java). Dinheiro vem como número decimal (BRL). */

export type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number }

export type ProductType = 'READY' | 'MADE_TO_ORDER' | 'CUSTOM'
export type Availability = 'IN_STOCK' | 'OUT_OF_STOCK' | 'MADE_TO_ORDER' | 'ON_REQUEST'

export type ApiImage = { id: number; url: string; alt: string; width: number | null; height: number | null }

export type ApiCategory = { slug: string; name: string; description: string | null; parentSlug: string | null }

export type ProductSummary = {
  slug: string
  name: string
  categorySlug: string
  categoryName: string
  type: ProductType
  price: number
  promoPrice: number | null
  effectivePrice: number
  currency: 'BRL'
  availability: Availability
  productionDays: number | null
  featured: boolean
  image: ApiImage | null
}

export type ProductOption = {
  name: string
  values: Array<{ id: number; label: string; priceDelta: number }>
}

export type ProductDetail = Omit<ProductSummary, 'featured' | 'image'> & {
  description: string
  weightGrams: number | null
  lengthMm: number | null
  widthMm: number | null
  heightMm: number | null
  seoTitle: string | null
  seoDescription: string | null
  options: ProductOption[]
  images: ApiImage[]
}

export type CartItem = {
  id: number
  productSlug: string
  productName: string
  imageUrl: string | null
  options: Array<{ group: string; label: string; priceDelta: number }>
  unitPrice: number
  quantity: number
  lineTotal: number
  maxQuantity: number
  problem: string | null
}

export type Cart = { items: CartItem[]; itemCount: number; subtotal: number; currency: 'BRL'; hasProblems: boolean }

export type ShippingMethod = 'DELIVERY' | 'PICKUP'
export type PaymentMethod = 'PIX' | 'CARD'

export type ShippingQuote = {
  method: ShippingMethod
  label: string
  price: number
  minDays: number | null
  maxDays: number | null
  demoValues: boolean
}

export type OrderStatus =
  'PENDING_PAYMENT' | 'PAID' | 'IN_PRODUCTION' | 'READY_FOR_PICKUP' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

export type OrderSummary = {
  number: number
  createdAt: string
  status: OrderStatus
  total: number
  itemCount: number
  firstItemName: string
  firstItemSlug: string | null
}

export type OrderDetail = {
  number: number
  createdAt: string
  status: OrderStatus
  items: Array<{
    productSlug: string | null
    productName: string
    sku: string
    optionsSummary: string | null
    unitPrice: number
    quantity: number
    lineTotal: number
  }>
  subtotal: number
  shippingTotal: number
  discountTotal: number
  total: number
  currency: 'BRL'
  paymentMethod: PaymentMethod
  paymentStatus: 'PENDING' | 'AUTHORIZED' | 'PAID' | 'FAILED' | 'CANCELLED' | 'REFUNDED' | null
  paymentInstructions: string | null
  shippingMethod: ShippingMethod
  shippingAddress: {
    recipientName: string
    postalCode: string
    street: string
    number: string
    complement: string | null
    district: string
    city: string
    state: string
  } | null
  customerNote: string | null
  history: Array<{ status: OrderStatus; at: string; note: string | null }>
  canCancel: boolean
  productionDays: number | null
}

export type Address = {
  id: number
  label: string
  recipientName: string
  postalCode: string
  street: string
  number: string
  complement: string | null
  district: string
  city: string
  state: string
  isDefault: boolean
}
