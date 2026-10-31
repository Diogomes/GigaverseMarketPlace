import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import type { Cart } from '../api/types'
import { useAuth } from '../auth/AuthProvider'
import { apiGet, apiSend } from '../lib/api'
import { CART_KEY } from './cartKeys'

/**
 * Carrinho: para clientes logados fica no servidor (preços sempre recalculados lá).
 * Visitantes guardam só a seleção no navegador — sem dados pessoais — e ela é enviada ao
 * servidor no login. Preços exibidos ao visitante são referência; o servidor confirma no checkout.
 */
export type LocalCartItem = {
  productSlug: string
  optionValueIds: number[]
  quantity: number
  name: string
  unitPrice: number
  optionsLabel: string | null
}

type AddInput = Omit<LocalCartItem, 'quantity'> & { quantity: number }

type CartContextValue = {
  serverCart: Cart | undefined
  localItems: LocalCartItem[]
  count: number
  loading: boolean
  add: (item: AddInput) => Promise<void>
  updateQuantity: (itemId: number, quantity: number) => Promise<void>
  remove: (itemId: number) => Promise<void>
  updateLocal: (index: number, quantity: number) => void
  removeLocal: (index: number) => void
}

const STORAGE_KEY = 'gv3d-cart'
const CartContext = createContext<CartContextValue | null>(null)

function readLocal(): LocalCartItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? (JSON.parse(raw) as LocalCartItem[]) : []
    return Array.isArray(parsed) ? parsed.slice(0, 30) : []
  } catch {
    return []
  }
}

function writeLocal(items: LocalCartItem[]) {
  try {
    if (items.length) localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // armazenamento indisponível: o carrinho do visitante vale só nesta aba
  }
}

const sameLine = (a: { productSlug: string; optionValueIds: number[] }, b: typeof a) =>
  a.productSlug === b.productSlug && [...a.optionValueIds].sort().join() === [...b.optionValueIds].sort().join()

export function CartProvider({ children }: { children: ReactNode }) {
  const { user, loading: authLoading } = useAuth()
  const queryClient = useQueryClient()
  const [localItems, setLocalItems] = useState<LocalCartItem[]>(readLocal)
  const merging = useRef(false)

  const { data: serverCart, isPending } = useQuery({
    queryKey: CART_KEY,
    queryFn: () => apiGet<Cart>('/api/v1/cart'),
    enabled: !!user,
  })

  const setLocal = useCallback((items: LocalCartItem[]) => {
    setLocalItems(items)
    writeLocal(items)
  }, [])

  // Após o login: envia o carrinho do visitante. O navegador é limpo ANTES do envio (restaurado só se
  // falhar): assim um recarregamento no meio do caminho não envia o mesmo carrinho duas vezes.
  useEffect(() => {
    if (!user || !localItems.length || merging.current) return
    merging.current = true
    const pending = localItems
    writeLocal([])
    apiSend<Cart>('POST', '/api/v1/cart/merge', {
      items: pending.map((i) => ({
        productSlug: i.productSlug,
        optionValueIds: i.optionValueIds,
        quantity: i.quantity,
      })),
    })
      .then((cart) => {
        queryClient.setQueryData(CART_KEY, cart)
        setLocalItems([])
      })
      .catch(() => writeLocal(pending))
      .finally(() => {
        merging.current = false
      })
  }, [user, localItems, queryClient])

  const add = useCallback(
    async (item: AddInput) => {
      if (user) {
        const cart = await apiSend<Cart>('POST', '/api/v1/cart/items', {
          productSlug: item.productSlug,
          optionValueIds: item.optionValueIds,
          quantity: item.quantity,
        })
        queryClient.setQueryData(CART_KEY, cart)
        return
      }
      const existing = localItems.findIndex((i) => sameLine(i, item))
      const next = [...localItems]
      if (existing >= 0)
        next[existing] = { ...next[existing], quantity: Math.min(99, next[existing].quantity + item.quantity) }
      else next.push(item)
      setLocal(next.slice(0, 30))
    },
    [user, localItems, queryClient, setLocal],
  )

  const updateQuantity = useCallback(
    async (itemId: number, quantity: number) => {
      queryClient.setQueryData(CART_KEY, await apiSend<Cart>('PUT', `/api/v1/cart/items/${itemId}`, { quantity }))
    },
    [queryClient],
  )

  const remove = useCallback(
    async (itemId: number) => {
      queryClient.setQueryData(CART_KEY, await apiSend<Cart>('DELETE', `/api/v1/cart/items/${itemId}`))
    },
    [queryClient],
  )

  const updateLocal = useCallback(
    (index: number, quantity: number) =>
      setLocal(
        localItems.map((i, idx) => (idx === index ? { ...i, quantity: Math.max(1, Math.min(99, quantity)) } : i)),
      ),
    [localItems, setLocal],
  )

  const removeLocal = useCallback(
    (index: number) => setLocal(localItems.filter((_, idx) => idx !== index)),
    [localItems, setLocal],
  )

  const count = user ? (serverCart?.itemCount ?? 0) : localItems.reduce((s, i) => s + i.quantity, 0)
  const value = useMemo<CartContextValue>(
    () => ({
      serverCart,
      localItems,
      count,
      loading: authLoading || (!!user && isPending),
      add,
      updateQuantity,
      remove,
      updateLocal,
      removeLocal,
    }),
    [
      serverCart,
      localItems,
      count,
      authLoading,
      user,
      isPending,
      add,
      updateQuantity,
      remove,
      updateLocal,
      removeLocal,
    ],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

// oxlint-disable-next-line react/only-export-components -- hook acoplado ao provider
export function useCart(): CartContextValue {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart deve ser usado dentro de CartProvider')
  return ctx
}
