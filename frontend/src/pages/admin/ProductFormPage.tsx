import { zodResolver } from '@hookform/resolvers/zod'
import { ImagePlus } from 'lucide-react'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useParams } from 'react-router'
import { buttonClass } from '../../components/ui/button'
import { DemoBanner } from '../../components/ui/DemoBanner'
import { InputField, SelectField, TextareaField } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { PRODUCT_STATUS, PRODUCT_TYPE } from '../../data/labels'
import { getAdminProduct, listCategories } from '../../data/store'
import type { Product } from '../../data/types'
import { centsToInput, slugify } from '../../lib/format'
import { NotFoundPage } from '../PlaceholderPage'
import { productSchema, type ProductFormInput } from './productSchema'

function toForm(p?: Product): ProductFormInput {
  return {
    name: p?.name ?? '',
    sku: p?.sku ?? '',
    categorySlug: p?.categorySlug ?? '',
    description: p?.description ?? '',
    price: p ? centsToInput(p.priceCents) : '',
    promoPrice: p?.promoPriceCents ? centsToInput(p.promoPriceCents) : '',
    type: p?.type ?? 'PRONTO',
    stock: String(p?.stock ?? 0),
    productionDays: p?.productionDays ? String(p.productionDays) : '',
    status: p?.status ?? 'RASCUNHO',
    images: undefined,
  }
}

export function ProductFormPage() {
  const { id } = useParams()
  const product = id ? getAdminProduct(id) : undefined
  if (id && !product) return <NotFoundPage />
  return <ProductForm key={id ?? 'novo'} product={product} />
}

function ProductForm({ product }: { product?: Product }) {
  const [saved, setSaved] = useState(false)
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<ProductFormInput>({ resolver: zodResolver(productSchema), defaultValues: toForm(product) })

  const [name, type, images] = useWatch({ control, name: ['name', 'type', 'images'] })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={product ? `Editar: ${product.name}` : 'Novo produto'}
        description="Campos marcados são validados aqui e novamente no servidor."
      />
      {product && <DemoBanner />}
      <form noValidate onSubmit={handleSubmit(() => setSaved(true))} className="grid gap-6 xl:grid-cols-[1fr_22rem]">
        <div className="flex flex-col gap-6">
          <Card>
            <h2 className="font-semibold">Informações</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <InputField
                  id="name"
                  label="Nome"
                  error={errors.name?.message}
                  hint={name ? `URL: /produtos/${slugify(name)}` : undefined}
                  {...register('name')}
                />
              </div>
              <InputField
                id="sku"
                label="SKU"
                placeholder="GV-ART-001"
                error={errors.sku?.message}
                {...register('sku')}
              />
              <SelectField
                id="categorySlug"
                label="Categoria"
                error={errors.categorySlug?.message}
                {...register('categorySlug')}
              >
                <option value="">Selecione…</option>
                {listCategories().map((c) => (
                  <option key={c.slug} value={c.slug}>
                    {c.name}
                  </option>
                ))}
              </SelectField>
              <div className="sm:col-span-2">
                <TextareaField
                  id="description"
                  label="Descrição"
                  error={errors.description?.message}
                  {...register('description')}
                />
              </div>
            </div>
          </Card>

          <Card>
            <h2 className="font-semibold">Imagens</h2>
            <label
              htmlFor="images"
              className="mt-4 flex cursor-pointer flex-col items-center gap-2 rounded-lg border border-dashed border-border px-6 py-8 text-center text-sm text-fg-muted hover:border-accent"
            >
              <ImagePlus aria-hidden className="size-8" />
              <span>Clique para selecionar imagens (JPG, PNG ou WebP, até 5 MB cada, máx. 8)</span>
              <span className="text-xs">O envio ao servidor será habilitado na Fase 04.</span>
            </label>
            <input
              id="images"
              type="file"
              multiple
              accept="image/jpeg,image/png,image/webp"
              className="sr-only"
              aria-invalid={errors.images ? true : undefined}
              {...register('images')}
            />
            {images && images.length > 0 && (
              <p className="mt-2 text-sm text-fg-muted">{images.length} arquivo(s) selecionado(s)</p>
            )}
            {errors.images && (
              <p role="alert" className="mt-2 text-xs text-danger">
                {errors.images.message}
              </p>
            )}
          </Card>
        </div>

        <div className="flex flex-col gap-6">
          <Card>
            <h2 className="font-semibold">Preço e estoque</h2>
            <div className="mt-4 flex flex-col gap-4">
              <InputField
                id="price"
                label="Preço (R$)"
                inputMode="decimal"
                placeholder="189,90"
                error={errors.price?.message}
                {...register('price')}
              />
              <InputField
                id="promoPrice"
                label="Preço promocional (R$, opcional)"
                inputMode="decimal"
                error={errors.promoPrice?.message}
                {...register('promoPrice')}
              />
              <SelectField id="type" label="Tipo de produto" {...register('type')}>
                {Object.entries(PRODUCT_TYPE).map(([k, v]) => (
                  <option key={k} value={k}>
                    {v}
                  </option>
                ))}
              </SelectField>
              <InputField
                id="stock"
                label="Estoque"
                type="number"
                min={0}
                error={errors.stock?.message}
                {...register('stock')}
              />
              {type !== 'PRONTO' && (
                <InputField
                  id="productionDays"
                  label="Prazo de produção (dias úteis)"
                  type="number"
                  min={1}
                  error={errors.productionDays?.message}
                  {...register('productionDays')}
                />
              )}
              <SelectField id="status" label="Status" {...register('status')}>
                {Object.entries(PRODUCT_STATUS).map(([k, v]) => (
                  <option key={k} value={k}>
                    {v.label}
                  </option>
                ))}
              </SelectField>
            </div>
          </Card>
          <div className="flex flex-col gap-3">
            <button type="submit" className={buttonClass('primary', 'lg')}>
              Salvar produto
            </button>
            <Link to="/admin/produtos" className={buttonClass('outline', 'lg')}>
              Cancelar
            </Link>
            {saved && <Notice>Dados válidos. A gravação será habilitada com a API de catálogo (Fase 04).</Notice>}
          </div>
        </div>
      </form>
    </div>
  )
}
