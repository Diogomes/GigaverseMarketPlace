import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../../auth/AuthProvider'
import { buttonClass } from '../../components/ui/button'
import { InputField } from '../../components/ui/Field'
import { AuthLayout } from './AuthLayout'
import { applyApiErrors } from './formErrors'
import { registerSchema, type RegisterInput } from './schemas'

export function RegisterPage() {
  const { register: createAccount } = useAuth()
  const navigate = useNavigate()
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterInput>({ resolver: zodResolver(registerSchema), defaultValues: { marketingOptIn: false } })

  async function onSubmit(data: RegisterInput) {
    setFormError(null)
    try {
      await createAccount({
        fullName: data.name,
        email: data.email,
        password: data.password,
        acceptTerms: data.acceptTerms === true,
        marketingOptIn: data.marketingOptIn,
      })
      navigate('/conta', { replace: true })
    } catch (e) {
      setFormError(applyApiErrors(e, setError, ['email', 'password']))
    }
  }

  return (
    <AuthLayout title="Criar conta" subtitle="Cadastre-se para acompanhar seus pedidos.">
      <form noValidate className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)}>
        {formError && (
          <p role="alert" className="rounded-lg border border-danger/40 bg-danger/10 px-3 py-2 text-sm text-danger">
            {formError}
          </p>
        )}
        <InputField
          id="name"
          label="Nome completo"
          autoComplete="name"
          error={errors.name?.message}
          {...register('name')}
        />
        <InputField
          id="email"
          label="E-mail"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <InputField
          id="password"
          label="Senha"
          type="password"
          autoComplete="new-password"
          hint="Mínimo de 10 caracteres. Frases longas são ótimas senhas."
          error={errors.password?.message}
          {...register('password')}
        />
        <InputField
          id="confirmPassword"
          label="Confirmar senha"
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />
        <div className="flex flex-col gap-1">
          <label className="flex items-start gap-2 text-sm">
            <input
              type="checkbox"
              className="mt-0.5 size-4 accent-[var(--gv-primary)]"
              aria-invalid={errors.acceptTerms ? true : undefined}
              aria-describedby={errors.acceptTerms ? 'acceptTerms-error' : undefined}
              {...register('acceptTerms')}
            />
            <span>Li e aceito os termos de uso e a política de privacidade.</span>
          </label>
          {errors.acceptTerms && (
            <p id="acceptTerms-error" role="alert" className="text-xs text-danger">
              {errors.acceptTerms.message}
            </p>
          )}
        </div>
        <label className="flex items-start gap-2 text-sm text-fg-muted">
          <input type="checkbox" className="mt-0.5 size-4 accent-[var(--gv-primary)]" {...register('marketingOptIn')} />
          <span>Quero receber novidades e promoções por e-mail (opcional; pode cancelar a qualquer momento).</span>
        </label>
        <button type="submit" disabled={isSubmitting} className={buttonClass('primary', 'lg')}>
          {isSubmitting ? 'Criando conta…' : 'Criar conta'}
        </button>
      </form>
      <p className="mt-6 text-center text-sm text-fg-muted">
        Já tem conta?{' '}
        <Link to="/entrar" className="font-semibold text-accent hover:underline">
          Entrar
        </Link>
      </p>
    </AuthLayout>
  )
}
