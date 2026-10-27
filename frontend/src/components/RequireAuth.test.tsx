import { screen } from '@testing-library/react'
import { useLocation } from 'react-router'
import { describe, expect, it } from 'vitest'
import { mockApi, renderApp } from '../test/authTestUtils'
import { RequireAuth } from './PreviewGuard'

function WhereAmI() {
  const location = useLocation()
  return <p>Login em {location.pathname + location.search}</p>
}

describe('RequireAuth', () => {
  it('sends anonymous visitors to the login page keeping the destination', async () => {
    mockApi(null)
    renderApp(
      <RequireAuth roles={['ADMIN']}>
        <p>Painel</p>
      </RequireAuth>,
      { route: '/admin', extraRoutes: [['/entrar', <WhereAmI key="entrar" />]] },
    )
    expect(await screen.findByText('Login em /entrar?next=%2Fadmin')).toBeInTheDocument()
  })

  it('blocks customers from staff areas', async () => {
    mockApi({ id: 1, email: 'c@exemplo.com', fullName: 'Cliente', role: 'CUSTOMER' })
    renderApp(
      <RequireAuth roles={['ADMIN', 'OPERATOR']}>
        <p>Painel</p>
      </RequireAuth>,
      { route: '/admin' },
    )
    expect(await screen.findByText('Acesso restrito')).toBeInTheDocument()
    expect(screen.queryByText('Painel')).not.toBeInTheDocument()
  })

  it('lets staff in', async () => {
    mockApi({ id: 2, email: 'a@exemplo.com', fullName: 'Admin', role: 'ADMIN' })
    renderApp(
      <RequireAuth roles={['ADMIN', 'OPERATOR']}>
        <p>Painel</p>
      </RequireAuth>,
      { route: '/admin' },
    )
    expect(await screen.findByText('Painel')).toBeInTheDocument()
  })
})
