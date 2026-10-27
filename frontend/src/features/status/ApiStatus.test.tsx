import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ApiStatus } from './ApiStatus'

function renderStatus() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <ApiStatus />
    </QueryClientProvider>,
  )
}

describe('ApiStatus', () => {
  it('shows connected when the backend answers ok', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(
        new Response(JSON.stringify({ service: 'gigaverse3d-api', status: 'ok', timestamp: '' }), { status: 200 }),
      )
    renderStatus()
    expect(await screen.findByText('API conectada')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/public/status',
      expect.objectContaining({ credentials: 'include', method: 'GET' }),
    )
  })

  it('shows unavailable when the request fails', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 503 }))
    renderStatus()
    expect(await screen.findByText('API indisponível')).toBeInTheDocument()
  })
})
