import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, SESSION_EXPIRED_EVENT, apiGet, apiSend } from './api'

const json = (status: number, body: unknown, headers: Record<string, string> = {}) =>
  new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  })

function clearCsrfCookie() {
  document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/'
}

beforeEach(clearCsrfCookie)
afterEach(() => {
  vi.restoreAllMocks()
  clearCsrfCookie()
})

describe('apiGet', () => {
  it('envia cookies (credentials: include) e devolve o JSON', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(200, { ok: true }))
    await expect(apiGet('/api/v1/public/status')).resolves.toEqual({ ok: true })
    expect(spy.mock.calls[0][1]).toEqual(expect.objectContaining({ method: 'GET', credentials: 'include' }))
  })

  it('204 vira null', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))
    await expect(apiGet('/api/v1/auth/me')).resolves.toBeNull()
  })

  it('converte problem+json em ApiError com erros por campo', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      json(400, { detail: 'Dados inválidos.', errors: [{ field: 'sku', message: 'SKU já existe.' }] }),
    )
    const err = await apiGet('/api/v1/admin/products').catch((e: unknown) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect(err).toMatchObject({ status: 400, message: 'Dados inválidos.', fieldErrors: [{ field: 'sku' }] })
  })

  it('mensagens amigáveis para 403 sem detalhe, 429 e corpo não-JSON', async () => {
    const spy = vi.spyOn(globalThis, 'fetch')
    spy.mockResolvedValueOnce(json(403, {}))
    await expect(apiGet('/x')).rejects.toMatchObject({ message: 'Você não tem permissão para esta ação.' })
    spy.mockResolvedValueOnce(json(429, { detail: 'qualquer' }))
    await expect(apiGet('/x')).rejects.toMatchObject({
      message: 'Muitas tentativas. Aguarde alguns minutos e tente novamente.',
    })
    spy.mockResolvedValueOnce(new Response('<html>erro</html>', { status: 502 }))
    await expect(apiGet('/x')).rejects.toMatchObject({ status: 502, message: 'Falha na requisição (502)' })
  })

  it('401 fora de /auth avisa que a sessão expirou; em /auth não', async () => {
    const listener = vi.fn()
    window.addEventListener(SESSION_EXPIRED_EVENT, listener)
    const spy = vi.spyOn(globalThis, 'fetch')
    spy.mockResolvedValueOnce(json(401, {}))
    await apiGet('/api/v1/account/orders').catch(() => undefined)
    spy.mockResolvedValueOnce(json(401, {}))
    await apiGet('/api/v1/auth/me').catch(() => undefined)
    expect(listener).toHaveBeenCalledTimes(1)
    window.removeEventListener(SESSION_EXPIRED_EVENT, listener)
  })
})

describe('apiSend', () => {
  it('busca o token CSRF, envia no cabeçalho e manda JSON', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      if (String(input).endsWith('/api/v1/auth/csrf')) {
        document.cookie = 'XSRF-TOKEN=token-ficticio; path=/'
        return json(200, { token: 'token-ficticio' })
      }
      return json(201, { id: 1 })
    })
    await expect(apiSend('POST', '/api/v1/cart/items', { productSlug: 'vaso', quantity: 1 })).resolves.toEqual({
      id: 1,
    })
    const [, init] = spy.mock.calls[1]
    const headers = init!.headers as Record<string, string>
    expect(headers['X-XSRF-TOKEN']).toBe('token-ficticio')
    expect(headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(String(init!.body))).toEqual({ productSlug: 'vaso', quantity: 1 })
  })

  it('reaproveita o cookie existente e repete uma vez com token novo após 403', async () => {
    document.cookie = 'XSRF-TOKEN=velho; path=/'
    const tokens: string[] = []
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      if (String(input).endsWith('/api/v1/auth/csrf')) {
        document.cookie = 'XSRF-TOKEN=novo; path=/'
        return json(200, { token: 'novo' })
      }
      const token = (init!.headers as Record<string, string>)['X-XSRF-TOKEN']
      tokens.push(token)
      return token === 'velho' ? json(403, {}) : new Response(null, { status: 204 })
    })
    await expect(apiSend('DELETE', '/api/v1/cart/items/1')).resolves.toBeUndefined()
    expect(tokens).toEqual(['velho', 'novo'])
  })

  it('envia FormData como multipart (sem forçar Content-Type)', async () => {
    document.cookie = 'XSRF-TOKEN=t; path=/'
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(201, { id: 9 }))
    const form = new FormData()
    form.set('altText', 'Foto fictícia')
    await apiSend('POST', '/api/v1/admin/products/1/images', form)
    const init = spy.mock.calls[0][1]!
    expect(init.body).toBe(form)
    expect((init.headers as Record<string, string>)['Content-Type']).toBeUndefined()
  })

  it('cabeçalhos extras (ex.: Idempotency-Key) são enviados', async () => {
    document.cookie = 'XSRF-TOKEN=t; path=/'
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(json(201, { number: 1001 }))
    await apiSend('POST', '/api/v1/checkout', { paymentMethod: 'PIX' }, { 'Idempotency-Key': 'chave-1' })
    expect((spy.mock.calls[0][1]!.headers as Record<string, string>)['Idempotency-Key']).toBe('chave-1')
  })
})
