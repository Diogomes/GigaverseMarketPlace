export type ProductType = 'PRONTO' | 'SOB_ENCOMENDA' | 'PERSONALIZADO'
export type ProductStatus = 'ATIVO' | 'RASCUNHO' | 'ARQUIVADO'

export type ProductOptionGroup = {
  name: string
  values: Array<{ label: string; extraCents?: number }>
}

/** Conjunto de imagens de demonstração; `base` resolve os recortes em /images (ver lib/images.ts). */
export type ImageSet = { base: string; alt: string }

export type Category = { slug: string; name: string; description: string; image?: string }

export type Product = {
  id: string
  slug: string
  sku: string
  name: string
  categorySlug: string
  description: string
  priceCents: number
  promoPriceCents?: number
  stock: number
  lowStockThreshold: number
  status: ProductStatus
  type: ProductType
  productionDays?: number
  featured: boolean
  options: ProductOptionGroup[]
  image?: ImageSet
}

export type OrderStatus = 'AGUARDANDO_PAGAMENTO' | 'EM_PRODUCAO' | 'ENVIADO' | 'ENTREGUE' | 'CANCELADO'

export type OrderItem = { productSlug: string; name: string; variant?: string; quantity: number; unitCents: number }

export type Order = {
  number: string
  customerId: string
  createdAt: string
  status: OrderStatus
  items: OrderItem[]
  shippingCents: number
  totalCents: number
  history: Array<{ status: OrderStatus; at: string }>
}

export type CustomerStatus = 'ATIVO' | 'INATIVO'

export type Customer = {
  id: string
  name: string
  email: string
  createdAt: string
  status: CustomerStatus
  tags: string[]
  notes: Array<{ id: string; author: string; at: string; text: string }>
}

export type Address = {
  id: string
  label: string
  recipient: string
  line: string
  district: string
  city: string
  state: string
  postalCode: string
  isDefault: boolean
}
