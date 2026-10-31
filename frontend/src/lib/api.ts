const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export type FieldError = { field: string; message: string }

/** Erro da API no formato RFC 9457 (problem+json). */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: FieldError[]

  constructor(status: number, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

/** Disparado quando uma chamada autenticada recebe 401 (sessão expirou ou foi encerrada). */
export const SESSION_EXPIRED_EVENT = 'gv3d:session-expired'

function notifyIfSessionLost(path: string, status: number) {
  const authProbe = path.startsWith('/api/v1/auth/')
  if (status === 401 && !authProbe) window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
}

async function toError(response: Response): Promise<ApiError> {
  let detail = `Falha na requisição (${response.status})`
  let errors: FieldError[] = []
  try {
    const body = (await response.json()) as { detail?: string; errors?: FieldError[] }
    detail = body.detail ?? detail
    errors = body.errors ?? []
  } catch {
    // corpo vazio ou não-JSON
  }
  if (response.status === 403 && !errors.length) detail = 'Você não tem permissão para esta ação.'
  if (response.status === 429) detail = 'Muitas tentativas. Aguarde alguns minutos e tente novamente.'
  return new ApiError(response.status, detail, errors)
}

/** GET JSON na API. Envia cookies de sessão; nunca usa tokens em localStorage. */
export async function apiGet<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    method: 'GET',
    credentials: 'include',
    headers: { Accept: 'application/json', ...init?.headers },
  })
  if (!response.ok) {
    notifyIfSessionLost(path, response.status)
    throw await toError(response)
  }
  if (response.status === 204) return null as T
  return (await response.json()) as T
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(`${name}=`))
    ?.slice(name.length + 1)
}

/** Garante o cookie XSRF-TOKEN (o backend o emite em GET /api/v1/auth/csrf). */
async function csrfToken(): Promise<string> {
  const existing = readCookie('XSRF-TOKEN')
  if (existing) return decodeURIComponent(existing)
  const { token } = await apiGet<{ token: string }>('/api/v1/auth/csrf')
  return decodeURIComponent(readCookie('XSRF-TOKEN') ?? token)
}

/** Baixa um arquivo (ex.: CSV) com a sessão atual, usando o nome indicado pelo servidor. */
export async function apiDownload(path: string, fallbackName: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}${path}`, { credentials: 'include' })
  if (!response.ok) {
    notifyIfSessionLost(path, response.status)
    throw await toError(response)
  }
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const name = /filename="?([^";]+)"?/.exec(disposition)?.[1] ?? fallbackName
  const url = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = url
  link.download = name
  link.click()
  URL.revokeObjectURL(url)
}

/** POST/PUT/DELETE com proteção CSRF (header X-XSRF-TOKEN) e cookie de sessão. FormData vai como multipart. */
export async function apiSend<T>(
  method: 'POST' | 'PUT' | 'DELETE',
  path: string,
  body?: unknown,
  extraHeaders: Record<string, string> = {},
): Promise<T> {
  const multipart = body instanceof FormData
  const send = async (token: string) =>
    fetch(`${API_BASE_URL}${path}`, {
      method,
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'X-XSRF-TOKEN': token,
        ...extraHeaders,
        ...(body === undefined || multipart ? {} : { 'Content-Type': 'application/json' }),
      },
      body: body === undefined ? undefined : multipart ? body : JSON.stringify(body),
    })
  let response = await send(await csrfToken())
  if (response.status === 403) {
    // token pode ter sido renovado (ex.: após login/logout): busca outro e tenta uma vez
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/'
    response = await send(await csrfToken())
  }
  if (!response.ok) {
    notifyIfSessionLost(path, response.status)
    throw await toError(response)
  }
  if (response.status === 204) return undefined as T
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}
