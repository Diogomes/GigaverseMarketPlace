import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { buttonClass } from '../../components/ui/button'
import { InputField } from '../../components/ui/Field'
import { Notice } from '../../components/ui/Notice'
import { AuthLayout } from './AuthLayout'
import { loginSchema, type LoginInput } from './schemas'

export function LoginPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({ resolver: zodResolver(loginSchema) })

  return (
    <AuthLayout title="Bem-vindo de volta!" subtitle="Acesse sua conta para continuar.">
      <form noValidate className="flex flex-col gap-4" onSubmit={handleSubmit(() => setSubmitted(true))}>
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
          Entrar
        </button>
        {submitted && <Notice>O login será habilitado na Fase 05 (autenticação). Nenhum dado foi enviado.</Notice>}
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
