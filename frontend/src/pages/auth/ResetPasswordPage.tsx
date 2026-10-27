import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router'
import { buttonClass } from '../../components/ui/button'
import { InputField } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { apiSend } from '../../lib/api'
import { AuthLayout } from './AuthLayout'
import { applyApiErrors } from './formErrors'
import { resetSchema, type ResetInput } from './schemas'

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [done, setDone] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ResetInput>({ resolver: zodResolver(resetSchema) })

  async function onSubmit(data: ResetInput) {
    setFormError(null)
    try {
      await apiSend('POST', '/api/v1/auth/password/reset', { token, newPassword: data.newPassword })
      setDone(true)
    } catch (e) {
      setFormError(applyApiErrors(e, setError, ['newPassword']))
    }
  }

  return (
    <AuthLayout title="Definir nova senha" subtitle="Escolha uma senha nova para a sua conta.">
      {!token ? (
        <Notice>Link inválido. Solicite um novo em “Esqueceu sua senha?”.</Notice>
      ) : done ? (
        <div className="flex flex-col gap-4">
          <Notice>Senha redefinida. Por segurança, as sessões abertas foram encerradas.</Notice>
          <Link to="/entrar" className={buttonClass('primary', 'lg')}>
            Ir para o login
          </Link>
        </div>
      ) : (
        <form noValidate className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)}>
          {formError && (
            <p role="alert" className="rounded-lg border border-danger/40 bg-danger/10 px-3 py-2 text-sm text-danger">
              {formError}{' '}
              <Link to="/recuperar-senha" className="font-semibold underline">
                Solicitar novo link
              </Link>
            </p>
          )}
          <InputField
            id="newPassword"
            label="Nova senha"
            type="password"
            autoComplete="new-password"
            hint="Mínimo de 10 caracteres."
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
          <button type="submit" disabled={isSubmitting} className={buttonClass('primary', 'lg')}>
            {isSubmitting ? 'Salvando…' : 'Salvar nova senha'}
          </button>
        </form>
      )}
    </AuthLayout>
  )
}
