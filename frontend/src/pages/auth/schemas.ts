import { z } from 'zod'

// Validação no cliente só melhora a experiência; o backend revalida tudo (Fase 05).

const email = z.email({ error: 'Informe um e-mail válido.' })

export const loginSchema = z.object({
  email,
  password: z.string().min(1, { error: 'Informe sua senha.' }),
})

export const registerSchema = z
  .object({
    name: z.string().trim().min(3, { error: 'Informe seu nome completo.' }).max(120),
    email,
    password: z
      .string()
      .min(10, { error: 'A senha deve ter pelo menos 10 caracteres.' })
      .max(128, { error: 'A senha deve ter no máximo 128 caracteres.' }),
    confirmPassword: z.string(),
    acceptTerms: z.literal(true, { error: 'É preciso aceitar os termos e a política de privacidade.' }),
    marketingOptIn: z.boolean(),
  })
  .refine((d) => d.password === d.confirmPassword, { error: 'As senhas não conferem.', path: ['confirmPassword'] })

export const forgotSchema = z.object({ email })

export type LoginInput = z.infer<typeof loginSchema>
export type RegisterInput = z.input<typeof registerSchema>
export type ForgotInput = z.infer<typeof forgotSchema>
