import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { Address } from '../../api/types'
import { applyApiErrors } from '../../pages/auth/formErrors'
import { buttonClass } from '../ui/button'
import { InputField, SelectField } from '../ui/Field'

const UFS = 'AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO'.split(' ')

const schema = z.object({
  label: z.string().trim().min(1, { error: 'Dê um nome (ex.: Casa).' }).max(40),
  recipientName: z.string().trim().min(3, { error: 'Informe o destinatário.' }).max(120),
  postalCode: z
    .string()
    .trim()
    .regex(/^[0-9]{5}-?[0-9]{3}$/, { error: 'CEP inválido.' }),
  street: z.string().trim().min(1, { error: 'Informe a rua.' }).max(160),
  number: z.string().trim().min(1, { error: 'Informe o número (ou S/N).' }).max(20),
  complement: z.string().trim().max(80),
  district: z.string().trim().min(1, { error: 'Informe o bairro.' }).max(80),
  city: z.string().trim().min(1, { error: 'Informe a cidade.' }).max(80),
  state: z.enum(UFS as [string, ...string[]], { error: 'UF inválida.' }),
  makeDefault: z.boolean(),
})

export type AddressInput = z.infer<typeof schema>

const FIELDS = [
  'label',
  'recipientName',
  'postalCode',
  'street',
  'number',
  'complement',
  'district',
  'city',
  'state',
] as const

export function AddressForm({
  initial,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  initial?: Address
  submitLabel: string
  onSubmit: (data: AddressInput) => Promise<void>
  onCancel?: () => void
}) {
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<AddressInput>({
    resolver: zodResolver(schema),
    defaultValues: {
      label: initial?.label ?? 'Casa',
      recipientName: initial?.recipientName ?? '',
      postalCode: initial ? `${initial.postalCode.slice(0, 5)}-${initial.postalCode.slice(5)}` : '',
      street: initial?.street ?? '',
      number: initial?.number ?? '',
      complement: initial?.complement ?? '',
      district: initial?.district ?? '',
      city: initial?.city ?? '',
      state: initial?.state ?? 'PE',
      makeDefault: initial?.isDefault ?? false,
    },
  })

  async function submit(data: AddressInput) {
    setFormError(null)
    try {
      await onSubmit(data)
    } catch (e) {
      setFormError(applyApiErrors(e, setError, [...FIELDS]))
    }
  }

  return (
    <form noValidate onSubmit={handleSubmit(submit)} className="grid gap-4 sm:grid-cols-6">
      {formError && (
        <p role="alert" className="text-sm text-danger sm:col-span-6">
          {formError}
        </p>
      )}
      <div className="sm:col-span-2">
        <InputField id="label" label="Nome do endereço" error={errors.label?.message} {...register('label')} />
      </div>
      <div className="sm:col-span-4">
        <InputField
          id="recipientName"
          label="Destinatário"
          autoComplete="name"
          error={errors.recipientName?.message}
          {...register('recipientName')}
        />
      </div>
      <div className="sm:col-span-2">
        <InputField
          id="postalCode"
          label="CEP"
          inputMode="numeric"
          autoComplete="postal-code"
          placeholder="00000-000"
          error={errors.postalCode?.message}
          {...register('postalCode')}
        />
      </div>
      <div className="sm:col-span-4">
        <InputField
          id="street"
          label="Rua"
          autoComplete="address-line1"
          error={errors.street?.message}
          {...register('street')}
        />
      </div>
      <div className="sm:col-span-2">
        <InputField id="number" label="Número" error={errors.number?.message} {...register('number')} />
      </div>
      <div className="sm:col-span-4">
        <InputField
          id="complement"
          label="Complemento (opcional)"
          autoComplete="address-line2"
          error={errors.complement?.message}
          {...register('complement')}
        />
      </div>
      <div className="sm:col-span-2">
        <InputField id="district" label="Bairro" error={errors.district?.message} {...register('district')} />
      </div>
      <div className="sm:col-span-3">
        <InputField
          id="city"
          label="Cidade"
          autoComplete="address-level2"
          error={errors.city?.message}
          {...register('city')}
        />
      </div>
      <div className="sm:col-span-1">
        <SelectField id="state" label="UF" error={errors.state?.message} {...register('state')}>
          {UFS.map((uf) => (
            <option key={uf}>{uf}</option>
          ))}
        </SelectField>
      </div>
      <label className="flex items-center gap-2 text-sm sm:col-span-6">
        <input type="checkbox" className="size-4 accent-[var(--gv-primary)]" {...register('makeDefault')} />
        Usar como endereço principal
      </label>
      <div className="flex gap-3 sm:col-span-6">
        <button type="submit" disabled={isSubmitting} className={buttonClass('primary')}>
          {isSubmitting ? 'Salvando…' : submitLabel}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} className={buttonClass('outline')}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  )
}
