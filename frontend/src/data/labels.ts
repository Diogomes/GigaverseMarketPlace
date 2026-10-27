import type { CustomerStatus, OrderStatus, ProductStatus, ProductType } from './types'

export type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger' | 'accent'

export const ORDER_STATUS: Record<OrderStatus, { label: string; tone: Tone }> = {
  AGUARDANDO_PAGAMENTO: { label: 'Aguardando pagamento', tone: 'warning' },
  EM_PRODUCAO: { label: 'Em produção', tone: 'accent' },
  ENVIADO: { label: 'Enviado', tone: 'info' },
  ENTREGUE: { label: 'Entregue', tone: 'success' },
  CANCELADO: { label: 'Cancelado', tone: 'danger' },
}

/** Ordem fixa usada em gráficos e filtros (a cor segue o status, nunca a posição). */
export const ORDER_STATUS_ORDER: OrderStatus[] = [
  'AGUARDANDO_PAGAMENTO',
  'EM_PRODUCAO',
  'ENVIADO',
  'ENTREGUE',
  'CANCELADO',
]

export const PRODUCT_STATUS: Record<ProductStatus, { label: string; tone: Tone }> = {
  ATIVO: { label: 'Ativo', tone: 'success' },
  RASCUNHO: { label: 'Rascunho', tone: 'neutral' },
  ARQUIVADO: { label: 'Arquivado', tone: 'warning' },
}

export const PRODUCT_TYPE: Record<ProductType, string> = {
  PRONTO: 'Pronta entrega',
  SOB_ENCOMENDA: 'Sob encomenda',
  PERSONALIZADO: 'Personalizado',
}

export const CUSTOMER_STATUS: Record<CustomerStatus, { label: string; tone: Tone }> = {
  ATIVO: { label: 'Ativo', tone: 'success' },
  INATIVO: { label: 'Inativo', tone: 'neutral' },
}
