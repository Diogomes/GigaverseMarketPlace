import { zodResolver } from '@hookform/resolvers/zod'
import { ImagePlus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { useAdminCategories, useAdminProduct, useCatalogActions, type AdminProduct } from '../../api/admin'
import { ApiError } from '../../lib/api'
import { useAuth } from '../../auth/AuthProvider'
import { ActionError, errorMessage, Loading, QueryError } from '../../components/admin/AdminUi'
import { PRODUCT_STATUS_UI, PRODUCT_TYPE_UI } from '../../components/admin/labels'
import { StockPanel } from '../../components/admin/StockPanel'
import { buttonClass } from '../../components/ui/button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { controlClass, InputField, SelectField, TextareaField } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { slugify } from '../../lib/format'
import { NotFoundPage } from '../PlaceholderPage'
import { applyApiErrors } from '../auth/formErrors'
import {
  ALLOWED_IMAGE_TYPES,
  MAX_IMAGE_BYTES,
  productSchema,
  toForm,
  toRequest,
  type ProductFormInput,
} from './productSchema'

const FIELDS: Array<keyof ProductFormInput> = [
  'name',
  'sku',
  'categoryId',
  'description',
  'price',
  'promoPrice',
  'type',
  'lowStockThreshold',
  'productionDays',
  'status',
]

export function ProductFormPage() {
  const { id } = useParams()
  const { data, error, isPending, refetch } = useAdminProduct(id)
  if (!id) return <ProductForm key="novo" />
  if (error instanceof ApiError && error.status === 404) return <NotFoundPage />
  if (error) return <QueryError error={error} onRetry={() => void refetch()} />
  if (isPending) return <Loading />
  // a versão (controle de concorrência) vem sempre da última leitura, atualizada após cada gravação
  return <ProductForm key={data.id} product={data} />
}

function ProductForm({ product }: { product?: AdminProduct }) {
  const navigate = useNavigate()
  const location = useLocation()
  const categories = useAdminCategories()
  const actions = useCatalogActions()
  const [failure, setFailure] = useState<string | null>(null)
  const [saved, setSaved] = useState<string | null>(
    (location.state as { created?: boolean } | null)?.created
      ? 'Produto criado. Agora adicione imagens e estoque.'
      : null,
  )
  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ProductFormInput>({ resolver: zodResolver(productSchema), defaultValues: toForm(product) })
  const [name, type] = useWatch({ control, name: ['name', 'type'] })

  async function onSubmit(values: ProductFormInput) {
    setFailure(null)
    setSaved(null)
    try {
      if (product) {
        await actions.updateProduct(product.id, toRequest(values, product))
        setSaved('Alterações salvas.')
      } else {
        const created = await actions.createProduct(toRequest(values))
        navigate(`/admin/produtos/${created.id}`, { replace: true, state: { created: true } })
      }
    } catch (e) {
      setFailure(applyApiErrors(e, setError, FIELDS))
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <Link to="/admin/produtos" className="text-sm font-semibold text-accent hover:underline">
        ← Produtos
      </Link>
      <PageHeader
        title={product ? `Editar: ${product.name}` : 'Novo produto'}
        description="Os campos são validados aqui e novamente no servidor."
      />
      {saved && <Notice>{saved}</Notice>}
      <form noValidate onSubmit={handleSubmit(onSubmit)} className="grid gap-6 xl:grid-cols-[1fr_22rem]">
        <div className="flex flex-col gap-6">
          <Card>
            <h2 className="font-semibold">Informações</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <InputField
                  id="name"
                  label="Nome"
                  error={errors.name?.message}
                  hint={
                    product ? `URL: /produtos/${product.slug}` : name ? `URL: /produtos/${slugify(name)}` : undefined
                  }
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
                id="categoryId"
                label="Categoria"
                error={errors.categoryId?.message}
                {...register('categoryId')}
              >
                <option value="">Selecione…</option>
                {categories.data?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                    {c.active ? '' : ' (inativa)'}
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
              {product && product.options.length > 0 && (
                <p className="text-xs text-fg-muted sm:col-span-2">
                  Opções ({product.options.map((o) => o.name).join(', ')}) são mantidas ao salvar.
                </p>
              )}
            </div>
          </Card>

          {product ? (
            <>
              <ImagesCard product={product} />
              {product.type === 'READY' && (
                <Card>
                  <h2 className="font-semibold">Estoque</h2>
                  <div className="mt-4">
                    <StockPanel productId={product.id} stock={product.stock} />
                  </div>
                </Card>
              )}
            </>
          ) : (
            <Card>
              <h2 className="font-semibold">Imagens e estoque</h2>
              <p className="mt-2 text-sm text-fg-muted">
                Salve o produto primeiro; em seguida você poderá enviar imagens e lançar a entrada de estoque.
              </p>
            </Card>
          )}
        </div>

        <div className="flex flex-col gap-6">
          <Card>
            <h2 className="font-semibold">Preço e disponibilidade</h2>
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
                {Object.entries(PRODUCT_TYPE_UI).map(([k, v]) => (
                  <option key={k} value={k}>
                    {v}
                  </option>
                ))}
              </SelectField>
              {type === 'READY' ? (
                <InputField
                  id="lowStockThreshold"
                  label="Estoque mínimo (alerta)"
                  type="number"
                  min={0}
                  error={errors.lowStockThreshold?.message}
                  {...register('lowStockThreshold')}
                />
              ) : (
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
                {Object.entries(PRODUCT_STATUS_UI).map(([k, v]) => (
                  <option key={k} value={k}>
                    {v.label}
                  </option>
                ))}
              </SelectField>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" className="size-4 accent-[var(--gv-primary)]" {...register('featured')} />
                Destacar na página inicial
              </label>
            </div>
          </Card>
          <div className="flex flex-col gap-3">
            <ActionError message={failure} />
            <button type="submit" className={buttonClass('primary', 'lg')} disabled={isSubmitting}>
              {isSubmitting ? 'Salvando…' : 'Salvar produto'}
            </button>
            <Link to="/admin/produtos" className={buttonClass('outline', 'lg')}>
              Cancelar
            </Link>
          </div>
        </div>
      </form>
    </div>
  )
}

function ImagesCard({ product }: { product: AdminProduct }) {
  const { user } = useAuth()
  const actions = useCatalogActions()
  const [file, setFile] = useState<File | null>(null)
  const [alt, setAlt] = useState('')
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const [removing, setRemoving] = useState<number | null>(null)
  const isAdmin = user?.role === 'ADMIN'

  async function upload(e: React.FormEvent) {
    e.preventDefault()
    setFailure(null)
    if (!file) return setFailure('Selecione uma imagem.')
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) return setFailure('Formatos aceitos: JPG, PNG ou WebP.')
    if (file.size > MAX_IMAGE_BYTES) return setFailure('A imagem deve ter no máximo 5 MB.')
    if (alt.trim().length < 3) return setFailure('Descreva a imagem (texto alternativo, mín. 3 caracteres).')
    setBusy(true)
    try {
      await actions.uploadImage(product.id, file, alt.trim())
      setFile(null)
      setAlt('')
      ;(e.target as HTMLFormElement).reset()
    } catch (err) {
      setFailure(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card>
      <h2 className="font-semibold">Imagens</h2>
      {product.images.length === 0 ? (
        <p className="mt-2 text-sm text-fg-muted">Nenhuma imagem enviada.</p>
      ) : (
        <ul className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {product.images.map((img) => (
            <li key={img.id} className="relative">
              <img src={img.url} alt={img.alt} className="aspect-square w-full rounded-lg object-cover" />
              {isAdmin && (
                <button
                  type="button"
                  aria-label={`Remover imagem: ${img.alt}`}
                  className="absolute right-1 top-1 inline-flex size-8 items-center justify-center rounded-md bg-black/60 text-white hover:bg-danger"
                  onClick={() => setRemoving(img.id)}
                >
                  <Trash2 aria-hidden className="size-4" />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
      {product.images.length < 8 && (
        <form onSubmit={upload} className="mt-4 flex flex-col gap-2">
          <label
            htmlFor="image-file"
            className="flex cursor-pointer flex-col items-center gap-2 rounded-lg border border-dashed border-border px-6 py-6 text-center text-sm text-fg-muted hover:border-accent"
          >
            <ImagePlus aria-hidden className="size-7" />
            <span>{file ? file.name : 'Selecionar imagem (JPG, PNG ou WebP, até 5 MB; máx. 8 por produto)'}</span>
          </label>
          <input
            id="image-file"
            type="file"
            accept={ALLOWED_IMAGE_TYPES.join(',')}
            className="sr-only"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
          <label className="text-sm">
            <span className="mb-1 block font-medium">Texto alternativo</span>
            <input
              value={alt}
              maxLength={160}
              onChange={(e) => setAlt(e.target.value)}
              placeholder="Ex.: Dragão azul articulado visto de lado"
              className={`${controlClass} h-10`}
            />
          </label>
          <ActionError message={failure} />
          <button type="submit" className={buttonClass('outline', 'md', 'self-end')} disabled={busy}>
            {busy ? 'Enviando…' : 'Enviar imagem'}
          </button>
        </form>
      )}
      <ConfirmDialog
        open={removing !== null}
        title="Remover imagem?"
        description="A imagem será apagada do armazenamento e deixará de aparecer na loja."
        confirmLabel="Remover"
        onCancel={() => setRemoving(null)}
        onConfirm={() => {
          const imageId = removing
          setRemoving(null)
          if (imageId !== null) actions.deleteImage(product.id, imageId).catch((e) => setFailure(errorMessage(e)))
        }}
      />
    </Card>
  )
}
