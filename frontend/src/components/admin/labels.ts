import type { CustomerStatus, ProductStatus, Segment, StaffRole, StockMovement } from '../../api/admin'
import type { ProductType } from '../../api/types'
import type { Tone } from '../../data/labels'

/** Rótulos dos valores reais da API administrativa. */
export const PRODUCT_STATUS_UI: Record<ProductStatus, { label: string; tone: Tone }> = {
  ACTIVE: { label: 'Ativo', tone: 'success' },
  DRAFT: { label: 'Rascunho', tone: 'neutral' },
  ARCHIVED: { label: 'Arquivado', tone: 'warning' },
}

export const PRODUCT_TYPE_UI: Record<ProductType, string> = {
  READY: 'Pronta entrega',
  MADE_TO_ORDER: 'Sob encomenda',
  CUSTOM: 'Personalizado',
}

export const CUSTOMER_STATUS_UI: Record<CustomerStatus, { label: string; tone: Tone }> = {
  ACTIVE: { label: 'Ativo', tone: 'success' },
  DISABLED: { label: 'Desativado', tone: 'neutral' },
  ANONYMIZED: { label: 'Anonimizado', tone: 'neutral' },
}

export const SEGMENT_UI: Record<Segment, string> = {
  NO_PURCHASE: 'Sem compra',
  REPEAT: 'Recorrentes (2+ compras)',
  LAPSED: 'Sem comprar há 90+ dias',
}

export const ROLE_UI: Record<StaffRole, string> = { ADMIN: 'Administrador', OPERATOR: 'Operador' }

export const MOVEMENT_UI: Record<StockMovement['type'], string> = {
  INITIAL: 'Saldo inicial',
  PURCHASE: 'Entrada (compra/produção)',
  ADJUSTMENT: 'Ajuste',
  SALE: 'Venda',
  RETURN: 'Devolução',
  CANCELLATION: 'Pedido cancelado',
}
