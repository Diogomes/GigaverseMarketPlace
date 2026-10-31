import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, mockApi, renderApp } from '../../test/authTestUtils'
import { PrivacyCard } from './PrivacyCard'

const user = { id: 3, email: 'cliente@exemplo.com', fullName: 'Cliente', role: 'CUSTOMER' as const }

afterEach(() => vi.restoreAllMocks())

describe('PrivacyCard', () => {
  it('asks for the password and an explicit confirmation before deleting', async () => {
    const bodies: unknown[] = []
    mockApi(user, (url, init) => {
      if (url.endsWith('/api/v1/account/privacy/delete-account')) {
        bodies.push(JSON.parse(String(init?.body)))
        return jsonResponse(200, { message: 'Conta excluída.' })
      }
      return undefined
    })
    renderApp(<PrivacyCard />, { route: '/conta/dados', extraRoutes: [['/', <p key="home">Página inicial</p>]] })
    const u = userEvent.setup()

    await u.click(await screen.findByRole('button', { name: /Excluir minha conta/ }))
    expect(screen.getByRole('alert')).toHaveTextContent('Confirme com sua senha.')
    expect(bodies).toHaveLength(0)

    await u.type(screen.getByLabelText('Sua senha'), 'minha-senha-123')
    await u.click(screen.getByRole('button', { name: /Excluir minha conta/ }))
    expect(bodies).toHaveLength(0) // ainda falta confirmar no diálogo
    await u.click(screen.getByRole('button', { name: 'Excluir definitivamente' }))

    await waitFor(() => expect(bodies).toEqual([{ password: 'minha-senha-123' }]))
    expect(await screen.findByText('Página inicial')).toBeInTheDocument()
  })

  it('shows the server reason when deletion is refused', async () => {
    mockApi(user, (url) =>
      url.endsWith('/delete-account') ? jsonResponse(409, { detail: 'Você tem pedidos em andamento.' }) : undefined,
    )
    renderApp(<PrivacyCard />)
    const u = userEvent.setup()
    await u.type(await screen.findByLabelText('Sua senha'), 'minha-senha-123')
    await u.click(screen.getByRole('button', { name: /Excluir minha conta/ }))
    await u.click(screen.getByRole('button', { name: 'Excluir definitivamente' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Você tem pedidos em andamento.')
  })
})
