import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router'
import { SESSION_EXPIRED_FLAG, useAuth } from '../../auth/AuthProvider'
import { Notice } from '../../components/ui/Notice'
import { buttonClass } from '../../components/ui/button'
import { InputField } from '../../components/ui/Field'
import { AuthLayout } from './AuthLayout'
import { applyApiErrors, safeNext } from './formErrors'
import { loginSchema, type LoginInput } from './schemas'

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [showPassword, setShowPassword] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({ resolver: zodResolver(loginSchema) })

  const next = safeNext(params.get('next'))
  if (user) return <Navigate to={next ?? (user.role === 'CUSTOMER' ? '/conta' : '/admin')} replace />

  async function onSubmit(data: LoginInput) {
    setFormError(null)
    try {
      sessionStorage.removeItem(SESSION_EXPIRED_FLAG)
    } catch {
      // ignora
    }
    try {
      const logged = await login(data.email, data.password)
      navigate(next ?? (logged.role === 'CUSTOMER' ? '/conta' : '/admin'), { replace: true })
    } catch (e) {
      setFormError(applyApiErrors(e, setError, ['email', 'password']))
    }
  }

  return (
    <AuthLayout title="Bem-vindo de volta!" subtitle="Acesse sua conta para continuar.">
      <form noValidate className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)}>
        {params.get('expirada') && !formError && (
          <Notice>Sua sessão expirou ou foi encerrada. Entre novamente para continuar.</Notice>
        )}
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
          placeholder="seu@email.com"
          error={errors.email?.message}
          {...register('email')}
        />
        <div className="relative">
          <InputField
            id="password"
            label="Senha"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder="Sua senha"
            className="pr-11"
            error={errors.password?.message}
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setShowPassword((s) => !s)}
            aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
            aria-pressed={showPassword}
            className="absolute right-1 top-7 inline-flex size-9 items-center justify-center rounded-md text-fg-muted hover:text-fg"
          >
            {showPassword ? <EyeOff aria-hidden className="size-4" /> : <Eye aria-hidden className="size-4" />}
          </button>
        </div>
        <Link to="/recuperar-senha" className="self-end text-sm font-medium text-accent hover:underline">
          Esqueceu sua senha?
        </Link>
        <button type="submit" disabled={isSubmitting} className={buttonClass('primary', 'lg')}>
          {isSubmitting ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
      <p className="mt-6 text-center text-sm text-fg-muted">
        Não tem conta?{' '}
        <Link to="/cadastro" className="font-semibold text-accent hover:underline">
          Criar conta
        </Link>
      </p>
    </AuthLayout>
  )
}
