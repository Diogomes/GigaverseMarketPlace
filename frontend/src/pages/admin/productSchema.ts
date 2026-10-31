import { z } from 'zod'
import type { AdminProduct, ProductRequest } from '../../api/admin'
import { centsToInput, parseBRLToCents } from '../../lib/format'

export const MAX_IMAGE_BYTES = 5 * 1024 * 1024
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

const money = (label: string) =>
  z
    .string()
    .trim()
    .refine((v) => (parseBRLToCents(v) ?? 0) > 0, { error: `Informe ${label} válido (ex.: 189,90).` })

// Validação de UX; o backend aplica as mesmas regras (CatalogDtos.ProductRequest) e é a fonte da verdade.
export const productSchema = z
  .object({
    name: z.string().trim().min(3, { error: 'Nome deve ter ao menos 3 caracteres.' }).max(120),
    sku: z
      .string()
      .trim()
      .regex(/^[A-Z0-9-]{3,32}$/, { error: 'SKU: 3 a 32 caracteres, letras maiúsculas, números e hífen.' }),
    categoryId: z.string().min(1, { error: 'Selecione uma categoria.' }),
    description: z.string().trim().min(10, { error: 'Descreva o produto (mín. 10 caracteres).' }).max(4000),
    price: money('um preço'),
    promoPrice: z.string().trim(),
    type: z.enum(['READY', 'MADE_TO_ORDER', 'CUSTOM']),
    lowStockThreshold: z
      .string()
      .trim()
      .regex(/^\d{1,6}$/, { error: 'Informe um número inteiro maior ou igual a zero.' }),
    productionDays: z.string().trim(),
    status: z.enum(['DRAFT', 'ACTIVE', 'ARCHIVED']),
    featured: z.boolean(),
  })
  .superRefine((d, ctx) => {
    if (d.promoPrice) {
      const promo = parseBRLToCents(d.promoPrice)
      const price = parseBRLToCents(d.price)
      if (promo === null || promo <= 0) {
        ctx.addIssue({ code: 'custom', path: ['promoPrice'], message: 'Preço promocional inválido.' })
      } else if (price !== null && promo >= price) {
        ctx.addIssue({ code: 'custom', path: ['promoPrice'], message: 'Deve ser menor que o preço normal.' })
      }
    }
    if (d.type !== 'READY') {
      const days = Number(d.productionDays)
      if (!d.productionDays || !Number.isInteger(days) || days < 1 || days > 120) {
        ctx.addIssue({
          code: 'custom',
          path: ['productionDays'],
          message: 'Informe o prazo de produção (1 a 120 dias úteis).',
        })
      }
    }
  })

export type ProductFormInput = z.input<typeof productSchema>

export function toForm(p?: AdminProduct): ProductFormInput {
  return {
    name: p?.name ?? '',
    sku: p?.sku ?? '',
    categoryId: p ? String(p.categoryId) : '',
    description: p?.description ?? '',
    price: p ? centsToInput(Math.round(p.price * 100)) : '',
    promoPrice: p?.promoPrice ? centsToInput(Math.round(p.promoPrice * 100)) : '',
    type: p?.type ?? 'READY',
    lowStockThreshold: String(p?.stock?.lowStockThreshold ?? 2),
    productionDays: p?.productionDays ? String(p.productionDays) : '',
    status: p?.status ?? 'DRAFT',
    featured: p?.featured ?? false,
  }
}

/** Monta o corpo da API. Opções existentes são preservadas (o formulário não as edita). */
export function toRequest(f: ProductFormInput, existing?: AdminProduct): ProductRequest {
  const cents = (v: string) => (parseBRLToCents(v) ?? 0) / 100
  return {
    name: f.name.trim(),
    slug: existing?.slug,
    sku: f.sku.trim(),
    description: f.description.trim(),
    categoryId: Number(f.categoryId),
    type: f.type,
    status: f.status,
    price: cents(f.price),
    promoPrice: f.promoPrice ? cents(f.promoPrice) : null,
    productionDays: f.type === 'READY' ? null : Number(f.productionDays),
    featured: f.featured,
    lowStockThreshold: Number(f.lowStockThreshold),
    options: (existing?.options ?? []).map((o) => ({
      name: o.name,
      values: o.values.map((v) => ({ label: v.label, priceDelta: v.priceDelta })),
    })),
    version: existing?.version,
  }
}
