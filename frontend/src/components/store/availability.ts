import type { Availability } from '../../api/types'

export const AVAILABILITY: Record<Availability, { label: string; tone: 'success' | 'warning' | 'accent' | 'neutral' }> =
  {
    IN_STOCK: { label: 'Em estoque', tone: 'success' },
    OUT_OF_STOCK: { label: 'Esgotado', tone: 'neutral' },
    MADE_TO_ORDER: { label: 'Sob encomenda', tone: 'warning' },
    ON_REQUEST: { label: 'Sob orçamento', tone: 'accent' },
  }
