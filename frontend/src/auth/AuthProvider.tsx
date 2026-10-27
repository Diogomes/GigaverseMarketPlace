import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useMemo, type ReactNode } from 'react'
import { apiGet, apiSend, ApiError } from '../lib/api'

export type Role = 'CUSTOMER' | 'OPERATOR' | 'ADMIN'
export type SessionUser = { id: number; email: string; fullName: string; role: Role }

export type RegisterInput = {
  fullName: string
  email: string
  password: string
  acceptTerms: boolean
  marketingOptIn: boolean
}

type AuthContextValue = {
  user: SessionUser | null
  loading: boolean
  isStaff: boolean
  login: (email: string, password: string) => Promise<SessionUser>
  register: (input: RegisterInput) => Promise<SessionUser>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)
const ME_KEY = ['auth', 'me']

/** Sessão vem do cookie HttpOnly no servidor; o frontend só consulta /auth/me. */
async function fetchMe(): Promise<SessionUser | null> {
  try {
    return await apiGet<SessionUser>('/api/v1/auth/me')
  } catch (e) {
    if (e instanceof ApiError && (e.status === 401 || e.status === 403)) return null
    throw e
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const { data, isPending } = useQuery({ queryKey: ME_KEY, queryFn: fetchMe, retry: false, staleTime: 60_000 })

  const setUser = useCallback((user: SessionUser | null) => queryClient.setQueryData(ME_KEY, user), [queryClient])

  const login = useCallback(
    async (email: string, password: string) => {
      const user = await apiSend<SessionUser>('POST', '/api/v1/auth/login', { email, password })
      setUser(user)
      return user
    },
    [setUser],
  )

  const register = useCallback(
    async (input: RegisterInput) => {
      const user = await apiSend<SessionUser>('POST', '/api/v1/auth/register', input)
      setUser(user)
      return user
    },
    [setUser],
  )

  const logout = useCallback(async () => {
    try {
      await apiSend('POST', '/api/v1/auth/logout')
    } finally {
      setUser(null)
      queryClient.removeQueries({ predicate: (q) => q.queryKey[0] !== 'auth' })
    }
  }, [queryClient, setUser])

  const user = data ?? null
  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading: isPending,
      isStaff: user?.role === 'ADMIN' || user?.role === 'OPERATOR',
      login,
      register,
      logout,
    }),
    [user, isPending, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// oxlint-disable-next-line react/only-export-components -- hook acoplado ao provider
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return ctx
}
