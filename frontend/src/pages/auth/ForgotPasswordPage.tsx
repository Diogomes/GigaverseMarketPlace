import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { buttonClass } from '../../components/ui/button'
import { InputField } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { AuthLayout } from './AuthLayout'
import { forgotSchema, type ForgotInput } from './schemas'

export function ForgotPasswordPage() {
  const [submitted, setSubmitted] = useState(false)
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotInput>({ resolver: zodResolver(forgotSchema) })

  return (
    <AuthLayout title="Recuperar senha" subtitle="Enviaremos um link de redefinição para o seu e-mail.">
      <form noValidate className="flex flex-col gap-4" onSubmit={handleSubmit(() => setSubmitted(true))}>
        <InputField
          id="email"
          label="E-mail"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <button type="submit" className={buttonClass('primary', 'lg')}>
          Enviar link
        </button>
        {submitted && (
          <Notice>
            A recuperação de senha será habilitada na Fase 05. A resposta real não revelará se o e-mail está cadastrado.
          </Notice>
        )}
      </form>
      <p className="mt-6 text-center text-sm">
        <Link to="/entrar" className="font-semibold text-accent hover:underline">
          Voltar para o login
        </Link>
      </p>
    </AuthLayout>
  )
}
