import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { buttonClass } from '../../components/ui/button'
import { InputField } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { apiSend } from '../../lib/api'
import { AuthLayout } from './AuthLayout'
import { applyApiErrors } from './formErrors'
import { forgotSchema, type ForgotInput } from './schemas'

export function ForgotPasswordPage() {
  const [message, setMessage] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ForgotInput>({ resolver: zodResolver(forgotSchema) })

  async function onSubmit(data: ForgotInput) {
    setFormError(null)
    try {
      const res = await apiSend<{ message: string }>('POST', '/api/v1/auth/password/forgot', data)
      setMessage(res.message)
    } catch (e) {
      setFormError(applyApiErrors(e, setError, ['email']))
    }
  }

  return (
    <AuthLayout title="Recuperar senha" subtitle="Enviaremos um link de redefinição para o seu e-mail.">
      {message ? (
        <Notice>{message}</Notice>
      ) : (
        <form noValidate className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)}>
          {formError && (
            <p role="alert" className="rounded-lg border border-danger/40 bg-danger/10 px-3 py-2 text-sm text-danger">
              {formError}
            </p>
          )}
          <InputField
            id="email"
            label="E-mail"
            type="email"
            autoComplete="email"
            error={errors.email?.message}
            {...register('email')}
          />
          <button type="submit" disabled={isSubmitting} className={buttonClass('primary', 'lg')}>
            {isSubmitting ? 'Enviando…' : 'Enviar link'}
          </button>
        </form>
      )}
      <p className="mt-6 text-center text-sm">
        <Link to="/entrar" className="font-semibold text-accent hover:underline">
          Voltar para o login
        </Link>
      </p>
    </AuthLayout>
  )
}
