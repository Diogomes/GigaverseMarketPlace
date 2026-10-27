import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { jsonResponse, mockApi, renderApp } from '../../test/authTestUtils'
import { safeNext } from './formErrors'
import { LoginPage } from './LoginPage'

const customer = { id: 1, email: 'ana@exemplo.com', fullName: 'Ana Teste', role: 'CUSTOMER' as const }

describe('LoginPage', () => {
  it('validates fields locally before calling the login endpoint', async () => {
    const fetchSpy = mockApi(null)
    renderApp(<LoginPage />, { route: '/entrar' })
    await userEvent.click(await screen.findByRole('button', { name: 'Entrar' }))
    expect(await screen.findByText('Informe um e-mail válido.')).toBeInTheDocument()
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('aria-invalid', 'true')
    expect(fetchSpy.mock.calls.some(([url]) => String(url).includes('/auth/login'))).toBe(false)
  })

  it('shows the generic server message on invalid credentials', async () => {
    mockApi(null, (url) =>
      url.endsWith('/auth/login') ? jsonResponse(401, { detail: 'E-mail ou senha inválidos.' }) : undefined,
    )
    renderApp(<LoginPage />, { route: '/entrar' })
    await userEvent.type(await screen.findByLabelText('E-mail'), 'ana@exemplo.com')
    await userEvent.type(screen.getByLabelText('Senha'), 'senha-errada-123')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.')
  })

  it('sends credentials with the CSRF header and redirects customers to their account', async () => {
    const fetchSpy = mockApi(null, (url) => (url.endsWith('/auth/login') ? jsonResponse(200, customer) : undefined))
    renderApp(<LoginPage />, { route: '/entrar', extraRoutes: [['/conta', <p key="conta">Área do cliente</p>]] })
    await userEvent.type(await screen.findByLabelText('E-mail'), 'ana@exemplo.com')
    await userEvent.type(screen.getByLabelText('Senha'), 'Senha-Bem-Longa-1')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(await screen.findByText('Área do cliente')).toBeInTheDocument()

    const loginCall = fetchSpy.mock.calls.find(([url]) => String(url).endsWith('/auth/login'))!
    const init = loginCall[1] as RequestInit
    expect(init.method).toBe('POST')
    expect(init.credentials).toBe('include')
    expect((init.headers as Record<string, string>)['X-XSRF-TOKEN']).toBe('token-de-teste')
  })

  it('does not show fake social login buttons', async () => {
    mockApi(null)
    renderApp(<LoginPage />, { route: '/entrar' })
    await screen.findByRole('button', { name: 'Entrar' })
    expect(screen.queryByRole('button', { name: /google|facebook/i })).not.toBeInTheDocument()
  })
})

describe('safeNext', () => {
  it.each([
    ['/admin/produtos', '/admin/produtos'],
    ['//evil.example', null],
    ['/\\evil.example', null],
    ['https://evil.example', null],
    [null, null],
  ])('%s -> %s', (input, expected) => {
    expect(safeNext(input)).toBe(expected)
  })
})
