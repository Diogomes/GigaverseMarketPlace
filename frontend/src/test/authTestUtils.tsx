import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { vi } from 'vitest'
import { AuthProvider, type SessionUser } from '../auth/AuthProvider'
import { CartProvider } from '../cart/CartProvider'

type Handler = (url: string, init?: RequestInit) => Response | undefined

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

/**
 * Simula o backend: /auth/me (sessão), /auth/csrf (grava o cookie XSRF-TOKEN) e
 * respostas extras por handler. Retorna o spy para inspecionar as chamadas.
 */
export function mockApi(me: SessionUser | null, extra: Handler = () => undefined) {
  document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/'
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input)
    const custom = extra(url, init)
    if (custom) return custom
    if (url.endsWith('/api/v1/auth/me')) return me ? json(200, me) : new Response(null, { status: 204 })
    if (url.endsWith('/api/v1/auth/csrf')) {
      document.cookie = 'XSRF-TOKEN=token-de-teste; path=/'
      return json(200, { token: 'token-de-teste', headerName: 'X-XSRF-TOKEN' })
    }
    return json(404, { detail: 'não simulado' })
  })
}

export { json as jsonResponse }

export function renderApp(ui: ReactNode, { route = '/', extraRoutes = [] as Array<[string, ReactNode]> } = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <AuthProvider>
        <CartProvider>
          <MemoryRouter initialEntries={[route]}>
            <Routes>
              <Route path="*" element={ui} />
              {extraRoutes.map(([path, el]) => (
                <Route key={path} path={path} element={el} />
              ))}
            </Routes>
          </MemoryRouter>
        </CartProvider>
      </AuthProvider>
    </QueryClientProvider>,
  )
}
