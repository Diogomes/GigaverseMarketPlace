import type { OrderStatus } from '../../api/types'
import type { Tone } from '../../data/labels'

/** Rótulos e cores dos status reais de pedido (API). */
export const ORDER_STATUS_UI: Record<OrderStatus, { label: string; tone: Tone }> = {
  PENDING_PAYMENT: { label: 'Aguardando pagamento', tone: 'warning' },
  PAID: { label: 'Pago', tone: 'info' },
  IN_PRODUCTION: { label: 'Em produção', tone: 'accent' },
  READY_FOR_PICKUP: { label: 'Pronto para retirada', tone: 'info' },
  SHIPPED: { label: 'Enviado', tone: 'info' },
  DELIVERED: { label: 'Entregue', tone: 'success' },
  CANCELLED: { label: 'Cancelado', tone: 'danger' },
}
