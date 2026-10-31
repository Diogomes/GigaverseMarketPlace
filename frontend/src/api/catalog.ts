import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiGet } from '../lib/api'
import type { ApiCategory, Page, ProductDetail, ProductSummary, ProductType } from './types'

export type ProductSort = 'relevance' | 'price_asc' | 'price_desc' | 'name' | 'newest'

export type ProductFilters = {
  q?: string
  category?: string
  type?: ProductType
  featured?: boolean
  sort?: ProductSort
  page?: number
  size?: number
}

function query(filters: ProductFilters): string {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([k, v]) => {
    if (v !== undefined && v !== '' && v !== null) params.set(k, String(v))
  })
  const s = params.toString()
  return s ? `?${s}` : ''
}

export function useCategories() {
  return useQuery({
    queryKey: ['catalog', 'categories'],
    queryFn: () => apiGet<ApiCategory[]>('/api/v1/public/categories'),
    staleTime: 5 * 60_000,
  })
}

export function useProducts(filters: ProductFilters) {
  return useQuery({
    queryKey: ['catalog', 'products', filters],
    queryFn: () => apiGet<Page<ProductSummary>>(`/api/v1/public/products${query(filters)}`),
    placeholderData: keepPreviousData,
  })
}

export function useProduct(slug: string) {
  return useQuery({
    queryKey: ['catalog', 'product', slug],
    queryFn: () => apiGet<ProductDetail>(`/api/v1/public/products/${encodeURIComponent(slug)}`),
    retry: false,
  })
}
