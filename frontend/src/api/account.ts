import { useQuery, useQueryClient } from '@tanstack/react-query'
import type { AddressInput } from '../components/account/AddressForm'
import { apiGet, apiSend } from '../lib/api'
import type { Address, OrderDetail, OrderSummary, Page } from './types'

export const ADDRESSES_KEY = ['account', 'addresses']

export function useAddresses(enabled = true) {
  return useQuery({
    queryKey: ADDRESSES_KEY,
    queryFn: () => apiGet<Address[]>('/api/v1/account/addresses'),
    enabled,
  })
}

/** Operações de endereço; atualizam a lista em cache. */
export function useAddressActions() {
  const queryClient = useQueryClient()
  const refresh = () => queryClient.invalidateQueries({ queryKey: ADDRESSES_KEY })
  return {
    create: async (data: AddressInput) => {
      const created = await apiSend<Address>('POST', '/api/v1/account/addresses', data)
      await refresh()
      return created
    },
    update: async (id: number, data: AddressInput) => {
      await apiSend<Address>('PUT', `/api/v1/account/addresses/${id}`, data)
      await refresh()
    },
    makeDefault: async (id: number) => {
      await apiSend<Address>('POST', `/api/v1/account/addresses/${id}/default`)
      await refresh()
    },
    remove: async (id: number) => {
      await apiSend('DELETE', `/api/v1/account/addresses/${id}`)
      await refresh()
    },
  }
}

export function useMyOrders(page: number, size = 10, group?: 'ACTIVE' | 'DELIVERED' | 'CANCELLED') {
  return useQuery({
    queryKey: ['account', 'orders', page, size, group],
    queryFn: () =>
      apiGet<Page<OrderSummary>>(`/api/v1/account/orders?page=${page}&size=${size}${group ? `&group=${group}` : ''}`),
  })
}

export function useMyOrder(number: string) {
  return useQuery({
    queryKey: ['account', 'order', number],
    queryFn: () => apiGet<OrderDetail>(`/api/v1/account/orders/${encodeURIComponent(number)}`),
    retry: false,
  })
}
