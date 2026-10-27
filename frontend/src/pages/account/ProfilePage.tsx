import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { buttonClass } from '../../components/ui/button'
import { InputField } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { Card, PageHeader } from '../../components/ui/PageHeader'
import { apiGet, apiSend } from '../../lib/api'
import { applyApiErrors } from '../auth/formErrors'

type Profile = { email: string; fullName: string; phone: string | null; marketingOptIn: boolean }

const profileSchema = z.object({
  fullName: z.string().trim().min(3, { error: 'Informe seu nome completo.' }).max(120),
  phone: z
    .string()
    .trim()
    .regex(/^(\+?[0-9]{10,15})?$/, { error: 'Telefone com DDD, só números (10 a 15 dígitos).' }),
  marketingOptIn: z.boolean(),
})
type ProfileInput = z.infer<typeof profileSchema>

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, { error: 'Informe a senha atual.' }),
    newPassword: z.string().min(10, { error: 'A senha deve ter pelo menos 10 caracteres.' }).max(128),
    confirmPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmPassword, { error: 'As senhas não conferem.', path: ['confirmPassword'] })
type PasswordInput = z.infer<typeof passwordSchema>

const PROFILE_KEY = ['account', 'profile']

export function AccountProfilePage() {
  const { data, isPending, isError } = useQuery({
    queryKey: PROFILE_KEY,
    queryFn: () => apiGet<Profile>('/api/v1/account/profile'),
  })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Meus dados" description="Informações e segurança da sua conta." />
      {isPending && (
        <p role="status" className="text-sm text-fg-muted">
          Carregando…
        </p>
      )}
      {isError && <Notice>Não foi possível carregar seus dados. Tente novamente.</Notice>}
      {data && <ProfileForm profile={data} />}
      <PasswordForm />
    </div>
  )
}

function ProfileForm({ profile }: { profile: Profile }) {
  const queryClient = useQueryClient()
  const [message, setMessage] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ProfileInput>({
    resolver: zodResolver(profileSchema),
    defaultValues: { fullName: profile.fullName, phone: profile.phone ?? '', marketingOptIn: profile.marketingOptIn },
  })

  async function onSubmit(data: ProfileInput) {
    setMessage(null)
    try {
      const saved = await apiSend<Profile>('PUT', '/api/v1/account/profile', { ...data, phone: data.phone || null })
      queryClient.setQueryData(PROFILE_KEY, saved)
      await queryClient.invalidateQueries({ queryKey: ['auth', 'me'] })
      setMessage('Dados atualizados.')
    } catch (e) {
      setMessage(applyApiErrors(e, setError, ['fullName', 'phone']))
    }
  }

  return (
    <Card>
      <h2 className="font-semibold">Dados pessoais</h2>
      <form noValidate className="mt-4 grid gap-4 sm:grid-cols-2" onSubmit={handleSubmit(onSubmit)}>
        <InputField
          id="fullName"
          label="Nome"
          autoComplete="name"
          error={errors.fullName?.message}
          {...register('fullName')}
        />
        <InputField
          id="email"
          label="E-mail"
          value={profile.email}
          disabled
          readOnly
          hint="Para trocar o e-mail, fale com o suporte."
        />
        <InputField
          id="phone"
          label="Telefone (opcional)"
          type="tel"
          autoComplete="tel"
          placeholder="81999998888"
          error={errors.phone?.message}
          {...register('phone')}
        />
        <label className="flex items-start gap-2 self-center text-sm sm:col-span-2">
          <input type="checkbox" className="mt-0.5 size-4 accent-[var(--gv-primary)]" {...register('marketingOptIn')} />
          <span>Quero receber novidades e promoções por e-mail (pode cancelar a qualquer momento).</span>
        </label>
        <div className="flex flex-wrap items-center gap-3 sm:col-span-2">
          <button type="submit" disabled={isSubmitting} className={buttonClass('primary')}>
            {isSubmitting ? 'Salvando…' : 'Salvar'}
          </button>
          {message && (
            <p role="status" className="text-sm text-fg-muted">
              {message}
            </p>
          )}
        </div>
      </form>
    </Card>
  )
}

function PasswordForm() {
  const [message, setMessage] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<PasswordInput>({ resolver: zodResolver(passwordSchema) })

  async function onSubmit(data: PasswordInput) {
    setMessage(null)
    try {
      await apiSend('PUT', '/api/v1/account/password', {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      })
      reset()
      setMessage('Senha alterada. As sessões abertas em outros dispositivos foram encerradas.')
    } catch (e) {
      setMessage(applyApiErrors(e, setError, ['currentPassword', 'newPassword']))
    }
  }

  return (
    <Card>
      <h2 className="font-semibold">Alterar senha</h2>
      <form noValidate className="mt-4 grid gap-4 sm:grid-cols-3" onSubmit={handleSubmit(onSubmit)}>
        <InputField
          id="currentPassword"
          label="Senha atual"
          type="password"
          autoComplete="current-password"
          error={errors.currentPassword?.message}
          {...register('currentPassword')}
        />
        <InputField
          id="newPassword"
          label="Nova senha"
          type="password"
          autoComplete="new-password"
          error={errors.newPassword?.message}
          {...register('newPassword')}
        />
        <InputField
          id="confirmPassword"
          label="Confirmar nova senha"
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />
        <div className="flex flex-wrap items-center gap-3 sm:col-span-3">
          <button type="submit" disabled={isSubmitting} className={buttonClass('primary')}>
            {isSubmitting ? 'Salvando…' : 'Alterar senha'}
          </button>
          {message && (
            <p role="status" className="text-sm text-fg-muted">
              {message}
            </p>
          )}
        </div>
      </form>
    </Card>
  )
}
