import { z } from 'zod'
import { parseBRLToCents } from '../../lib/format'

export const MAX_IMAGE_BYTES = 5 * 1024 * 1024
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

const money = (label: string) =>
  z
    .string()
    .trim()
    .refine((v) => (parseBRLToCents(v) ?? 0) > 0, { error: `Informe ${label} válido (ex.: 189,90).` })

// Validação de UX; o backend aplicará as mesmas regras (Fase 04) e é a fonte da verdade.
export const productSchema = z
  .object({
    name: z.string().trim().min(3, { error: 'Nome deve ter ao menos 3 caracteres.' }).max(120),
    sku: z
      .string()
      .trim()
      .regex(/^[A-Z0-9-]{3,32}$/, { error: 'SKU: 3 a 32 caracteres, letras maiúsculas, números e hífen.' }),
    categorySlug: z.string().min(1, { error: 'Selecione uma categoria.' }),
    description: z.string().trim().min(10, { error: 'Descreva o produto (mín. 10 caracteres).' }).max(4000),
    price: money('um preço'),
    promoPrice: z.string().trim(),
    type: z.enum(['PRONTO', 'SOB_ENCOMENDA', 'PERSONALIZADO']),
    stock: z
      .string()
      .trim()
      .regex(/^\d{1,6}$/, { error: 'Informe um número inteiro maior ou igual a zero.' }),
    productionDays: z.string().trim(),
    status: z.enum(['ATIVO', 'RASCUNHO', 'ARQUIVADO']),
    images: z
      .custom<FileList | undefined>()
      .refine((files) => !files || files.length <= 8, { error: 'Envie no máximo 8 imagens.' })
      .refine((files) => !files || [...files].every((f) => ALLOWED_IMAGE_TYPES.includes(f.type)), {
        error: 'Formatos aceitos: JPG, PNG ou WebP.',
      })
      .refine((files) => !files || [...files].every((f) => f.size <= MAX_IMAGE_BYTES), {
        error: 'Cada imagem deve ter no máximo 5 MB.',
      }),
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
    if (d.type !== 'PRONTO') {
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
