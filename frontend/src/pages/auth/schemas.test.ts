import { describe, expect, it } from 'vitest'
import { forgotSchema, loginSchema, registerSchema, resetSchema } from './schemas'

const valid = {
  name: 'Cliente Fictício',
  email: 'cliente@exemplo.com',
  password: 'Frase-Longa-Ficticia-1',
  confirmPassword: 'Frase-Longa-Ficticia-1',
  acceptTerms: true as const,
  marketingOptIn: false,
}

const messages = (r: { success: boolean; error?: { issues: Array<{ path: PropertyKey[]; message: string }> } }) =>
  Object.fromEntries((r.error?.issues ?? []).map((i) => [String(i.path[0]), i.message]))

describe('registerSchema', () => {
  it('aceita um cadastro válido', () => {
    expect(registerSchema.safeParse(valid).success).toBe(true)
  })

  it('exige nome, e-mail válido, senha longa e aceite dos termos', () => {
    const r = registerSchema.safeParse({
      ...valid,
      name: ' A ',
      email: 'sem-arroba',
      password: 'curta',
      confirmPassword: 'curta',
      acceptTerms: false,
    })
    expect(messages(r)).toEqual(
      expect.objectContaining({
        name: 'Informe seu nome completo.',
        email: 'Informe um e-mail válido.',
        password: 'A senha deve ter pelo menos 10 caracteres.',
        acceptTerms: 'É preciso aceitar os termos e a política de privacidade.',
      }),
    )
  })

  it('recusa senhas diferentes e senha acima de 128 caracteres', () => {
    expect(messages(registerSchema.safeParse({ ...valid, confirmPassword: 'Outra-Frase-Longa-1' }))).toEqual({
      confirmPassword: 'As senhas não conferem.',
    })
    const long = 'a'.repeat(129)
    expect(messages(registerSchema.safeParse({ ...valid, password: long, confirmPassword: long })).password).toBe(
      'A senha deve ter no máximo 128 caracteres.',
    )
  })

  it('o nome é aparado (espaços nas pontas não contam)', () => {
    const r = registerSchema.safeParse({ ...valid, name: '   Ana   ' })
    expect(r.success && r.data.name).toBe('Ana')
  })
})

describe('login, recuperação e redefinição', () => {
  it('login exige e-mail e senha', () => {
    expect(loginSchema.safeParse({ email: 'cliente@exemplo.com', password: 'x' }).success).toBe(true)
    expect(messages(loginSchema.safeParse({ email: '', password: '' }))).toEqual({
      email: 'Informe um e-mail válido.',
      password: 'Informe sua senha.',
    })
  })

  it('recuperação só pede um e-mail válido', () => {
    expect(forgotSchema.safeParse({ email: 'cliente@exemplo.com' }).success).toBe(true)
    expect(forgotSchema.safeParse({ email: 'cliente@' }).success).toBe(false)
  })

  it('redefinição exige senha longa e confirmação igual', () => {
    expect(
      resetSchema.safeParse({ newPassword: 'Nova-Frase-Longa-1', confirmPassword: 'Nova-Frase-Longa-1' }).success,
    ).toBe(true)
    expect(
      messages(resetSchema.safeParse({ newPassword: 'Nova-Frase-Longa-1', confirmPassword: 'diferente' })),
    ).toEqual({
      confirmPassword: 'As senhas não conferem.',
    })
  })
})
